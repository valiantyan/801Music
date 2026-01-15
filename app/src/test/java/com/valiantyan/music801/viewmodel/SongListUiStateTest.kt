package com.valiantyan.music801.viewmodel

import com.valiantyan.music801.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 测试 SongListUiState 数据类的创建与默认值
 */
class SongListUiStateTest {
    @Test
    fun `创建默认状态时字段应为默认值`() : Unit {
        // Arrange
        val expectedSongs: List<Song> = emptyList()
        // Act
        val actualState: SongListUiState = SongListUiState()
        // Assert
        assertEquals(expectedSongs, actualState.songs)
        assertFalse(actualState.isLoading)
        assertFalse(actualState.isEmpty)
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
        val inputSongs: List<Song> = listOf(inputSong)
        // Act
        val actualState: SongListUiState = SongListUiState(
            songs = inputSongs,
            isLoading = true,
            isEmpty = false,
            error = "加载失败",
        )
        // Assert
        assertEquals(inputSongs, actualState.songs)
        assertTrue(actualState.isLoading)
        assertFalse(actualState.isEmpty)
        assertEquals("加载失败", actualState.error)
    }
}
