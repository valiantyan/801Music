package com.valiantyan.music801.player

import android.media.AudioFocusRequest

/**
 * 音频焦点控制接口
 *
 * 隔离系统 [android.media.AudioManager]，便于测试与替换。
 */
interface AudioFocusController {
    /**
     * 请求音频焦点
     *
     * @param request 音频焦点请求
     * @return 请求结果
     */
    fun requestFocus(request: AudioFocusRequest): Int

    /**
     * 释放音频焦点
     *
     * @param request 音频焦点请求
     * @return 释放结果
     */
    fun abandonFocus(request: AudioFocusRequest): Int
}
