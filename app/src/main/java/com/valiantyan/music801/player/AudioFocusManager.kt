package com.valiantyan.music801.player

import android.content.Context
import android.media.AudioAttributes as PlatformAudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager

/**
 * 音频焦点管理器
 *
 * 负责与系统音频焦点 API 交互，避免播放逻辑与系统交互混杂。
 *
 * @param context 用于获取系统 [AudioManager] 的上下文
 * @param onFocusChange 音频焦点变化回调
 */
class AudioFocusManager(
    context: Context,
    private val onFocusChange: (Int) -> Unit,
    audioFocusController: AudioFocusController = AudioManagerAudioFocusController(
        context = context,
    ),
) {
    /**
     * 系统音频管理器，用于请求与释放音频焦点
     */
    private val audioFocusController: AudioFocusController = audioFocusController
    /**
     * AudioFocus 监听器，集中处理焦点变化
     */
    private val audioFocusChangeListener: AudioManager.OnAudioFocusChangeListener =
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            handleFocusChange(focusChange = focusChange)
        }
    /**
     * AudioFocus 请求对象，确保与媒体属性保持一致
     */
    private val audioFocusRequest: AudioFocusRequest =
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(buildPlatformAudioAttributes())
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()

    /**
     * 请求音频焦点，确保播放前得到系统许可
     *
     * @return 是否成功获取音频焦点
     */
    fun requestFocus(): Boolean {
        val result: Int = audioFocusController.requestFocus(audioFocusRequest)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    /**
     * 释放音频焦点，避免占用系统资源
     */
    fun abandonFocus(): Unit {
        audioFocusController.abandonFocus(audioFocusRequest)
    }

    /**
     * 构建平台 [PlatformAudioAttributes] 以供音频焦点请求使用
     */
    private fun buildPlatformAudioAttributes(): PlatformAudioAttributes {
        val attributes: PlatformAudioAttributes = PlatformAudioAttributes.Builder()
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .build()
        return attributes
    }

    /**
     * 分发音频焦点变化，便于测试与集中处理
     *
     * @param focusChange 音频焦点变化类型
     */
    internal fun handleFocusChange(focusChange: Int): Unit {
        onFocusChange.invoke(focusChange)
    }
}
