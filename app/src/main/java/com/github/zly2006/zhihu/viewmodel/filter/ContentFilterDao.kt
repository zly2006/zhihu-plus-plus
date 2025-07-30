package com.github.zly2006.zhihu.viewmodel.filter

import androidx.room.*

@Dao
interface ContentFilterDao {

    @Query("SELECT * FROM ${ContentViewRecord.TABLE_NAME} WHERE id = :id")
    suspend fun getViewRecord(id: String): ContentViewRecord?

    @Query("SELECT * FROM ${ContentViewRecord.TABLE_NAME} WHERE targetType = :targetType AND targetId = :targetId")
    suspend fun getViewRecordByTarget(targetType: String, targetId: String): ContentViewRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateViewRecord(record: ContentViewRecord)

    @Query("UPDATE ${ContentViewRecord.TABLE_NAME} SET viewCount = viewCount + 1, lastViewTime = :currentTime WHERE id = :id")
    suspend fun incrementViewCount(id: String, currentTime: Long = System.currentTimeMillis())

    @Query("UPDATE ${ContentViewRecord.TABLE_NAME} SET hasInteraction = 1, lastInteractionTime = :currentTime WHERE id = :id")
    suspend fun markAsInteracted(id: String, currentTime: Long = System.currentTimeMillis())

    @Query("SELECT * FROM ${ContentViewRecord.TABLE_NAME} WHERE viewCount > :maxCount")
    suspend fun getFilteredContent(maxCount: Int = ContentViewRecord.MAX_VIEW_COUNT_WITHOUT_INTERACTION): List<ContentViewRecord>

    @Query("SELECT id FROM ${ContentViewRecord.TABLE_NAME} WHERE viewCount > :maxCount AND hasInteraction = 0")
    suspend fun getFilteredContentIds(maxCount: Int = ContentViewRecord.MAX_VIEW_COUNT_WITHOUT_INTERACTION): List<String>

    @Query("DELETE FROM ${ContentViewRecord.TABLE_NAME} WHERE firstViewTime < :cutoffTime")
    suspend fun cleanupOldRecords(cutoffTime: Long)

    @Query("DELETE FROM ${ContentViewRecord.TABLE_NAME}")
    suspend fun clearAllRecords()

    @Query("SELECT COUNT(*) FROM ${ContentViewRecord.TABLE_NAME}")
    suspend fun getRecordCount(): Int
}
