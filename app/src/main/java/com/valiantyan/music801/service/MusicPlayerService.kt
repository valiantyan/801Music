package com.valiantyan.music801.service

import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionResult
import com.valiantyan.music801.data.repository.PlayerRepository
import com.valiantyan.music801.data.repository.PlayerRepositoryImpl
import com.valiantyan.music801.di.PlayerRepositoryHolder

/**
 * 媒体会话服务基础框架
 *
 * 负责在系统层暴露媒体会话入口，具体会话配置在后续任务中完成。
 */
class MusicPlayerService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    internal var isCreated: Boolean = false
    internal var isDestroyed: Boolean = false
    internal var isSessionCreated: Boolean = false

    override fun onCreate(): Unit {
        super.onCreate()
        isCreated = true
        val repository: PlayerRepository = PlayerRepositoryHolder.getOrCreate(
            context = applicationContext,
        )
        val sessionPlayer: androidx.media3.common.Player? = resolveSessionPlayer(
            repository = repository,
        )
        if (sessionPlayer == null) {
            return
        }
        val sessionCallback: PlaybackSessionCallback = PlaybackSessionCallback(
            playerRepository = repository,
        )
        val createdSession: MediaSession = MediaSession.Builder(this, sessionPlayer)
            .setCallback(sessionCallback)
            .build()
        mediaSession = createdSession
        isSessionCreated = true
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy(): Unit {
        isDestroyed = true
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private fun resolveSessionPlayer(repository: PlayerRepository): androidx.media3.common.Player? {
        val concreteRepository: PlayerRepositoryImpl? = repository as? PlayerRepositoryImpl
        return concreteRepository?.getSessionPlayer()
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
