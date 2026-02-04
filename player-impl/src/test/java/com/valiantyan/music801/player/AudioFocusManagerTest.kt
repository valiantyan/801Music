package com.valiantyan.music801.player

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * 测试 AudioFocusManager 音频焦点行为
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [android.os.Build.VERSION_CODES.TIRAMISU])
class AudioFocusManagerTest {
    @Test
    fun `请求焦点成功应返回true`() {
        // Arrange - 构造返回成功的控制器
        val context: Context = RuntimeEnvironment.getApplication()
        val controller: TestAudioFocusController = TestAudioFocusController(
            requestResult = AudioManager.AUDIOFOCUS_REQUEST_GRANTED,
        )
        val manager: AudioFocusManager = AudioFocusManager(
            context = context,
            onFocusChange = {},
            audioFocusController = controller,
        )
        // Act
        val actualResult: Boolean = manager.requestFocus()
        // Assert
        assertTrue(actualResult)
    }

    @Test
    fun `请求焦点失败应返回false`() {
        // Arrange - 构造返回失败的控制器
        val context: Context = RuntimeEnvironment.getApplication()
        val controller: TestAudioFocusController = TestAudioFocusController(
            requestResult = AudioManager.AUDIOFOCUS_REQUEST_FAILED,
        )
        val manager: AudioFocusManager = AudioFocusManager(
            context = context,
            onFocusChange = {},
            audioFocusController = controller,
        )
        // Act
        val actualResult: Boolean = manager.requestFocus()
        // Assert
        assertFalse(actualResult)
    }

    @Test
    fun `释放焦点应通知控制器`() {
        // Arrange - 构造可记录调用次数的控制器
        val context: Context = RuntimeEnvironment.getApplication()
        val controller: TestAudioFocusController = TestAudioFocusController(
            requestResult = AudioManager.AUDIOFOCUS_REQUEST_GRANTED,
        )
        val manager: AudioFocusManager = AudioFocusManager(
            context = context,
            onFocusChange = {},
            audioFocusController = controller,
        )
        // Act
        manager.abandonFocus()
        // Assert
        assertEquals(1, controller.abandonCount)
    }

    @Test
    fun `收到焦点变化应回调上层`() {
        // Arrange - 验证焦点变化传递
        val context: Context = RuntimeEnvironment.getApplication()
        val controller: TestAudioFocusController = TestAudioFocusController(
            requestResult = AudioManager.AUDIOFOCUS_REQUEST_GRANTED,
        )
        var actualFocusChange: Int = 0
        val manager: AudioFocusManager = AudioFocusManager(
            context = context,
            onFocusChange = { focusChange -> actualFocusChange = focusChange },
            audioFocusController = controller,
        )
        // Act
        manager.handleFocusChange(focusChange = AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        // Assert
        assertEquals(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, actualFocusChange)
    }
}

private class TestAudioFocusController(
    private val requestResult: Int,
) : AudioFocusController {
    var abandonCount: Int = 0

    override fun requestFocus(request: AudioFocusRequest): Int {
        return requestResult
    }

    override fun abandonFocus(request: AudioFocusRequest): Int {
        abandonCount += 1
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
}
