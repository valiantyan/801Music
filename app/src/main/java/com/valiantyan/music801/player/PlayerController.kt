package com.valiantyan.music801.player

import com.valiantyan.music801.domain.model.PlaybackState
import com.valiantyan.music801.domain.model.Song
import kotlinx.coroutines.flow.StateFlow

/**
 * 播放控制器接口
 *
 * 通过媒体会话控制播放并暴露统一播放状态。
 */
interface PlayerController {
    /**
     * 播放状态流
     */
    val playbackState: StateFlow<PlaybackState>

    /**
     * 设置播放队列
     *
     * @param songs 播放队列
     * @param startIndex 起始索引
     */
    fun setQueue(
        songs: List<Song>,
        startIndex: Int,
    ): Unit

    /**
     * 开始播放
     */
    fun play(): Unit

    /**
     * 暂停播放
     */
    fun pause(): Unit

    /**
     * 跳转到指定位置
     *
     * @param position 目标位置（毫秒）
     */
    fun seekTo(position: Long): Unit

    /**
     * 切换到下一首
     */
    fun skipToNext(): Unit

    /**
     * 切换到上一首
     */
    fun skipToPrevious(): Unit
}
