package com.valiantyan.music801.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valiantyan.music801.data.repository.AudioRepository
import com.valiantyan.music801.domain.model.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 歌曲列表 ViewModel
 *
 * 管理歌曲列表界面状态，负责从 [AudioRepository] 获取歌曲数据。
 *
 * @param audioRepository 音频数据仓库
 */
class SongListViewModel(
    private val audioRepository: AudioRepository,
) : ViewModel() {
    /**
     * UI 状态（可变）
     */
    private val _uiState = MutableStateFlow(SongListUiState(isLoading = true))

    /**
     * UI 状态（只读）
     */
    val uiState: StateFlow<SongListUiState> = _uiState.asStateFlow()

    /**
     * 当前加载任务
     */
    private var loadJob: Job? = null

    init {
        loadSongs()
    }

    /**
     * 加载歌曲列表
     */
    fun loadSongs() {
        loadJob?.cancel()
        setLoadingState()
        loadJob = viewModelScope.launch {
            audioRepository.getAllSongs()
                .catch { exception ->
                    setErrorState(message = exception.message ?: "获取歌曲列表失败")
                }
                .collect { songs ->
                    setSongsState(songs = songs)
                }
        }
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.update { currentState ->
            currentState.copy(error = null)
        }
    }

    private fun setLoadingState() {
        _uiState.update { currentState ->
            currentState.copy(
                isLoading = true,
                isEmpty = false,
                error = null,
            )
        }
    }

    private fun setSongsState(songs: List<Song>) {
        _uiState.update { currentState ->
            currentState.copy(
                songs = songs,
                isLoading = false,
                isEmpty = songs.isEmpty(),
                error = null,
            )
        }
    }

    private fun setErrorState(message: String) {
        _uiState.update { currentState ->
            currentState.copy(
                isLoading = false,
                error = message,
            )
        }
    }
}
