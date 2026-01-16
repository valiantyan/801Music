package com.valiantyan.music801.player

import android.app.Application
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * 测试 Media3PlayerManager 初始化行为
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class Media3PlayerManagerTest {
    @Test
    fun `初始化后应创建ExoPlayer实例`() {
        // Arrange - 创建管理器用于验证播放器实例
        val context: Application = RuntimeEnvironment.getApplication()
        // Act
        val manager: Media3PlayerManager = Media3PlayerManager(context = context)
        val actualPlayer: ExoPlayer = manager.exoPlayer
        // Assert
        assertNotNull(actualPlayer)
        manager.release()
    }

    @Test
    fun `初始化后应配置音频属性`() {
        // Arrange - 创建管理器用于验证音频属性
        val context: Application = RuntimeEnvironment.getApplication()
        val manager: Media3PlayerManager = Media3PlayerManager(context = context)
        // Act
        val actualAttributes: AudioAttributes = manager.exoPlayer.audioAttributes
        // Assert
        assertIntEquals(expected = C.AUDIO_CONTENT_TYPE_MUSIC, actual = actualAttributes.contentType)
        assertIntEquals(expected = C.USAGE_MEDIA, actual = actualAttributes.usage)
        manager.release()
    }

    @Test
    fun `调用播放后应设置媒体项并准备播放`() {
        // Arrange - 创建管理器与音频 Uri
        val context: Application = RuntimeEnvironment.getApplication()
        val manager: Media3PlayerManager = Media3PlayerManager(context = context)
        val inputUri: android.net.Uri = android.net.Uri.parse("file:///storage/emulated/0/Music/test.mp3")
        // Act
        manager.play(uri = inputUri)
        val actualUri: android.net.Uri? = manager.exoPlayer.currentMediaItem?.localConfiguration?.uri
        val actualPlayWhenReady: Boolean = manager.exoPlayer.playWhenReady
        // Assert
        assertNotNull(actualUri)
        assertBooleanEquals(expected = true, actual = actualPlayWhenReady)
        manager.release()
    }

    @Test
    fun `调用暂停后应停止播放`() {
        // Arrange - 先触发播放再验证暂停
        val context: Application = RuntimeEnvironment.getApplication()
        val manager: Media3PlayerManager = Media3PlayerManager(context = context)
        val inputUri: android.net.Uri = android.net.Uri.parse("file:///storage/emulated/0/Music/test.mp3")
        manager.play(uri = inputUri)
        // Act
        manager.pause()
        val actualPlayWhenReady: Boolean = manager.exoPlayer.playWhenReady
        // Assert
        assertBooleanEquals(expected = false, actual = actualPlayWhenReady)
        manager.release()
    }

    @Test
    fun `调用停止后应回到空闲状态`() {
        // Arrange - 先触发播放再验证停止状态
        val context: Application = RuntimeEnvironment.getApplication()
        val manager: Media3PlayerManager = Media3PlayerManager(context = context)
        val inputUri: android.net.Uri = android.net.Uri.parse("file:///storage/emulated/0/Music/test.mp3")
        manager.play(uri = inputUri)
        // Act
        manager.stop()
        val actualState: Int = manager.exoPlayer.playbackState
        // Assert
        assertIntEquals(expected = Player.STATE_IDLE, actual = actualState)
        manager.release()
    }

    @Test
    fun `跳转到指定位置后应更新播放位置`() {
        // Arrange - 先准备播放再进行跳转
        val context: Application = RuntimeEnvironment.getApplication()
        val manager: Media3PlayerManager = Media3PlayerManager(context = context)
        val inputUri: android.net.Uri = android.net.Uri.parse("file:///storage/emulated/0/Music/test.mp3")
        val inputPosition: Long = 1234L
        manager.play(uri = inputUri)
        // Act
        manager.seekTo(position = inputPosition)
        val actualPosition: Long = manager.exoPlayer.currentPosition
        // Assert
        assertLongEquals(expected = inputPosition, actual = actualPosition)
        manager.release()
    }

    private fun assertIntEquals(
        expected: Int,
        actual: Int,
    ): Unit {
        // JUnit Java API 不支持命名参数，使用位置参数
        assertEquals(expected, actual)
    }

    private fun assertBooleanEquals(
        expected: Boolean,
        actual: Boolean,
    ): Unit {
        // JUnit Java API 不支持命名参数，使用位置参数
        assertEquals(expected, actual)
    }

    private fun assertLongEquals(
        expected: Long,
        actual: Long,
    ): Unit {
        // JUnit Java API 不支持命名参数，使用位置参数
        assertEquals(expected, actual)
    }
}
