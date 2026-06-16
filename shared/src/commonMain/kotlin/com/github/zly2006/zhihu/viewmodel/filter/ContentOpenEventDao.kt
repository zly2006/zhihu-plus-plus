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
import androidx.room.Query

@Dao
interface ContentOpenEventDao {
    @Insert
    suspend fun insert(event: ContentOpenEvent): Long

    /** 批量查询已打开过的内容身份键，供详情页导航或内容级已读判断使用。 */
    @Query(
        """
        SELECT contentType || ':' || contentId
        FROM ${ContentOpenEvent.TABLE_NAME}
        WHERE (contentType || ':' || contentId) IN (:keys)
        """,
    )
    suspend fun getOpenedContentKeysByKeys(keys: List<String>): List<String>

    @Query(
        """
        SELECT contentType || ':' || contentId
        FROM ${ContentOpenEvent.TABLE_NAME}
        WHERE (contentType || ':' || contentId) IN (:keys)
        AND openedAt >= :openedAfter
        """,
    )
    suspend fun getOpenedContentKeysByKeysSince(
        keys: List<String>,
        openedAfter: Long,
    ): List<String>

    @Query(
        """
        SELECT DISTINCT questionId
        FROM ${ContentOpenEvent.TABLE_NAME}
        WHERE questionId IN (:questionIds)
        AND openedAt >= :openedAfter
        """,
    )
    suspend fun getOpenedQuestionIdsSince(
        questionIds: List<Long>,
        openedAfter: Long,
    ): List<Long>
}
