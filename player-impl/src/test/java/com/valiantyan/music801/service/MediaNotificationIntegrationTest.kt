package com.valiantyan.music801.service

import android.app.Application
import android.app.Notification
import android.os.Build
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaSession
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.android.controller.ServiceController

/**
 * 媒体通知端到端集成测试
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class MediaNotificationIntegrationTest {
    @After
    fun tearDown(): Unit {
        RuntimeEnvironment.setQualifiers("")
    }

    @Test
    fun `服务创建后应可构建媒体样式通知`() {
        val controller: ServiceController<MusicPlayerService> = Robolectric.buildService(
            MusicPlayerService::class.java,
        )
        val service: MusicPlayerService = controller.create().get()
        val session: MediaSession? = service.getMediaSessionForTesting()
        val manager: PlayerNotificationManager? = service.getNotificationManagerForTesting()
        assertNotNull(session)
        assertNotNull(manager)
        val mediaMetadata: MediaMetadata = MediaMetadata.Builder()
            .setTitle("Integration Title")
            .setArtist("Integration Artist")
            .build()
        val mediaItem: MediaItem = MediaItem.Builder()
            .setUri("file:///tmp/integration.mp3")
            .setMediaMetadata(mediaMetadata)
            .build()
        session?.player?.setMediaItem(mediaItem)
        val notification: Notification? = manager?.buildMediaStyleNotification(
            isPlaying = true,
            mediaSession = session!!,
        )
        assertNotNull(notification)
        controller.destroy()
    }

    @Test
    fun `主题切换后通知仍可正常构建`() {
        val context: Application = RuntimeEnvironment.getApplication()
        val manager: PlayerNotificationManager = PlayerNotificationManager(context = context)
        val inputNotification: Notification = manager.buildNotification(
            title = "Theme Title",
            artist = "Theme Artist",
            isPlaying = false,
        )
        val inputTitle: CharSequence? = inputNotification.extras.getCharSequence(
            Notification.EXTRA_TITLE,
        )
        assertEqualsAny(expected = "Theme Title", actual = inputTitle)
        RuntimeEnvironment.setQualifiers("+night")
        val updatedNotification: Notification = manager.buildNotification(
            title = "Theme Title",
            artist = "Theme Artist",
            isPlaying = false,
        )
        val actualTitle: CharSequence? = updatedNotification.extras.getCharSequence(
            Notification.EXTRA_TITLE,
        )
        assertEqualsAny(expected = "Theme Title", actual = actualTitle)
        assertTrue(updatedNotification.extras != null)
    }
}

private fun assertEqualsAny(
    expected: Any?,
    actual: Any?,
): Unit {
    org.junit.Assert.assertEquals(expected, actual)
}
