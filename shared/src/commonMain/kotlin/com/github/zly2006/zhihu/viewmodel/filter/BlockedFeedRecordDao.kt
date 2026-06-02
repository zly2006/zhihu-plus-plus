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
