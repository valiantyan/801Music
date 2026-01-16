package com.valiantyan.music801.di

import com.valiantyan.music801.data.repository.PlayerRepository

/**
 * 播放器仓库提供者
 */
interface PlayerRepositoryProvider {
    /**
     * 提供共享的 [PlayerRepository]
     */
    fun providePlayerRepository(): PlayerRepository
}
