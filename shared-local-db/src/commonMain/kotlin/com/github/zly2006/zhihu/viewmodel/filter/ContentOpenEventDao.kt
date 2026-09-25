/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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
import androidx.room.Query

@Dao
interface ContentOpenEventDao {
    @Insert
    suspend fun insert(event: ContentOpenEvent): Long

    /** 按类型和 ID 查询，复用联合索引，避免拼接表达式扫描全部历史。 */
    suspend fun getOpenedContentKeysByKeys(keys: List<String>): List<String> =
        queryContentKeys(keys, ::getOpenedContentKeys)

    suspend fun getRemotelySyncedContentKeysByKeys(keys: List<String>): List<String> =
        queryContentKeys(keys, ::getRemotelySyncedContentKeys)

    @Query(
        """
        SELECT DISTINCT contentType || ':' || contentId
        FROM ${ContentOpenEvent.TABLE_NAME}
        WHERE contentType = :type AND contentId IN (:ids)
        """,
    )
    suspend fun getOpenedContentKeys(type: String, ids: List<String>): List<String>

    @Query(
        """
        SELECT DISTINCT contentType || ':' || contentId
        FROM ${ContentOpenEvent.TABLE_NAME}
        WHERE contentType = :type AND contentId IN (:ids) AND openFrom = 'remote_sync'
        """,
    )
    suspend fun getRemotelySyncedContentKeys(type: String, ids: List<String>): List<String>
}

private suspend fun queryContentKeys(
    keys: List<String>,
    query: suspend (String, List<String>) -> List<String>,
): List<String> = keys.distinct().groupBy { it.substringBefore(':') }.flatMap { (type, typedKeys) ->
    // Leave room for the type parameter on SQLite versions with a 999-variable limit.
    typedKeys.map { it.substringAfter(':') }.chunked(500).flatMap { ids -> query(type, ids) }
}
