package com.valiantyan.music801.data.datasource

import android.content.ContentResolver
import android.provider.MediaStore

/**
 * Android 平台的 MediaStore ID 解析器
 */
class AndroidMediaStoreIdResolver(
    private val contentResolver: ContentResolver,
) : MediaStoreIdResolver {
    override fun resolveId(filePath: String): Long? {
        val projection: Array<String> = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.DATA} = ?"
        val selectionArgs: Array<String> = arrayOf(filePath)
        return try {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    return null
                }
                val idIndex: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                cursor.getLong(idIndex)
            }
        } catch (e: SecurityException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
