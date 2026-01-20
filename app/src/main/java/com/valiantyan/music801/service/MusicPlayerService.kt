package com.valiantyan.music801.service

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionResult
import com.valiantyan.music801.domain.model.PlaybackState
import com.valiantyan.music801.data.repository.PlayerRepository
import com.valiantyan.music801.data.repository.PlayerRepositoryImpl
import com.valiantyan.music801.di.PlayerRepositoryHolder
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 媒体会话服务基础框架
 *
 * 负责在系统层暴露媒体会话入口，具体会话配置在后续任务中完成。
 */
class MusicPlayerService : MediaSessionService() {
    private companion object {
        private const val TAG: String = "MediaNotification"
    }
    private var mediaSession: MediaSession? = null
    private val serviceScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var playbackStateJob: Job? = null
    private var notificationManager: PlayerNotificationManager? = null
    private var lastIsPlaying: Boolean? = null
    private var lastSongId: String? = null
    internal var isCreated: Boolean = false
    internal var isDestroyed: Boolean = false
    internal var isSessionCreated: Boolean = false
    internal var isStateSyncStarted: Boolean = false
    internal var isNotificationInitialized: Boolean = false

    override fun onCreate(): Unit {
        super.onCreate()
        isCreated = true
        android.util.Log.d(TAG, "service onCreate")
        val repository: PlayerRepository = PlayerRepositoryHolder.getOrCreate(
            context = applicationContext,
        )
        val basePlayer: Player? = resolveSessionPlayer(
            repository = repository,
        )
        if (basePlayer == null) {
            return
        }
        val sessionPlayer: Player = MediaSessionPlayer(
            player = basePlayer,
        )
        val sessionCallback: PlaybackSessionCallback = PlaybackSessionCallback(
            playerRepository = repository,
        )
        val sessionId: String = buildSessionId()
        val createdSession: MediaSession = MediaSession.Builder(this, sessionPlayer)
            .setId(sessionId)
            .setPeriodicPositionUpdateEnabled(true)
            .setCallback(sessionCallback)
            .build()
        mediaSession = createdSession
        isSessionCreated = true
        notificationManager = PlayerNotificationManager(context = applicationContext)
            .also { manager -> manager.createNotificationChannel() }
        isNotificationInitialized = true
        android.util.Log.d(TAG, "session created: id=$sessionId")
        val manager: PlayerNotificationManager? = notificationManager
        if (manager != null) {
            manager.attachToSession(mediaSession = createdSession)
            val notification = manager.buildNotification(
                song = null,
                isPlaying = false,
            )
            startForeground(
                manager.getNotificationId(),
                notification,
            )
        }
        startPlaybackStateSync(
            session = createdSession,
            repository = repository,
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy(): Unit {
        isDestroyed = true
        playbackStateJob?.cancel()
        playbackStateJob = null
        serviceScope.cancel()
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private fun resolveSessionPlayer(repository: PlayerRepository): androidx.media3.common.Player? {
        val concreteRepository: PlayerRepositoryImpl? = repository as? PlayerRepositoryImpl
        return concreteRepository?.getSessionPlayer()
    }

    private fun buildSessionId(): String {
        return UUID.randomUUID().toString()
    }

    internal fun startPlaybackStateSync(
        session: MediaSession,
        repository: PlayerRepository,
    ): Unit {
        playbackStateJob?.cancel()
        playbackStateJob = serviceScope.launch {
            repository.playbackState.collectLatest { state ->
                val error: PlaybackException? = resolvePlaybackException(state = state)
                session.setPlaybackException(error)
                logPlaybackStateChange(state = state)
                notificationManager?.updateCurrentSong(song = state.currentSong)
            }
        }
        isStateSyncStarted = true
        android.util.Log.d(TAG, "state sync started")
    }

    internal fun resolvePlaybackException(state: PlaybackState): PlaybackException? {
        val error: Exception? = state.error
        if (error is PlaybackException) {
            return error
        }
        return null
    }

    private fun logPlaybackStateChange(state: PlaybackState): Unit {
        val songId: String? = state.currentSong?.id
        val isPlaying: Boolean = state.isPlaying
        val shouldLog: Boolean = songId != lastSongId || lastIsPlaying != isPlaying
        if (!shouldLog) {
            return
        }
        lastSongId = songId
        lastIsPlaying = isPlaying
        android.util.Log.d(
            TAG,
            "playbackState: title=${state.currentSong?.title} isPlaying=$isPlaying",
        )
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

/**
 * 媒体会话播放控制回调
 */
internal class PlaybackSessionCallback(
    private val playerRepository: PlayerRepository,
) : MediaSession.Callback {
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onPlayerCommandRequest(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        playerCommand: Int,
    ): Int {
        when (playerCommand) {
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> playerRepository.skipToNext()
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> playerRepository.skipToPrevious()
            Player.COMMAND_STOP -> {
                playerRepository.pause()
                playerRepository.seekTo(position = 0L)
            }
        }
        return SessionResult.RESULT_SUCCESS
    }
}
