package com.valiantyan.music801.player

import android.app.Application
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
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
    fun init_createsExoPlayerInstance() {
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
    fun init_configuresAudioAttributes() {
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

    private fun assertIntEquals(
        expected: Int,
        actual: Int,
    ): Unit {
        // JUnit Java API 不支持命名参数，使用位置参数
        assertEquals(expected, actual)
    }
}
