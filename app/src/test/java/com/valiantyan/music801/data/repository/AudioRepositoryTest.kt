package com.valiantyan.music801.data.repository

import com.valiantyan.music801.data.datasource.AudioFileScanner
import com.valiantyan.music801.data.datasource.MediaMetadataExtractor
import com.valiantyan.music801.domain.model.ScanProgress
import com.valiantyan.music801.domain.model.Song
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
 * 测试 AudioRepository 扫描接口
 */
class AudioRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var repository: AudioRepository
    private lateinit var mockScanner: AudioFileScanner
    private lateinit var mockMetadataExtractor: MediaMetadataExtractor

    @Before
    fun setUp() {
        mockMetadataExtractor = mock()
        mockScanner = mock()
        repository = AudioRepository(mockScanner)
    }

    @Test
    fun `测试扫描方法调用 - 验证调用 AudioFileScanner`() = runTest {
        // Given: 创建测试目录
        val testDir = tempFolder.newFolder("music")
        val file1 = File(testDir, "song1.mp3").apply { createNewFile() }
        val file2 = File(testDir, "song2.aac").apply { createNewFile() }

        // Mock AudioFileScanner 返回歌曲
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

        // Mock scanDirectory 返回进度流
        val progressFlow = kotlinx.coroutines.flow.flow {
            emit(ScanProgress(0, null, testDir.absolutePath, true))
            // 模拟找到歌曲
            emit(ScanProgress(1, null, file1.absolutePath, true))
            emit(ScanProgress(2, null, file2.absolutePath, true))
            emit(ScanProgress(2, 2, null, false))
        }

        whenever(mockScanner.scanDirectory(eq(testDir.absolutePath), any()))
            .thenReturn(progressFlow)

        // When: 调用扫描方法
        val songs = mutableListOf<Song>()
        repository.scanAudioFiles(testDir.absolutePath) { song ->
            songs.add(song)
        }.collect { progress ->
            // 收集进度更新
        }

        // Then: 验证 AudioFileScanner 被调用
        verify(mockScanner, times(1)).scanDirectory(eq(testDir.absolutePath), any())
    }

    @Test
    fun `测试扫描结果的 Flow 订阅 - 验证可以订阅歌曲列表`() = runTest {
        // Given: 创建测试目录和文件
        val testDir = tempFolder.newFolder("music")
        val file1 = File(testDir, "song1.mp3").apply { createNewFile() }
        val file2 = File(testDir, "song2.aac").apply { createNewFile() }

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

        // Mock scanDirectory 返回进度流，并在回调中传递歌曲
        var capturedCallback: ((Song) -> Unit)? = null
        val progressFlow = kotlinx.coroutines.flow.flow {
            emit(ScanProgress(0, null, testDir.absolutePath, true))
            // 在第一个进度更新后调用回调
            capturedCallback?.invoke(song1)
            emit(ScanProgress(1, null, file1.absolutePath, true))
            // 在第二个进度更新后调用回调
            capturedCallback?.invoke(song2)
            emit(ScanProgress(2, null, file2.absolutePath, true))
            emit(ScanProgress(2, 2, null, false))
        }

        whenever(mockScanner.scanDirectory(eq(testDir.absolutePath), any()))
            .thenAnswer { invocation ->
                capturedCallback = invocation.getArgument(1)
                progressFlow
            }

        // When: 订阅扫描结果
        val allSongs = mutableListOf<Song>()
        repository.scanAudioFiles(testDir.absolutePath) { song ->
            allSongs.add(song)
        }.collect { progress ->
            // 收集进度更新
        }

        // Then: 验证可以订阅到歌曲
        assertEquals("应该找到 2 个歌曲", 2, allSongs.size)
    }

    @Test
    fun `测试扫描结果缓存 - 验证缓存机制`() = runTest {
        // Given: 创建测试目录和文件
        val testDir = tempFolder.newFolder("music")
        val file1 = File(testDir, "song1.mp3").apply { createNewFile() }

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

        // Mock scanDirectory 返回进度流
        var capturedCallback: ((Song) -> Unit)? = null
        val progressFlow = kotlinx.coroutines.flow.flow {
            emit(ScanProgress(0, null, testDir.absolutePath, true))
            // 在进度更新时调用回调
            capturedCallback?.invoke(song1)
            emit(ScanProgress(1, null, file1.absolutePath, true))
            emit(ScanProgress(1, 1, null, false))
        }

        whenever(mockScanner.scanDirectory(eq(testDir.absolutePath), any()))
            .thenAnswer { invocation ->
                capturedCallback = invocation.getArgument(1)
                progressFlow
            }

        // When: 第一次扫描
        repository.scanAudioFiles(testDir.absolutePath) { song ->
            // 收集歌曲
        }.collect { progress ->
            // 收集进度更新
        }

        // Then: 验证缓存中有数据
        val cachedSongs = repository.getAllSongs().first()
        assertEquals("缓存应该包含扫描到的歌曲", 1, cachedSongs.size)
        assertEquals("缓存的歌曲应该匹配", song1.id, cachedSongs.first().id)

        // When: 再次获取（不重新扫描）
        val secondGetSongs = repository.getAllSongs().first()

        // Then: 应该从缓存获取，不需要重新扫描
        assertEquals("应该从缓存获取相同的数据", 1, secondGetSongs.size)
        assertEquals("缓存的歌曲应该匹配", song1.id, secondGetSongs.first().id)
        
        // 验证 AudioFileScanner 只被调用一次（第一次扫描时）
        verify(mockScanner, times(1)).scanDirectory(any(), any())
    }

    @Test
    fun `测试获取所有歌曲 - 验证 Flow 订阅`() = runTest {
        // Given: 先扫描一些歌曲
        val testDir = tempFolder.newFolder("music")
        val file1 = File(testDir, "song1.mp3").apply { createNewFile() }
        val file2 = File(testDir, "song2.aac").apply { createNewFile() }

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

        var capturedCallback: ((Song) -> Unit)? = null
        val progressFlow = kotlinx.coroutines.flow.flow {
            emit(ScanProgress(0, null, testDir.absolutePath, true))
            // 在进度更新时调用回调
            capturedCallback?.invoke(song1)
            emit(ScanProgress(1, null, file1.absolutePath, true))
            capturedCallback?.invoke(song2)
            emit(ScanProgress(2, null, file2.absolutePath, true))
            emit(ScanProgress(2, 2, null, false))
        }

        whenever(mockScanner.scanDirectory(eq(testDir.absolutePath), any()))
            .thenAnswer { invocation ->
                capturedCallback = invocation.getArgument(1)
                progressFlow
            }

        // 先执行扫描
        repository.scanAudioFiles(testDir.absolutePath) { song ->
            // 收集歌曲
        }.collect { progress ->
            // 收集进度更新
        }

        // When: 订阅 getAllSongs Flow
        val allSongs = repository.getAllSongs().first()

        // Then: 应该获取到所有歌曲
        assertEquals("应该获取到 2 个歌曲", 2, allSongs.size)
        assertTrue("应该包含 song1", allSongs.any { it.id == song1.id })
        assertTrue("应该包含 song2", allSongs.any { it.id == song2.id })
    }
}
