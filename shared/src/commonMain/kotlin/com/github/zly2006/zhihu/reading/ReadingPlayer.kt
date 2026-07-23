/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.github.zly2006.zhihu.reading

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import com.fleeksoft.ksoup.Ksoup
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.navDestination
import com.github.zly2006.zhihu.shared.data.target
import com.github.zly2006.zhihu.shared.platform.SettingsStore
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Instant

const val READING_PREFERENCES_KEY = "continuousReadingPreferences"
const val READING_PLAYBACK_SPEED_KEY = "continuousReadingPlaybackSpeed"
const val DEFAULT_READING_PLAYBACK_SPEED = 1f
const val MIN_READING_PLAYBACK_SPEED = 0.5f
const val MAX_READING_PLAYBACK_SPEED = 2f

private const val MAX_READING_QUEUE_SIZE = 100
private const val MAX_READING_COMMENT_COUNT = 50
private const val MAX_REGISTERED_READING_SOURCES = 32

@Serializable
enum class ReadingContentType(
    val displayName: String,
) {
    Answer("回答"),
    Article("文章"),
    Pin("想法"),
    Question("问题"),
}

@Serializable
enum class ReadingTemplateField(
    val displayName: String,
    val description: String,
) {
    ContentType("内容类型", "例如“这是一条回答”"),
    Title("标题", "回答的问题标题或专栏文章标题"),
    Author("作者", "内容作者名称"),
    Body("正文", "内容的主要文字"),
    PublishedAt("发布时间", "内容的发布时间"),
    VoteUpCount("点赞数", "当前赞同或点赞数量"),
    Comments("评论区", "按所选排序朗读前 N 条评论"),
}

@Serializable
enum class ReadingCommentOrder(
    val displayName: String,
    val apiValue: String,
) {
    Score("按热度", "score"),
    Time("按时间", "ts"),
}

@Serializable
enum class ReadingPublishedTimeMode(
    val displayName: String,
) {
    Absolute("绝对时间"),
    Relative("相对时间"),
}

@Serializable
enum class ReadingRelativeTimePrecision(
    val displayName: String,
) {
    Second("秒"),
    Hour("时"),
    Day("天"),
}

@Serializable
data class ReadingPreferences(
    val fieldOrder: List<ReadingTemplateField> = ReadingTemplateField.entries,
    val enabledFields: Set<ReadingTemplateField> = setOf(
        ReadingTemplateField.ContentType,
        ReadingTemplateField.Title,
        ReadingTemplateField.Author,
        ReadingTemplateField.Body,
    ),
    val publishedTimeMode: ReadingPublishedTimeMode = ReadingPublishedTimeMode.Absolute,
    val relativeTimePrecision: ReadingRelativeTimePrecision = ReadingRelativeTimePrecision.Second,
    val commentCount: Int = 3,
    val commentOrder: ReadingCommentOrder = ReadingCommentOrder.Score,
    val readCommentAuthor: Boolean = true,
    val queueLimit: Int = 5,
    val transitionText: String = "本条内容朗读完毕，接下来朗读第 {index} 条，共 {total} 条：{contentType}。",
) {
    fun normalized(): ReadingPreferences {
        val normalizedCommentCount = commentCount.coerceIn(0, MAX_READING_COMMENT_COUNT)
        val normalizedOrder = buildList {
            addAll(fieldOrder.distinct())
            addAll(ReadingTemplateField.entries.filterNot(::contains))
        }
        val normalizedEnabledFields = enabledFields
            .intersect(ReadingTemplateField.entries.toSet())
            .ifEmpty { setOf(ReadingTemplateField.Body) }
            .let { fields ->
                if (fields == setOf(ReadingTemplateField.Comments) && normalizedCommentCount == 0) {
                    setOf(ReadingTemplateField.Body)
                } else {
                    fields
                }
            }
        return copy(
            fieldOrder = normalizedOrder,
            enabledFields = normalizedEnabledFields,
            commentCount = normalizedCommentCount,
            queueLimit = queueLimit.coerceIn(1, MAX_READING_QUEUE_SIZE),
        )
    }

    val shouldLoadComments: Boolean
        get() = ReadingTemplateField.Comments in enabledFields && commentCount > 0
}

@Serializable
data class ReadingQueueItem(
    val contentType: ReadingContentType,
    val id: Long,
    val title: String = "",
    val author: String = "",
    val questionId: Long? = null,
    val bodyHtml: String? = null,
    val publishedAt: Long = 0L,
    val updatedAt: Long = 0L,
    val voteUpCount: Int = -1,
    val commentCount: Int = -1,
) {
    val key: String
        get() = "${contentType.name}:$id"

    val displayTitle: String
        get() = title
            .cleanReadingMetadata()
            .takeIf(String::isNotBlank)
            ?: when (contentType) {
                ReadingContentType.Pin -> author.takeIf(String::isNotBlank)?.let { "${it}的想法" } ?: "想法"
                else -> contentType.displayName
            }

    fun withoutBody(): ReadingQueueItem = if (bodyHtml == null) this else copy(bodyHtml = null)

    fun toDestination(sourceId: String? = null): NavDestination = when (contentType) {
        ReadingContentType.Answer -> Article(
            title = title,
            type = ArticleType.Answer,
            id = id,
            authorName = author,
            readingQueueSourceId = sourceId,
        )
        ReadingContentType.Article -> Article(
            title = title,
            type = ArticleType.Article,
            id = id,
            authorName = author,
            readingQueueSourceId = sourceId,
        )
        ReadingContentType.Pin -> Pin(
            id = id,
            authorName = author,
            readingQueueSourceId = sourceId,
        )
        ReadingContentType.Question -> Question(
            questionId = id,
            title = title,
            readingQueueSourceId = sourceId,
        )
    }
}

@Serializable
data class ReadingComment(
    val author: String,
    val body: String,
)

data class ResolvedReadingContent(
    val contentType: ReadingContentType,
    val title: String,
    val author: String,
    val body: String,
    val publishedAt: Long,
    val voteUpCount: Int,
    val comments: List<ReadingComment>,
    val updatedAt: Long = 0L,
)

enum class ReadingPlaybackStatus {
    Idle,
    Initializing,
    Loading,
    Playing,
    Paused,
    Error,
}

data class ReadingPlayerState(
    val status: ReadingPlaybackStatus = ReadingPlaybackStatus.Idle,
    val queue: List<ReadingQueueItem> = emptyList(),
    val currentIndex: Int = -1,
    val currentChunkIndex: Int = 0,
    val totalChunks: Int = 0,
    val errorMessage: String? = null,
    val engineLabel: String = "",
    val availableEngineLabels: List<String> = emptyList(),
    val sourceId: String? = null,
    val playbackSpeed: Float = DEFAULT_READING_PLAYBACK_SPEED,
) {
    val hasSession: Boolean
        get() = queue.isNotEmpty() && currentIndex in queue.indices

    val currentItem: ReadingQueueItem?
        get() = queue.getOrNull(currentIndex)

    val canPlayPrevious: Boolean
        get() = hasSession && currentIndex > 0

    val canPlayNext: Boolean
        get() = hasSession && currentIndex < queue.lastIndex

    val isActivelyPlaying: Boolean
        get() = status == ReadingPlaybackStatus.Initializing ||
            status == ReadingPlaybackStatus.Loading ||
            status == ReadingPlaybackStatus.Playing
}

data class ReadingStartRequest(
    val queue: List<ReadingQueueItem>,
    val preferences: ReadingPreferences,
    val startIndex: Int = 0,
    val sourceId: String? = null,
    val playbackSpeed: Float = DEFAULT_READING_PLAYBACK_SPEED,
)

interface ReadingPlayerController {
    val state: State<ReadingPlayerState>
    val isSupported: Boolean

    fun start(request: ReadingStartRequest)

    fun togglePlayPause()

    fun playPrevious()

    fun playNext()

    fun playAt(index: Int)

    fun setPlaybackSpeed(speed: Float)

    fun stop()
}

@Composable
expect fun rememberReadingPlayerController(): ReadingPlayerController

private val readingPreferencesJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun loadReadingPreferences(settings: SettingsStore): ReadingPreferences = settings
    .getStringOrNull(READING_PREFERENCES_KEY)
    ?.let { encoded ->
        runCatching {
            readingPreferencesJson.decodeFromString<ReadingPreferences>(encoded)
        }.getOrNull()
    }?.normalized()
    ?: ReadingPreferences()

fun saveReadingPreferences(
    settings: SettingsStore,
    preferences: ReadingPreferences,
) {
    settings.putString(
        READING_PREFERENCES_KEY,
        readingPreferencesJson.encodeToString(preferences.normalized()),
    )
}

fun normalizeReadingPlaybackSpeed(speed: Float): Float = if (speed.isFinite()) {
    speed.coerceIn(MIN_READING_PLAYBACK_SPEED, MAX_READING_PLAYBACK_SPEED)
} else {
    DEFAULT_READING_PLAYBACK_SPEED
}

fun loadReadingPlaybackSpeed(settings: SettingsStore): Float = normalizeReadingPlaybackSpeed(
    settings.getFloat(READING_PLAYBACK_SPEED_KEY, DEFAULT_READING_PLAYBACK_SPEED),
)

fun saveReadingPlaybackSpeed(
    settings: SettingsStore,
    speed: Float,
) {
    settings.putFloat(READING_PLAYBACK_SPEED_KEY, normalizeReadingPlaybackSpeed(speed))
}

fun buildReadingSpeechText(
    content: ResolvedReadingContent,
    preferences: ReadingPreferences,
    nowEpochSeconds: Long = Clock.System.now().epochSeconds,
): String {
    val normalized = preferences.normalized()
    return normalized.fieldOrder
        .asSequence()
        .filter { it in normalized.enabledFields }
        .mapNotNull { field ->
            when (field) {
                ReadingTemplateField.ContentType -> when (content.contentType) {
                    ReadingContentType.Answer -> "这是一条回答。"
                    ReadingContentType.Article -> "这是一篇专栏文章。"
                    ReadingContentType.Pin -> "这是一条想法。"
                    ReadingContentType.Question -> "这是一个问题。"
                }
                ReadingTemplateField.Title ->
                    content.title
                        .cleanReadingMetadata()
                        .takeIf(String::isNotBlank)
                        ?.let { "标题：$it。" }
                ReadingTemplateField.Author ->
                    content.author
                        .cleanReadingMetadata()
                        .takeIf(String::isNotBlank)
                        ?.let { "作者：$it。" }
                ReadingTemplateField.Body -> content.body.takeIf(String::isNotBlank)
                ReadingTemplateField.PublishedAt ->
                    when (normalized.publishedTimeMode) {
                        ReadingPublishedTimeMode.Absolute ->
                            content.publishedAt
                                .takeIf { it > 0 }
                                ?.let { "发布时间：${formatReadingDateTime(it)}。" }
                        ReadingPublishedTimeMode.Relative ->
                            content.updatedAt
                                .takeIf { it > 0 }
                                ?.let {
                                    "最后编辑于${formatReadingRelativeTime(it, nowEpochSeconds, normalized.relativeTimePrecision)}。"
                                }
                    }
                ReadingTemplateField.VoteUpCount ->
                    content.voteUpCount
                        .takeIf { it >= 0 }
                        ?.let { "点赞数：$it。" }
                ReadingTemplateField.Comments ->
                    content.comments
                        .takeIf(List<ReadingComment>::isNotEmpty)
                        ?.mapIndexed { index, comment ->
                            buildString {
                                append("第")
                                append(index + 1)
                                append("条评论。")
                                if (normalized.readCommentAuthor && comment.author.isNotBlank()) {
                                    append("评论作者：")
                                    append(comment.author)
                                    append("。")
                                }
                                append(comment.body)
                            }
                        }?.joinToString("\n")
            }
        }.joinToString("\n")
        .trim()
}

fun buildReadingTemplatePreview(preferences: ReadingPreferences): String {
    val normalized = preferences.normalized()
    return normalized.fieldOrder
        .asSequence()
        .filter { it in normalized.enabledFields }
        .mapNotNull { field ->
            when (field) {
                ReadingTemplateField.ContentType -> "{内容类型}"
                ReadingTemplateField.Title -> "标题：{标题}。"
                ReadingTemplateField.Author -> "作者：{作者}。"
                ReadingTemplateField.Body -> "{正文}"
                ReadingTemplateField.PublishedAt -> when (normalized.publishedTimeMode) {
                    ReadingPublishedTimeMode.Absolute -> "发布时间：{绝对时间}。"
                    ReadingPublishedTimeMode.Relative ->
                        "最后编辑于{距最后编辑时间，精确到${normalized.relativeTimePrecision.displayName}}。"
                }
                ReadingTemplateField.VoteUpCount -> "点赞数：{点赞数}。"
                ReadingTemplateField.Comments ->
                    normalized.commentCount
                        .takeIf { it > 0 }
                        ?.let { count ->
                            buildString {
                                append("第{评论序号（1-")
                                append(count)
                                append("）}条评论。")
                                if (normalized.readCommentAuthor) {
                                    append("评论作者：{评论作者}。")
                                }
                                append("{评论正文}")
                            }
                        }
            }
        }.joinToString("\n")
        .trim()
}

fun renderReadingTransition(
    template: String,
    nextItem: ReadingQueueItem,
    nextIndex: Int,
    total: Int,
): String {
    val author = nextItem.author.cleanReadingMetadata()
    val normalizedTemplate = if (author.isBlank()) {
        template.replace(
            Regex("(?:[，,]\\s*)?(?:作者|author)\\s*[：:]?\\s*\\{author}", RegexOption.IGNORE_CASE),
            "",
        )
    } else {
        template
    }
    return normalizedTemplate
        .replace("{index}", (nextIndex + 1).toString())
        .replace("{total}", total.toString())
        .replace("{contentType}", nextItem.contentType.displayName)
        .replace("{title}", nextItem.displayTitle)
        .replace("{author}", author)
        .replace("，。", "。")
        .replace(",。", "。")
        .trim()
}

fun ReadingQueueItem.hasReadableFields(preferences: ReadingPreferences): Boolean {
    val normalized = preferences.normalized()
    return normalized.enabledFields.any { field ->
        when (field) {
            ReadingTemplateField.ContentType -> true
            ReadingTemplateField.Title -> title.cleanReadingMetadata().isNotBlank()
            ReadingTemplateField.Author -> author.cleanReadingMetadata().isNotBlank()
            ReadingTemplateField.Body -> !bodyHtml.isNullOrBlank()
            ReadingTemplateField.PublishedAt -> when (normalized.publishedTimeMode) {
                ReadingPublishedTimeMode.Absolute -> publishedAt > 0
                ReadingPublishedTimeMode.Relative -> updatedAt > 0
            }
            ReadingTemplateField.VoteUpCount -> voteUpCount >= 0
            ReadingTemplateField.Comments -> normalized.shouldLoadComments
        }
    }
}

/**
 * Android TextToSpeech.stop() cannot resume inside an utterance, so these chunks also serve as
 * pause/resume checkpoints. Sentence boundaries minimize replay while [maxLength] bounds text without punctuation.
 *
 * https://developer.android.com/reference/android/speech/tts/TextToSpeech#stop()
 */
fun splitReadingSpeechIntoChunks(
    text: String,
    maxLength: Int = 120,
): List<String> {
    if (text.isBlank()) return emptyList()
    require(maxLength > 0)

    val sentenceEnds = setOf('。', '！', '？', '!', '?', '…', '\n')
    val clauseEnds = setOf('，', ',', '；', ';', '：', ':')
    val sentenceClosings = setOf('”', '’', '"', '\'', '）', ')', '】', ']', '》', '〉', '」', '』')
    val chunks = mutableListOf<String>()
    var start = 0
    while (start < text.length) {
        while (start < text.length && text[start].isWhitespace()) start++
        if (start >= text.length) break

        var hardEnd = start
        var length = 0
        while (hardEnd < text.length && length < maxLength) {
            val current = text[hardEnd++]
            if (
                current in '\uD800'..'\uDBFF' &&
                hardEnd < text.length &&
                text[hardEnd] in '\uDC00'..'\uDFFF'
            ) {
                hardEnd++
            }
            length++
        }

        var sentenceEnd: Int? = null
        var cursor = start
        while (cursor < hardEnd) {
            val current = text[cursor]
            val isAsciiPeriod = current == '.' &&
                (
                    cursor + 1 >= text.length ||
                        text[cursor + 1].isWhitespace() ||
                        text[cursor + 1] in sentenceClosings
                )
            if (current in sentenceEnds || isAsciiPeriod) {
                sentenceEnd = cursor + 1
                while (
                    sentenceEnd < hardEnd &&
                    (
                        text[sentenceEnd] in sentenceEnds ||
                            text[sentenceEnd] == '.' ||
                            text[sentenceEnd] in sentenceClosings
                    )
                ) {
                    sentenceEnd++
                }
                break
            }
            cursor++
        }

        val end = sentenceEnd ?: if (hardEnd < text.length) {
            (hardEnd - 1 downTo start)
                .firstOrNull { text[it] in clauseEnds }
                ?.plus(1)
                ?: hardEnd
        } else {
            hardEnd
        }
        text
            .substring(start, end)
            .trim()
            .takeIf(String::isNotEmpty)
            ?.let(chunks::add)
        start = end
    }
    return chunks
}

object ReadingQueueSourceRegistry {
    private val sources = LinkedHashMap<String, List<ReadingQueueItem>>()

    fun register(
        sourceId: String,
        items: List<ReadingQueueItem>,
    ) {
        val normalizedItems = items
            .distinctBy(ReadingQueueItem::key)
            .map(ReadingQueueItem::withoutBody)
        if (normalizedItems.isEmpty()) {
            sources.remove(sourceId)
            return
        }
        if (sources[sourceId] == normalizedItems) return

        sources.remove(sourceId)
        sources[sourceId] = normalizedItems
        while (sources.size > MAX_REGISTERED_READING_SOURCES) {
            val oldestSourceId = sources.keys.first()
            sources.remove(oldestSourceId)
        }
    }

    fun queueStartingAt(
        current: ReadingQueueItem,
        sourceId: String?,
        limit: Int,
        fallbackAfterCurrent: List<ReadingQueueItem> = emptyList(),
    ): List<ReadingQueueItem> {
        val safeLimit = limit.coerceIn(1, MAX_READING_QUEUE_SIZE)
        val source = sourceId?.let(sources::get)
        val currentIndex = source?.indexOfFirst { it.key == current.key } ?: -1
        if (source == null || currentIndex < 0) {
            return (listOf(current) + fallbackAfterCurrent)
                .distinctBy(ReadingQueueItem::key)
                .take(safeLimit)
        }

        val sourceQueue = source
            .drop(currentIndex)
            .take(safeLimit)
            .map { item -> if (item.key == current.key) current else item }
        if (fallbackAfterCurrent.isEmpty()) return sourceQueue

        val sourceContinuationKeys = sourceQueue.drop(1).map(ReadingQueueItem::key)
        val fallbackKeys = fallbackAfterCurrent
            .distinctBy(ReadingQueueItem::key)
            .map(ReadingQueueItem::key)
        if (
            sourceContinuationKeys.isNotEmpty() &&
            fallbackKeys.take(sourceContinuationKeys.size) != sourceContinuationKeys
        ) {
            return sourceQueue
        }

        return (sourceQueue + fallbackAfterCurrent)
            .distinctBy(ReadingQueueItem::key)
            .take(safeLimit)
    }

    internal fun clearForTesting() {
        sources.clear()
    }
}

@Composable
fun RegisterReadingQueueSource(
    sourceId: String,
    items: List<FeedDisplayItem>,
) {
    val queueItems = items.toReadingQueueSourceItems()
    SideEffect {
        ReadingQueueSourceRegistry.register(sourceId, queueItems)
    }
}

internal fun List<FeedDisplayItem>.toReadingQueueSourceItems(): List<ReadingQueueItem> =
    asSequence()
        .filterNot { it.isFiltered }
        .mapNotNull(FeedDisplayItem::toReadingQueueItem)
        .toList()

fun FeedDisplayItem.toReadingQueueItem(): ReadingQueueItem? {
    val destination = navDestination ?: return null
    return raw?.toReadingQueueItem(destination)
        ?: feed?.target?.toReadingQueueItem()
        ?: when (destination) {
            is Article -> ReadingQueueItem(
                contentType = when (destination.type) {
                    ArticleType.Answer -> ReadingContentType.Answer
                    ArticleType.Article -> ReadingContentType.Article
                },
                id = destination.id,
                title = title.cleanReadingMetadata().ifBlank { destination.title.cleanReadingMetadata() },
                author = authorName.cleanReadingMetadata().ifBlank { destination.authorName.cleanReadingMetadata() },
            )
            is Pin -> ReadingQueueItem(
                contentType = ReadingContentType.Pin,
                id = destination.id,
                author = authorName.cleanReadingMetadata().ifBlank { destination.authorName.cleanReadingMetadata() },
            )
            is Question -> ReadingQueueItem(
                contentType = ReadingContentType.Question,
                id = destination.questionId,
                title = title.cleanReadingMetadata().ifBlank { destination.title.cleanReadingMetadata() },
            )
            else -> null
        }
}

private fun Feed.Target.toReadingQueueItem(): ReadingQueueItem? = when (this) {
    is Feed.AnswerTarget -> ReadingQueueItem(
        contentType = ReadingContentType.Answer,
        id = id,
        title = question.title,
        author = author?.name.orEmpty(),
        questionId = question.id,
        bodyHtml = content.takeIf(String::isNotBlank),
        publishedAt = createdTime,
        updatedAt = updatedTime,
        voteUpCount = voteupCount,
        commentCount = commentCount,
    )
    is Feed.ArticleTarget -> ReadingQueueItem(
        contentType = ReadingContentType.Article,
        id = id,
        title = title,
        author = author.name,
        bodyHtml = content.takeIf(String::isNotBlank),
        publishedAt = created,
        updatedAt = updated,
        voteUpCount = voteupCount,
        commentCount = commentCount,
    )
    is Feed.PinTarget -> ReadingQueueItem(
        contentType = ReadingContentType.Pin,
        id = id,
        author = author.name,
        bodyHtml = contentHtml.takeIf(String::isNotBlank),
        publishedAt = created,
        updatedAt = updated,
        voteUpCount = likeCount,
        commentCount = commentCount,
    )
    is Feed.QuestionTarget -> ReadingQueueItem(
        contentType = ReadingContentType.Question,
        id = id,
        title = title,
        bodyHtml = detail.takeIf(String::isNotBlank),
        publishedAt = created,
        updatedAt = updatedTime,
        commentCount = commentCount,
    )
    else -> null
}

fun DataHolder.Content.toReadingQueueItem(destination: NavDestination): ReadingQueueItem? = when (this) {
    is DataHolder.Answer -> ReadingQueueItem(
        contentType = ReadingContentType.Answer,
        id = id,
        title = question.title,
        author = author.name,
        questionId = question.id,
        bodyHtml = content,
        publishedAt = createdTime,
        updatedAt = updatedTime,
        voteUpCount = voteupCount,
        commentCount = commentCount,
    )
    is DataHolder.Article -> ReadingQueueItem(
        contentType = ReadingContentType.Article,
        id = id,
        title = title,
        author = author.name,
        bodyHtml = content,
        publishedAt = created,
        updatedAt = updated,
        voteUpCount = voteupCount,
        commentCount = commentCount,
    )
    is DataHolder.Pin -> ReadingQueueItem(
        contentType = ReadingContentType.Pin,
        id = id.toLongOrNull() ?: (destination as? Pin)?.id ?: return null,
        author = author.name,
        bodyHtml = contentHtml.ifBlank {
            content
                .filterIsInstance<DataHolder.Pin.ContentText>()
                .joinToString("\n") { item -> item.content }
        },
        publishedAt = created,
        updatedAt = updated,
        voteUpCount = likeCount,
        commentCount = commentCount,
    )
    is DataHolder.Question -> ReadingQueueItem(
        contentType = ReadingContentType.Question,
        id = id,
        title = title,
        author = author.name,
        bodyHtml = detail,
        publishedAt = created,
        updatedAt = updatedTime,
        voteUpCount = voteupCount,
        commentCount = commentCount,
    )
    else -> null
}

private fun formatReadingDateTime(seconds: Long): String {
    val dateTime = Instant.fromEpochSeconds(seconds).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dateTime.year}年${dateTime.month.number}月${dateTime.day}日 ${dateTime.hour}点${dateTime.minute}分"
}

private fun formatReadingRelativeTime(
    updatedAt: Long,
    nowEpochSeconds: Long,
    precision: ReadingRelativeTimePrecision,
): String {
    val difference = (nowEpochSeconds - updatedAt).coerceAtLeast(0L)
    if (difference == 0L) return "刚刚"

    var remaining = difference
    val days = remaining / 86_400
    remaining %= 86_400
    val hours = remaining / 3_600
    remaining %= 3_600
    val minutes = remaining / 60
    val seconds = remaining % 60
    val parts = buildList {
        if (days > 0) add("${days}天")
        if (precision != ReadingRelativeTimePrecision.Day && hours > 0) add("${hours}小时")
        if (precision == ReadingRelativeTimePrecision.Second) {
            if (minutes > 0) add("${minutes}分")
            if (seconds > 0) add("${seconds}秒")
        }
    }
    if (parts.isEmpty()) {
        return when (precision) {
            ReadingRelativeTimePrecision.Second -> "刚刚"
            ReadingRelativeTimePrecision.Hour -> "不到1小时前"
            ReadingRelativeTimePrecision.Day -> "不到1天前"
        }
    }
    return parts.joinToString("") + "前"
}

private fun String?.cleanReadingMetadata(): String {
    val value = orEmpty()
        .takeUnless { it.equals("loading...", ignoreCase = true) }
        .orEmpty()
        .trim()
    return if ('<' in value || '&' in value) Ksoup.parse(value).text().trim() else value
}
