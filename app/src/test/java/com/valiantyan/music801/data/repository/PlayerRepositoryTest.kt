package com.valiantyan.music801.data.repository

import com.valiantyan.music801.domain.model.Song
import android.net.Uri
import com.valiantyan.music801.domain.model.PlaybackState
import com.valiantyan.music801.player.MediaPlayerManager
import com.valiantyan.music801.player.MediaQueueManager
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 测试 PlayerRepository 播放接口定义
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [android.os.Build.VERSION_CODES.TIRAMISU])
class PlayerRepositoryTest {
    @Test
    fun `播放控制接口应更新状态`() : Unit = runTest {
        // Arrange - 初始化队列与仓库
        val inputSongs: List<Song> = listOf(
            createSong(id = "/storage/music/song1.mp3", title = "Song 1"),
            createSong(id = "/storage/music/song2.mp3", title = "Song 2"),
        )
        val fakeManager: FakeMediaPlayerManager = FakeMediaPlayerManager()
        val inputRepository: PlayerRepository = PlayerRepositoryImpl(
            mediaQueueManager = MediaQueueManager(),
            mediaPlayerManager = fakeManager,
            dispatcher = Dispatchers.Unconfined,
        )
        // Act - 执行播放控制操作
        inputRepository.setQueue(songs = inputSongs, startIndex = 0)
        inputRepository.play()
        inputRepository.seekTo(position = 1200L)
        inputRepository.pause()
        val actualState: PlaybackState = inputRepository.playbackState.value
        // Assert - 状态应被正确更新
        assertEqualsAny(expected = inputSongs[0], actual = actualState.currentSong)
        assertFalse(actualState.isPlaying)
        assertEqualsLong(expected = 1200L, actual = actualState.position)
        assertEqualsInt(expected = 0, actual = actualState.currentIndex)
        assertEqualsAny(expected = Uri.fromFile(File(inputSongs[0].filePath)), actual = fakeManager.lastUri)
    }

    @Test
    fun `订阅播放状态应收到更新`() : Unit = runTest {
        // Arrange - 初始化队列与仓库
        val inputSongs: List<Song> = listOf(
            createSong(id = "/storage/music/song1.mp3", title = "Song 1"),
        )
        val fakeManager: FakeMediaPlayerManager = FakeMediaPlayerManager()
        val inputRepository: PlayerRepository = PlayerRepositoryImpl(
            mediaQueueManager = MediaQueueManager(),
            mediaPlayerManager = fakeManager,
            dispatcher = Dispatchers.Unconfined,
        )
        inputRepository.setQueue(songs = inputSongs, startIndex = 0)
        // Act - 触发播放并订阅状态
        inputRepository.play()
        val actualState: PlaybackState =
            inputRepository.playbackState.first { state -> state.isPlaying }
        // Assert - 订阅应收到播放状态
        assertTrue(actualState.isPlaying)
    }

    @Test
    fun `释放资源应通知播放器并清空状态`() : Unit = runTest {
        // Arrange - 初始化队列与仓库
        val inputSongs: List<Song> = listOf(
            createSong(id = "/storage/music/song1.mp3", title = "Song 1"),
        )
        val fakeManager: FakeMediaPlayerManager = FakeMediaPlayerManager()
        val inputRepository: PlayerRepository = PlayerRepositoryImpl(
            mediaQueueManager = MediaQueueManager(),
            mediaPlayerManager = fakeManager,
            dispatcher = Dispatchers.Unconfined,
        )
        inputRepository.setQueue(songs = inputSongs, startIndex = 0)
        // Act
        inputRepository.release()
        val actualState: PlaybackState = inputRepository.playbackState.value
        // Assert
        assertTrue(fakeManager.releaseCalled)
        assertEqualsAny(expected = null, actual = actualState.currentSong)
        assertFalse(actualState.isPlaying)
    }

    private fun createSong(
        id: String,
        title: String,
    ): Song {
        return Song(
            id = id,
            title = title,
            artist = "Artist",
            album = null,
            duration = 180000L,
            filePath = id,
            fileSize = 1024L,
            dateAdded = 1700000000000L,
            albumArtPath = null,
        )
    }
}

/**
 * 测试用播放器管理器，用于验证仓库与播放器的交互
 */
private class FakeMediaPlayerManager : MediaPlayerManager {
    /**
     * 播放状态流，用于模拟播放器状态变化
     */
    private val _playbackState: MutableStateFlow<PlaybackState> =
        MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    var lastUri: Uri? = null
    var releaseCalled: Boolean = false

    override fun play(uri: Uri): Unit {
        lastUri = uri
        _playbackState.value = _playbackState.value.copy(isPlaying = true)
    }

    override fun pause(): Unit {
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
    }

    override fun resume(): Unit {
        _playbackState.value = _playbackState.value.copy(isPlaying = true)
    }

    override fun stop(): Unit {
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
    }

    override fun seekTo(position: Long): Unit {
        _playbackState.value = _playbackState.value.copy(position = position)
    }

    override fun playbackState(): kotlinx.coroutines.flow.Flow<PlaybackState> = playbackState

    override fun release(): Unit {
        releaseCalled = true
    }
}

private fun assertEqualsAny(
    expected: Any?,
    actual: Any?,
): Unit {
    // JUnit Java API 不支持命名参数，使用封装函数适配规范
    assertEquals(expected, actual)
}

private fun assertEqualsLong(
    expected: Long,
    actual: Long,
): Unit {
    // JUnit Java API 不支持命名参数，使用封装函数适配规范
    assertEquals(expected, actual)
}

private fun assertEqualsInt(
    expected: Int,
    actual: Int,
): Unit {
    // JUnit Java API 不支持命名参数，使用封装函数适配规范
    assertEquals(expected, actual)
}
