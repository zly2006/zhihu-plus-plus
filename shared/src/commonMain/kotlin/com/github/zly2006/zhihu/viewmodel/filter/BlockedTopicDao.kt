/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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

    @Query("SELECT topicName FROM blocked_topics WHERE topicId = :topicId")
    suspend fun getTopicNameById(topicId: String): String
}
