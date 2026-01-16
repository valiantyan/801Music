package com.valiantyan.music801.player

import android.app.Application
import android.os.Build
import android.os.SystemClock
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * 测试 Media3PlayerManager 启动性能
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class Media3PlayerManagerPerformanceTest {
    @Test
    fun `播放启动耗时应小于500ms`() {
        // Arrange - 创建播放器与测试 Uri
        val context: Application = RuntimeEnvironment.getApplication()
        val manager: Media3PlayerManager = Media3PlayerManager(context = context)
        val inputUri: android.net.Uri = buildTempAudioUri(context = context)
        // Act
        val startTimeMs: Long = SystemClock.elapsedRealtime()
        manager.play(uri = inputUri)
        val elapsedMs: Long = SystemClock.elapsedRealtime() - startTimeMs
        // Assert
        assertTrue(elapsedMs < 500L)
        manager.release()
    }

    /**
     * 创建用于测试的临时音频文件 Uri
     */
    private fun buildTempAudioUri(context: Application): android.net.Uri {
        val file: java.io.File = java.io.File(context.cacheDir, "test-audio.mp3")
        if (!file.exists()) {
            file.writeBytes(byteArrayOf(0x00))
        }
        return android.net.Uri.fromFile(file)
    }
}
