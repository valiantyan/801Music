package com.valiantyan.music801.domain.model

/**
 * 播放状态数据
 *
 * @param currentSong 当前播放歌曲
 * @param isPlaying 是否正在播放
 * @param position 当前播放位置（毫秒）
 * @param duration 当前歌曲时长（毫秒）
 * @param queue 播放队列
 * @param currentIndex 当前播放索引
 */
data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
)
