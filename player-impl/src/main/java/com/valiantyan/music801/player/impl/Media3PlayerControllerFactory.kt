package com.valiantyan.music801.player.impl

import android.content.Context
import com.valiantyan.music801.di.PlayerControllerHolder
import com.valiantyan.music801.player.PlayerController
import com.valiantyan.music801.player.PlayerControllerFactory

/**
 * Media3 播放控制器工厂实现
 */
internal class Media3PlayerControllerFactory : PlayerControllerFactory {
    override fun getOrCreate(context: Context): PlayerController {
        return PlayerControllerHolder.getOrCreate(context = context)
    }

    override fun clear(): Unit {
        PlayerControllerHolder.clear()
    }
}
