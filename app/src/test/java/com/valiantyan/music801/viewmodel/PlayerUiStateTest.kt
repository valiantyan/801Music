package com.valiantyan.music801.viewmodel

import com.valiantyan.music801.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 测试 PlayerUiState 数据类的创建与默认值
 */
class PlayerUiStateTest {
    @Test
    fun `创建默认状态时字段应为默认值`() : Unit {
        // Arrange
        val expectedQueue: List<Song> = emptyList()
        // Act
        val actualState: PlayerUiState = PlayerUiState()
        // Assert
        assertNull(actualState.currentSong)
        assertFalse(actualState.isPlaying)
        assertEquals(0L, actualState.position)
        assertEquals(0L, actualState.duration)
        assertEquals(expectedQueue, actualState.queue)
        assertEquals(-1, actualState.currentIndex)
        assertFalse(actualState.isLoading)
        assertNull(actualState.error)
    }

    @Test
    fun `创建包含数据状态时字段应正确赋值`() : Unit {
        // Arrange
        val inputSong: Song = Song(
            id = "/storage/music/song1.mp3",
            title = "Song 1",
            artist = "Artist 1",
            album = "Album 1",
            duration = 180000L,
            filePath = "/storage/music/song1.mp3",
            fileSize = 1024L,
            dateAdded = 1700000000000L,
            albumArtPath = null,
        )
        val inputQueue: List<Song> = listOf(inputSong)
        // Act
        val actualState: PlayerUiState = PlayerUiState(
            currentSong = inputSong,
            isPlaying = true,
            position = 1200L,
            duration = 180000L,
            queue = inputQueue,
            currentIndex = 0,
            isLoading = true,
            error = "加载失败",
        )
        // Assert
        assertEquals(inputSong, actualState.currentSong)
        assertTrue(actualState.isPlaying)
        assertEquals(1200L, actualState.position)
        assertEquals(180000L, actualState.duration)
        assertEquals(inputQueue, actualState.queue)
        assertEquals(0, actualState.currentIndex)
        assertTrue(actualState.isLoading)
        assertEquals("加载失败", actualState.error)
    }
}
