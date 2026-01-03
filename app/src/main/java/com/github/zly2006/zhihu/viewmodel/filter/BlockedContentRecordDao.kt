package com.github.zly2006.zhihu.viewmodel.filter

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BlockedContentRecordDao {
    /**
     * 获取所有被屏蔽内容记录，按时间倒序
     */
    @Query("SELECT * FROM ${BlockedContentRecord.TABLE_NAME} ORDER BY blockedTime DESC LIMIT :limit")
    suspend fun getRecentBlockedRecords(limit: Int = BlockedContentRecord.MAX_RECORDS): List<BlockedContentRecord>

    /**
     * 插入新的屏蔽记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: BlockedContentRecord): Long

    /**
     * 删除指定记录
     */
    @Query("DELETE FROM ${BlockedContentRecord.TABLE_NAME} WHERE id = :id")
    suspend fun deleteRecord(id: Long)

    /**
     * 清空所有记录
     */
    @Query("DELETE FROM ${BlockedContentRecord.TABLE_NAME}")
    suspend fun clearAllRecords()

    /**
     * 获取记录总数
     */
    @Query("SELECT COUNT(*) FROM ${BlockedContentRecord.TABLE_NAME}")
    suspend fun getRecordCount(): Int

    /**
     * 删除最旧的记录，保持记录数不超过限制
     */
    @Query("""
        DELETE FROM ${BlockedContentRecord.TABLE_NAME} 
        WHERE id IN (
            SELECT id FROM ${BlockedContentRecord.TABLE_NAME} 
            ORDER BY blockedTime ASC 
            LIMIT :deleteCount
        )
    """)
    suspend fun deleteOldestRecords(deleteCount: Int)

    /**
     * 检查并维护记录数量
     */
    suspend fun maintainRecordLimit() {
        val count = getRecordCount()
        if (count > BlockedContentRecord.MAX_RECORDS) {
            val deleteCount = count - BlockedContentRecord.MAX_RECORDS
            deleteOldestRecords(deleteCount)
        }
    }
}
