package com.valiantyan.music801.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.valiantyan.music801.player.PlayerController

/**
 * PlayerViewModel 工厂类
 *
 * 用于创建 [PlayerViewModel]，提供必要的播放控制器依赖。
 *
 * @param playerController 播放控制器
 */
class PlayerViewModelFactory(
    private val playerController: PlayerController,
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
            return PlayerViewModel(playerController = playerController) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
