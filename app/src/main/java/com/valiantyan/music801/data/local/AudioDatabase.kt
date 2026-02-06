package com.valiantyan.music801.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.valiantyan.music801.data.local.dao.LibrarySyncStateDao
import com.valiantyan.music801.data.local.dao.SongDao
import com.valiantyan.music801.data.local.entity.LibrarySyncStateEntity
import com.valiantyan.music801.data.local.entity.ScanStatusConverter
import com.valiantyan.music801.data.local.entity.SongEntity

/**
 * 音频数据库
 */
@Database(
    entities = [SongEntity::class, LibrarySyncStateEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(ScanStatusConverter::class)
abstract class AudioDatabase : RoomDatabase() {
    /**
     * 提供 [SongDao]
     */
    abstract fun songDao(): SongDao

    /**
     * 提供 [LibrarySyncStateDao]
     */
    abstract fun librarySyncStateDao(): LibrarySyncStateDao

    companion object {
        /**
         * 数据库文件名
         */
        private const val DATABASE_NAME: String = "audio_library.db"

        @Volatile
        private var instance: AudioDatabase? = null

        /**
         * 获取数据库单例
         */
        fun getInstance(context: Context): AudioDatabase {
            val existing: AudioDatabase? = instance
            if (existing != null) {
                return existing
            }
            return synchronized(this) {
                val created: AudioDatabase = Room.databaseBuilder(
                    context.applicationContext,
                    AudioDatabase::class.java,
                    DATABASE_NAME,
                ).build()
                instance = created
                created
            }
        }
    }
}
