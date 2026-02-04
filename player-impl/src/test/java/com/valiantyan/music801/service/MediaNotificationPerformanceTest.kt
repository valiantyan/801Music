package com.valiantyan.music801.service

import android.app.Application
import android.os.Build
import android.os.SystemClock
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import java.util.UUID
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * 媒体通知性能测试
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class MediaNotificationPerformanceTest {
    @Test
    fun `通知栏更新耗时应小于200ms`() {
        val context: Application = RuntimeEnvironment.getApplication()
        val manager: PlayerNotificationManager = PlayerNotificationManager(context = context)
        val player: ExoPlayer = ExoPlayer.Builder(context).build()
        val mediaSession: MediaSession = MediaSession.Builder(context, player)
            .setId(UUID.randomUUID().toString())
            .build()
        manager.createNotificationChannel()
        val startTimeMs: Long = SystemClock.elapsedRealtime()
        manager.buildMediaStyleNotification(
            isPlaying = true,
            mediaSession = mediaSession,
        )
        val elapsedMs: Long = SystemClock.elapsedRealtime() - startTimeMs
        assertTrue(elapsedMs < 200L)
        mediaSession.release()
        player.release()
    }
}
