package com.valiantyan.music801.player

import android.content.Context

/**
 * 播放控制器工厂
 *
 * 由实现模块提供具体实现以隔离播放器细节。
 */
interface PlayerControllerFactory {
    /**
     * 获取或创建播放控制器
     *
     * @param context 用于创建控制器的上下文
     */
    fun getOrCreate(context: Context): PlayerController

    /**
     * 清理控制器资源
     */
    fun clear(): Unit
}
