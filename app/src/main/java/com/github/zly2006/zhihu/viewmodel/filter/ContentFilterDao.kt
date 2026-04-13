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

    @Query("SELECT id FROM ${ContentViewRecord.TABLE_NAME} WHERE id IN (:ids) AND viewCount > :maxCount AND hasInteraction = 0")
    suspend fun getFilteredContentIdsByIds(ids: List<String>, maxCount: Int = ContentViewRecord.MAX_VIEW_COUNT_WITHOUT_INTERACTION): List<String>

    @Query("SELECT id FROM ${ContentViewRecord.TABLE_NAME} WHERE id IN (:ids)")
    suspend fun getViewedContentIdsByIds(ids: List<String>): List<String>

    @Query("DELETE FROM ${ContentViewRecord.TABLE_NAME} WHERE firstViewTime < :cutoffTime")
    suspend fun cleanupOldRecords(cutoffTime: Long)

    @Query("DELETE FROM ${ContentViewRecord.TABLE_NAME}")
    suspend fun clearAllRecords()

    @Query("SELECT COUNT(*) FROM ${ContentViewRecord.TABLE_NAME}")
    suspend fun getRecordCount(): Int
}
