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
        val inputUri: android.net.Uri = android.net.Uri.parse("file:///storage/emulated/0/Music/test.mp3")
        // Act
        val startTimeMs: Long = SystemClock.elapsedRealtime()
        manager.play(uri = inputUri)
        val elapsedMs: Long = SystemClock.elapsedRealtime() - startTimeMs
        // Assert
        assertTrue(elapsedMs < 500L)
        manager.release()
    }
}
