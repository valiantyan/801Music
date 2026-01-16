package com.valiantyan.music801.data.util

/**
 * 音频文件格式识别工具类
 * 
 * 用于识别和验证设备存储中的音频文件格式。
 * 支持常见音频格式：MP3、AAC、FLAC、WAV、OGG、M4A
 */
object AudioFormatRecognizer {

    /**
     * 支持的音频文件扩展名（小写）
     */
    private val SUPPORTED_AUDIO_EXTENSIONS = setOf(
        "mp3",
        "aac",
        "flac",
        "wav",
        "ogg",
        "m4a"
    )

    /**
     * 检查给定的文件路径是否为支持的音频文件
     * 
     * @param filePath 文件路径或文件名
     * @return 如果是支持的音频格式返回 true，否则返回 false
     */
    fun isAudioFile(filePath: String): Boolean {
        if (filePath.isBlank()) {
            return false
        }
        val extension = extractExtension(filePath) ?: return false
        return SUPPORTED_AUDIO_EXTENSIONS.contains(extension.lowercase())
    }

    /**
     * 从文件路径中提取扩展名
     * 
     * @param filePath 文件路径
     * @return 扩展名（不包含点号），如果无法提取则返回 null
     */
    private fun extractExtension(filePath: String): String? {
        val lastDotIndex = filePath.lastIndexOf('.')
        if (lastDotIndex == -1 || lastDotIndex == filePath.length - 1) {
            return null
        }
        val lastSlashIndex = filePath.lastIndexOf('/')
        if (lastSlashIndex > lastDotIndex) {
            return null
        }
        return filePath.substring(lastDotIndex + 1)
    }

    /**
     * 获取所有支持的音频文件扩展名列表
     * 
     * @return 支持的扩展名集合（小写）
     */
    fun getSupportedExtensions(): Set<String> {
        return SUPPORTED_AUDIO_EXTENSIONS.toSet()
    }
}
