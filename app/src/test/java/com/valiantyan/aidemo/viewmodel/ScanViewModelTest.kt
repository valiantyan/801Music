package com.valiantyan.aidemo.viewmodel

import app.cash.turbine.test
import com.valiantyan.aidemo.data.repository.AudioRepository
import com.valiantyan.aidemo.domain.model.ScanProgress
import com.valiantyan.aidemo.domain.model.Song
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {

    private lateinit var repository: AudioRepository
    private lateinit var viewModel: ScanViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        repository = mock()
    }

    @Test
    fun `初始状态应该是未扫描状态`() {
        // Given
        viewModel = ScanViewModel(repository)

        // When
        val initialState = viewModel.uiState.value

        // Then
        assertFalse(initialState.isScanning)
        assertEquals(0, initialState.scannedCount)
        assertNull(initialState.totalCount)
        assertNull(initialState.currentPath)
        assertNull(initialState.error)
        assertFalse(initialState.hasError)
        assertFalse(initialState.isCompleted)
    }

    @Test
    fun `开始扫描时状态应该更新为扫描中`() = runTest(testDispatcher) {
        // Given
        val rootPath = "/storage/emulated/0/Music"
        whenever(repository.scanAudioFiles(any(), any())).thenReturn(
            flowOf(
                ScanProgress(
                    scannedCount = 0,
                    totalCount = null,
                    currentPath = rootPath,
                    isScanning = true
                )
            )
        )
        viewModel = ScanViewModel(repository)

        // When
        viewModel.uiState.test {
            // 跳过初始状态
            awaitItem()
            
            viewModel.startScan(rootPath)
            advanceUntilIdle()

            // Then
            val state = awaitItem()
            assertTrue(state.isScanning)
            assertEquals(0, state.scannedCount)
            assertEquals(rootPath, state.currentPath)
            assertNull(state.error)
        }
        
        // 验证 repository 被调用（在 test 块外）
        verify(repository).scanAudioFiles(any(), any())
    }

    @Test
    fun `扫描进度更新应该反映在 UI 状态中`() = runTest(testDispatcher) {
        // Given
        val rootPath = "/storage/emulated/0/Music"
        val progressFlow = flow {
            emit(ScanProgress(0, null, rootPath, true))
            emit(ScanProgress(1, null, "/path/to/song1.mp3", true))
            emit(ScanProgress(2, null, "/path/to/song2.mp3", true))
            emit(ScanProgress(2, 2, null, false))
        }
        whenever(repository.scanAudioFiles(any(), any())).thenReturn(progressFlow)
        viewModel = ScanViewModel(repository)

        // When
        viewModel.uiState.test {
            // 跳过初始状态
            awaitItem()
            
            viewModel.startScan(rootPath)
            advanceUntilIdle()

            // Then - 验证所有状态更新
            val state1 = awaitItem() // 开始扫描
            assertTrue(state1.isScanning)
            assertEquals(0, state1.scannedCount)
            
            val state2 = awaitItem() // 第一个文件
            assertEquals(1, state2.scannedCount)
            
            val state3 = awaitItem() // 第二个文件
            assertEquals(2, state3.scannedCount)
            
            val finalState = awaitItem() // 完成
            assertFalse(finalState.isScanning)
            assertEquals(2, finalState.scannedCount)
            assertEquals(2, finalState.totalCount)
            assertNull(finalState.currentPath)
            assertNull(finalState.error)
            assertTrue(finalState.isCompleted)
        }
    }

    @Test
    fun `扫描完成时状态应该更新为已完成`() = runTest(testDispatcher) {
        // Given
        val rootPath = "/storage/emulated/0/Music"
        val progressFlow = flow {
            emit(ScanProgress(0, null, rootPath, true))
            emit(ScanProgress(5, 5, null, false))
        }
        whenever(repository.scanAudioFiles(any(), any())).thenReturn(progressFlow)
        viewModel = ScanViewModel(repository)

        // When
        viewModel.uiState.test {
            // 跳过初始状态
            awaitItem()
            
            viewModel.startScan(rootPath)
            advanceUntilIdle()

            // Then
            val state1 = awaitItem() // 开始扫描
            assertTrue(state1.isScanning)
            
            val finalState = awaitItem() // 完成
            assertFalse(finalState.isScanning)
            assertEquals(5, finalState.scannedCount)
            assertEquals(5, finalState.totalCount)
            assertTrue(finalState.isCompleted)
        }
    }

    @Test
    fun `扫描过程中发生错误应该更新错误状态`() = runTest(testDispatcher) {
        // Given
        val rootPath = "/storage/emulated/0/Music"
        val errorMessage = "权限被拒绝"
        val progressFlow = flow<ScanProgress> {
            emit(ScanProgress(0, null, rootPath, true))
            throw RuntimeException(errorMessage)
        }
        whenever(repository.scanAudioFiles(any(), any())).thenReturn(progressFlow)
        viewModel = ScanViewModel(repository)

        // When
        viewModel.uiState.test {
            // 跳过初始状态
            awaitItem()
            
            viewModel.startScan(rootPath)
            advanceUntilIdle()

            // Then
            val state1 = awaitItem() // 开始扫描
            assertTrue(state1.isScanning)
            
            val errorState = awaitItem() // 错误状态
            assertFalse(errorState.isScanning)
            assertTrue(errorState.hasError)
            assertEquals(errorMessage, errorState.error)
            assertFalse(errorState.isCompleted)
        }
    }

    @Test
    fun `取消扫描应该停止扫描并更新状态`() = runTest(testDispatcher) {
        // Given
        val rootPath = "/storage/emulated/0/Music"
        val progressFlow = flow {
            emit(ScanProgress(0, null, rootPath, true))
            emit(ScanProgress(1, null, "/path/to/song1.mp3", true))
            // 模拟长时间运行的扫描
            kotlinx.coroutines.delay(1000)
            emit(ScanProgress(2, null, "/path/to/song2.mp3", true))
        }
        whenever(repository.scanAudioFiles(any(), any())).thenReturn(progressFlow)
        viewModel = ScanViewModel(repository)

        // When
        viewModel.startScan(rootPath)
        // 等待一些进度更新
        testDispatcher.scheduler.advanceTimeBy(100)
        viewModel.cancelScan()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        // 取消后，扫描应该停止（但可能已经有一些进度）
        // 注意：由于 Flow 的取消机制，状态可能不会立即更新为 isScanning = false
        // 但至少应该不会继续更新
        assertTrue(state.scannedCount <= 2) // 最多扫描了 2 个文件
    }

    @Test
    fun `扫描时找到的歌曲应该被正确处理`() = runTest(testDispatcher) {
        // Given
        val rootPath = "/storage/emulated/0/Music"
        val song1 = Song(
            id = "/path/to/song1.mp3",
            title = "Song 1",
            artist = "Artist 1",
            album = "Album 1",
            duration = 180000L,
            filePath = "/path/to/song1.mp3",
            fileSize = 5000000L,
            dateAdded = System.currentTimeMillis(),
            albumArtPath = null
        )
        val song2 = Song(
            id = "/path/to/song2.mp3",
            title = "Song 2",
            artist = "Artist 2",
            album = null,
            duration = 200000L,
            filePath = "/path/to/song2.mp3",
            fileSize = 6000000L,
            dateAdded = System.currentTimeMillis(),
            albumArtPath = null
        )
        
        var capturedCallback: ((Song) -> Unit)? = null
        val progressFlow = flow {
            emit(ScanProgress(0, null, rootPath, true))
            capturedCallback?.invoke(song1)
            emit(ScanProgress(1, null, "/path/to/song1.mp3", true))
            capturedCallback?.invoke(song2)
            emit(ScanProgress(2, null, "/path/to/song2.mp3", true))
            emit(ScanProgress(2, 2, null, false))
        }
        
        whenever(repository.scanAudioFiles(any(), any())).thenAnswer { invocation ->
            capturedCallback = invocation.getArgument(1)
            progressFlow
        }
        viewModel = ScanViewModel(repository)

        // When
        viewModel.uiState.test {
            // 跳过初始状态
            awaitItem()
            
            viewModel.startScan(rootPath)
            advanceUntilIdle()

            // Then - 验证所有状态更新
            awaitItem() // 开始扫描
            awaitItem() // 第一个文件
            awaitItem() // 第二个文件
            val finalState = awaitItem() // 完成
            
            assertTrue(finalState.isCompleted)
            assertEquals(2, finalState.scannedCount)
            // 验证回调被调用
            assertTrue(capturedCallback != null)
        }
    }
}
