package com.github.zly2006.zhihu.viewmodel.local

import androidx.room.*

@Dao
interface LocalContentDao {
    // CrawlingTask 相关操作
    @Query("SELECT * FROM ${CrawlingTask.TABLE_NAME} WHERE status = :status ORDER BY priority DESC, createdAt ASC")
    suspend fun getTasksByStatus(status: CrawlingStatus): List<CrawlingTask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: CrawlingTask)

    @Update
    suspend fun updateTask(task: CrawlingTask)

    // CrawlingResult 相关操作
    @Query("SELECT * FROM ${CrawlingResult.TABLE_NAME} WHERE taskId = :taskId")
    suspend fun getResultByTaskId(taskId: Int): CrawlingResult?

    @Query("SELECT * FROM ${CrawlingResult.TABLE_NAME} WHERE id = :resultId")
    suspend fun getResultById(resultId: Int): CrawlingResult?

    @Query("SELECT r.* FROM ${CrawlingResult.TABLE_NAME} r INNER JOIN ${CrawlingTask.TABLE_NAME} t ON r.taskId = t.id WHERE t.reason = :reason ORDER BY r.crawledAt DESC")
    suspend fun getResultsByReason(reason: CrawlingReason): List<CrawlingResult>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: CrawlingResult)

    @Query("SELECT COUNT(*) FROM ${CrawlingResult.TABLE_NAME} r INNER JOIN ${CrawlingTask.TABLE_NAME} t ON r.taskId = t.id WHERE t.reason = :reason")
    suspend fun getResultCountByReason(reason: CrawlingReason): Int

    // LocalFeed 相关操作
    @Query("SELECT * FROM ${LocalFeed.TABLE_NAME} WHERE id = :feedId")
    suspend fun getFeedById(feedId: String): LocalFeed?

    @Query("SELECT * FROM ${LocalFeed.TABLE_NAME} ORDER BY userFeedback DESC LIMIT :limit")
    suspend fun getTopRatedFeeds(limit: Int): List<LocalFeed>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(feed: LocalFeed)

    @Update
    suspend fun updateFeed(feed: LocalFeed)

    @Update
    suspend fun updateFeedFeedback(feed: LocalFeed)

    @Query("SELECT * FROM ${LocalFeed.TABLE_NAME} WHERE resultId = :resultId")
    suspend fun getFeedByResultId(resultId: Int): LocalFeed?

    // UserBehavior 相关操作
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBehavior(behavior: UserBehavior)

    @Query("SELECT * FROM ${UserBehavior.TABLE_NAME} WHERE timestamp > :since ORDER BY timestamp DESC")
    suspend fun getRecentBehaviors(since: Long): List<UserBehavior>

    @Query("SELECT * FROM ${UserBehavior.TABLE_NAME} WHERE timestamp > :startTime ORDER BY timestamp DESC")
    suspend fun getBehaviorsSince(startTime: Long): List<UserBehavior>
}
