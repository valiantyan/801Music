package com.valiantyan.music801.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valiantyan.music801.player.PlayerController
import com.valiantyan.music801.domain.model.PlaybackState
import com.valiantyan.music801.domain.model.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 播放器 ViewModel
 *
 * 管理播放控制与界面状态，订阅 [PlayerController] 的播放状态。
 *
 * @param playerController 播放控制器
 */
class PlayerViewModel(
    private val playerController: PlayerController,
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
        playerController.play()
    }

    /**
     * 暂停播放
     */
    fun pause(): Unit {
        playerController.pause()
    }

    /**
     * 设置播放队列
     *
     * @param songs 播放队列
     * @param startIndex 起始索引
     */
    fun setQueue(
        songs: List<Song>,
        startIndex: Int,
    ): Unit {
        playerController.setQueue(songs = songs, startIndex = startIndex)
    }

    /**
     * 跳转播放位置
     *
     * @param position 目标位置（毫秒）
     */
    fun seekTo(position: Long): Unit {
        playerController.seekTo(position = position)
    }

    /**
     * 切换到下一首
     */
    fun skipToNext(): Unit {
        playerController.skipToNext()
    }

    /**
     * 切换到上一首
     */
    fun skipToPrevious(): Unit {
        playerController.skipToPrevious()
    }

    /**
     * 通过 [viewModelScope] 调用 [PlayerController.toggleFavorite]，避免阻塞主线程
     */
    fun toggleFavorite(): Unit {
        viewModelScope.launch {
            playerController.toggleFavorite()
        }
    }

    /**
     * 订阅播放状态
     */
    private fun observePlaybackState(): Unit {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            playerController.playbackState.collect { state ->
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
                isPlayWhenReady = state.isPlayWhenReady,
                position = state.position,
                duration = state.duration,
                playbackState = state.playbackState,
                queue = state.queue,
                currentIndex = state.currentIndex,
                isFavorite = state.isFavorite,
                isLoading = false,
                error = state.error?.message,
            )
        }
    }
}
