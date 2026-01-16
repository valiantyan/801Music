package com.valiantyan.music801.data

import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import com.valiantyan.music801.data.datasource.AudioFileScanner
import com.valiantyan.music801.data.datasource.MediaMetadataExtractor
import com.valiantyan.music801.data.datasource.MetadataRetriever
import com.valiantyan.music801.data.repository.AudioRepository
import com.valiantyan.music801.domain.model.ScanProgress
import com.valiantyan.music801.domain.model.Song
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AudioScanIntegrationTest {
    @Test
    fun `扫描有效文件时返回歌曲与进度信息`(): Unit = runBlocking {
        val context: Context = getContext()
        val rootDir: File = createRootDir(context = context, name = "scan_valid")
        val subDir: File = createDirectory(parent = rootDir, name = "sub")
        createAudioFile(parent = rootDir, fileName = "song1.mp3")
        createAudioFile(parent = rootDir, fileName = "song2.flac")
        createAudioFile(parent = subDir, fileName = "song3.m4a")
        createNonAudioFile(parent = rootDir, fileName = "readme.txt")
        val repository: AudioRepository = createRepository(failPaths = emptySet())
        val progressUpdates: List<ScanProgress> = repository.scanAudioFiles(rootPath = rootDir.absolutePath).toList()
        val actualSongs: List<Song> = repository.songs.value
        assertEquals(3, actualSongs.size)
        assertTrue(progressUpdates.isNotEmpty())
        val lastProgress: ScanProgress = progressUpdates.last()
        assertEquals(3, lastProgress.scannedCount)
        assertEquals(3, lastProgress.totalCount)
        assertFalse(lastProgress.isScanning)
        val hasPathUpdate: Boolean = progressUpdates.any { progress: ScanProgress ->
            progress.currentPath?.contains("song") == true
        }
        assertTrue(hasPathUpdate)
    }

    @Test
    fun `扫描包含损坏文件时跳过并完成`(): Unit = runBlocking {
        val context: Context = getContext()
        val rootDir: File = createRootDir(context = context, name = "scan_corrupted")
        val okFile1: File = createAudioFile(parent = rootDir, fileName = "ok1.mp3")
        val okFile2: File = createAudioFile(parent = rootDir, fileName = "ok2.aac")
        val corruptedFile: File = createAudioFile(parent = rootDir, fileName = "bad.flac")
        val failPaths: Set<String> = setOf(corruptedFile.absolutePath)
        val repository: AudioRepository = createRepository(failPaths = failPaths)
        val progressUpdates: List<ScanProgress> = repository.scanAudioFiles(rootPath = rootDir.absolutePath).toList()
        val actualSongs: List<Song> = repository.songs.value
        val expectedPaths: MutableSet<String> = mutableSetOf()
        expectedPaths.add(okFile1.absolutePath)
        expectedPaths.add(okFile2.absolutePath)
        assertEquals(2, actualSongs.size)
        assertTrue(actualSongs.all { song: Song -> expectedPaths.contains(song.filePath) })
        assertTrue(progressUpdates.isNotEmpty())
    }

    @Test
    fun `扫描空目录时返回空结果`(): Unit = runBlocking {
        val context: Context = getContext()
        val rootDir: File = createRootDir(context = context, name = "scan_empty")
        val repository: AudioRepository = createRepository(failPaths = emptySet())
        val progressUpdates: List<ScanProgress> = repository.scanAudioFiles(rootPath = rootDir.absolutePath).toList()
        val actualSongs: List<Song> = repository.songs.value
        assertTrue(actualSongs.isEmpty())
        assertEquals(0, progressUpdates.last().scannedCount)
        assertFalse(progressUpdates.last().isScanning)
    }

    @Test
    fun `扫描无效路径时返回未扫描状态`(): Unit = runBlocking {
        val context: Context = getContext()
        val rootDir: File = createRootDir(context = context, name = "scan_invalid_root")
        val missingDir: File = File(rootDir, "missing")
        val repository: AudioRepository = createRepository(failPaths = emptySet())
        val progressUpdates: List<ScanProgress> = repository.scanAudioFiles(rootPath = missingDir.absolutePath).toList()
        val lastProgress: ScanProgress = progressUpdates.last()
        assertEquals(0, lastProgress.scannedCount)
        assertEquals(null, lastProgress.totalCount)
        assertFalse(lastProgress.isScanning)
    }

    @Test
    fun `扫描不可读目录时不会崩溃`(): Unit = runBlocking {
        val context: Context = getContext()
        val rootDir: File = createRootDir(context = context, name = "scan_unreadable")
        val unreadableDir: File = createDirectory(parent = rootDir, name = "no_access")
        createAudioFile(parent = rootDir, fileName = "song1.mp3")
        createAudioFile(parent = unreadableDir, fileName = "secret.mp3")
        unreadableDir.setReadable(false, false)
        try {
            val repository: AudioRepository = createRepository(failPaths = emptySet())
            val progressUpdates: List<ScanProgress> = repository.scanAudioFiles(rootPath = rootDir.absolutePath).toList()
            val actualSongs: List<Song> = repository.songs.value
            assertTrue(progressUpdates.isNotEmpty())
            assertTrue(actualSongs.isNotEmpty())
        } finally {
            unreadableDir.setReadable(true, false)
        }
    }

    @Test
    fun `扫描一千文件时耗时低于阈值`(): Unit = runBlocking {
        val context: Context = getContext()
        val rootDir: File = createRootDir(context = context, name = "scan_perf")
        createAudioFiles(parent = rootDir, count = 1000)
        val repository: AudioRepository = createRepository(failPaths = emptySet())
        val startTimeMs: Long = SystemClock.elapsedRealtime()
        repository.scanAudioFiles(rootPath = rootDir.absolutePath).toList()
        val endTimeMs: Long = SystemClock.elapsedRealtime()
        val durationMs: Long = endTimeMs - startTimeMs
        val actualSongs: List<Song> = repository.songs.value
        assertEquals(1000, actualSongs.size)
        assertTrue(durationMs < 30000L)
    }

    private fun getContext(): Context = ApplicationProvider.getApplicationContext()

    private fun createRepository(failPaths: Set<String>): AudioRepository {
        val metadataExtractor: MediaMetadataExtractor = createMetadataExtractor(failPaths = failPaths)
        val scanner: AudioFileScanner = AudioFileScanner(metadataExtractor)
        return AudioRepository(scanner)
    }

    private fun createMetadataExtractor(failPaths: Set<String>): MediaMetadataExtractor {
        return MediaMetadataExtractor(metadataRetrieverFactory = { FakeMetadataRetriever(failPaths = failPaths) })
    }

    private fun createRootDir(context: Context, name: String): File {
        val rootDir: File = File(context.cacheDir, name)
        if (rootDir.exists()) {
            rootDir.deleteRecursively()
        }
        val created: Boolean = rootDir.mkdirs()
        assertTrue(created || rootDir.exists())
        return rootDir
    }

    private fun createDirectory(parent: File, name: String): File {
        val dir: File = File(parent, name)
        val created: Boolean = dir.mkdirs()
        assertTrue(created || dir.exists())
        return dir
    }

    private fun createAudioFile(parent: File, fileName: String): File {
        val file: File = File(parent, fileName)
        file.writeText(text = "data")
        return file
    }

    private fun createNonAudioFile(parent: File, fileName: String): File {
        val file: File = File(parent, fileName)
        file.writeText(text = "not_audio")
        return file
    }

    private fun createAudioFiles(parent: File, count: Int): List<File> {
        val files: MutableList<File> = mutableListOf()
        for (index: Int in 1..count) {
            files.add(createAudioFile(parent = parent, fileName = "song$index.mp3"))
        }
        return files
    }
}

private class FakeMetadataRetriever(
    private val failPaths: Set<String>,
) : MetadataRetriever {
    private var dataSource: String? = null

    override fun setDataSource(path: String) {
        if (failPaths.contains(path)) {
            throw IllegalStateException("模拟损坏文件")
        }
        dataSource = path
    }

    override fun extractMetadata(key: Int): String? {
        val currentPath: String = dataSource ?: return null
        return when (key) {
            android.media.MediaMetadataRetriever.METADATA_KEY_TITLE -> File(currentPath).nameWithoutExtension
            android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST -> "测试艺术家"
            android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM -> "测试专辑"
            android.media.MediaMetadataRetriever.METADATA_KEY_DURATION -> "120000"
            else -> null
        }
    }

    override fun release() {
        dataSource = null
    }
}
