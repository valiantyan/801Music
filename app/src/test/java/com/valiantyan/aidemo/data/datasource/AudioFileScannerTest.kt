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

    @Test
    fun `测试扫描进度正确计算和更新 - 验证进度递增`() = runTest {
        // Given: 创建包含多个音频文件的目录
        val musicDir = tempFolder.newFolder("music")
        val files = (1..10).map { i ->
            File(musicDir, "song$i.mp3").apply { createNewFile() }
        }

        // Mock 元数据提取器返回 Song 对象
        val songs = files.mapIndexed { index, file ->
            Song(
                id = file.absolutePath,
                title = "Song ${index + 1}",
                artist = "Artist ${index + 1}",
                album = null,
                duration = 180000L,
                filePath = file.absolutePath,
                fileSize = file.length(),
                dateAdded = file.lastModified(),
                albumArtPath = null
            )
        }

        // Mock 每个文件返回对应的 Song
        songs.forEachIndexed { index, song ->
            val file = files[index]
            whenever(mockMetadataExtractor.extractMetadata(
                eq(file.absolutePath),
                eq(file.length()),
                eq(file.lastModified())
            )).thenReturn(song)
        }

        // When: 扫描目录并收集进度更新
        val progressUpdates = mutableListOf<ScanProgress>()
        scanner.scanDirectory(musicDir.absolutePath) { song ->
            // 收集歌曲
        }.collect { progress ->
            progressUpdates.add(progress)
        }

        // Then: 验证进度正确递增
        assertTrue("应该收到多个进度更新", progressUpdates.size > 1)
        
        // 验证第一个进度更新（开始扫描）
        val firstProgress = progressUpdates.first()
        assertEquals("开始扫描时计数应为 0", 0, firstProgress.scannedCount)
        assertTrue("应该正在扫描", firstProgress.isScanning)
        
        // 验证中间进度更新递增
        val intermediateProgresses = progressUpdates.drop(1).dropLast(1)
        if (intermediateProgresses.isNotEmpty()) {
            var previousCount = 0
            intermediateProgresses.forEach { progress ->
                assertTrue("扫描计数应该递增", progress.scannedCount >= previousCount)
                assertTrue("应该正在扫描", progress.isScanning)
                previousCount = progress.scannedCount
            }
        }
        
        // 验证最后一个进度更新（完成扫描）
        val lastProgress = progressUpdates.last()
        assertEquals("完成扫描时计数应为 10", 10, lastProgress.scannedCount)
        assertEquals("总文件数应该等于已扫描数", 10, lastProgress.totalCount)
        assertFalse("扫描应该已完成", lastProgress.isScanning)
    }

    @Test
    fun `测试大量文件扫描的性能 - 验证扫描大量文件时的性能`() = runTest {
        // Given: 创建包含大量音频文件的目录（100个文件）
        val musicDir = tempFolder.newFolder("music")
        val fileCount = 100
        val files = (1..fileCount).map { i ->
            File(musicDir, "song$i.mp3").apply { createNewFile() }
        }

        // Mock 元数据提取器返回 Song 对象
        val songs = files.mapIndexed { index, file ->
            Song(
                id = file.absolutePath,
                title = "Song ${index + 1}",
                artist = "Artist ${index + 1}",
                album = null,
                duration = 180000L,
                filePath = file.absolutePath,
                fileSize = file.length(),
                dateAdded = file.lastModified(),
                albumArtPath = null
            )
        }

        // Mock 每个文件返回对应的 Song
        songs.forEachIndexed { index, song ->
            val file = files[index]
            whenever(mockMetadataExtractor.extractMetadata(
                eq(file.absolutePath),
                eq(file.length()),
                eq(file.lastModified())
            )).thenReturn(song)
        }

        // When: 扫描目录并测量时间
        val startTime = System.currentTimeMillis()
        val allSongs = mutableListOf<Song>()
        scanner.scanDirectory(musicDir.absolutePath) { song ->
            allSongs.add(song)
        }.collect { progress ->
            // 收集进度更新
        }
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // Then: 验证扫描结果和性能
        assertEquals("应该找到所有文件", fileCount, allSongs.size)
        
        // 性能要求：100个文件应该在合理时间内完成（例如 < 5秒）
        // 注意：实际性能取决于系统，这里主要验证不会超时或卡死
        assertTrue("扫描应该在合理时间内完成（< 30秒）", duration < 30000)
        
        // 验证所有文件都被扫描到
        (1..fileCount).forEach { i ->
            assertTrue("应该包含 song$i.mp3", 
                allSongs.any { it.filePath.contains("song$i.mp3") })
        }
    }

    @Test
    fun `测试扫描进度更新频率 - 验证不会过于频繁更新`() = runTest {
        // Given: 创建包含多个音频文件的目录
        val musicDir = tempFolder.newFolder("music")
        val files = (1..20).map { i ->
            File(musicDir, "song$i.mp3").apply { createNewFile() }
        }

        // Mock 元数据提取器返回 Song 对象
        val songs = files.mapIndexed { index, file ->
            Song(
                id = file.absolutePath,
                title = "Song ${index + 1}",
                artist = "Artist ${index + 1}",
                album = null,
                duration = 180000L,
                filePath = file.absolutePath,
                fileSize = file.length(),
                dateAdded = file.lastModified(),
                albumArtPath = null
            )
        }

        // Mock 每个文件返回对应的 Song
        songs.forEachIndexed { index, song ->
            val file = files[index]
            whenever(mockMetadataExtractor.extractMetadata(
                eq(file.absolutePath),
                eq(file.length()),
                eq(file.lastModified())
            )).thenReturn(song)
        }

        // When: 扫描目录并收集进度更新
        val progressUpdates = mutableListOf<ScanProgress>()
        scanner.scanDirectory(musicDir.absolutePath) { song ->
            // 收集歌曲
        }.collect { progress ->
            progressUpdates.add(progress)
        }

        // Then: 验证进度更新数量合理
        // 应该有开始、中间更新（每个文件一个）、完成更新
        // 至少应该有 2 个更新（开始和完成），最多可能有 22 个（开始 + 20个文件 + 完成）
        assertTrue("应该有进度更新", progressUpdates.size >= 2)
        assertTrue("进度更新数量应该合理（不超过文件数+2）", 
            progressUpdates.size <= files.size + 2)
    }
}
