package com.valiantyan.music801.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.valiantyan.music801.data.repository.AudioRepository

/**
 * ScanViewModel 工厂类
 * 
 * 用于创建 ScanViewModel 实例，提供必要的依赖（AudioRepository）。
 * 
 * 注意：v1.0 使用手动依赖注入，后续版本可迁移到 Hilt。
 * 
 * @param audioRepository 音频数据仓库
 */
class ScanViewModelFactory(
    private val audioRepository: AudioRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScanViewModel::class.java)) {
            return ScanViewModel(audioRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
