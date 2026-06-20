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

package com.github.zly2006.zhihu.editor

import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

/**
 * publish 接口的响应是“双层 JSON”：
 * - response.data.result 是 JSON 字符串，需要再次 parse 才能拿到发布后的内容 id。
 *
 * 这里不强依赖完整数据模型，只提取回答和想法发布都需要的字段。
 */
fun parsePublishContentId(resultText: String): Long? =
    runCatching {
        ZhihuJson.json.decodeFromString(DataHolder.PublishResult.serializer(), resultText)
    }.getOrNull()
        ?.let { result -> result.publish?.id ?: result.id }
        ?.toLongOrNull()

@Serializable
data class PublishTrace(
    val traceId: String,
)

@Serializable
data class PublishCommentsPermission(
    @SerialName("comment_permission")
    val commentPermission: String = "all",
)
