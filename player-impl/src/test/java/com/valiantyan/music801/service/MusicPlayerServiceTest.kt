package com.valiantyan.music801.service

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession

/**
 * 测试 MusicPlayerService 基础生命周期
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [android.os.Build.VERSION_CODES.TIRAMISU])
class MusicPlayerServiceTest {
    @Test
    fun `服务创建时应标记已创建并初始化会话`() {
        val inputController: ServiceController<MusicPlayerService> = Robolectric.buildService(
            MusicPlayerService::class.java,
        )
        val actualService: MusicPlayerService = inputController.create().get()
        val actualCreated: Boolean = actualService.isCreated
        val actualSessionCreated: Boolean = actualService.isSessionCreated
        val actualNotificationInitialized: Boolean = actualService.isNotificationInitialized
        assertTrue(actualCreated)
        assertTrue(actualSessionCreated)
        assertTrue(actualNotificationInitialized)
        val session: MediaSession? = actualService.getMediaSessionForTesting()
        assertNotNull(session)
        val customLayout: List<CommandButton>? = session?.customLayout
        assertNotNull(customLayout)
        assertTrue(customLayout?.isNotEmpty() == true)
        assertNotNull(actualService.getNotificationManagerForTesting())
    }

    @Test
    fun `服务销毁时应标记已销毁`() {
        val inputController: ServiceController<MusicPlayerService> = Robolectric.buildService(
            MusicPlayerService::class.java,
        )
        val actualService: MusicPlayerService = inputController.create().get()
        inputController.destroy()
        val actualDestroyed: Boolean = actualService.isDestroyed
        assertTrue(actualDestroyed)
    }
}
