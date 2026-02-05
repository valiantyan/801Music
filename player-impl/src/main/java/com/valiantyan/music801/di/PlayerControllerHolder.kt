package com.valiantyan.music801.di

import android.content.Context
import com.valiantyan.music801.player.PlayerController
import com.valiantyan.music801.player.MediaControllerManager

/**
 * 播放控制器单例管理
 */
internal object PlayerControllerHolder {
    /**
     * 缓存单例 [PlayerController]，避免重复建立媒体会话连接
     */
    private var controller: PlayerController? = null

    /**
     * 获取或创建 [PlayerController]
     */
    fun getOrCreate(context: Context): PlayerController {
        val existing: PlayerController? = controller
        if (existing != null) {
            return existing
        }
        val created: PlayerController = MediaControllerManager(context = context)
        controller = created
        return created
    }

    /**
     * 清理控制器资源
     */
    fun clear(): Unit {
        val existing: PlayerController? = controller
        if (existing == null) {
            return
        }
        if (existing is MediaControllerManager) {
            existing.release()
        }
        controller = null
    }
}
