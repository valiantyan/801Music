package com.valiantyan.music801.player

import android.content.Context

/**
 * 播放控制器注册表
 *
 * 通过注册工厂隐藏实现细节，保持 app 仅依赖 API 层。
 */
object PlayerControllerRegistry {
    @Volatile
    private var factory: PlayerControllerFactory? = null

    /**
     * 注册播放控制器工厂
     *
     * @param factory 具体实现工厂
     */
    fun setFactory(factory: PlayerControllerFactory): Unit {
        this.factory = factory
    }

    /**
     * 获取播放控制器
     *
     * @param context 用于创建控制器的上下文
     */
    fun getOrCreate(context: Context): PlayerController {
        val resolvedFactory: PlayerControllerFactory? = factory
        if (resolvedFactory != null) {
            return resolvedFactory.getOrCreate(context = context)
        }
        synchronized(this) {
            val lockedFactory: PlayerControllerFactory? = factory
            if (lockedFactory != null) {
                return lockedFactory.getOrCreate(context = context)
            }
            throw IllegalStateException("PlayerControllerFactory 未初始化")
        }
    }

    /**
     * 清理播放控制器资源
     */
    fun clear(): Unit {
        val resolvedFactory: PlayerControllerFactory? = factory
        if (resolvedFactory != null) {
            resolvedFactory.clear()
        }
    }
}
