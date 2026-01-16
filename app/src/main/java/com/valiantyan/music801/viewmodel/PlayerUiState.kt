package com.valiantyan.music801.viewmodel

import com.valiantyan.music801.domain.model.Song

/**
 * 播放器界面状态
 *
 * @param currentSong 当前播放歌曲
 * @param isPlaying 是否正在播放
 * @param position 当前播放位置（毫秒）
 * @param duration 当前歌曲总时长（毫秒）
 * @param queue 播放队列
 * @param currentIndex 当前播放索引
 * @param isLoading 是否正在加载
 * @param error 错误信息
 */
data class PlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val isLoading: Boolean = false,
    val error: String? = null,
)
