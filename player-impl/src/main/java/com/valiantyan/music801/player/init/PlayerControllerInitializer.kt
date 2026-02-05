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
    /**
     * 应用启动时注册 [Media3PlayerControllerFactory]，保证业务层可直接获取 [PlayerControllerRegistry]
     */
    override fun create(context: Context): Unit {
        PlayerControllerRegistry.setFactory(Media3PlayerControllerFactory())
    }

    /**
     * 当前初始化器无需依赖其他初始化器
     */
    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
