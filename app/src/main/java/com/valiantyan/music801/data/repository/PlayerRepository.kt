package com.valiantyan.music801.data.repository

import com.valiantyan.music801.domain.model.PlaybackState
import com.valiantyan.music801.domain.model.Song
import kotlinx.coroutines.flow.StateFlow

/**
 * 播放器仓库接口
 *
 * 定义播放控制与状态订阅的统一入口，具体播放引擎由实现类负责。
 */
interface PlayerRepository {
    /**
     * 播放状态流
     */
    val playbackState: StateFlow<PlaybackState>

    /**
     * 设置播放队列
     *
     * @param songs 播放队列
     * @param startIndex 开始播放索引
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
     * 跳转到指定播放位置
     *
     * @param position 目标播放位置（毫秒）
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

    /**
     * 释放播放器资源
     */
    fun release(): Unit
}
