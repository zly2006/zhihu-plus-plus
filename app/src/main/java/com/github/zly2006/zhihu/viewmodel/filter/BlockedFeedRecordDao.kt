package com.github.zly2006.zhihu.viewmodel.filter

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedFeedRecordDao {
    @Query("SELECT * FROM ${BlockedFeedRecord.TABLE_NAME} ORDER BY blockedTime DESC")
    fun observeAll(): Flow<List<BlockedFeedRecord>>

    @Query("SELECT * FROM ${BlockedFeedRecord.TABLE_NAME} ORDER BY blockedTime DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = BlockedFeedRecord.MAX_RECORDS): List<BlockedFeedRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: BlockedFeedRecord): Long

    @Query("DELETE FROM ${BlockedFeedRecord.TABLE_NAME} WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM ${BlockedFeedRecord.TABLE_NAME}")
    suspend fun clearAll()

    @Query(
        """
        DELETE FROM ${BlockedFeedRecord.TABLE_NAME}
        WHERE id NOT IN (
            SELECT id FROM ${BlockedFeedRecord.TABLE_NAME}
            ORDER BY blockedTime DESC
            LIMIT ${BlockedFeedRecord.MAX_RECORDS}
        )
    """,
    )
    suspend fun maintainLimit()
}
