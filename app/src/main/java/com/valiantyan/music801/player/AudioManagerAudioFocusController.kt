package com.valiantyan.music801.player

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager

/**
 * 系统音频焦点控制实现
 *
 * 通过 [AudioManager] 代理焦点请求与释放。
 *
 * @param context 用于获取系统 [AudioManager]
 */
class AudioManagerAudioFocusController(
    context: Context,
) : AudioFocusController {
    /**
     * 系统音频管理器，用于请求与释放音频焦点
     */
    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /**
     * 请求音频焦点
     *
     * @param request 音频焦点请求
     * @return 请求结果
     */
    override fun requestFocus(request: AudioFocusRequest): Int {
        return audioManager.requestAudioFocus(request)
    }

    /**
     * 释放音频焦点
     *
     * @param request 音频焦点请求
     * @return 释放结果
     */
    override fun abandonFocus(request: AudioFocusRequest): Int {
        return audioManager.abandonAudioFocusRequest(request)
    }
}
