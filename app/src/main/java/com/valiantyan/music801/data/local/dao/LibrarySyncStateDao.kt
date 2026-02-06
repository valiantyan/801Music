package com.valiantyan.music801.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.valiantyan.music801.data.local.entity.LibrarySyncStateEntity

/**
 * 扫描同步状态数据访问对象
 */
@Dao
interface LibrarySyncStateDao {
    /**
     * 查询同步状态
     */
    @Query("SELECT * FROM library_sync_state WHERE id = :id")
    suspend fun findById(id: Int): LibrarySyncStateEntity?

    /**
     * 写入同步状态
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: LibrarySyncStateEntity): Unit
}
