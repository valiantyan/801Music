package com.valiantyan.music801.di

import android.content.Context
import com.valiantyan.music801.data.repository.PlayerRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * 测试 PlayerRepositoryHolder 单例行为
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [android.os.Build.VERSION_CODES.TIRAMISU])
class PlayerRepositoryHolderTest {
    @After
    fun tearDown(): Unit {
        PlayerRepositoryHolder.clear()
    }

    @Test
    fun `重复获取应返回同一实例`() {
        // Arrange - 获取上下文
        val context: Context = RuntimeEnvironment.getApplication()
        // Act
        val first: PlayerRepository = PlayerRepositoryHolder.getOrCreate(context = context)
        val second: PlayerRepository = PlayerRepositoryHolder.getOrCreate(context = context)
        // Assert
        assertEquals(first, second)
    }

    @Test
    fun `清理后应创建新实例`() {
        // Arrange - 获取上下文并创建实例
        val context: Context = RuntimeEnvironment.getApplication()
        val first: PlayerRepository = PlayerRepositoryHolder.getOrCreate(context = context)
        // Act
        PlayerRepositoryHolder.clear()
        val second: PlayerRepository = PlayerRepositoryHolder.getOrCreate(context = context)
        // Assert
        assertEquals(false, first === second)
    }
}
