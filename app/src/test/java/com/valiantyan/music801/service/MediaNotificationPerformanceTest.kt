package com.valiantyan.music801.service

import android.app.Application
import android.os.Build
import android.os.SystemClock
import androidx.media3.session.MediaSession
import com.valiantyan.music801.domain.model.Song
import com.valiantyan.music801.player.Media3PlayerManager
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
        val playerManager: Media3PlayerManager = Media3PlayerManager(context = context)
        val mediaSession: MediaSession = MediaSession.Builder(context, playerManager.exoPlayer)
            .setId(UUID.randomUUID().toString())
            .build()
        val inputSong: Song = Song(
            id = "perf-id",
            title = "perf-title",
            artist = "perf-artist",
            album = "perf-album",
            duration = 1000L,
            filePath = "/tmp/perf.mp3",
            fileSize = 1L,
            dateAdded = 0L,
            albumArtPath = null,
        )
        manager.createNotificationChannel()
        val startTimeMs: Long = SystemClock.elapsedRealtime()
        manager.buildMediaStyleNotification(
            song = inputSong,
            isPlaying = true,
            mediaSession = mediaSession,
        )
        val elapsedMs: Long = SystemClock.elapsedRealtime() - startTimeMs
        assertTrue(elapsedMs < 200L)
        mediaSession.release()
        playerManager.release()
    }
}
