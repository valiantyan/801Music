package com.valiantyan.music801.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.valiantyan.music801.data.repository.AudioRepository

/**
 * SongListViewModel 工厂
 *
 * @param audioRepository 音频数据仓库
 */
class SongListViewModelFactory(
    private val audioRepository: AudioRepository,
) : ViewModelProvider.Factory {
    /**
     * 创建 [SongListViewModel] 实例
     *
     * @param modelClass 目标 [ViewModel] 类型
     * @return 对应的 [ViewModel] 实例
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SongListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SongListViewModel(audioRepository = audioRepository) as T
        }
        throw IllegalArgumentException("未知的 ViewModel 类型: ${modelClass.name}")
    }
}
