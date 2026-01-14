package com.valiantyan.aidemo.data.datasource

import com.valiantyan.aidemo.data.util.AudioFormatRecognizer
import com.valiantyan.aidemo.domain.model.ScanProgress
import com.valiantyan.aidemo.domain.model.Song
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.*
import java.io.File

/**
 * 测试 AudioFileScanner 文件扫描功能
 */
class AudioFileScannerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var scanner: AudioFileScanner
    private lateinit var mockMetadataExtractor: MediaMetadataExtractor

    @Before
    fun setUp() {
        mockMetadataExtractor = mock()
        scanner = AudioFileScanner(mockMetadataExtractor)
    }

    @Test
    fun `测试单目录扫描 - 扫描单个目录中的音频文件`() = runTest {
        // Given: 创建测试目录和音频文件
        val testDir = tempFolder.newFolder("music")
        val audioFile1 = File(testDir, "song1.mp3").apply { createNewFile() }
        val audioFile2 = File(testDir, "song2.aac").apply { createNewFile() }
        val nonAudioFile = File(testDir, "document.pdf").apply { createNewFile() }

        // Mock 元数据提取器返回 Song 对象
        val song1 = Song(
            id = audioFile1.absolutePath,
            title = "Song 1",
            artist = "Artist 1",
            album = null,
            duration = 180000L,
            filePath = audioFile1.absolutePath,
            fileSize = audioFile1.length(),
            dateAdded = audioFile1.lastModified(),
            albumArtPath = null
        )
        val song2 = Song(
            id = audioFile2.absolutePath,
            title = "Song 2",
            artist = "Artist 2",
            album = null,
            duration = 200000L,
            filePath = audioFile2.absolutePath,
            fileSize = audioFile2.length(),
            dateAdded = audioFile2.lastModified(),
            albumArtPath = null
        )

        whenever(mockMetadataExtractor.extractMetadata(
            eq(audioFile1.absolutePath),
            eq(audioFile1.length()),
            eq(audioFile1.lastModified())
        )).thenReturn(song1)

        whenever(mockMetadataExtractor.extractMetadata(
            eq(audioFile2.absolutePath),
            eq(audioFile2.length()),
            eq(audioFile2.lastModified())
        )).thenReturn(song2)

        // When: 扫描目录并收集所有歌曲和进度
        val allSongs = mutableListOf<Song>()
        scanner.scanDirectory(testDir.absolutePath) { song ->
            allSongs.add(song)
        }.collect { progress ->
            // 收集进度更新（用于触发 Flow 执行）
        }

        // Then: 应该只找到音频文件
        assertEquals("应该找到 2 个音频文件", 2, allSongs.size)
        assertTrue("应该包含 song1.mp3", allSongs.any { it.filePath.contains("song1.mp3") })
        assertTrue("应该包含 song2.aac", allSongs.any { it.filePath.contains("song2.aac") })
        assertFalse("不应该包含 PDF 文件", allSongs.any { it.filePath.contains("document.pdf") })
    }

    @Test
    fun `测试递归子目录扫描 - 扫描包含子目录的目录树`() = runTest {
        // Given: 创建目录结构
        // music/
        //   ├── song1.mp3
        //   └── subfolder/
        //       ├── song2.aac
        //       └── nested/
        //           └── song3.flac
        val musicDir = tempFolder.newFolder("music")
        val subfolder = File(musicDir, "subfolder").apply { mkdirs() }
        val nested = File(subfolder, "nested").apply { mkdirs() }

        val file1 = File(musicDir, "song1.mp3").apply { createNewFile() }
        val file2 = File(subfolder, "song2.aac").apply { createNewFile() }
        val file3 = File(nested, "song3.flac").apply { createNewFile() }
        File(musicDir, "readme.txt").apply { createNewFile() } // 非音频文件

        // Mock 元数据提取器返回 Song 对象
        val song1 = Song(
            id = file1.absolutePath,
            title = "Song 1",
            artist = "Artist 1",
            album = null,
            duration = 180000L,
            filePath = file1.absolutePath,
            fileSize = file1.length(),
            dateAdded = file1.lastModified(),
            albumArtPath = null
        )
        val song2 = Song(
            id = file2.absolutePath,
            title = "Song 2",
            artist = "Artist 2",
            album = null,
            duration = 200000L,
            filePath = file2.absolutePath,
            fileSize = file2.length(),
            dateAdded = file2.lastModified(),
            albumArtPath = null
        )
        val song3 = Song(
            id = file3.absolutePath,
            title = "Song 3",
            artist = "Artist 3",
            album = null,
            duration = 220000L,
            filePath = file3.absolutePath,
            fileSize = file3.length(),
            dateAdded = file3.lastModified(),
            albumArtPath = null
        )

        whenever(mockMetadataExtractor.extractMetadata(
            eq(file1.absolutePath),
            eq(file1.length()),
            eq(file1.lastModified())
        )).thenReturn(song1)

        whenever(mockMetadataExtractor.extractMetadata(
            eq(file2.absolutePath),
            eq(file2.length()),
            eq(file2.lastModified())
        )).thenReturn(song2)

        whenever(mockMetadataExtractor.extractMetadata(
            eq(file3.absolutePath),
            eq(file3.length()),
            eq(file3.lastModified())
        )).thenReturn(song3)

        // When: 递归扫描目录并收集所有歌曲和进度
        val allSongs = mutableListOf<Song>()
        scanner.scanDirectory(musicDir.absolutePath) { song ->
            allSongs.add(song)
        }.collect { progress ->
            // 收集进度更新（用于触发 Flow 执行）
        }

        // Then: 应该找到所有层级的音频文件
        assertEquals("应该找到 3 个音频文件", 3, allSongs.size)
        assertTrue("应该包含 song1.mp3", allSongs.any { it.filePath.contains("song1.mp3") })
        assertTrue("应该包含 song2.aac", allSongs.any { it.filePath.contains("song2.aac") })
        assertTrue("应该包含 song3.flac", allSongs.any { it.filePath.contains("song3.flac") })
        assertFalse("不应该包含文本文件", allSongs.any { it.filePath.contains("readme.txt") })
    }

    @Test
    fun `测试扫描进度 Flow 的发送 - 验证进度更新事件`() = runTest {
        // Given: 创建包含多个音频文件的目录
        val musicDir = tempFolder.newFolder("music")
        val file1 = File(musicDir, "song1.mp3").apply { createNewFile() }
        val file2 = File(musicDir, "song2.aac").apply { createNewFile() }
        val file3 = File(musicDir, "song3.flac").apply { createNewFile() }

        // Mock 元数据提取器返回 Song 对象
        val song1 = Song(
            id = file1.absolutePath,
            title = "Song 1",
            artist = "Artist 1",
            album = null,
            duration = 180000L,
            filePath = file1.absolutePath,
            fileSize = file1.length(),
            dateAdded = file1.lastModified(),
            albumArtPath = null
        )
        val song2 = Song(
            id = file2.absolutePath,
            title = "Song 2",
            artist = "Artist 2",
            album = null,
            duration = 200000L,
            filePath = file2.absolutePath,
            fileSize = file2.length(),
            dateAdded = file2.lastModified(),
            albumArtPath = null
        )
        val song3 = Song(
            id = file3.absolutePath,
            title = "Song 3",
            artist = "Artist 3",
            album = null,
            duration = 220000L,
            filePath = file3.absolutePath,
            fileSize = file3.length(),
            dateAdded = file3.lastModified(),
            albumArtPath = null
        )

        whenever(mockMetadataExtractor.extractMetadata(any(), any(), any()))
            .thenReturn(song1, song2, song3)

        // When: 扫描目录并收集进度更新
        val progressUpdates = mutableListOf<ScanProgress>()
        val songs = mutableListOf<Song>()
        scanner.scanDirectory(musicDir.absolutePath) { song ->
            songs.add(song)
        }.collect { progress ->
            progressUpdates.add(progress)
        }

        // Then: 应该收到进度更新
        assertTrue("应该收到至少一个进度更新", progressUpdates.isNotEmpty())
        
        // 验证最后一个进度更新应该是完成状态
        val lastProgress = progressUpdates.last()
        assertEquals("已扫描文件数应该匹配", 3, lastProgress.scannedCount)
        assertFalse("扫描应该已完成", lastProgress.isScanning)
        assertEquals("应该找到 3 个音频文件", 3, songs.size)
    }

    @Test
    fun `测试空目录扫描`() = runTest {
        // Given: 创建空目录
        val emptyDir = tempFolder.newFolder("empty")

        // When: 扫描空目录
        val songs = mutableListOf<Song>()
        scanner.scanDirectory(emptyDir.absolutePath) { song ->
            songs.add(song)
        }.collect { progress ->
            // 收集进度更新
        }

        // Then: 应该返回空列表
        assertTrue("空目录应该返回空列表", songs.isEmpty())
    }

    @Test
    fun `测试只包含非音频文件的目录`() = runTest {
        // Given: 创建只包含非音频文件的目录
        val dir = tempFolder.newFolder("documents")
        File(dir, "readme.txt").apply { createNewFile() }
        File(dir, "image.jpg").apply { createNewFile() }
        File(dir, "video.mp4").apply { createNewFile() }

        // When: 扫描目录
        val songs = mutableListOf<Song>()
        scanner.scanDirectory(dir.absolutePath) { song ->
            songs.add(song)
        }.collect { progress ->
            // 收集进度更新
        }

        // Then: 应该返回空列表
        assertTrue("只包含非音频文件的目录应该返回空列表", songs.isEmpty())
    }

    @Test
    fun `测试扫描进度包含当前路径信息`() = runTest {
        // Given: 创建包含音频文件的目录
        val musicDir = tempFolder.newFolder("music")
        val file1 = File(musicDir, "song1.mp3").apply { createNewFile() }
        val file2 = File(musicDir, "song2.aac").apply { createNewFile() }

        // Mock 元数据提取器返回 Song 对象
        val song1 = Song(
            id = file1.absolutePath,
            title = "Song 1",
            artist = "Artist 1",
            album = null,
            duration = 180000L,
            filePath = file1.absolutePath,
            fileSize = file1.length(),
            dateAdded = file1.lastModified(),
            albumArtPath = null
        )
        val song2 = Song(
            id = file2.absolutePath,
            title = "Song 2",
            artist = "Artist 2",
            album = null,
            duration = 200000L,
            filePath = file2.absolutePath,
            fileSize = file2.length(),
            dateAdded = file2.lastModified(),
            albumArtPath = null
        )

        whenever(mockMetadataExtractor.extractMetadata(any(), any(), any()))
            .thenReturn(song1, song2)

        // When: 扫描目录并收集进度更新
        val progressUpdates = mutableListOf<ScanProgress>()
        scanner.scanDirectory(musicDir.absolutePath) { song ->
            // 收集歌曲
        }.collect { progress ->
            progressUpdates.add(progress)
        }

        // Then: 进度更新应该包含当前扫描路径信息
        val progressWithPath = progressUpdates.find { it.currentPath != null }
        assertNotNull("至少应该有一个进度更新包含当前路径", progressWithPath)
        assertTrue("当前路径应该包含目录路径", 
            progressWithPath?.currentPath?.contains(musicDir.name) == true)
    }
}
