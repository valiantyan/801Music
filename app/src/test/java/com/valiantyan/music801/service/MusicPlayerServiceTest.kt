package com.valiantyan.music801.service

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config

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
        assertTrue(actualCreated)
        assertTrue(actualSessionCreated)
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
}
