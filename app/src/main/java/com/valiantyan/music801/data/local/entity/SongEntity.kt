package com.valiantyan.music801.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 歌曲表实体
 *
 * @param id 主键标识
 * @param title 标题
 * @param artist 艺术家
 * @param album 专辑
 * @param duration 时长（毫秒）
 * @param filePath 文件路径
 * @param fileSize 文件大小（字节）
 * @param dateAdded 添加时间（时间戳）
 * @param albumArtPath 封面路径
 * @param modifiedAt 文件修改时间（时间戳）
 * @param scannedAt 扫描入库时间（时间戳）
 */
@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["filePath"], unique = true),
        Index(value = ["dateAdded"]),
        Index(value = ["modifiedAt"]),
    ],
)
data class SongEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val duration: Long,
    val filePath: String,
    val fileSize: Long,
    val dateAdded: Long,
    val albumArtPath: String?,
    val modifiedAt: Long,
    val scannedAt: Long,
)
