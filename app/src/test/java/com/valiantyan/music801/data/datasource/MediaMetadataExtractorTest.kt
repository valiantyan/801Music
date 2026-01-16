package com.valiantyan.music801.data.datasource

import android.media.MediaMetadataRetriever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * 测试 MediaMetadataExtractor 元数据提取逻辑
 */
class MediaMetadataExtractorTest {

    private lateinit var extractor: MediaMetadataExtractor
    private lateinit var mockRetriever: MetadataRetriever

    @Before
    fun setUp() {
        mockRetriever = mock()
        extractor = MediaMetadataExtractor { mockRetriever }
    }

    @Test
    fun `提取完整元数据 - 标题、艺术家、专辑、时长`() {
        // Given
        val filePath = "/storage/emulated/0/Music/song.mp3"
        val fileSize = 1024000L // 1MB
        val dateAdded = System.currentTimeMillis()

        // Mock MediaMetadataRetriever 返回完整元数据
        whenever(mockRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE))
            .thenReturn("Test Song")
        whenever(mockRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST))
            .thenReturn("Test Artist")
        whenever(mockRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM))
            .thenReturn("Test Album")
        whenever(mockRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))
            .thenReturn("180000") // 3分钟，单位：毫秒

        // When
        val song = extractor.extractMetadata(filePath, fileSize, dateAdded)

        // Then
        assertNotNull("应该成功提取元数据", song)
        assertEquals("文件路径应该匹配", filePath, song?.filePath)
        assertEquals("文件大小应该匹配", fileSize, song?.fileSize)
        assertEquals("添加时间应该匹配", dateAdded, song?.dateAdded)
        assertEquals("标题应该匹配", "Test Song", song?.title)
        assertEquals("艺术家应该匹配", "Test Artist", song?.artist)
        assertEquals("专辑应该匹配", "Test Album", song?.album)
        assertEquals("时长应该匹配", 180000L, song?.duration)
        assertNull("封面应该为 null", song?.albumArtPath)

        // 验证调用了 setDataSource 和 release
        verify(mockRetriever).setDataSource(filePath)
        verify(mockRetriever).release()
    }

    @Test
    fun `处理元数据缺失 - 使用文件名作为标题`() {
        // Given
        val filePath = "/storage/emulated/0/Music/Unknown Song.mp3"
        val fileSize = 1024000L
        val dateAdded = System.currentTimeMillis()

        // Mock MediaMetadataRetriever 返回空或 null 的元数据
        whenever(mockRetriever.extractMetadata(any())).thenReturn(null)

        // When
        val song = extractor.extractMetadata(filePath, fileSize, dateAdded)

        // Then
        assertNotNull("即使元数据缺失也应该返回 Song 对象", song)
        assertEquals("标题应该使用文件名（去除扩展名）", "Unknown Song", song?.title)
        assertEquals("艺术家应该有默认值", "未知艺术家", song?.artist)
        assertNull("专辑应该为 null", song?.album)
        assertEquals("时长应该为 0", 0L, song?.duration)
    }

    @Test
    fun `处理部分元数据缺失 - 只有标题和时长`() {
        // Given
        val filePath = "/storage/emulated/0/Music/track.mp3"
        val fileSize = 1024000L
        val dateAdded = System.currentTimeMillis()

        // Mock 只有部分元数据
        whenever(mockRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE))
            .thenReturn("Track Title")
        whenever(mockRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST))
            .thenReturn(null)
        whenever(mockRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM))
            .thenReturn(null)
        whenever(mockRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))
            .thenReturn("120000") // 2分钟

        // When
        val song = extractor.extractMetadata(filePath, fileSize, dateAdded)

        // Then
        assertNotNull("应该返回 Song 对象", song)
        assertEquals("标题应该匹配", "Track Title", song?.title)
        assertEquals("艺术家应该有默认值", "未知艺术家", song?.artist)
        assertNull("专辑应该为 null", song?.album)
        assertEquals("时长应该匹配", 120000L, song?.duration)
    }

    @Test
    fun `处理空字符串元数据 - 使用默认值`() {
        // Given
        val filePath = "/storage/emulated/0/Music/Empty Metadata.mp3"
        val fileSize = 1024000L
        val dateAdded = System.currentTimeMillis()

        // Mock 返回空字符串
        whenever(mockRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE))
            .thenReturn("")
        whenever(mockRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST))
            .thenReturn("   ") // 空白字符
        whenever(mockRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM))
            .thenReturn("")
        whenever(mockRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))
            .thenReturn("")

        // When
        val song = extractor.extractMetadata(filePath, fileSize, dateAdded)

        // Then
        assertNotNull("应该返回 Song 对象", song)
        assertEquals("标题应该使用文件名", "Empty Metadata", song?.title)
        assertEquals("艺术家应该有默认值", "未知艺术家", song?.artist)
        assertNull("专辑应该为 null", song?.album)
        assertEquals("时长应该为 0", 0L, song?.duration)
    }

    @Test
    fun `处理不同音频格式的元数据提取 - MP3`() {
        // Given
        val filePath = "/storage/emulated/0/Music/song.mp3"
        val fileSize = 1024000L
        val dateAdded = System.currentTimeMillis()

        whenever(mockRetriever.extractMetadata(any())).thenReturn(null)

        // When
        val song = extractor.extractMetadata(filePath, fileSize, dateAdded)

        // Then
        assertNotNull("MP3 格式应该能提取元数据", song)
        assertEquals("文件路径应该匹配", filePath, song?.filePath)
    }

    @Test
    fun `处理不同音频格式的元数据提取 - AAC`() {
        // Given
        val filePath = "/storage/emulated/0/Music/song.aac"
        val fileSize = 1024000L
        val dateAdded = System.currentTimeMillis()

        whenever(mockRetriever.extractMetadata(any())).thenReturn(null)

        // When
        val song = extractor.extractMetadata(filePath, fileSize, dateAdded)

        // Then
        assertNotNull("AAC 格式应该能提取元数据", song)
        assertEquals("文件路径应该匹配", filePath, song?.filePath)
    }

    @Test
    fun `处理不同音频格式的元数据提取 - FLAC`() {
        // Given
        val filePath = "/storage/emulated/0/Music/song.flac"
        val fileSize = 1024000L
        val dateAdded = System.currentTimeMillis()

        whenever(mockRetriever.extractMetadata(any())).thenReturn(null)

        // When
        val song = extractor.extractMetadata(filePath, fileSize, dateAdded)

        // Then
        assertNotNull("FLAC 格式应该能提取元数据", song)
        assertEquals("文件路径应该匹配", filePath, song?.filePath)
    }

    @Test
    fun `处理文件不存在或无法读取的情况`() {
        // Given
        val filePath = "/storage/emulated/0/Music/nonexistent.mp3"
        val fileSize = 0L
        val dateAdded = System.currentTimeMillis()

        // Mock setDataSource 抛出异常
        doThrow(RuntimeException("File not found"))
            .whenever(mockRetriever).setDataSource(filePath)

        // When
        val song = extractor.extractMetadata(filePath, fileSize, dateAdded)

        // Then
        assertNull("文件不存在时应该返回 null", song)
        // 验证即使异常也调用了 release
        verify(mockRetriever).release()
    }

    @Test
    fun `验证 Song 对象的 id 字段使用文件路径`() {
        // Given
        val filePath = "/storage/emulated/0/Music/song.mp3"
        val fileSize = 1024000L
        val dateAdded = System.currentTimeMillis()

        whenever(mockRetriever.extractMetadata(any())).thenReturn(null)

        // When
        val song = extractor.extractMetadata(filePath, fileSize, dateAdded)

        // Then
        assertNotNull("应该返回 Song 对象", song)
        assertEquals("id 应该等于文件路径", filePath, song?.id)
    }

    @Test
    fun `处理文件名包含特殊字符的情况`() {
        // Given
        val filePath = "/storage/emulated/0/Music/My Song (2024) [Remix].mp3"
        val fileSize = 1024000L
        val dateAdded = System.currentTimeMillis()

        whenever(mockRetriever.extractMetadata(any())).thenReturn(null)

        // When
        val song = extractor.extractMetadata(filePath, fileSize, dateAdded)

        // Then
        assertNotNull("应该能处理特殊字符", song)
        assertEquals(
            "标题应该正确提取文件名（去除扩展名）",
            "My Song (2024) [Remix]",
            song?.title,
        )
    }

    @Test
    fun `处理无效的时长字符串`() {
        // Given
        val filePath = "/storage/emulated/0/Music/song.mp3"
        val fileSize = 1024000L
        val dateAdded = System.currentTimeMillis()

        whenever(mockRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE))
            .thenReturn("Test Song")
        whenever(mockRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))
            .thenReturn("invalid") // 无效的时长字符串

        // When
        val song = extractor.extractMetadata(filePath, fileSize, dateAdded)

        // Then
        assertNotNull("应该返回 Song 对象", song)
        assertEquals("无效时长应该转换为 0", 0L, song?.duration)
    }

    @Test
    fun `封面路径默认为 null`() {
        // Given
        val filePath = "/storage/emulated/0/Music/song.mp3"
        val fileSize = 1024000L
        val dateAdded = System.currentTimeMillis()

        whenever(mockRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE))
            .thenReturn("Test Song")

        // When
        val song = extractor.extractMetadata(filePath, fileSize, dateAdded)

        // Then
        assertNotNull("应该返回 Song 对象", song)
        assertNull("封面路径应该为 null（当前版本不提取封面）", song?.albumArtPath)
    }
}
