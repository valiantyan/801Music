package com.valiantyan.music801.di

import com.valiantyan.music801.player.PlayerController

/**
 * 播放控制器提供者
 */
interface PlayerControllerProvider {
    /**
     * 提供共享的 [PlayerController]
     */
    fun providePlayerController(): PlayerController
}
