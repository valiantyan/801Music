package com.valiantyan.music801.viewmodel

/**
 * 扫描 UI 状态
 *
 * 用于管理扫描界面的所有状态信息，包括扫描进度、错误信息等。
 *
 * @param isScanning 是否正在扫描
 * @param scannedCount 已扫描文件数
 * @param totalCount 总文件数（可能未知，为 null）
 * @param currentPath 当前扫描路径（可选）
 * @param error 错误信息（如果有）
 */
data class ScanUiState(
    val isScanning: Boolean = false,
    val scannedCount: Int = 0,
    val totalCount: Int? = null,
    val currentPath: String? = null,
    val error: String? = null,
) {
    /**
     * 是否处于错误状态
     */
    val hasError: Boolean
        get() = error != null

    /**
     * 扫描是否已完成
     */
    val isCompleted: Boolean
        get() = !isScanning && error == null && scannedCount > 0
}
