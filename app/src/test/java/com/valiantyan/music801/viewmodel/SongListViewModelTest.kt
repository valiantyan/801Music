package com.valiantyan.music801.viewmodel

import com.valiantyan.music801.data.repository.AudioRepository
import com.valiantyan.music801.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * 测试 SongListViewModel 获取数据与状态更新
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SongListViewModelTest {
    private lateinit var repository: AudioRepository
    private lateinit var viewModel: SongListViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `初始化时进入加载状态并完成加载`() = runTest(testDispatcher) {
        // Arrange
        val inputSongs: List<Song> = emptyList()
        whenever(repository.getAllSongs()).thenReturn(flowOf(inputSongs))
        // Act
        viewModel = SongListViewModel(repository)
        val loadingState: SongListUiState = viewModel.uiState.value
        advanceUntilIdle()
        val loadedState: SongListUiState = viewModel.uiState.value
        // Assert
        assertTrue(loadingState.isLoading)
        assertFalse(loadedState.isLoading)
        assertTrue(loadedState.isEmpty)
        assertEquals(inputSongs, loadedState.songs)
        assertNull(loadedState.error)
        verify(repository).getAllSongs()
    }

    @Test
    fun `获取歌曲列表后更新为空与加载状态`() = runTest(testDispatcher) {
        // Arrange
        val inputSong: Song = createSong(
            id = "/storage/music/song1.mp3",
            title = "Song 1",
            artist = "Artist 1",
        )
        val inputSongs: List<Song> = listOf(inputSong)
        whenever(repository.getAllSongs()).thenReturn(flowOf(inputSongs))
        // Act
        viewModel = SongListViewModel(repository)
        advanceUntilIdle()
        val loadedState: SongListUiState = viewModel.uiState.value
        // Assert
        assertFalse(loadedState.isLoading)
        assertFalse(loadedState.isEmpty)
        assertEquals(1, loadedState.songs.size)
        assertEquals(inputSong, loadedState.songs.first())
    }

    @Test
    fun `加载失败时进入错误状态`() = runTest(testDispatcher) {
        // Arrange
        val inputMessage = "加载失败"
        whenever(repository.getAllSongs()).thenReturn(
            flow {
                throw IllegalStateException(inputMessage)
            },
        )
        // Act
        viewModel = SongListViewModel(repository)
        advanceUntilIdle()
        val errorState: SongListUiState = viewModel.uiState.value
        // Assert
        assertFalse(errorState.isLoading)
        assertEquals(inputMessage, errorState.error)
    }

    private fun createSong(
        id: String,
        title: String,
        artist: String,
    ): Song {
        return Song(
            id = id,
            title = title,
            artist = artist,
            album = null,
            duration = 180000L,
            filePath = id,
            fileSize = 1024L,
            dateAdded = 1700000000000L,
            albumArtPath = null,
        )
    }
}
