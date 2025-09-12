package com.github.zly2006.zhihu.viewmodel.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface LocalContentDao {
    // CrawlingTask 相关操作
    @Query("SELECT * FROM crawling_tasks WHERE status = :status ORDER BY priority DESC, createdAt ASC")
    suspend fun getTasksByStatus(status: CrawlingStatus): List<CrawlingTask>

    @Query("SELECT COUNT(*) FROM crawling_tasks WHERE reason = :reason AND status = :status")
    suspend fun getTaskCountByReasonAndStatus(reason: CrawlingReason, status: CrawlingStatus): Int

    @Insert
    suspend fun insertTask(task: CrawlingTask): Long

    @Insert
    suspend fun insertTasks(tasks: List<CrawlingTask>)

    @Update
    suspend fun updateTask(task: CrawlingTask)

    @Query("DELETE FROM crawling_tasks WHERE status = :status AND createdAt < :before")
    suspend fun cleanupOldTasks(status: CrawlingStatus, before: Long)

    // CrawlingResult 相关操作
    @Query("SELECT * FROM crawling_results WHERE reason = :reason ORDER BY score DESC, createdAt DESC")
    suspend fun getResultsByReason(reason: CrawlingReason): List<CrawlingResult>

    @Query("SELECT COUNT(*) FROM crawling_results WHERE reason = :reason")
    suspend fun getResultCountByReason(reason: CrawlingReason): Int

    @Query("SELECT * FROM crawling_results WHERE contentId = :contentId LIMIT 1")
    suspend fun getResultByContentId(contentId: String): CrawlingResult?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: CrawlingResult): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<CrawlingResult>)

    @Query("DELETE FROM crawling_results WHERE createdAt < :before")
    suspend fun cleanupOldResults(before: Long)

    // LocalFeed 相关操作
    @Query("SELECT * FROM local_feeds WHERE resultId = :resultId LIMIT 1")
    suspend fun getFeedByResultId(resultId: Long): LocalFeed?

    @Query("SELECT * FROM local_feeds ORDER BY userFeedback DESC, createdAt DESC LIMIT :limit")
    suspend fun getTopRatedFeeds(limit: Int): List<LocalFeed>

    @Query("SELECT * FROM local_feeds ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentFeeds(limit: Int): List<LocalFeed>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(feed: LocalFeed)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeeds(feeds: List<LocalFeed>)

    @Query("UPDATE local_feeds SET userFeedback = :feedback WHERE id = :feedId")
    suspend fun updateFeedFeedback(feedId: String, feedback: Double)

    @Query("DELETE FROM local_feeds WHERE createdAt < :before")
    suspend fun cleanupOldFeeds(before: Long)

    // UserBehavior 相关操作
    @Insert
    suspend fun insertBehavior(behavior: UserBehavior)

    @Query("SELECT * FROM user_behaviors WHERE contentId = :contentId ORDER BY timestamp DESC")
    suspend fun getBehaviorsByContentId(contentId: String): List<UserBehavior>

    @Query("SELECT * FROM user_behaviors WHERE action = :action AND timestamp > :since")
    suspend fun getBehaviorsByActionSince(action: String, since: Long): List<UserBehavior>

    @Query("SELECT contentId, COUNT(*) as count FROM user_behaviors WHERE action = 'like' GROUP BY contentId ORDER BY count DESC LIMIT :limit")
    suspend fun getMostLikedContent(limit: Int): List<ContentLikeCount>

    @Query("DELETE FROM user_behaviors WHERE timestamp < :before")
    suspend fun cleanupOldBehaviors(before: Long)
}

data class ContentLikeCount(
    val contentId: String,
    val count: Int,
)
