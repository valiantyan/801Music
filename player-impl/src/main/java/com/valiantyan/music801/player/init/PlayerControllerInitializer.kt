package com.valiantyan.music801.player.init

import android.content.Context
import androidx.startup.Initializer
import com.valiantyan.music801.player.PlayerControllerRegistry
import com.valiantyan.music801.player.impl.Media3PlayerControllerFactory

/**
 * 播放控制器初始化器
 *
 * 使用 AndroidX Startup 自动注册播放控制器工厂。
 */
class PlayerControllerInitializer : Initializer<Unit> {
    override fun create(context: Context): Unit {
        PlayerControllerRegistry.setFactory(Media3PlayerControllerFactory())
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
