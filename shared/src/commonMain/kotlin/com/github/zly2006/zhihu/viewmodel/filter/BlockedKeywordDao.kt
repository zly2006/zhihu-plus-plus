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
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BlockedKeywordDao {
    @Query("SELECT * FROM ${BlockedKeyword.TABLE_NAME} ORDER BY createdTime DESC")
    suspend fun getAllKeywords(): List<BlockedKeyword>

    @Query("SELECT * FROM ${BlockedKeyword.TABLE_NAME} WHERE id = :id")
    suspend fun getKeywordById(id: Long): BlockedKeyword?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeyword(keyword: BlockedKeyword): Long

    @Delete
    suspend fun deleteKeyword(keyword: BlockedKeyword)

    @Query("DELETE FROM ${BlockedKeyword.TABLE_NAME} WHERE id = :id")
    suspend fun deleteKeywordById(id: Long)

    @Query("DELETE FROM ${BlockedKeyword.TABLE_NAME}")
    suspend fun clearAllKeywords()

    @Query("SELECT COUNT(*) FROM ${BlockedKeyword.TABLE_NAME}")
    suspend fun getKeywordCount(): Int
}
