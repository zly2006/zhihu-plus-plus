package com.github.zly2006.zhihu.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import java.nio.charset.StandardCharsets
import java.util.Base64

private val zhidaJson = Json { ignoreUnknownKeys = true }
private val zhidaDoneMarkers = setOf("[DONE]", "DONE")
private val summaryTextKeys = setOf("content", "text", "summary", "answer")

fun encodeZhidaAttachmentValue(contentId: Long, contentType: String): String {
    val uppercaseType = when (contentType.lowercase()) {
        "answer" -> "ANSWER"
        "article" -> "ARTICLE"
        else -> contentType.uppercase()
    }
    val source = "$contentId|::|$uppercaseType"
    return Base64.getEncoder().encodeToString(source.toByteArray(StandardCharsets.UTF_8))
}

fun buildZhidaSummaryPayload(
    contentId: Long,
    contentType: String,
    title: String,
    messageContent: String = "这篇内容讲了什么",
): String = buildJsonObject {
    put("quiz_type", "QT_CHAT")
    put(
        "attachments",
        buildJsonArray {
            add(
                buildJsonObject {
                    put("type", "DOC")
                    put("value", encodeZhidaAttachmentValue(contentId, contentType))
                    put("title", title)
                },
            )
        },
    )
    put("message_source_type", "text")
    put("session_id", "")
    put("zhida_source", "one_tap_summary")
    put("content_id", contentId.toString())
    put("content_type", contentType)
    put("message_content", messageContent)
}.toString()

fun parseSummaryChunkFromSseData(data: String): String? {
    val payload = data.trim()
    if (payload.isBlank() || payload in zhidaDoneMarkers) {
        return null
    }

    val jsonElement = runCatching { zhidaJson.parseToJsonElement(payload) }.getOrNull()
    if (jsonElement == null) {
        return payload
    }
    if (jsonElement !is JsonObject) {
        return payload
    }
    return extractSummaryChunk(jsonElement)
}

fun mergeSummaryChunk(current: String, chunk: String): String {
    if (chunk.isBlank()) return current
    if (current.isBlank()) return chunk
    return when {
        chunk.startsWith(current) -> chunk
        current.endsWith(chunk) -> current
        else -> current + chunk
    }
}

private fun extractSummaryChunk(element: JsonElement): String? {
    if (element !is JsonObject) return null

    // Common SSE shapes (OpenAI-like or Zhihu wrapped payloads)
    val directCandidates = listOf(
        element.path("choices", 0, "delta", "content"),
        element.path("choices", 0, "message", "content"),
        element.path("choices", 0, "content"),
        element.path("data", "delta", "content"),
        element.path("data", "message", "content"),
        element.path("data", "content"),
        element.path("data", "text"),
        element.path("message", "content"),
        element.path("content"),
        element.path("text"),
    )
    directCandidates.firstOrNull { !it.isNullOrBlank() }?.let { return it }

    // Some payloads nest another JSON string inside `data`.
    val nestedString = element.path("data")
    if (!nestedString.isNullOrBlank()) {
        val nestedJson = runCatching { zhidaJson.parseToJsonElement(nestedString) }.getOrNull()
        if (nestedJson != null) {
            val nestedChunk = extractSummaryChunk(nestedJson)
            if (!nestedChunk.isNullOrBlank()) {
                return nestedChunk
            }
        }
        return nestedString
    }

    findSummaryByKeys(element)?.let { return it }

    return null
}

private fun JsonObject.path(vararg segments: Any): String? {
    var cursor: JsonElement = this
    for (segment in segments) {
        cursor = when {
            segment is String && cursor is JsonObject -> cursor[segment] ?: return null
            segment is Int && cursor is JsonArray -> cursor.getOrNull(segment) ?: return null
            else -> return null
        }
    }
    return (cursor as? JsonPrimitive)?.contentOrNull
}

private fun findSummaryByKeys(element: JsonElement): String? = when (element) {
    is JsonObject -> {
        summaryTextKeys.forEach { key ->
            val value = (element[key] as? JsonPrimitive)?.contentOrNull
            if (!value.isNullOrBlank()) return value
        }
        element.values.firstNotNullOfOrNull { findSummaryByKeys(it) }
    }
    is JsonArray -> element.firstNotNullOfOrNull { findSummaryByKeys(it) }
    else -> null
}
