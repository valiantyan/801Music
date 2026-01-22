package com.valiantyan.music801.service

import android.app.Application
import android.app.Notification
import android.os.Build
import com.valiantyan.music801.di.PlayerRepositoryHolder
import com.valiantyan.music801.domain.model.Song
import com.valiantyan.music801.data.repository.PlayerRepository
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.android.controller.ServiceController
import org.robolectric.shadows.ShadowLooper

/**
 * 媒体通知端到端集成测试
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class MediaNotificationIntegrationTest {
    @After
    fun tearDown(): Unit {
        PlayerRepositoryHolder.clear()
        RuntimeEnvironment.setQualifiers("")
    }

    @Test
    fun `播放队列更新后通知应同步当前歌曲`() {
        val context: Application = RuntimeEnvironment.getApplication()
        PlayerRepositoryHolder.clear()
        val controller: ServiceController<MusicPlayerService> = Robolectric.buildService(
            MusicPlayerService::class.java,
        )
        val service: MusicPlayerService = controller.create().get()
        val repository: PlayerRepository = PlayerRepositoryHolder.getOrCreate(
            context = context,
        )
        val inputSong: Song = Song(
            id = "integration-song",
            title = "Integration Title",
            artist = "Integration Artist",
            album = null,
            duration = 1000L,
            filePath = "/tmp/integration.mp3",
            fileSize = 1L,
            dateAdded = 0L,
            albumArtPath = null,
        )
        repository.setQueue(
            songs = listOf(inputSong),
            startIndex = 0,
        )
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        val manager: PlayerNotificationManager? = service.getNotificationManagerForTesting()
        val actualSongId: String? = manager?.getCurrentSongIdForTesting()
        assertEqualsAny(expected = inputSong.id, actual = actualSongId)
        controller.destroy()
    }

    @Test
    fun `主题切换后通知仍可正常构建`() {
        val context: Application = RuntimeEnvironment.getApplication()
        val manager: PlayerNotificationManager = PlayerNotificationManager(context = context)
        val inputSong: Song = Song(
            id = "theme-song",
            title = "Theme Title",
            artist = "Theme Artist",
            album = null,
            duration = 1000L,
            filePath = "/tmp/theme.mp3",
            fileSize = 1L,
            dateAdded = 0L,
            albumArtPath = null,
        )
        val inputNotification: Notification = manager.buildNotification(
            song = inputSong,
            isPlaying = false,
        )
        val inputTitle: CharSequence? = inputNotification.extras.getCharSequence(
            Notification.EXTRA_TITLE,
        )
        assertEqualsAny(expected = inputSong.title, actual = inputTitle)
        RuntimeEnvironment.setQualifiers("+night")
        val updatedNotification: Notification = manager.buildNotification(
            song = inputSong,
            isPlaying = false,
        )
        val actualTitle: CharSequence? = updatedNotification.extras.getCharSequence(
            Notification.EXTRA_TITLE,
        )
        assertEqualsAny(expected = inputSong.title, actual = actualTitle)
        assertTrue(updatedNotification.extras != null)
    }
}

private fun assertEqualsAny(
    expected: Any?,
    actual: Any?,
): Unit {
    // JUnit Java API 不支持命名参数，使用封装函数适配规范
    org.junit.Assert.assertEquals(expected, actual)
}
