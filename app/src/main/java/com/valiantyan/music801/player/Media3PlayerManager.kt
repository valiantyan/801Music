package com.valiantyan.music801.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.valiantyan.music801.domain.model.PlaybackState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Media3 播放器管理实现
 *
 * 封装 [ExoPlayer] 初始化与状态监听，向上提供统一播放控制与状态流。
 *
 * @param context 用于创建 [ExoPlayer] 的应用上下文
 */
class Media3PlayerManager(
    context: Context,
) : MediaPlayerManager {
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

    init {
        // 统一监听入口，避免多处订阅造成状态不一致
        exoPlayer.addListener(playerListener)
        updatePlaybackState(error = null)
    }

    /**
     * 播放指定音频资源
     *
     * @param uri 音频资源 [Uri]
     */
    override fun play(uri: Uri): Unit {
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
    }

    /**
     * 跳转到指定位置
     *
     * @param position 目标位置（毫秒）
     */
    override fun seekTo(position: Long): Unit {
        exoPlayer.seekTo(position)
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
     * 构建 [Player.Listener] 以集中处理播放器状态变化
     */
    private fun buildPlayerListener(): Player.Listener {
        val listener: Player.Listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean): Unit {
                updatePlaybackState(error = null)
            }

            override fun onPlaybackStateChanged(playbackState: Int): Unit {
                updatePlaybackState(error = null)
            }

            override fun onPlayerError(error: PlaybackException): Unit {
                updatePlaybackState(error = error)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int): Unit {
                updatePlaybackState(error = null)
            }
        }
        return listener
    }

    /**
     * 统一更新 [PlaybackState]，避免多处写入导致状态不一致
     */
    private fun updatePlaybackState(error: PlaybackException?): Unit {
        val state: PlaybackState = buildPlaybackState(
            player = exoPlayer,
            error = error,
        )
        playbackStateFlow.value = state
    }

    /**
     * 将 [Player] 的状态转换为领域模型 [PlaybackState]
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
}
