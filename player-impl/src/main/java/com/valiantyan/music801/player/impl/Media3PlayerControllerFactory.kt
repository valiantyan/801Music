package com.valiantyan.music801.player.impl

import android.content.Context
import com.valiantyan.music801.di.PlayerControllerHolder
import com.valiantyan.music801.player.PlayerController
import com.valiantyan.music801.player.PlayerControllerFactory

/**
 * Media3 播放控制器工厂实现
 */
internal class Media3PlayerControllerFactory : PlayerControllerFactory {
    /**
     * 复用 [PlayerControllerHolder] 的单例策略，避免重复创建控制器
     */
    override fun getOrCreate(context: Context): PlayerController {
        return PlayerControllerHolder.getOrCreate(context = context)
    }

    /**
     * 释放单例控制器持有的资源
     */
    override fun clear(): Unit {
        PlayerControllerHolder.clear()
    }
}
