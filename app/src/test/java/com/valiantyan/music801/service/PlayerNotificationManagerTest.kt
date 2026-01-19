package com.valiantyan.music801.service

import android.app.Application
import android.app.Notification
import android.os.Build
import com.valiantyan.music801.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * 测试 PlayerNotificationManager 基础通知
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class PlayerNotificationManagerTest {
    @Test
    fun `构建通知应包含标题和作者`() {
        // Arrange
        val context: Application = RuntimeEnvironment.getApplication()
        val manager: PlayerNotificationManager = PlayerNotificationManager(context = context)
        val inputSong: Song = Song(
            id = "id",
            title = "标题",
            artist = "作者",
            album = null,
            duration = 1000L,
            filePath = "/tmp/test.mp3",
            fileSize = 1L,
            dateAdded = 0L,
            albumArtPath = null,
        )
        // Act
        manager.createNotificationChannel()
        val actualNotification: Notification = manager.buildNotification(
            song = inputSong,
            isPlaying = true,
        )
        // Assert
        val actualTitle: CharSequence? = actualNotification.extras.getCharSequence(
            Notification.EXTRA_TITLE,
        )
        val actualArtist: CharSequence? = actualNotification.extras.getCharSequence(
            Notification.EXTRA_TEXT,
        )
        assertEquals("标题", actualTitle)
        assertEquals("作者", actualArtist)
    }

    @Test
    fun `播放中通知应保持常驻`() {
        // Arrange
        val context: Application = RuntimeEnvironment.getApplication()
        val manager: PlayerNotificationManager = PlayerNotificationManager(context = context)
        // Act
        val actualNotification: Notification = manager.buildNotification(
            song = null,
            isPlaying = true,
        )
        // Assert
        val isOngoing: Boolean =
            (actualNotification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        assertTrue(isOngoing)
    }
}
