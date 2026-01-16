package com.valiantyan.music801.di

import com.valiantyan.music801.data.repository.AudioRepository

/**
 * 音频仓库提供者
 */
interface AudioRepositoryProvider {
    /**
     * 提供共享的 [AudioRepository]
     */
    fun provideAudioRepository(): AudioRepository
}
