package com.valiantyan.music801.data.repository

import com.valiantyan.music801.domain.model.Song
import com.valiantyan.music801.player.MediaQueueManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 测试 PlayerRepository 播放接口定义
 */
class PlayerRepositoryTest {
    @Test
    fun `播放控制接口应更新状态`() : Unit = runTest {
        // Arrange - 初始化队列与仓库
        val inputSongs: List<Song> = listOf(
            createSong(id = "/storage/music/song1.mp3", title = "Song 1"),
            createSong(id = "/storage/music/song2.mp3", title = "Song 2"),
        )
        val inputRepository: PlayerRepository = PlayerRepositoryImpl(
            mediaQueueManager = MediaQueueManager(),
        )
        // Act - 执行播放控制操作
        inputRepository.setQueue(songs = inputSongs, startIndex = 0)
        inputRepository.play()
        inputRepository.seekTo(position = 1200L)
        inputRepository.pause()
        val actualState = inputRepository.playbackState.value
        // Assert - 状态应被正确更新
        assertEquals(inputSongs[0], actualState.currentSong)
        assertFalse(actualState.isPlaying)
        assertEquals(1200L, actualState.position)
        assertEquals(0, actualState.currentIndex)
    }

    @Test
    fun `订阅播放状态应收到更新`() : Unit = runTest {
        // Arrange - 初始化队列与仓库
        val inputSongs: List<Song> = listOf(
            createSong(id = "/storage/music/song1.mp3", title = "Song 1"),
        )
        val inputRepository: PlayerRepository = PlayerRepositoryImpl(
            mediaQueueManager = MediaQueueManager(),
        )
        inputRepository.setQueue(songs = inputSongs, startIndex = 0)
        // Act - 触发播放并订阅状态
        inputRepository.play()
        val actualState = inputRepository.playbackState.first { state -> state.isPlaying }
        // Assert - 订阅应收到播放状态
        assertTrue(actualState.isPlaying)
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
