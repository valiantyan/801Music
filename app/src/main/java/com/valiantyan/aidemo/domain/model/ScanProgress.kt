package com.valiantyan.aidemo.domain.model

/**
 * 扫描进度模型
 * 
 * @param scannedCount 已扫描文件数
 * @param totalCount 总文件数（可能未知，为 null）
 * @param currentPath 当前扫描路径（可选）
 * @param isScanning 是否正在扫描
 */
data class ScanProgress(
    val scannedCount: Int,
    val totalCount: Int?,
    val currentPath: String?,
    val isScanning: Boolean
)
