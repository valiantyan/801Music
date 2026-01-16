package com.valiantyan.music801.data.datasource

/**
 * 元数据检索器接口
 * 
 * 用于抽象 MediaMetadataRetriever，便于测试和扩展
 */
interface MetadataRetriever {
    /**
     * 设置数据源（文件路径）
     *
     * @param path 音频文件路径
     */
    fun setDataSource(path: String)

    /**
     * 提取元数据
     *
     * @param key 元数据键（如 MediaMetadataRetriever.METADATA_KEY_TITLE）
     * @return 元数据值，如果不存在则返回 null
     */
    fun extractMetadata(key: Int): String?

    /**
     * 释放资源
     */
    fun release()
}
