package com.valiantyan.music801.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 扫描同步状态表实体
 *
 * @param id 单行主键
 * @param lastScanAt 上次扫描时间
 * @param lastFullScanAt 上次全量扫描时间
 * @param lastSyncToken 上次同步标记
 * @param lastScanStatus 上次扫描状态
 * @param lastError 上次错误信息
 */
@Entity(tableName = "library_sync_state")
data class LibrarySyncStateEntity(
    @PrimaryKey
    val id: Int,
    val lastScanAt: Long?,
    val lastFullScanAt: Long?,
    val lastSyncToken: String?,
    val lastScanStatus: ScanStatus?,
    val lastError: String?,
)
