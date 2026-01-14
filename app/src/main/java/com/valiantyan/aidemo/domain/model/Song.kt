package com.valiantyan.aidemo.domain.model

/**
 * 歌曲领域模型
 * 
 * @param id 唯一标识符（文件路径）
 * @param title 标题
 * @param artist 艺术家
 * @param album 专辑（可选）
 * @param duration 时长（毫秒）
 * @param filePath 文件路径
 * @param fileSize 文件大小（字节）
 * @param dateAdded 添加时间（时间戳）
 * @param albumArtPath 封面路径（可选）
 */
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val duration: Long,
    val filePath: String,
    val fileSize: Long,
    val dateAdded: Long,
    val albumArtPath: String?
)
