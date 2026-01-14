package com.valiantyan.aidemo.data.datasource

import android.media.MediaMetadataRetriever

/**
 * Android MediaMetadataRetriever 的包装实现
 */
class AndroidMetadataRetriever : MetadataRetriever {
    private val retriever = MediaMetadataRetriever()

    override fun setDataSource(path: String) {
        retriever.setDataSource(path)
    }

    override fun extractMetadata(key: Int): String? {
        return retriever.extractMetadata(key)
    }

    override fun release() {
        retriever.release()
    }
}
