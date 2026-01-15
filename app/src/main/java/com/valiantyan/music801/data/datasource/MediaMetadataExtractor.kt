package com.valiantyan.music801.data.datasource

import com.valiantyan.music801.domain.model.Song
import android.media.MediaMetadataRetriever
import java.io.File

/**
 * 音频元数据提取器
 * 
 * 使用 MediaMetadataRetriever 从音频文件中提取元数据（标题、艺术家、专辑、时长等）
 * 处理元数据缺失的情况，使用文件名作为默认标题
 */
class MediaMetadataExtractor(
    private val metadataRetrieverFactory: () -> MetadataRetriever = { AndroidMetadataRetriever() }
) {

    /**
     * 从音频文件中提取元数据并创建 Song 对象
     * 
     * @param filePath 音频文件路径
     * @param fileSize 文件大小（字节）
     * @param dateAdded 添加时间（时间戳，毫秒）
     * @return Song 对象，如果提取失败则返回 null
     */
    fun extractMetadata(filePath: String, fileSize: Long, dateAdded: Long): Song? {
        val retriever = metadataRetrieverFactory()
        
        return try {
            retriever.setDataSource(filePath)
            
            // 提取元数据
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }
                ?: extractTitleFromFileName(filePath)
            
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() }
                ?: "未知艺术家"
            
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() }
                ?: null
            
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLongOrNull() ?: 0L
            
            // 注意：封面图片需要通过 embeddedPicture 方法获取，这里先设为 null
            // 未来可以在 AudioFileScanner 中单独处理封面提取
            val albumArtPath: String? = null
            
            Song(
                id = filePath, // 使用文件路径作为唯一标识
                title = title,
                artist = artist,
                album = album,
                duration = duration,
                filePath = filePath,
                fileSize = fileSize,
                dateAdded = dateAdded,
                albumArtPath = albumArtPath
            )
        } catch (e: Exception) {
            // 如果提取失败，返回 null
            // 在实际使用中，可以记录日志
            null
        } finally {
            retriever.release()
        }
    }

    /**
     * 从文件名中提取标题（去除扩展名）
     * 
     * @param filePath 文件路径
     * @return 文件名（不含扩展名）
     */
    private fun extractTitleFromFileName(filePath: String): String {
        val fileName = File(filePath).nameWithoutExtension
        return fileName.ifBlank { "未知标题" }
    }
}
