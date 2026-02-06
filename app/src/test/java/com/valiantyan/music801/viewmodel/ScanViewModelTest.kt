package com.valiantyan.music801.viewmodel

import app.cash.turbine.test
import com.valiantyan.music801.data.repository.AudioRepository
import com.valiantyan.music801.domain.model.ScanMode
import com.valiantyan.music801.domain.model.ScanProgress
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
import org.mockito.kotlin.eq
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
        viewModel = ScanViewModel(repository)
        val initialState: ScanUiState = viewModel.uiState.value
        assertFalse(initialState.isScanning)
        assertEquals(0, initialState.scannedCount)
        assertNull(initialState.totalCount)
        assertNull(initialState.currentPath)
        assertNull(initialState.error)
        assertFalse(initialState.hasError)
        assertFalse(initialState.isCompleted)
    }

    @Test
    fun `指定根目录启动扫描时应使用全量初扫模式`() = runTest(testDispatcher) {
        val rootPath: String = "/storage/emulated/0/Music"
        whenever(repository.scanAndSync(any(), any())).thenReturn(
            flowOf(
                ScanProgress(
                    scannedCount = 0,
                    totalCount = null,
                    currentPath = rootPath,
                    isScanning = true,
                ),
            ),
        )
        viewModel = ScanViewModel(repository)
        viewModel.uiState.test {
            awaitItem()
            viewModel.startScan(rootPath)
            advanceUntilIdle()
            val state: ScanUiState = awaitItem()
            assertTrue(state.isScanning)
            assertEquals(rootPath, state.currentPath)
            assertNull(state.error)
        }
        verify(repository).scanAndSync(
            eq(ScanMode.FULL_INITIAL),
            eq(listOf(rootPath)),
        )
    }

    @Test
    fun `手动扫描应透传模式与目录集合`() = runTest(testDispatcher) {
        val inputDirectories: List<String> = listOf(
            "/storage/emulated/0/Music",
            "/storage/emulated/0/Podcasts",
        )
        whenever(repository.scanAndSync(any(), any())).thenReturn(
            flowOf(
                ScanProgress(
                    scannedCount = 0,
                    totalCount = null,
                    currentPath = inputDirectories.first(),
                    isScanning = true,
                ),
            ),
        )
        viewModel = ScanViewModel(repository)
        viewModel.startScan(
            scanMode = ScanMode.MANUAL_FULL,
            selectedDirectories = inputDirectories,
        )
        advanceUntilIdle()
        verify(repository).scanAndSync(
            eq(ScanMode.MANUAL_FULL),
            eq(inputDirectories),
        )
    }

    @Test
    fun `扫描进度更新应该反映在UI状态中`() = runTest(testDispatcher) {
        val rootPath: String = "/storage/emulated/0/Music"
        val progressFlow = flow {
            emit(ScanProgress(0, null, rootPath, true))
            emit(ScanProgress(1, null, "/path/to/song1.mp3", true))
            emit(ScanProgress(2, null, "/path/to/song2.mp3", true))
            emit(ScanProgress(2, 2, null, false))
        }
        whenever(repository.scanAndSync(any(), any())).thenReturn(progressFlow)
        viewModel = ScanViewModel(repository)
        viewModel.uiState.test {
            awaitItem()
            viewModel.startScan(rootPath)
            advanceUntilIdle()
            val state1: ScanUiState = awaitItem()
            assertTrue(state1.isScanning)
            val state2: ScanUiState = awaitItem()
            assertEquals(1, state2.scannedCount)
            val state3: ScanUiState = awaitItem()
            assertEquals(2, state3.scannedCount)
            val finalState: ScanUiState = awaitItem()
            assertFalse(finalState.isScanning)
            assertEquals(2, finalState.scannedCount)
            assertEquals(2, finalState.totalCount)
            assertNull(finalState.currentPath)
            assertNull(finalState.error)
            assertTrue(finalState.isCompleted)
        }
    }

    @Test
    fun `扫描过程中发生错误应该更新错误状态`() = runTest(testDispatcher) {
        val rootPath: String = "/storage/emulated/0/Music"
        val inputErrorMessage: String = "权限被拒绝"
        val progressFlow = flow<ScanProgress> {
            emit(ScanProgress(0, null, rootPath, true))
            throw IllegalStateException(inputErrorMessage)
        }
        whenever(repository.scanAndSync(any(), any())).thenReturn(progressFlow)
        viewModel = ScanViewModel(repository)
        viewModel.uiState.test {
            awaitItem()
            viewModel.startScan(rootPath)
            advanceUntilIdle()
            val scanningState: ScanUiState = awaitItem()
            assertTrue(scanningState.isScanning)
            val errorState: ScanUiState = awaitItem()
            assertFalse(errorState.isScanning)
            assertTrue(errorState.hasError)
            assertEquals(inputErrorMessage, errorState.error)
            assertFalse(errorState.isCompleted)
        }
    }

    @Test
    fun `取消扫描应该停止扫描并更新状态`() = runTest(testDispatcher) {
        val rootPath: String = "/storage/emulated/0/Music"
        val progressFlow = flow {
            emit(ScanProgress(0, null, rootPath, true))
            emit(ScanProgress(1, null, "/path/to/song1.mp3", true))
            kotlinx.coroutines.delay(1000)
            emit(ScanProgress(2, null, "/path/to/song2.mp3", true))
        }
        whenever(repository.scanAndSync(any(), any())).thenReturn(progressFlow)
        viewModel = ScanViewModel(repository)
        viewModel.startScan(rootPath)
        testDispatcher.scheduler.advanceTimeBy(100)
        viewModel.cancelScan()
        advanceUntilIdle()
        val state: ScanUiState = viewModel.uiState.value
        assertFalse(state.isScanning)
        assertEquals("扫描已取消", state.error)
        assertTrue(state.scannedCount <= 2)
    }

    @Test
    fun `未选择目录时应直接返回错误状态`() = runTest {
        viewModel = ScanViewModel(repository)
        viewModel.startScan(
            scanMode = ScanMode.MANUAL_FULL,
            selectedDirectories = emptyList(),
        )
        val actualState: ScanUiState = viewModel.uiState.value
        assertEquals("未选择扫描目录", actualState.error)
        verify(repository, org.mockito.kotlin.never()).scanAndSync(any(), any())
    }
}
