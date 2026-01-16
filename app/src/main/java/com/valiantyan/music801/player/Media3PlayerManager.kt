package com.valiantyan.music801.player

import android.content.Context
import android.net.Uri
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioAttributes as PlatformAudioAttributes
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.valiantyan.music801.domain.model.PlaybackState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Media3 播放器管理实现
 *
 * 封装 [ExoPlayer] 初始化与状态监听，向上提供统一播放控制与状态流。
 *
 * @param context 用于创建 [ExoPlayer] 的应用上下文
 * @param progressUpdateIntervalMs 播放进度更新间隔（毫秒）
 * @param dispatcher 播放进度更新使用的协程调度器
 */
class Media3PlayerManager(
    context: Context,
    progressUpdateIntervalMs: Long = DEFAULT_PROGRESS_UPDATE_INTERVAL_MS,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : MediaPlayerManager {
    /**
     * 播放进度更新间隔（毫秒）
     */
    private val progressUpdateIntervalMs: Long = progressUpdateIntervalMs
    /**
     * 系统音频管理器，用于请求与释放音频焦点
     */
    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    /**
     * 记录焦点变更前音量，以便恢复用户音量
     */
    private var lastVolume: Float = 1.0f
    /**
     * 标记焦点恢复后是否需要自动恢复播放
     */
    private var resumeOnFocusGain: Boolean = false
    /**
     * 持有更新任务的协程作用域以便统一取消
     */
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
    /**
     * 控制进度更新的任务，避免重复启动
     */
    private var progressJob: Job? = null
    /**
     * 复用单一 [ExoPlayer] 实例以保证状态一致性
     */
    internal val exoPlayer: ExoPlayer = buildExoPlayer(
        context = context,
        audioAttributes = buildAudioAttributes(),
    )
    /**
     * 复用单一监听器避免重复注册导致状态更新混乱
     */
    private val playerListener: Player.Listener = buildPlayerListener()
    /**
     * 统一状态出口，便于上层订阅并保持状态一致
     */
    private val playbackStateFlow: MutableStateFlow<PlaybackState> =
        MutableStateFlow(PlaybackState())
    /**
     * 记录最近一次播放错误，避免被进度刷新覆盖
     */
    private var lastError: PlaybackException? = null
    /**
     * AudioFocus 监听器，集中处理焦点变化
     */
    private val audioFocusChangeListener: AudioManager.OnAudioFocusChangeListener =
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            handleAudioFocusChange(focusChange = focusChange)
        }
    /**
     * AudioFocus 请求对象，确保与 [AudioAttributes] 保持一致
     */
    private val audioFocusRequest: AudioFocusRequest =
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(buildPlatformAudioAttributes())
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()

    init {
        // 统一监听入口，避免多处订阅造成状态不一致
        exoPlayer.addListener(playerListener)
        startProgressUpdates()
        updatePlaybackState(error = null)
    }

    /**
     * 播放指定音频资源
     *
     * @param uri 音频资源 [Uri]
     */
    override fun play(uri: Uri): Unit {
        if (!requestAudioFocus()) {
            handlePlaybackError(
                error = PlaybackException(
                    "audio-focus-denied",
                    null,
                    PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK,
                ),
            )
            return
        }
        clearPlaybackError()
        val mediaItem: MediaItem = MediaItem.fromUri(uri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    /**
     * 暂停播放
     */
    override fun pause(): Unit {
        exoPlayer.pause()
        abandonAudioFocus()
    }

    /**
     * 恢复播放
     */
    override fun resume(): Unit {
        exoPlayer.play()
    }

    /**
     * 停止播放
     */
    override fun stop(): Unit {
        exoPlayer.stop()
        abandonAudioFocus()
    }

    /**
     * 跳转到指定位置
     *
     * @param position 目标位置（毫秒）
     */
    override fun seekTo(position: Long): Unit {
        exoPlayer.seekTo(position)
        updatePlaybackState(error = null)
    }

    /**
     * 获取播放状态流
     *
     * @return 播放状态 [Flow]
     */
    override fun playbackState(): Flow<PlaybackState> = playbackStateFlow

    /**
     * 释放播放器资源
     */
    override fun release(): Unit {
        // 先移除监听，避免释放后回调触发导致崩溃
        exoPlayer.removeListener(playerListener)
        stopProgressUpdates()
        coroutineScope.cancel()
        abandonAudioFocus()
        exoPlayer.release()
    }

    /**
     * 统一构建 [ExoPlayer]，确保音频属性在初始化阶段完成配置
     */
    private fun buildExoPlayer(
        context: Context,
        audioAttributes: AudioAttributes,
    ): ExoPlayer {
        val player: ExoPlayer = ExoPlayer.Builder(context)
            // Media3 Java API 不支持命名参数，使用位置参数
            .setAudioAttributes(audioAttributes, true)
            .build()
        return player
    }

    /**
     * 构建播放用 [AudioAttributes]，确保系统正确识别媒体用途
     */
    private fun buildAudioAttributes(): AudioAttributes {
        val attributes: AudioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
        return attributes
    }

    /**
     * 构建平台 [PlatformAudioAttributes] 以供音频焦点请求使用
     */
    private fun buildPlatformAudioAttributes(): PlatformAudioAttributes {
        val attributes: PlatformAudioAttributes = PlatformAudioAttributes.Builder()
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .build()
        return attributes
    }

    /**
     * 构建 [Player.Listener] 以集中处理播放器状态变化
     */
    private fun buildPlayerListener(): Player.Listener {
        val listener: Player.Listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean): Unit {
                if (isPlaying) {
                    clearPlaybackError()
                }
                updatePlaybackState(error = null)
            }

            override fun onPlaybackStateChanged(playbackState: Int): Unit {
                updatePlaybackState(error = null)
            }

            override fun onPlayerError(error: PlaybackException): Unit {
                handlePlaybackError(error = error)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int): Unit {
                updatePlaybackState(error = null)
            }
        }
        return listener
    }

    /**
     * 启动进度更新任务，确保播放或跳转后状态能及时同步
     */
    private fun startProgressUpdates(): Unit {
        if (progressJob != null) {
            return
        }
        progressJob = coroutineScope.launch {
            while (isActive) {
                updatePlaybackState(error = null)
                delay(progressUpdateIntervalMs)
            }
        }
    }

    /**
     * 停止进度更新任务，避免资源泄漏
     */
    private fun stopProgressUpdates(): Unit {
        progressJob?.cancel()
        progressJob = null
    }

    /**
     * 清理错误状态，避免成功播放后仍展示旧错误
     */
    private fun clearPlaybackError(): Unit {
        lastError = null
    }

    /**
     * 统一更新 [PlaybackState]，避免多处写入导致状态不一致
     */
    private fun updatePlaybackState(error: PlaybackException?): Unit {
        if (error != null) {
            lastError = error
        }
        val state: PlaybackState = buildPlaybackState(
            player = exoPlayer,
            error = resolvePlaybackError(error = error),
        )
        playbackStateFlow.value = state
    }

    /**
     * 将 [Player] 的状态转换为领域模型 [PlaybackState]
     *
     * @param player 提供原始状态数据的播放器
     * @param error 播放错误信息
     * @return 映射后的播放状态
     */
    internal fun buildPlaybackState(
        player: Player,
        error: PlaybackException?,
    ): PlaybackState {
        return PlaybackState(
            isPlaying = player.isPlaying,
            position = player.currentPosition,
            duration = player.duration,
            bufferedPosition = player.bufferedPosition,
            playbackState = player.playbackState,
            error = error,
        )
    }

    /**
     * 处理播放错误并同步状态，确保错误信息可被上层观察
     *
     * @param error 播放异常
     */
    internal fun handlePlaybackError(error: PlaybackException): Unit {
        exoPlayer.stop()
        abandonAudioFocus()
        updatePlaybackState(error = error)
    }

    /**
     * 处理音频焦点变化，确保播放行为与系统规则一致
     *
     * @param focusChange 音频焦点变化类型
     */
    internal fun handleAudioFocusChange(focusChange: Int): Unit {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                restoreVolume()
                if (resumeOnFocusGain) {
                    resumeOnFocusGain = false
                    exoPlayer.play()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeOnFocusGain = false
                exoPlayer.pause()
                abandonAudioFocus()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                resumeOnFocusGain = exoPlayer.isPlaying
                exoPlayer.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                resumeOnFocusGain = exoPlayer.isPlaying
                duckVolume()
            }
        }
    }

    /**
     * 请求音频焦点，确保播放前得到系统许可
     *
     * @return 是否成功获取音频焦点
     */
    private fun requestAudioFocus(): Boolean {
        val result: Int = audioManager.requestAudioFocus(audioFocusRequest)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    /**
     * 释放音频焦点，避免占用系统资源
     */
    private fun abandonAudioFocus(): Unit {
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
    }

    /**
     * 降低音量以响应 [AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK]
     */
    private fun duckVolume(): Unit {
        lastVolume = exoPlayer.volume
        exoPlayer.volume = DUCK_VOLUME
    }

    /**
     * 恢复音量，避免长期保持低音量
     */
    private fun restoreVolume(): Unit {
        exoPlayer.volume = lastVolume
    }

    /**
     * 优先返回最新错误，避免刷新逻辑清空错误状态
     *
     * @param error 当前回调携带的错误
     * @return 最终用于状态的错误
     */
    private fun resolvePlaybackError(error: PlaybackException?): PlaybackException? {
        if (error != null) {
            return error
        }
        return lastError
    }

    private companion object {
        /**
         * 默认进度更新间隔（500ms）
         */
        private const val DEFAULT_PROGRESS_UPDATE_INTERVAL_MS: Long = 500L
        /**
         * 音量降低比例（20%）
         */
        private const val DUCK_VOLUME: Float = 0.2f
    }
}
