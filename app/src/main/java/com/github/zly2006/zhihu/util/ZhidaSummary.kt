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

package com.github.zly2006.zhihu.util

typealias ZhidaSummaryAttachment = com.github.zly2006.zhihu.shared.util.ZhidaSummaryAttachment
typealias ZhidaSummaryRequest = com.github.zly2006.zhihu.shared.util.ZhidaSummaryRequest
typealias ZhidaSummarySsePayload = com.github.zly2006.zhihu.shared.util.ZhidaSummarySsePayload
typealias ZhidaSummaryAnswerData = com.github.zly2006.zhihu.shared.util.ZhidaSummaryAnswerData
typealias ZhidaSummaryErrorDetail = com.github.zly2006.zhihu.shared.util.ZhidaSummaryErrorDetail
typealias ZhidaSummaryErrorData = com.github.zly2006.zhihu.shared.util.ZhidaSummaryErrorData

fun encodeZhidaAttachmentValue(contentId: Long, contentType: String): String =
    com.github.zly2006.zhihu.shared.util
        .encodeZhidaAttachmentValue(contentId, contentType)

fun buildZhidaSummaryRequest(
    contentId: Long,
    contentType: String,
    title: String,
    messageContent: String = "这篇内容讲了什么",
): ZhidaSummaryRequest = com.github.zly2006.zhihu.shared.util.buildZhidaSummaryRequest(
    contentId = contentId,
    contentType = contentType,
    title = title,
    messageContent = messageContent,
)

fun serializeZhidaSummaryRequest(request: ZhidaSummaryRequest): String =
    com.github.zly2006.zhihu.shared.util
        .serializeZhidaSummaryRequest(request)

fun parseZhidaSsePayload(
    data: String,
    fallbackEvent: String? = null,
): ZhidaSummarySsePayload? = com.github.zly2006.zhihu.shared.util
    .parseZhidaSsePayload(data, fallbackEvent)

fun decodeZhidaAnswerData(data: kotlinx.serialization.json.JsonElement): ZhidaSummaryAnswerData? =
    com.github.zly2006.zhihu.shared.util
        .decodeZhidaAnswerData(data)

fun decodeZhidaStreamErrorMessage(data: kotlinx.serialization.json.JsonElement): String? =
    com.github.zly2006.zhihu.shared.util
        .decodeZhidaStreamErrorMessage(data)

fun mergeSummaryChunk(current: String, chunk: String): String =
    com.github.zly2006.zhihu.shared.util
        .mergeSummaryChunk(current, chunk)
