package com.valiantyan.music801.service

import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionResult
import com.valiantyan.music801.data.repository.PlayerRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * 测试 PlaybackSessionCallback 命令分发
 */
class PlaybackSessionCallbackTest {
    @Test
    fun `下一首命令应触发切歌`() {
        // Arrange
        val mockRepository: PlayerRepository = mock()
        val inputCallback: PlaybackSessionCallback = PlaybackSessionCallback(
            playerRepository = mockRepository,
        )
        val inputSession: MediaSession = mock()
        val inputController: MediaSession.ControllerInfo = mock()
        // Act
        val actualResult: Int = inputCallback.onPlayerCommandRequest(
            session = inputSession,
            controller = inputController,
            playerCommand = Player.COMMAND_SEEK_TO_NEXT,
        )
        // Assert
        assertEquals(SessionResult.RESULT_SUCCESS, actualResult)
        verify(mockRepository).skipToNext()
    }

    @Test
    fun `上一首命令应触发切歌`() {
        // Arrange
        val mockRepository: PlayerRepository = mock()
        val inputCallback: PlaybackSessionCallback = PlaybackSessionCallback(
            playerRepository = mockRepository,
        )
        val inputSession: MediaSession = mock()
        val inputController: MediaSession.ControllerInfo = mock()
        // Act
        val actualResult: Int = inputCallback.onPlayerCommandRequest(
            session = inputSession,
            controller = inputController,
            playerCommand = Player.COMMAND_SEEK_TO_PREVIOUS,
        )
        // Assert
        assertEquals(SessionResult.RESULT_SUCCESS, actualResult)
        verify(mockRepository).skipToPrevious()
    }

    @Test
    fun `停止命令应触发暂停并归零进度`() {
        // Arrange
        val mockRepository: PlayerRepository = mock()
        val inputCallback: PlaybackSessionCallback = PlaybackSessionCallback(
            playerRepository = mockRepository,
        )
        val inputSession: MediaSession = mock()
        val inputController: MediaSession.ControllerInfo = mock()
        // Act
        val actualResult: Int = inputCallback.onPlayerCommandRequest(
            session = inputSession,
            controller = inputController,
            playerCommand = Player.COMMAND_STOP,
        )
        // Assert
        assertEquals(SessionResult.RESULT_SUCCESS, actualResult)
        verify(mockRepository).pause()
        verify(mockRepository).seekTo(position = 0L)
    }
}
