package com.valiantyan.music801.service

import android.os.Build
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Uninterruptibles
import com.valiantyan.music801.player.PlayerCommands
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 测试 PlaybackSessionCallback 命令分发
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class PlaybackSessionCallbackTest {
    @Test
    fun `下一首命令应触发切歌`() {
        // Arrange
        val mockPlayer: Player = mock()
        val inputCallback: MusicPlayerService.PlaybackSessionCallback =
            MusicPlayerService().PlaybackSessionCallback(
                player = mockPlayer,
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
        verify(mockPlayer).seekToNextMediaItem()
    }

    @Test
    fun `上一首命令应触发切歌`() {
        // Arrange
        val mockPlayer: Player = mock()
        val inputCallback: MusicPlayerService.PlaybackSessionCallback =
            MusicPlayerService().PlaybackSessionCallback(
                player = mockPlayer,
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
        verify(mockPlayer).seekToPreviousMediaItem()
    }

    @Test
    fun `停止命令应触发暂停并归零进度`() {
        // Arrange
        val mockPlayer: Player = mock()
        val inputCallback: MusicPlayerService.PlaybackSessionCallback =
            MusicPlayerService().PlaybackSessionCallback(
                player = mockPlayer,
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
        verify(mockPlayer).pause()
        verify(mockPlayer).seekTo(0L)
    }

    @Test
    fun `自定义命令应返回成功`() {
        val mockPlayer: Player = mock()
        val inputCallback: MusicPlayerService.PlaybackSessionCallback =
            MusicPlayerService().PlaybackSessionCallback(
                player = mockPlayer,
            )
        val inputMediaItem: MediaItem = MediaItem.Builder()
            .setMediaId("test-media-id")
            .setUri("file:///storage/test.mp3")
            .build()
        whenever(mockPlayer.currentMediaItem).thenReturn(inputMediaItem)
        val inputSession: MediaSession = mock()
        val inputController: MediaSession.ControllerInfo = mock()
        val inputCommand: SessionCommand = SessionCommand(
            PlayerCommands.ACTION_TOGGLE_FAVORITE,
            Bundle(),
        )
        val actualResult: Int = inputCallback.onCustomCommand(
            session = inputSession,
            controller = inputController,
            customCommand = inputCommand,
            args = Bundle(),
        ).let { future -> Uninterruptibles.getUninterruptibly(future) }.resultCode
        assertEquals(SessionResult.RESULT_SUCCESS, actualResult)
    }

    @Test
    fun `未知自定义命令应返回不支持`() {
        val mockPlayer: Player = mock()
        val inputCallback: MusicPlayerService.PlaybackSessionCallback =
            MusicPlayerService().PlaybackSessionCallback(
                player = mockPlayer,
            )
        val inputSession: MediaSession = mock()
        val inputController: MediaSession.ControllerInfo = mock()
        val inputCommand: SessionCommand = SessionCommand(
            "com.valiantyan.music801.action.UNKNOWN",
            Bundle(),
        )
        val actualResult: Int = inputCallback.onCustomCommand(
            session = inputSession,
            controller = inputController,
            customCommand = inputCommand,
            args = Bundle(),
        ).let { future -> Uninterruptibles.getUninterruptibly(future) }.resultCode
        assertEquals(SessionResult.RESULT_ERROR_NOT_SUPPORTED, actualResult)
    }
}
