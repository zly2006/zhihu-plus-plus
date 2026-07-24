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

package com.chloemlla.zhplus.shared.comment
import com.chloemlla.zhplus.shared.data.DataHolder
import com.chloemlla.zhplus.shared.data.ZhihuJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import com.chloemlla.zhplus.viewmodel.comment.CommentSortOrder as CommentSortOrderImpl

fun decodeZhihuCommentData(
    json: JsonObject,
    limit: Int,
): List<DataHolder.Comment> =
    json["data"]
        ?.jsonArray
        ?.mapNotNull { element ->
            runCatching {
                ZhihuJson.decodeJson<DataHolder.Comment>(element)
            }.getOrNull()
        }?.take(limit)
        .orEmpty()

// Re-export from viewmodel package for backward compatibility with tests
typealias CommentSortOrder = CommentSortOrderImpl
