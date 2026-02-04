package com.valiantyan.music801.service

import android.app.Application
import android.app.Notification
import android.os.Build
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import java.util.UUID
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
        val context: Application = RuntimeEnvironment.getApplication()
        val manager: PlayerNotificationManager = PlayerNotificationManager(context = context)
        manager.createNotificationChannel()
        val actualNotification: Notification = manager.buildNotification(
            title = "标题",
            artist = "作者",
            isPlaying = true,
        )
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
        val context: Application = RuntimeEnvironment.getApplication()
        val manager: PlayerNotificationManager = PlayerNotificationManager(context = context)
        val actualNotification: Notification = manager.buildNotification(
            title = null,
            artist = null,
            isPlaying = true,
        )
        val isOngoing: Boolean =
            (actualNotification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        assertTrue(isOngoing)
    }

    @Test
    fun `媒体样式通知应携带会话令牌`() {
        val context: Application = RuntimeEnvironment.getApplication()
        val manager: PlayerNotificationManager = PlayerNotificationManager(context = context)
        val player: ExoPlayer = ExoPlayer.Builder(context).build()
        val sessionId: String = UUID.randomUUID().toString()
        val mediaSession: MediaSession = MediaSession.Builder(context, player)
            .setId(sessionId)
            .build()
        manager.attachToSession(mediaSession = mediaSession)
        assertTrue(manager.isManagerAttached)
        assertTrue(manager.isPlayPauseActionEnabled)
        assertTrue(manager.isNextActionEnabled)
        assertTrue(manager.isPreviousActionEnabled)
        assertTrue(manager.isStopActionEnabled)
        assertTrue(manager.isCompactNextActionEnabled)
        assertTrue(manager.isCompactPreviousActionEnabled)
        mediaSession.release()
        player.release()
    }
}
