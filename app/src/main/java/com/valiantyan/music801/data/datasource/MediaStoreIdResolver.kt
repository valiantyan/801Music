package com.valiantyan.music801.data.datasource

/**
 * MediaStore 音频 ID 解析器
 */
interface MediaStoreIdResolver {
    /**
     * 通过文件路径解析 MediaStore 音频 ID
     */
    fun resolveId(filePath: String): Long?
}
