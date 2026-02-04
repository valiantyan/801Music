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
        private const val TAG: String = "MediaNotification"
    }
    private val serviceScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private var notificationManager: PlayerNotificationManager? = null
    private var lastPlaybackErrorCode: Int? = null
    private var lastPlaybackErrorMessage: String? = null
    internal var isCreated: Boolean = false
    internal var isDestroyed: Boolean = false
    internal var isSessionCreated: Boolean = false
    internal var isNotificationInitialized: Boolean = false
    private val playerListener: Player.Listener = buildPlayerListener()

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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

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

    private fun buildPlayer(): ExoPlayer {
        val attributes: AudioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        return ExoPlayer.Builder(applicationContext)
            .setAudioAttributes(attributes, true)
            .build()
    }

    private fun buildSessionId(): String {
        return UUID.randomUUID().toString()
    }

    private fun buildCustomLayout(): List<CommandButton> {
        val favoriteCommand: SessionCommand = buildFavoriteCommand()
        val favoriteButton: CommandButton = CommandButton.Builder()
            .setSessionCommand(favoriteCommand)
            .setDisplayName(FAVORITE_BUTTON_LABEL)
            .setIconResId(com.valiantyan.music801.player.impl.R.drawable.ic_favorite_notification)
            .build()
        return listOf(favoriteButton)
    }

    private fun buildFavoriteCommand(): SessionCommand {
        return SessionCommand(PlayerCommands.ACTION_TOGGLE_FAVORITE, Bundle())
    }

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

internal class PlaybackSessionCallback(
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
                SessionResult(SessionResult.RESULT_SUCCESS)
            }
            else -> SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED)
        }
        return Futures.immediateFuture(result)
    }
}

private class MediaSessionForegroundController(
    private val service: MediaSessionService,
) : ForegroundServiceController {
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

    override fun stopSelf(): Unit {
        service.stopSelf()
    }
}

private const val FAVORITE_BUTTON_LABEL: String = "收藏"
private const val CALLBACK_TAG: String = "MediaNotificationCallback"
