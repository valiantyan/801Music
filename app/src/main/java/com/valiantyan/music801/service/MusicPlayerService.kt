package com.valiantyan.music801.service

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * 媒体会话服务基础框架
 *
 * 负责在系统层暴露媒体会话入口，具体会话配置在后续任务中完成。
 */
class MusicPlayerService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    internal var isCreated: Boolean = false
    internal var isDestroyed: Boolean = false

    override fun onCreate(): Unit {
        super.onCreate()
        isCreated = true
        // TODO: To be implemented in Story STORY-005
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy(): Unit {
        isDestroyed = true
        super.onDestroy()
    }
}
