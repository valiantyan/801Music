package com.valiantyan.music801.service

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.util.Log
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.core.app.ServiceCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.valiantyan.music801.player.PlayerCommands
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * 媒体会话服务
 *
 * 在 Service 内创建 [ExoPlayer] 与 [MediaSession]，提供系统级播放控制入口。
 */
class MusicPlayerService : MediaSessionService() {
    private companion object {
        /**
         * 日志标签
         */
        private const val TAG: String = "MediaNotification"
    }
    /**
     * Service 生命周期协程作用域，用于管理异步任务
     */
    private val serviceScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    /**
     * 当前活跃的 [MediaSession] 实例
     */
    private var mediaSession: MediaSession? = null
    /**
     * 当前播放引擎实例
     */
    private var player: ExoPlayer? = null
    /**
     * 通知管理器，负责前台服务通知与渠道管理
     */
    private var notificationManager: PlayerNotificationManager? = null
    /**
     * 用于在 [toggleFavorite] 中维护收藏状态，并在 [onCustomCommand] 回执中复用
     */
    private val favoriteMediaIds: MutableSet<String> = mutableSetOf()
    /**
     * 缓存最近一次播放错误码，用于避免重复上报
     */
    private var lastPlaybackErrorCode: Int? = null
    /**
     * 缓存最近一次播放错误信息，用于避免重复上报
     */
    private var lastPlaybackErrorMessage: String? = null
    /**
     * 测试辅助标记：是否已执行 [onCreate]
     */
    internal var isCreated: Boolean = false
    /**
     * 测试辅助标记：是否已执行 [onDestroy]
     */
    internal var isDestroyed: Boolean = false
    /**
     * 测试辅助标记：是否已创建 [MediaSession]
     */
    internal var isSessionCreated: Boolean = false
    /**
     * 测试辅助标记：是否已初始化通知管理器
     */
    internal var isNotificationInitialized: Boolean = false
    /**
     * 播放器监听器，用于统一处理错误与日志
     */
    private val playerListener: Player.Listener = buildPlayerListener()

    /**
     * 初始化 [ExoPlayer] 与 [MediaSession]，并建立通知渠道
     */
    override fun onCreate(): Unit {
        super.onCreate()
        isCreated = true
        Log.d(TAG, "service onCreate")
        val exoPlayer: ExoPlayer = buildPlayer()
        player = exoPlayer
        exoPlayer.addListener(playerListener)
        val sessionPlayer: Player = MediaSessionPlayer(player = exoPlayer)
        val sessionCallback: PlaybackSessionCallback = PlaybackSessionCallback(player = exoPlayer)
        val sessionId: String = buildSessionId()
        val sessionBuilder: MediaSession.Builder = MediaSession.Builder(this, sessionPlayer)
            .setId(sessionId)
            .setPeriodicPositionUpdateEnabled(true)
            .setCallback(sessionCallback)
            .setCustomLayout(buildCustomLayout())
        val sessionActivity: PendingIntent? = buildSessionActivity()
        if (sessionActivity != null) {
            sessionBuilder.setSessionActivity(sessionActivity)
        }
        val createdSession: MediaSession = sessionBuilder.build()
        mediaSession = createdSession
        isSessionCreated = true
        val serviceController: ForegroundServiceController = MediaSessionForegroundController(service = this)
        notificationManager = PlayerNotificationManager(
            context = applicationContext,
            serviceController = serviceController,
        ).also { manager -> manager.createNotificationChannel() }
        isNotificationInitialized = true
        Log.d(TAG, "session created: id=$sessionId")
        notificationManager?.attachToSession(mediaSession = createdSession)
    }

    /**
     * 返回当前 [MediaSession]，供控制端建立连接
     */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    /**
     * 释放播放器与会话资源，避免泄漏
     */
    override fun onDestroy(): Unit {
        isDestroyed = true
        player?.removeListener(playerListener)
        player?.release()
        player = null
        mediaSession?.release()
        mediaSession = null
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * 构建默认 [ExoPlayer] 实例并设置音频属性
     */
    private fun buildPlayer(): ExoPlayer {
        val attributes: AudioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        return ExoPlayer.Builder(applicationContext)
            .setAudioAttributes(attributes, true)
            .build()
    }

    /**
     * 生成会话 ID，保证多进程连接可区分
     */
    private fun buildSessionId(): String {
        return UUID.randomUUID().toString()
    }

    /**
     * 构建通知栏自定义按钮布局
     */
    private fun buildCustomLayout(): List<CommandButton> {
        val favoriteCommand: SessionCommand = buildFavoriteCommand()
        val favoriteButton: CommandButton = CommandButton.Builder()
            .setSessionCommand(favoriteCommand)
            .setDisplayName(FAVORITE_BUTTON_LABEL)
            .setIconResId(com.valiantyan.music801.player.impl.R.drawable.ic_favorite_notification)
            .build()
        return listOf(favoriteButton)
    }

    /**
     * 构建收藏切换命令，供通知栏按钮使用
     */
    private fun buildFavoriteCommand(): SessionCommand {
        return SessionCommand(PlayerCommands.ACTION_TOGGLE_FAVORITE, Bundle())
    }

    /**
     * 构建用于跳转应用的 [PendingIntent]
     */
    private fun buildSessionActivity(): PendingIntent? {
        val intent: Intent? = packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            Log.w(TAG, "launch intent not found")
            return null
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * 在 [onCustomCommand] 中切换收藏状态，保证回执返回最新结果
     *
     * @param mediaId 媒体 ID
     */
    private fun toggleFavorite(mediaId: String): Boolean {
        val isFavorite: Boolean = favoriteMediaIds.contains(mediaId)
        return if (isFavorite) {
            favoriteMediaIds.remove(mediaId)
            false
        } else {
            favoriteMediaIds.add(mediaId)
            true
        }
    }

    /**
     * 统一 [onCustomCommand] 成功回执结构，便于客户端解析
     *
     * @param mediaId 媒体 ID
     * @param isFavorite 收藏状态
     */
    private fun buildFavoriteExtras(
        mediaId: String,
        isFavorite: Boolean,
    ): Bundle {
        val extras: Bundle = Bundle()
        extras.putString(EXTRA_MEDIA_ID, mediaId)
        extras.putBoolean(EXTRA_IS_FAVORITE, isFavorite)
        return extras
    }

    /**
     * 统一 [onCustomCommand] 失败回执结构，便于客户端识别原因
     *
     * @param reason 失败原因
     */
    private fun buildFavoriteErrorExtras(reason: String): Bundle {
        val extras: Bundle = Bundle()
        extras.putString(EXTRA_ERROR_MESSAGE, reason)
        return extras
    }

    /**
     * 构建播放器监听器并同步错误状态
     */
    private fun buildPlayerListener(): Player.Listener {
        return object : Player.Listener {
            override fun onPlayerError(error: PlaybackException): Unit {
                Log.e(TAG, "player error: ${PlaybackException.getErrorCodeName(error.errorCode)}", error)
                updatePlaybackError(error = error)
            }

            override fun onEvents(player: Player, events: Player.Events): Unit {
                if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                    events.contains(Player.EVENT_IS_PLAYING_CHANGED)
                ) {
                    Log.d(TAG, "player events: isPlaying=${player.isPlaying}")
                }
            }
        }
    }

    /**
     * 向 [MediaSession] 上报播放错误
     */
    private fun updatePlaybackError(error: PlaybackException): Unit {
        val session: MediaSession? = mediaSession
        if (session == null) {
            return
        }
        if (!shouldUpdatePlaybackError(error = error)) {
            return
        }
        session.setPlaybackException(error)
    }

    /**
     * 判断是否需要更新错误状态，避免重复写入
     */
    private fun shouldUpdatePlaybackError(error: PlaybackException?): Boolean {
        val newCode: Int? = error?.errorCode
        val newMessage: String? = error?.message
        val shouldUpdate: Boolean =
            newCode != lastPlaybackErrorCode || newMessage != lastPlaybackErrorMessage
        if (!shouldUpdate) {
            return false
        }
        lastPlaybackErrorCode = newCode
        lastPlaybackErrorMessage = newMessage
        return true
    }

    /**
     * 媒体会话回调，用于将系统命令转发到 [Player] 并处理收藏逻辑
     */
    internal inner class PlaybackSessionCallback(
        private val player: Player,
    ) : MediaSession.Callback {
        @Suppress("OVERRIDE_DEPRECATION")
        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommand: Int,
        ): Int {
            when (playerCommand) {
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> player.seekToNextMediaItem()
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> player.seekToPreviousMediaItem()
                Player.COMMAND_STOP -> {
                    player.pause()
                    player.seekTo(0L)
                }
            }
            return SessionResult.RESULT_SUCCESS
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            val result: SessionResult = when (customCommand.customAction) {
                PlayerCommands.ACTION_TOGGLE_FAVORITE -> {
                    Log.d(CALLBACK_TAG, "custom command: toggle favorite")
                    val mediaId: String? = player.currentMediaItem?.mediaId
                    if (mediaId.isNullOrBlank()) {
                        SessionResult(
                            SessionResult.RESULT_ERROR_BAD_VALUE,
                            buildFavoriteErrorExtras(reason = "media-id-missing"),
                        )
                    } else {
                        val isFavorite: Boolean = toggleFavorite(mediaId = mediaId)
                        SessionResult(
                            SessionResult.RESULT_SUCCESS,
                            buildFavoriteExtras(
                                mediaId = mediaId,
                                isFavorite = isFavorite,
                            ),
                        )
                    }
                }
                else -> SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED)
            }
            return Futures.immediateFuture(result)
        }
    }

    @VisibleForTesting
    internal fun getMediaSessionForTesting(): MediaSession? {
        return mediaSession
    }

    @VisibleForTesting
    internal fun getNotificationManagerForTesting(): PlayerNotificationManager? {
        return notificationManager
    }
}

private class MediaSessionPlayer(
    player: Player,
) : ForwardingPlayer(player) {
    /**
     * 扩展默认可用命令，补齐上一首/下一首支持
     */
    override fun getAvailableCommands(): Player.Commands {
        val baseCommands: Player.Commands = super.getAvailableCommands()
        val builder: Player.Commands.Builder = Player.Commands.Builder()
        builder.addAll(baseCommands)
        builder.add(Player.COMMAND_SEEK_TO_NEXT)
        builder.add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        builder.add(Player.COMMAND_SEEK_TO_PREVIOUS)
        builder.add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
        return builder.build()
    }

    /**
     * 声明扩展命令可用性，兼容外部控制器
     */
    override fun isCommandAvailable(command: Int): Boolean {
        return when (command) {
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> true
            else -> super.isCommandAvailable(command)
        }
    }
}

private class MediaSessionForegroundController(
    private val service: MediaSessionService,
) : ForegroundServiceController {
    /**
     * 通过 [ServiceCompat] 启动前台服务
     */
    override fun startForeground(
        notificationId: Int,
        notification: android.app.Notification,
    ): Unit {
        ServiceCompat.startForeground(
            service,
            notificationId,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
    }

    override fun stopForeground(): Unit {
        ServiceCompat.stopForeground(
            service,
            ServiceCompat.STOP_FOREGROUND_REMOVE,
        )
    }

    /**
     * 请求停止服务
     */
    override fun stopSelf(): Unit {
        service.stopSelf()
    }
}

/**
 * 收藏按钮文案
 */
private const val FAVORITE_BUTTON_LABEL: String = "收藏"
/**
 * 回调日志标签
 */
private const val CALLBACK_TAG: String = "MediaNotificationCallback"

/**
 * 收藏命令结果的媒体 ID 键
 */
private const val EXTRA_MEDIA_ID: String = PlayerCommands.EXTRA_MEDIA_ID

/**
 * 收藏命令结果的收藏状态键
 */
private const val EXTRA_IS_FAVORITE: String = PlayerCommands.EXTRA_IS_FAVORITE

/**
 * 收藏命令结果的错误信息键
 */
private const val EXTRA_ERROR_MESSAGE: String = PlayerCommands.EXTRA_ERROR_MESSAGE
