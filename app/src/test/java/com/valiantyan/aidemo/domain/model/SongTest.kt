package com.valiantyan.aidemo.domain.model

import org.junit.Test
import org.junit.Assert.*

/**
 * 测试 Song 数据模型的创建和属性访问
 */
class SongTest {

    @Test
    fun `创建 Song 对象并验证所有属性`() {
        // Given
        val song = Song(
            id = "test-id",
            title = "Test Song",
            artist = "Test Artist",
            album = "Test Album",
            duration = 180000L, // 3分钟
            filePath = "/storage/emulated/0/Music/test.mp3",
            fileSize = 1024000L, // 1MB
            dateAdded = 1234567890L,
            albumArtPath = "/storage/emulated/0/Music/art.jpg"
        )

        // Then
        assertEquals("test-id", song.id)
        assertEquals("Test Song", song.title)
        assertEquals("Test Artist", song.artist)
        assertEquals("Test Album", song.album)
        assertEquals(180000L, song.duration)
        assertEquals("/storage/emulated/0/Music/test.mp3", song.filePath)
        assertEquals(1024000L, song.fileSize)
        assertEquals(1234567890L, song.dateAdded)
        assertEquals("/storage/emulated/0/Music/art.jpg", song.albumArtPath)
    }

    @Test
    fun `创建 Song 对象时 album 和 albumArtPath 可以为空`() {
        // Given
        val song = Song(
            id = "test-id",
            title = "Test Song",
            artist = "Test Artist",
            album = null,
            duration = 180000L,
            filePath = "/storage/emulated/0/Music/test.mp3",
            fileSize = 1024000L,
            dateAdded = 1234567890L,
            albumArtPath = null
        )

        // Then
        assertNull(song.album)
        assertNull(song.albumArtPath)
    }

    @Test
    fun `Song 对象支持数据类相等性比较`() {
        // Given
        val song1 = Song(
            id = "test-id",
            title = "Test Song",
            artist = "Test Artist",
            album = null,
            duration = 180000L,
            filePath = "/storage/emulated/0/Music/test.mp3",
            fileSize = 1024000L,
            dateAdded = 1234567890L,
            albumArtPath = null
        )

        val song2 = Song(
            id = "test-id",
            title = "Test Song",
            artist = "Test Artist",
            album = null,
            duration = 180000L,
            filePath = "/storage/emulated/0/Music/test.mp3",
            fileSize = 1024000L,
            dateAdded = 1234567890L,
            albumArtPath = null
        )

        // Then
        assertEquals(song1, song2)
        assertEquals(song1.hashCode(), song2.hashCode())
    }
}
