package com.valiantyan.music801.player

import android.net.Uri
import com.valiantyan.music801.domain.model.PlaybackState
import kotlinx.coroutines.flow.Flow

/**
 * 播放器管理接口
 *
 * 统一封装底层播放引擎能力，暴露可测试的播放控制与状态流。
 */
interface MediaPlayerManager {
    /**
     * 播放指定音频资源
     *
     * @param uri 音频文件 Uri
     */
    fun play(uri: Uri): Unit

    /**
     * 暂停播放
     */
    fun pause(): Unit

    /**
     * 恢复播放
     */
    fun resume(): Unit

    /**
     * 停止播放
     */
    fun stop(): Unit

    /**
     * 跳转到指定位置
     *
     * @param position 目标位置（毫秒）
     */
    fun seekTo(position: Long): Unit

    /**
     * 获取播放状态流
     *
     * @return 播放状态 Flow
     */
    fun playbackState(): Flow<PlaybackState>

    /**
     * 释放播放器资源
     */
    fun release(): Unit
}
