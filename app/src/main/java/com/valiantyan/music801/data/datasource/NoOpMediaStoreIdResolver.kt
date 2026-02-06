package com.valiantyan.music801.data.datasource

/**
 * 空实现的 MediaStore ID 解析器
 */
class NoOpMediaStoreIdResolver : MediaStoreIdResolver {
    override fun resolveId(filePath: String): Long? {
        return null
    }
}
