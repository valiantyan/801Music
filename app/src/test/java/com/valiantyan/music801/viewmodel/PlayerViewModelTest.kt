package com.valiantyan.music801.viewmodel

import com.valiantyan.music801.data.repository.PlayerRepository
import com.valiantyan.music801.domain.model.PlaybackState
import com.valiantyan.music801.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * 测试 PlayerViewModel 播放控制与状态同步
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {
    private lateinit var repository: PlayerRepository
    private lateinit var viewModel: PlayerViewModel

    @Before
    fun setup(): Unit {
        Dispatchers.setMain(Dispatchers.Unconfined)
        repository = mock()
    }

    @After
    fun tearDown(): Unit {
        Dispatchers.resetMain()
    }

    @Test
    fun `播放状态变化时应同步 UI 状态`() : Unit = runTest {
        // Arrange
        val inputSong: Song = createSong(id = "/storage/music/song1.mp3", title = "Song 1")
        val inputStateFlow: MutableStateFlow<PlaybackState> = MutableStateFlow(PlaybackState())
        whenever(repository.playbackState).thenReturn(inputStateFlow)
        // Act
        viewModel = PlayerViewModel(repository)
        inputStateFlow.value = PlaybackState(
            currentSong = inputSong,
            isPlaying = true,
            position = 1200L,
            duration = 180000L,
            queue = listOf(inputSong),
            currentIndex = 0,
        )
        advanceUntilIdle()
        val actualState: PlayerUiState = viewModel.uiState.value
        // Assert
        assertEquals(inputSong, actualState.currentSong)
        assertTrue(actualState.isPlaying)
        assertEquals(1200L, actualState.position)
        assertEquals(180000L, actualState.duration)
        assertEquals(0, actualState.currentIndex)
    }

    @Test
    fun `调用播放控制应委托仓库`() : Unit = runTest {
        // Arrange
        val inputStateFlow: MutableStateFlow<PlaybackState> = MutableStateFlow(PlaybackState())
        whenever(repository.playbackState).thenReturn(inputStateFlow)
        viewModel = PlayerViewModel(repository)
        // Act
        viewModel.setQueue(
            songs = listOf(createSong(id = "/storage/music/song1.mp3", title = "Song 1")),
            startIndex = 0,
        )
        viewModel.play()
        viewModel.seekTo(position = 1000L)
        viewModel.pause()
        viewModel.skipToNext()
        viewModel.skipToPrevious()
        // Assert
        verify(repository).setQueue(
            songs = listOf(createSong(id = "/storage/music/song1.mp3", title = "Song 1")),
            startIndex = 0,
        )
        verify(repository).play()
        verify(repository).seekTo(position = 1000L)
        verify(repository).pause()
        verify(repository).skipToNext()
        verify(repository).skipToPrevious()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `设置队列后应同步当前歌曲信息`() : Unit = runTest {
        // Arrange
        val inputSong: Song = createSong(id = "/storage/music/song1.mp3", title = "Song 1")
        val inputStateFlow: MutableStateFlow<PlaybackState> = MutableStateFlow(PlaybackState())
        whenever(repository.playbackState).thenReturn(inputStateFlow)
        viewModel = PlayerViewModel(repository)
        // Act
        inputStateFlow.value = PlaybackState(
            currentSong = inputSong,
            isPlaying = false,
            position = 0L,
            duration = 180000L,
            queue = listOf(inputSong),
            currentIndex = 0,
        )
        advanceUntilIdle()
        val actualState: PlayerUiState = viewModel.uiState.value
        // Assert
        assertEquals(inputSong, actualState.currentSong)
        assertEquals(0, actualState.currentIndex)
        assertEquals(1, actualState.queue.size)
    }

    private fun createSong(
        id: String,
        title: String,
    ): Song {
        return Song(
            id = id,
            title = title,
            artist = "Artist",
            album = null,
            duration = 180000L,
            filePath = id,
            fileSize = 1024L,
            dateAdded = 1700000000000L,
            albumArtPath = null,
        )
    }
}
