package com.valiantyan.music801.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 测试 ScanProgress 数据模型的创建和状态更新
 */
class ScanProgressTest {

    @Test
    fun `创建 ScanProgress 对象并验证所有属性`() {
        // Given
        val progress = ScanProgress(
            scannedCount = 10,
            totalCount = 100,
            currentPath = "/storage/emulated/0/Music",
            isScanning = true,
        )

        // Then
        assertEquals(10, progress.scannedCount)
        assertEquals(100, progress.totalCount)
        assertEquals("/storage/emulated/0/Music", progress.currentPath)
        assertTrue(progress.isScanning)
    }

    @Test
    fun `创建 ScanProgress 时 totalCount 和 currentPath 可以为空`() {
        // Given
        val progress = ScanProgress(
            scannedCount = 5,
            totalCount = null,
            currentPath = null,
            isScanning = true,
        )

        // Then
        assertEquals(5, progress.scannedCount)
        assertNull(progress.totalCount)
        assertNull(progress.currentPath)
        assertTrue(progress.isScanning)
    }

    @Test
    fun `ScanProgress 对象支持数据类相等性比较`() {
        // Given
        val progress1 = ScanProgress(
            scannedCount = 10,
            totalCount = 100,
            currentPath = "/storage/emulated/0/Music",
            isScanning = true,
        )

        val progress2 = ScanProgress(
            scannedCount = 10,
            totalCount = 100,
            currentPath = "/storage/emulated/0/Music",
            isScanning = true,
        )

        // Then
        assertEquals(progress1, progress2)
        assertEquals(progress1.hashCode(), progress2.hashCode())
    }

    @Test
    fun `ScanProgress 可以表示扫描完成状态`() {
        // Given
        val progress = ScanProgress(
            scannedCount = 100,
            totalCount = 100,
            currentPath = null,
            isScanning = false,
        )

        // Then
        assertEquals(100, progress.scannedCount)
        assertEquals(100, progress.totalCount)
        assertNull(progress.currentPath)
        assertFalse(progress.isScanning)
    }

    @Test
    fun `ScanProgress 可以表示扫描进行中状态`() {
        // Given
        val progress = ScanProgress(
            scannedCount = 50,
            totalCount = 100,
            currentPath = "/storage/emulated/0/Music/Album1",
            isScanning = true,
        )

        // Then
        assertEquals(50, progress.scannedCount)
        assertEquals(100, progress.totalCount)
        assertEquals("/storage/emulated/0/Music/Album1", progress.currentPath)
        assertTrue(progress.isScanning)
    }
}
