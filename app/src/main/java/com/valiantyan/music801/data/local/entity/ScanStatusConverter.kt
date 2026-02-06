package com.valiantyan.music801.data.local.entity

import androidx.room.TypeConverter

/**
 * 扫描状态类型转换器
 */
class ScanStatusConverter {
    /**
     * 将 [ScanStatus] 转为字符串
     */
    @TypeConverter
    fun fromStatus(status: ScanStatus?): String? {
        return status?.name
    }

    /**
     * 将字符串转为 [ScanStatus]
     */
    @TypeConverter
    fun toStatus(value: String?): ScanStatus? {
        return value?.let { statusValue: String ->
            ScanStatus.valueOf(statusValue)
        }
    }
}
