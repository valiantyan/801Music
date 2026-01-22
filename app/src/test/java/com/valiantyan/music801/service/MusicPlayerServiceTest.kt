package com.valiantyan.music801.service

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import androidx.media3.common.PlaybackException
import com.valiantyan.music801.domain.model.PlaybackState

/**
 * 测试 MusicPlayerService 基础生命周期
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [android.os.Build.VERSION_CODES.TIRAMISU])
class MusicPlayerServiceTest {
    @Test
    fun `服务创建时应标记已创建`() {
        // Arrange
        val inputController: ServiceController<MusicPlayerService> = Robolectric.buildService(
            MusicPlayerService::class.java,
        )
        // Act
        val actualService: MusicPlayerService = inputController.create().get()
        // Assert
        val actualCreated: Boolean = actualService.isCreated
        val actualSessionCreated: Boolean = actualService.isSessionCreated
        val actualStateSyncStarted: Boolean = actualService.isStateSyncStarted
        assertTrue(actualCreated)
        assertTrue(actualSessionCreated)
        assertTrue(actualStateSyncStarted)
    }

    @Test
    fun `服务销毁时应标记已销毁`() {
        // Arrange
        val inputController: ServiceController<MusicPlayerService> = Robolectric.buildService(
            MusicPlayerService::class.java,
        )
        val actualService: MusicPlayerService = inputController.create().get()
        // Act
        inputController.destroy()
        // Assert
        val actualDestroyed: Boolean = actualService.isDestroyed
        assertTrue(actualDestroyed)
    }

    @Test
    fun `播放错误为PlaybackException时应返回异常`() {
        // Arrange
        val inputController: ServiceController<MusicPlayerService> = Robolectric.buildService(
            MusicPlayerService::class.java,
        )
        val actualService: MusicPlayerService = inputController.create().get()
        // Media3 Java API 不支持命名参数，使用位置参数
        val inputError: PlaybackException = PlaybackException(
            "test",
            null,
            PlaybackException.ERROR_CODE_UNSPECIFIED,
        )
        val inputState: PlaybackState = PlaybackState(
            error = inputError,
        )
        // Act
        val actualError: PlaybackException? = actualService.resolvePlaybackException(
            state = inputState,
        )
        // Assert
        assertEqualsAny(expected = inputError, actual = actualError)
    }

    @Test
    fun `非PlaybackException错误应忽略`() {
        // Arrange
        val inputController: ServiceController<MusicPlayerService> = Robolectric.buildService(
            MusicPlayerService::class.java,
        )
        val actualService: MusicPlayerService = inputController.create().get()
        val inputState: PlaybackState = PlaybackState(
            error = IllegalStateException("test"),
        )
        // Act
        val actualError: PlaybackException? = actualService.resolvePlaybackException(
            state = inputState,
        )
        // Assert
        assertEqualsAny(expected = null, actual = actualError)
    }
}

private fun assertEqualsAny(
    expected: Any?,
    actual: Any?,
): Unit {
    // JUnit Java API 不支持命名参数，使用封装函数适配规范
    org.junit.Assert.assertEquals(expected, actual)
}
