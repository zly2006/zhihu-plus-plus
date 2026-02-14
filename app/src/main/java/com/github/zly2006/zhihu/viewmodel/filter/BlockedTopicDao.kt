package com.github.zly2006.zhihu.viewmodel.filter

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * 屏蔽主题数据访问对象
 */
@Dao
interface BlockedTopicDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: BlockedTopic): Long

    @Query("DELETE FROM blocked_topics WHERE topicId = :topicId")
    suspend fun deleteTopicById(topicId: String)

    @Query("SELECT * FROM blocked_topics ORDER BY addedTime DESC")
    suspend fun getAllTopics(): List<BlockedTopic>

    @Query("SELECT COUNT(*) FROM blocked_topics")
    suspend fun getTopicCount(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_topics WHERE topicId = :topicId)")
    suspend fun isTopicBlocked(topicId: String): Boolean

    @Query("DELETE FROM blocked_topics")
    suspend fun clearAllTopics()

    @Query("SELECT topicId FROM blocked_topics WHERE topicId IN (:topicIds)")
    suspend fun getBlockedTopicIds(topicIds: List<String>): List<String>
}
