package com.valiantyan.music801.player

import com.valiantyan.music801.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 测试 MediaQueueManager 基础队列管理
 */
class MediaQueueManagerTest {
    @Test
    fun `设置队列后应返回当前歌曲`() {
        // Arrange - 准备队列起点以验证索引生效
        val inputSongs: List<Song> = listOf(
            Song(
                id = "/storage/music/song1.mp3",
                title = "Song 1",
                artist = "Artist 1",
                album = "Album 1",
                duration = 180000L,
                filePath = "/storage/music/song1.mp3",
                fileSize = 1024L,
                dateAdded = 1700000000000L,
                albumArtPath = null,
            ),
            Song(
                id = "/storage/music/song2.mp3",
                title = "Song 2",
                artist = "Artist 2",
                album = "Album 2",
                duration = 200000L,
                filePath = "/storage/music/song2.mp3",
                fileSize = 2048L,
                dateAdded = 1700000000100L,
                albumArtPath = null,
            ),
        )
        val inputStartIndex: Int = 1
        val manager: MediaQueueManager = MediaQueueManager()
        // Act
        manager.setQueue(songs = inputSongs, startIndex = inputStartIndex)
        val actualCurrentSong: Song? = manager.getCurrentSong()
        // Assert
        assertEquals(inputSongs[1], actualCurrentSong)
        assertTrue(manager.isLastSong())
        assertFalse(manager.isFirstSong())
    }

    @Test
    fun `获取下一首应返回正确歌曲`() {
        // Arrange - 验证当前索引后的下一首可被读取
        val inputSongs: List<Song> = listOf(
            Song(
                id = "/storage/music/song1.mp3",
                title = "Song 1",
                artist = "Artist 1",
                album = "Album 1",
                duration = 180000L,
                filePath = "/storage/music/song1.mp3",
                fileSize = 1024L,
                dateAdded = 1700000000000L,
                albumArtPath = null,
            ),
            Song(
                id = "/storage/music/song2.mp3",
                title = "Song 2",
                artist = "Artist 2",
                album = "Album 2",
                duration = 200000L,
                filePath = "/storage/music/song2.mp3",
                fileSize = 2048L,
                dateAdded = 1700000000100L,
                albumArtPath = null,
            ),
        )
        val manager: MediaQueueManager = MediaQueueManager()
        // Act
        manager.setQueue(songs = inputSongs, startIndex = 0)
        val actualNextSong: Song? = manager.getNextSong()
        // Assert
        assertEquals(inputSongs[1], actualNextSong)
    }

    @Test
    fun `到最后一首时获取下一首应回到第一首`() {
        // Arrange - 验证循环播放时下一首回到开头
        val inputSongs: List<Song> = listOf(
            Song(
                id = "/storage/music/song1.mp3",
                title = "Song 1",
                artist = "Artist 1",
                album = "Album 1",
                duration = 180000L,
                filePath = "/storage/music/song1.mp3",
                fileSize = 1024L,
                dateAdded = 1700000000000L,
                albumArtPath = null,
            ),
            Song(
                id = "/storage/music/song2.mp3",
                title = "Song 2",
                artist = "Artist 2",
                album = "Album 2",
                duration = 200000L,
                filePath = "/storage/music/song2.mp3",
                fileSize = 2048L,
                dateAdded = 1700000000100L,
                albumArtPath = null,
            ),
        )
        val manager: MediaQueueManager = MediaQueueManager()
        // Act
        manager.setQueue(songs = inputSongs, startIndex = 1)
        val actualNextSong: Song? = manager.getNextSong()
        // Assert
        assertEquals(inputSongs[0], actualNextSong)
    }

    @Test
    fun `切换到下一首应更新当前索引`() {
        // Arrange - 验证切歌动作会更新当前歌曲
        val inputSongs: List<Song> = listOf(
            Song(
                id = "/storage/music/song1.mp3",
                title = "Song 1",
                artist = "Artist 1",
                album = "Album 1",
                duration = 180000L,
                filePath = "/storage/music/song1.mp3",
                fileSize = 1024L,
                dateAdded = 1700000000000L,
                albumArtPath = null,
            ),
            Song(
                id = "/storage/music/song2.mp3",
                title = "Song 2",
                artist = "Artist 2",
                album = "Album 2",
                duration = 200000L,
                filePath = "/storage/music/song2.mp3",
                fileSize = 2048L,
                dateAdded = 1700000000100L,
                albumArtPath = null,
            ),
        )
        val manager: MediaQueueManager = MediaQueueManager()
        // Act
        manager.setQueue(songs = inputSongs, startIndex = 0)
        val actualMoved: Boolean = manager.moveToNext()
        val actualCurrentSong: Song? = manager.getCurrentSong()
        // Assert
        assertTrue(actualMoved)
        assertEquals(inputSongs[1], actualCurrentSong)
        assertTrue(manager.isLastSong())
    }

    @Test
    fun `到最后一首时切换下一首应回到第一首`() {
        // Arrange - 验证循环播放时切歌回到开头
        val inputSongs: List<Song> = listOf(
            Song(
                id = "/storage/music/song1.mp3",
                title = "Song 1",
                artist = "Artist 1",
                album = "Album 1",
                duration = 180000L,
                filePath = "/storage/music/song1.mp3",
                fileSize = 1024L,
                dateAdded = 1700000000000L,
                albumArtPath = null,
            ),
            Song(
                id = "/storage/music/song2.mp3",
                title = "Song 2",
                artist = "Artist 2",
                album = "Album 2",
                duration = 200000L,
                filePath = "/storage/music/song2.mp3",
                fileSize = 2048L,
                dateAdded = 1700000000100L,
                albumArtPath = null,
            ),
        )
        val manager: MediaQueueManager = MediaQueueManager()
        // Act
        manager.setQueue(songs = inputSongs, startIndex = 1)
        val actualMoved: Boolean = manager.moveToNext()
        val actualCurrentSong: Song? = manager.getCurrentSong()
        // Assert
        assertTrue(actualMoved)
        assertEquals(inputSongs[0], actualCurrentSong)
        assertTrue(manager.isFirstSong())
    }

    @Test
    fun `切换到上一首应更新当前索引`() {
        // Arrange - 验证向前切歌会更新当前歌曲
        val inputSongs: List<Song> = listOf(
            Song(
                id = "/storage/music/song1.mp3",
                title = "Song 1",
                artist = "Artist 1",
                album = "Album 1",
                duration = 180000L,
                filePath = "/storage/music/song1.mp3",
                fileSize = 1024L,
                dateAdded = 1700000000000L,
                albumArtPath = null,
            ),
            Song(
                id = "/storage/music/song2.mp3",
                title = "Song 2",
                artist = "Artist 2",
                album = "Album 2",
                duration = 200000L,
                filePath = "/storage/music/song2.mp3",
                fileSize = 2048L,
                dateAdded = 1700000000100L,
                albumArtPath = null,
            ),
        )
        val manager: MediaQueueManager = MediaQueueManager()
        // Act
        manager.setQueue(songs = inputSongs, startIndex = 1)
        val actualMoved: Boolean = manager.moveToPrevious()
        val actualCurrentSong: Song? = manager.getCurrentSong()
        // Assert
        assertTrue(actualMoved)
        assertEquals(inputSongs[0], actualCurrentSong)
        assertTrue(manager.isFirstSong())
    }

    @Test
    fun `单首歌曲循环时切歌应保持当前歌曲`() {
        // Arrange - 验证单首歌曲循环切换保持自身
        val inputSongs: List<Song> = listOf(
            Song(
                id = "/storage/music/song1.mp3",
                title = "Song 1",
                artist = "Artist 1",
                album = "Album 1",
                duration = 180000L,
                filePath = "/storage/music/song1.mp3",
                fileSize = 1024L,
                dateAdded = 1700000000000L,
                albumArtPath = null,
            ),
        )
        val manager: MediaQueueManager = MediaQueueManager()
        // Act
        manager.setQueue(songs = inputSongs, startIndex = 0)
        val actualMoveNext: Boolean = manager.moveToNext()
        val actualMovePrevious: Boolean = manager.moveToPrevious()
        val actualCurrentSong: Song? = manager.getCurrentSong()
        // Assert
        assertTrue(actualMoveNext)
        assertTrue(actualMovePrevious)
        assertEquals(inputSongs[0], actualCurrentSong)
    }
}
