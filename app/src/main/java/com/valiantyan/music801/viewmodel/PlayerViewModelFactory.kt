package com.valiantyan.music801.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.valiantyan.music801.data.repository.PlayerRepository

/**
 * PlayerViewModel 工厂类
 *
 * 用于创建 [PlayerViewModel]，提供必要的播放仓库依赖。
 *
 * @param playerRepository 播放器仓库
 */
class PlayerViewModelFactory(
    private val playerRepository: PlayerRepository,
) : ViewModelProvider.Factory {

    /**
     * 创建 [PlayerViewModel] 实例
     *
     * @param modelClass 目标 [ViewModel] 类型
     * @return 对应的 [ViewModel] 实例
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
            return PlayerViewModel(playerRepository = playerRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
