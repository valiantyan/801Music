package com.valiantyan.music801.di

import android.content.Context
import com.valiantyan.music801.data.repository.PlayerRepository
import com.valiantyan.music801.data.repository.PlayerRepositoryImpl
import com.valiantyan.music801.player.Media3PlayerManager
import com.valiantyan.music801.player.MediaPlayerManager
import com.valiantyan.music801.player.MediaQueueManager

/**
 * 播放器仓库持有者
 *
 * 通过单例复用 [PlayerRepository]，避免配置变更导致播放器重建。
 */
object PlayerRepositoryHolder {
    /**
     * 全局播放器仓库实例
     */
    private var repository: PlayerRepository? = null

    /**
     * 获取或创建播放器仓库
     *
     * @param context 用于创建播放器所需的上下文
     * @return 播放器仓库实例
     */
    fun getOrCreate(context: Context): PlayerRepository {
        val existing: PlayerRepository? = repository
        if (existing != null) {
            return existing
        }
        val appContext: Context = context.applicationContext
        val mediaPlayerManager: MediaPlayerManager = Media3PlayerManager(context = appContext)
        val created: PlayerRepository = PlayerRepositoryImpl(
            mediaQueueManager = MediaQueueManager(),
            mediaPlayerManager = mediaPlayerManager,
        )
        repository = created
        return created
    }

    /**
     * 释放并清理播放器仓库实例
     */
    fun clear(): Unit {
        val existing: PlayerRepository? = repository
        if (existing == null) {
            return
        }
        existing.release()
        repository = null
    }
}
