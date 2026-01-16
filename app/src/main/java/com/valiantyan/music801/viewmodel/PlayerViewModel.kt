package com.valiantyan.music801.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valiantyan.music801.data.repository.PlayerRepository
import com.valiantyan.music801.domain.model.PlaybackState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 播放器 ViewModel
 *
 * 管理播放控制与界面状态，订阅 [PlayerRepository] 的播放状态。
 *
 * @param playerRepository 播放器数据仓库
 */
class PlayerViewModel(
    private val playerRepository: PlayerRepository,
) : ViewModel() {
    /**
     * UI 状态（可变）
     */
    private val _uiState: MutableStateFlow<PlayerUiState> = MutableStateFlow(PlayerUiState())

    /**
     * UI 状态（只读）
     */
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    /**
     * 播放状态订阅任务
     */
    private var playbackJob: Job? = null

    init {
        observePlaybackState()
    }

    /**
     * 开始播放
     */
    fun play(): Unit {
        playerRepository.play()
    }

    /**
     * 暂停播放
     */
    fun pause(): Unit {
        playerRepository.pause()
    }

    /**
     * 跳转播放位置
     *
     * @param position 目标位置（毫秒）
     */
    fun seekTo(position: Long): Unit {
        playerRepository.seekTo(position = position)
    }

    /**
     * 切换到下一首
     */
    fun skipToNext(): Unit {
        playerRepository.skipToNext()
    }

    /**
     * 切换到上一首
     */
    fun skipToPrevious(): Unit {
        playerRepository.skipToPrevious()
    }

    /**
     * 订阅播放状态
     */
    private fun observePlaybackState(): Unit {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            playerRepository.playbackState.collect { state ->
                updateUiState(state = state)
            }
        }
    }

    /**
     * 映射播放状态到 UI
     *
     * @param state 播放状态
     */
    private fun updateUiState(state: PlaybackState): Unit {
        _uiState.update { currentState ->
            currentState.copy(
                currentSong = state.currentSong,
                isPlaying = state.isPlaying,
                position = state.position,
                duration = state.duration,
                queue = state.queue,
                currentIndex = state.currentIndex,
                isLoading = false,
                error = null,
            )
        }
    }
}
