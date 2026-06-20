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
import kotlin.random.Random
import kotlin.time.Clock

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

internal fun newPublishTraceId(): String =
    "${Clock.System.now().toEpochMilliseconds()},${randomUuidV4()}"

private fun randomUuidV4(random: Random = Random.Default): String {
    val bytes = ByteArray(16) { random.nextInt(256).toByte() }
    bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x40).toByte()
    bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()

    return buildString(36) {
        bytes.forEachIndexed { index, byte ->
            if (index == 4 || index == 6 || index == 8 || index == 10) {
                append('-')
            }
            val value = byte.toInt() and 0xff
            append(UUID_HEX[value ushr 4])
            append(UUID_HEX[value and 0x0f])
        }
    }
}

private val UUID_HEX = "0123456789abcdef".toCharArray()
