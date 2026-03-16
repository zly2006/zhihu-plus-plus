package com.github.zly2006.zhihu.util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.charset.StandardCharsets
import java.util.Base64

private val zhidaJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
private val zhidaDoneMarkers = setOf("[DONE]", "DONE")

@Serializable
data class ZhidaSummaryAttachment(
    val type: String = "DOC",
    val value: String,
    val title: String,
)

@Serializable
data class ZhidaSummaryRequest(
    @SerialName("quiz_type")
    val quizType: String = "QT_CHAT",
    val attachments: List<ZhidaSummaryAttachment>,
    @SerialName("message_source_type")
    val messageSourceType: String = "text",
    @SerialName("session_id")
    val sessionId: String = "",
    @SerialName("zhida_source")
    val zhidaSource: String = "one_tap_summary",
    @SerialName("content_id")
    val contentId: String,
    @SerialName("content_type")
    val contentType: String,
    @SerialName("message_content")
    val messageContent: String = "这篇内容讲了什么",
)

@Serializable
data class ZhidaSummarySsePayload(
    val event: String,
    val data: JsonElement = JsonNull,
)

@Serializable
private data class ZhidaSummarySseEnvelope(
    val event: String? = null,
    val data: JsonElement = JsonNull,
)

@Serializable
data class ZhidaSummaryAnswerData(
    val status: Int? = null,
    val delta: Boolean = false,
    val summary: String = "",
)

@Serializable
data class ZhidaSummaryErrorDetail(
    val message: String? = null,
)

@Serializable
data class ZhidaSummaryErrorData(
    val message: String? = null,
    val error: ZhidaSummaryErrorDetail? = null,
)

fun encodeZhidaAttachmentValue(contentId: Long, contentType: String): String {
    val uppercaseType = when (contentType.lowercase()) {
        "answer" -> "ANSWER"
        "article" -> "ARTICLE"
        else -> contentType.uppercase()
    }
    val source = "$contentId|::|$uppercaseType"
    return Base64.getEncoder().encodeToString(source.toByteArray(StandardCharsets.UTF_8))
}

fun buildZhidaSummaryRequest(
    contentId: Long,
    contentType: String,
    title: String,
    messageContent: String = "这篇内容讲了什么",
): ZhidaSummaryRequest = ZhidaSummaryRequest(
    attachments = listOf(
        ZhidaSummaryAttachment(
            value = encodeZhidaAttachmentValue(contentId = contentId, contentType = contentType),
            title = title,
        ),
    ),
    contentId = contentId.toString(),
    contentType = contentType,
    messageContent = messageContent,
)

fun serializeZhidaSummaryRequest(request: ZhidaSummaryRequest): String = zhidaJson.encodeToString(request)

fun parseZhidaSsePayload(data: String, fallbackEvent: String? = null): ZhidaSummarySsePayload? {
    val payload = data.trim()
    if (payload.isBlank() || payload in zhidaDoneMarkers) {
        return null
    }
    val element = runCatching { zhidaJson.parseToJsonElement(payload) }.getOrNull()
    if (element is JsonObject) {
        val looksLikeEnvelope = "event" in element || "data" in element
        if (looksLikeEnvelope) {
            val envelope = runCatching {
                zhidaJson.decodeFromJsonElement<ZhidaSummarySseEnvelope>(element)
            }.getOrNull()
            if (envelope != null) {
                val event = envelope.event ?: fallbackEvent ?: return null
                return ZhidaSummarySsePayload(event = event, data = envelope.data)
            }
        }
        if (fallbackEvent != null) {
            return ZhidaSummarySsePayload(event = fallbackEvent, data = element)
        }
    }
    val fallback = fallbackEvent ?: return null
    val rawData = element ?: JsonPrimitive(payload)
    return ZhidaSummarySsePayload(event = fallback, data = rawData)
}

fun decodeZhidaAnswerData(data: JsonElement): ZhidaSummaryAnswerData? {
    val normalized = normalizeZhidaDataElement(data) ?: return null
    if (normalized is JsonPrimitive) return null
    return runCatching { zhidaJson.decodeFromJsonElement<ZhidaSummaryAnswerData>(normalized) }.getOrNull()
}

fun decodeZhidaStreamErrorMessage(data: JsonElement): String? {
    val normalized = normalizeZhidaDataElement(data) ?: return null
    if (normalized is JsonPrimitive) {
        return normalized.contentOrNull
    }
    val bySerializable = runCatching {
        val parsed = zhidaJson.decodeFromJsonElement<ZhidaSummaryErrorData>(normalized)
        parsed.message ?: parsed.error?.message
    }.getOrNull()
    if (!bySerializable.isNullOrBlank()) {
        return bySerializable
    }
    return runCatching {
        val obj = normalized.jsonObject
        obj["message"]?.jsonPrimitive?.contentOrNull
            ?: obj["error"]
                ?.jsonObject
                ?.get("message")
                ?.jsonPrimitive
                ?.contentOrNull
    }.getOrNull()
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

private fun normalizeZhidaDataElement(data: JsonElement): JsonElement? {
    if (data !is JsonPrimitive) return data
    val content = data.contentOrNull ?: return null
    val trimmed = content.trim()
    if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
        return JsonPrimitive(content)
    }
    return runCatching { zhidaJson.parseToJsonElement(trimmed) }.getOrElse { JsonPrimitive(content) }
}
