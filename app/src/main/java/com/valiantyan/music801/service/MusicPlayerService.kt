package com.valiantyan.music801.service

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.valiantyan.music801.player.Media3PlayerManager

/**
 * 媒体会话服务基础框架
 *
 * 负责在系统层暴露媒体会话入口，具体会话配置在后续任务中完成。
 */
class MusicPlayerService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var mediaPlayerManager: Media3PlayerManager? = null
    internal var isCreated: Boolean = false
    internal var isDestroyed: Boolean = false
    internal var isSessionCreated: Boolean = false

    override fun onCreate(): Unit {
        super.onCreate()
        isCreated = true
        val createdManager: Media3PlayerManager = Media3PlayerManager(
            context = applicationContext,
        )
        val createdSession: MediaSession = MediaSession.Builder(this, createdManager.exoPlayer)
            .build()
        mediaPlayerManager = createdManager
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
        mediaPlayerManager?.release()
        mediaPlayerManager = null
        super.onDestroy()
    }
}
