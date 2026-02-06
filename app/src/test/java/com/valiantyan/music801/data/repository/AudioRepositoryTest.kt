package com.valiantyan.music801.data.repository

import com.valiantyan.music801.data.datasource.AudioFileScanner
import com.valiantyan.music801.data.local.dao.LibrarySyncStateDao
import com.valiantyan.music801.data.local.dao.SongDao
import com.valiantyan.music801.data.local.entity.SongEntity
import com.valiantyan.music801.domain.model.InitialScanDecision
import com.valiantyan.music801.domain.model.ScanMode
import com.valiantyan.music801.domain.model.ScanProgress
import com.valiantyan.music801.domain.model.Song
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AudioRepositoryTest {
    private lateinit var repository: AudioRepository
    private lateinit var mockScanner: AudioFileScanner
    private lateinit var mockSongDao: SongDao
    private lateinit var mockSyncDao: LibrarySyncStateDao
    private val testDispatcher: CoroutineDispatcher = Dispatchers.Unconfined

    @Before
    fun setUp() {
        mockScanner = mock()
        mockSongDao = mock()
        mockSyncDao = mock()
        repository = AudioRepository(
            audioFileScanner = mockScanner,
            songDao = mockSongDao,
            librarySyncStateDao = mockSyncDao,
            ioDispatcher = testDispatcher,
            rootPathProvider = { ROOT_PATH },
            nowProvider = { FIXED_TIME },
        )
    }

    @Test
    fun `库为空时返回首扫决策`() = runTest {
        whenever(mockSongDao.countSongs()).thenReturn(0)
        val actualDecision: InitialScanDecision = repository.ensureInitialScanIfNeeded()
        assertEquals(InitialScanDecision.RUN_INITIAL_SCAN, actualDecision)
    }

    @Test
    fun `库非空时跳过首扫`() = runTest {
        whenever(mockSongDao.countSongs()).thenReturn(1)
        val actualDecision: InitialScanDecision = repository.ensureInitialScanIfNeeded()
        assertEquals(InitialScanDecision.SKIP_ALREADY_HAS_DATA, actualDecision)
    }

    @Test
    fun `扫描成功后写入歌曲并更新同步状态`() = runTest {
        val song: Song = Song(
            id = "/storage/song.mp3",
            title = "Song",
            artist = "Artist",
            album = null,
            duration = 1000L,
            filePath = "/storage/song.mp3",
            fileSize = 2048L,
            dateAdded = 100L,
            albumArtPath = null,
        )
        var capturedCallback: ((Song) -> Unit)? = null
        val progressFlow: Flow<ScanProgress> = flow {
            emit(
                ScanProgress(
                    scannedCount = 0,
                    totalCount = null,
                    currentPath = ROOT_PATH,
                    isScanning = true,
                ),
            )
            capturedCallback?.invoke(song)
            emit(
                ScanProgress(
                    scannedCount = 1,
                    totalCount = 1,
                    currentPath = null,
                    isScanning = false,
                ),
            )
        }
        whenever(mockScanner.scanDirectory(eq(ROOT_PATH), any())).thenAnswer { invocation ->
            capturedCallback = invocation.getArgument(1)
            progressFlow
        }
        whenever(mockSyncDao.findById(id = 1)).thenReturn(null)
        repository.scanAndSync(scanMode = ScanMode.FULL_INITIAL).collect { _ -> }
        val captor: KArgumentCaptor<List<SongEntity>> = argumentCaptor()
        verify(mockSongDao).upsertAll(captor.capture())
        verify(mockSyncDao, times(2)).upsert(state = any())
        val actualEntities: List<SongEntity> = captor.firstValue
        assertEquals(1, actualEntities.size)
        assertEquals(song.id, actualEntities.first().id)
    }

    @Test
    fun `手动扫描仅应执行用户选择目录`() = runTest {
        val inputDirectories: List<String> = listOf(
            "/storage/emulated/0/Music",
            "/storage/emulated/0/Podcasts",
        )
        whenever(mockScanner.scanDirectory(eq(inputDirectories[0]), any())).thenReturn(
            flowOf(
                ScanProgress(
                    scannedCount = 0,
                    totalCount = null,
                    currentPath = inputDirectories[0],
                    isScanning = true,
                ),
                ScanProgress(
                    scannedCount = 1,
                    totalCount = 1,
                    currentPath = null,
                    isScanning = false,
                ),
            ),
        )
        whenever(mockScanner.scanDirectory(eq(inputDirectories[1]), any())).thenReturn(
            flowOf(
                ScanProgress(
                    scannedCount = 0,
                    totalCount = null,
                    currentPath = inputDirectories[1],
                    isScanning = true,
                ),
                ScanProgress(
                    scannedCount = 1,
                    totalCount = 1,
                    currentPath = null,
                    isScanning = false,
                ),
            ),
        )
        whenever(mockSyncDao.findById(id = 1)).thenReturn(null)
        repository.scanAndSync(
            scanMode = ScanMode.MANUAL_FULL,
            selectedDirectories = inputDirectories,
        ).collect { _ -> }
        verify(mockScanner, times(1)).scanDirectory(eq(inputDirectories[0]), any())
        verify(mockScanner, times(1)).scanDirectory(eq(inputDirectories[1]), any())
    }

    @Test
    fun `手动扫描取消时不应写入歌曲`() = runTest {
        val localDispatcher = StandardTestDispatcher(testScheduler)
        val localRepository: AudioRepository = AudioRepository(
            audioFileScanner = mockScanner,
            songDao = mockSongDao,
            librarySyncStateDao = mockSyncDao,
            ioDispatcher = localDispatcher,
            rootPathProvider = { ROOT_PATH },
            nowProvider = { FIXED_TIME },
        )
        val inputSong: Song = Song(
            id = "/storage/cancel-song.mp3",
            title = "Cancel Song",
            artist = "Artist",
            album = null,
            duration = 1000L,
            filePath = "/storage/cancel-song.mp3",
            fileSize = 2048L,
            dateAdded = 100L,
            albumArtPath = null,
        )
        var capturedCallback: ((Song) -> Unit)? = null
        whenever(mockScanner.scanDirectory(eq(ROOT_PATH), any())).thenAnswer { invocation ->
            capturedCallback = invocation.getArgument(1)
            flow {
                capturedCallback?.invoke(inputSong)
                emit(
                    ScanProgress(
                        scannedCount = 1,
                        totalCount = null,
                        currentPath = ROOT_PATH,
                        isScanning = true,
                    ),
                )
                delay(10_000)
                emit(
                    ScanProgress(
                        scannedCount = 1,
                        totalCount = 1,
                        currentPath = null,
                        isScanning = false,
                    ),
                )
            }
        }
        whenever(mockSyncDao.findById(id = 1)).thenReturn(null)
        val collectJob = launch {
            localRepository.scanAndSync(scanMode = ScanMode.MANUAL_FULL).collect { _ -> }
        }
        advanceUntilIdle()
        collectJob.cancel()
        advanceUntilIdle()
        verify(mockSongDao, never()).upsertAll(any())
    }

    private companion object {
        /**
         * 扫描根路径
         */
        private const val ROOT_PATH: String = "/storage/emulated/0"
        /**
         * 固定时间戳
         */
        private const val FIXED_TIME: Long = 1000L
    }
}
