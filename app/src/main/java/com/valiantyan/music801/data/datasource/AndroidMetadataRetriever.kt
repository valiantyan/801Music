package com.valiantyan.music801.data.datasource

import android.media.MediaMetadataRetriever

/**
 * [MediaMetadataRetriever] 的包装实现
 */
class AndroidMetadataRetriever : MetadataRetriever {
    /**
     * 系统元数据检索器实例
     */
    private val retriever = MediaMetadataRetriever()

    /**
     * 设置音频数据源路径
     *
     * @param path 音频文件路径
     */
    override fun setDataSource(path: String) {
        retriever.setDataSource(path)
    }

    /**
     * 读取指定元数据
     *
     * @param key 元数据键
     * @return 元数据内容
     */
    override fun extractMetadata(key: Int): String? {
        return retriever.extractMetadata(key)
    }

    /**
     * 释放系统资源
     */
    override fun release() {
        retriever.release()
    }
}
