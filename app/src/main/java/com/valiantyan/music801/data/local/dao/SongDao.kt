package com.valiantyan.music801.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.valiantyan.music801.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

/**
 * 歌曲数据访问对象
 */
@Dao
interface SongDao {
    /**
     * 订阅歌曲列表
     */
    @Query("SELECT * FROM songs ORDER BY dateAdded DESC")
    fun observeSongs(): Flow<List<SongEntity>>

    /**
     * 统计歌曲数量
     */
    @Query("SELECT COUNT(*) FROM songs")
    suspend fun countSongs(): Int

    /**
     * 批量写入歌曲
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(songs: List<SongEntity>): Unit
}
