/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.github.zly2006.zhihu.reading

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.toFeedDisplayItemNavDestinationJson
import com.github.zly2006.zhihu.shared.platform.SettingsStore
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadingPlayerTest {
    @AfterTest
    fun clearRegistry() {
        ReadingQueueSourceRegistry.clearForTesting()
    }

    @Test
    fun speechTemplateFollowsOrderAndSkipsMissingFields() {
        val preferences = ReadingPreferences(
            fieldOrder = listOf(
                ReadingTemplateField.Author,
                ReadingTemplateField.Title,
                ReadingTemplateField.Body,
                ReadingTemplateField.ContentType,
            ),
            enabledFields = setOf(
                ReadingTemplateField.Author,
                ReadingTemplateField.Title,
                ReadingTemplateField.Body,
                ReadingTemplateField.ContentType,
            ),
        )
        val text = buildReadingSpeechText(
            content = ResolvedReadingContent(
                contentType = ReadingContentType.Pin,
                title = "",
                author = "测试作者",
                body = "想法正文。",
                publishedAt = 0L,
                voteUpCount = -1,
                comments = emptyList(),
            ),
            preferences = preferences,
        )

        assertEquals("作者：测试作者。\n想法正文。\n这是一条想法。", text)
        assertFalse(text.contains("标题"))
    }

    @Test
    fun relativePublishedTimeUsesLastEditAndSelectedPrecision() {
        val content = ResolvedReadingContent(
            contentType = ReadingContentType.Article,
            title = "测试文章",
            author = "测试作者",
            body = "正文",
            publishedAt = 183_944L,
            voteUpCount = 0,
            comments = emptyList(),
            updatedAt = 100L,
        )
        val basePreferences = ReadingPreferences(
            fieldOrder = listOf(ReadingTemplateField.PublishedAt),
            enabledFields = setOf(ReadingTemplateField.PublishedAt),
            publishedTimeMode = ReadingPublishedTimeMode.Relative,
        )
        val nowEpochSeconds = 183_945L

        assertEquals(
            "最后编辑于2天3小时4分5秒前。",
            buildReadingSpeechText(content, basePreferences, nowEpochSeconds),
        )
        assertEquals(
            "最后编辑于2天3小时前。",
            buildReadingSpeechText(
                content,
                basePreferences.copy(relativeTimePrecision = ReadingRelativeTimePrecision.Hour),
                nowEpochSeconds,
            ),
        )
        assertEquals(
            "最后编辑于2天前。",
            buildReadingSpeechText(
                content,
                basePreferences.copy(relativeTimePrecision = ReadingRelativeTimePrecision.Day),
                nowEpochSeconds,
            ),
        )
    }

    @Test
    fun absolutePublishedTimeKeepsUsingOriginalPublishTimestamp() {
        val text = buildReadingSpeechText(
            content = ResolvedReadingContent(
                contentType = ReadingContentType.Answer,
                title = "",
                author = "",
                body = "",
                publishedAt = 1_710_000_000L,
                voteUpCount = -1,
                comments = emptyList(),
                updatedAt = 1L,
            ),
            preferences = ReadingPreferences(
                fieldOrder = listOf(ReadingTemplateField.PublishedAt),
                enabledFields = setOf(ReadingTemplateField.PublishedAt),
                publishedTimeMode = ReadingPublishedTimeMode.Absolute,
            ),
            nowEpochSeconds = 1_710_100_000L,
        )

        assertTrue(text.startsWith("发布时间："))
        assertFalse(text.contains("最后编辑"))
    }

    @Test
    fun commentAuthorIsOnlyReadWhenEnabled() {
        val content = ResolvedReadingContent(
            contentType = ReadingContentType.Answer,
            title = "",
            author = "",
            body = "",
            publishedAt = 0L,
            voteUpCount = -1,
            comments = listOf(
                ReadingComment(
                    author = "评论作者",
                    body = "评论正文。",
                ),
            ),
        )
        val preferences = ReadingPreferences(
            fieldOrder = listOf(ReadingTemplateField.Comments),
            enabledFields = setOf(ReadingTemplateField.Comments),
            commentCount = 1,
        )

        assertEquals(
            "第1条评论。评论作者：评论作者。评论正文。",
            buildReadingSpeechText(content, preferences),
        )
        assertEquals(
            "第1条评论。评论正文。",
            buildReadingSpeechText(content, preferences.copy(readCommentAuthor = false)),
        )
    }

    @Test
    fun templatePreviewFollowsOrderAndNestedOptionsWithoutTransitionText() {
        val preferences = ReadingPreferences(
            fieldOrder = listOf(
                ReadingTemplateField.Body,
                ReadingTemplateField.PublishedAt,
                ReadingTemplateField.Comments,
                ReadingTemplateField.Title,
            ),
            enabledFields = setOf(
                ReadingTemplateField.Body,
                ReadingTemplateField.PublishedAt,
                ReadingTemplateField.Comments,
            ),
            publishedTimeMode = ReadingPublishedTimeMode.Relative,
            relativeTimePrecision = ReadingRelativeTimePrecision.Hour,
            commentCount = 2,
            readCommentAuthor = false,
            transitionText = "不应出现在模板预览中",
        )

        assertEquals(
            """
            {正文}
            最后编辑于{距最后编辑时间，精确到时}。
            第{评论序号（1-2）}条评论。{评论正文}
            """.trimIndent(),
            buildReadingTemplatePreview(preferences),
        )
    }

    @Test
    fun commentsAreOnlyRequestedWhenEnabledWithPositiveCount() {
        assertFalse(ReadingPreferences(commentCount = 3).shouldLoadComments)
        assertFalse(
            ReadingPreferences(
                enabledFields = setOf(ReadingTemplateField.Comments),
                commentCount = 0,
            ).shouldLoadComments,
        )
        assertTrue(
            ReadingPreferences(
                enabledFields = setOf(ReadingTemplateField.Comments),
                commentCount = 3,
            ).shouldLoadComments,
        )
    }

    @Test
    fun transitionReplacesAllSupportedPlaceholders() {
        val text = renderReadingTransition(
            template = "{index}/{total} {contentType} {title} {author}",
            nextItem = ReadingQueueItem(
                contentType = ReadingContentType.Article,
                id = 2,
                title = "标题",
                author = "作者",
            ),
            nextIndex = 1,
            total = 5,
        )

        assertEquals("2/5 文章 标题 作者", text)
    }

    @Test
    fun transitionOmitsAnUnavailableAuthorClause() {
        val text = renderReadingTransition(
            template = "下一条：{contentType}，作者：{author}。",
            nextItem = ReadingQueueItem(
                contentType = ReadingContentType.Pin,
                id = 2,
            ),
            nextIndex = 1,
            total = 5,
        )

        assertEquals("下一条：想法。", text)
    }

    @Test
    fun queueStartsAtCurrentItemAndNeverExceedsLimit() {
        val items = (1L..6L).map { id ->
            ReadingQueueItem(
                contentType = ReadingContentType.Answer,
                id = id,
                title = "回答$id",
            )
        }
        ReadingQueueSourceRegistry.register("question:1", items)
        val current = items[2].copy(bodyHtml = "<p>正文</p>")

        val queue = ReadingQueueSourceRegistry.queueStartingAt(
            current = current,
            sourceId = "question:1",
            limit = 3,
        )

        assertEquals(listOf(3L, 4L, 5L), queue.map(ReadingQueueItem::id))
        assertEquals("<p>正文</p>", queue.first().bodyHtml)
    }

    @Test
    fun explicitOriginWinsWhenTheSameItemAppearsInMultipleSources() {
        val current = ReadingQueueItem(ReadingContentType.Answer, id = 1)
        val sourceA = listOf(current, ReadingQueueItem(ReadingContentType.Answer, id = 2))
        val sourceB = listOf(current, ReadingQueueItem(ReadingContentType.Answer, id = 3))
        ReadingQueueSourceRegistry.register("source:a", sourceA)
        ReadingQueueSourceRegistry.register("source:b", sourceB)

        val queue = ReadingQueueSourceRegistry.queueStartingAt(
            current = current,
            sourceId = "source:a",
            limit = 3,
        )

        assertEquals(listOf(1L, 2L), queue.map(ReadingQueueItem::id))
    }

    @Test
    fun directNavigationDoesNotReuseAnOldOrigin() {
        val current = ReadingQueueItem(ReadingContentType.Answer, id = 1)
        ReadingQueueSourceRegistry.register(
            "source:a",
            listOf(current, ReadingQueueItem(ReadingContentType.Answer, id = 2)),
        )
        val queue = ReadingQueueSourceRegistry.queueStartingAt(
            current = current,
            sourceId = null,
            limit = 3,
        )

        assertEquals(listOf(1L), queue.map(ReadingQueueItem::id))
    }

    @Test
    fun answerSwitchedBeyondItsOriginFallsBackToQuestionOrder() {
        ReadingQueueSourceRegistry.register(
            "home:feed",
            listOf(
                ReadingQueueItem(ReadingContentType.Answer, id = 1),
                ReadingQueueItem(ReadingContentType.Article, id = 2),
            ),
        )
        val current = ReadingQueueItem(
            contentType = ReadingContentType.Answer,
            id = 10,
            questionId = 100,
            bodyHtml = "<p>当前回答</p>",
        )

        val queue = ReadingQueueSourceRegistry.queueStartingAt(
            current = current,
            sourceId = "home:feed",
            limit = 3,
            fallbackAfterCurrent = listOf(
                ReadingQueueItem(ReadingContentType.Answer, id = 11, questionId = 100),
                ReadingQueueItem(ReadingContentType.Answer, id = 12, questionId = 100),
                ReadingQueueItem(ReadingContentType.Answer, id = 13, questionId = 100),
            ),
        )

        assertEquals(listOf(10L, 11L, 12L), queue.map(ReadingQueueItem::id))
        assertEquals("<p>当前回答</p>", queue.first().bodyHtml)
    }

    @Test
    fun matchingOriginStillWinsOverQuestionFallback() {
        val current = ReadingQueueItem(ReadingContentType.Answer, id = 1)
        ReadingQueueSourceRegistry.register(
            "home:feed",
            listOf(current, ReadingQueueItem(ReadingContentType.Article, id = 2)),
        )

        val queue = ReadingQueueSourceRegistry.queueStartingAt(
            current = current,
            sourceId = "home:feed",
            limit = 3,
            fallbackAfterCurrent = listOf(
                ReadingQueueItem(ReadingContentType.Answer, id = 11),
            ),
        )

        assertEquals(listOf(1L, 2L), queue.map(ReadingQueueItem::id))
    }

    @Test
    fun exhaustedMatchingOriginFallsBackToQuestionOrder() {
        val current = ReadingQueueItem(ReadingContentType.Answer, id = 1)
        ReadingQueueSourceRegistry.register("question:1:answers:default", listOf(current))

        val queue = ReadingQueueSourceRegistry.queueStartingAt(
            current = current,
            sourceId = "question:1:answers:default",
            limit = 3,
            fallbackAfterCurrent = listOf(
                ReadingQueueItem(ReadingContentType.Answer, id = 11),
                ReadingQueueItem(ReadingContentType.Answer, id = 12),
            ),
        )

        assertEquals(listOf(1L, 11L, 12L), queue.map(ReadingQueueItem::id))
    }

    @Test
    fun matchingQuestionOriginExtendsAfterItsLoadedItems() {
        val current = ReadingQueueItem(ReadingContentType.Answer, id = 1)
        val loadedNext = ReadingQueueItem(ReadingContentType.Answer, id = 11)
        ReadingQueueSourceRegistry.register(
            "question:1:answers:default",
            listOf(current, loadedNext),
        )

        val queue = ReadingQueueSourceRegistry.queueStartingAt(
            current = current,
            sourceId = "question:1:answers:default",
            limit = 4,
            fallbackAfterCurrent = listOf(
                loadedNext,
                ReadingQueueItem(ReadingContentType.Answer, id = 12),
                ReadingQueueItem(ReadingContentType.Answer, id = 13),
            ),
        )

        assertEquals(listOf(1L, 11L, 12L, 13L), queue.map(ReadingQueueItem::id))
    }

    @Test
    fun partiallyMatchingFallbackDoesNotMixDivergingOrderIntoOrigin() {
        val current = ReadingQueueItem(ReadingContentType.Answer, id = 1)
        val firstLoaded = ReadingQueueItem(ReadingContentType.Answer, id = 11)
        val secondLoaded = ReadingQueueItem(ReadingContentType.Answer, id = 12)
        ReadingQueueSourceRegistry.register(
            "question:1:answers:default",
            listOf(current, firstLoaded, secondLoaded),
        )

        val queue = ReadingQueueSourceRegistry.queueStartingAt(
            current = current,
            sourceId = "question:1:answers:default",
            limit = 5,
            fallbackAfterCurrent = listOf(
                firstLoaded,
                ReadingQueueItem(ReadingContentType.Answer, id = 99),
                ReadingQueueItem(ReadingContentType.Answer, id = 13),
            ),
        )

        assertEquals(listOf(1L, 11L, 12L), queue.map(ReadingQueueItem::id))
    }

    @Test
    fun queueSourceExcludesFilteredFeedItems() {
        fun item(
            id: Long,
            filtered: Boolean,
        ) = FeedDisplayItem(
            title = "回答$id",
            summary = null,
            details = "",
            feed = null,
            navDestinationJson = Article(
                type = ArticleType.Answer,
                id = id,
            ).toFeedDisplayItemNavDestinationJson(),
            isFiltered = filtered,
        )

        val queueItems = listOf(item(1, false), item(2, true), item(3, false))
            .toReadingQueueSourceItems()

        assertEquals(listOf(1L, 3L), queueItems.map(ReadingQueueItem::id))
    }

    @Test
    fun chunkingUsesEachSentenceAsAResumeBoundary() {
        val chunks = splitReadingSpeechIntoChunks(
            text = "第一句话。第二句话很长很长。第三句话。",
        )

        assertEquals(
            listOf("第一句话。", "第二句话很长很长。", "第三句话。"),
            chunks,
        )
    }

    @Test
    fun longSentenceUsesAClauseBoundaryBeforeTheLimit() {
        val text = "甲".repeat(70) + "，" + "乙".repeat(70)

        val chunks = splitReadingSpeechIntoChunks(text)

        assertEquals(listOf("甲".repeat(70) + "，", "乙".repeat(70)), chunks)
    }

    @Test
    fun unpunctuatedTextIsLimitedTo120Characters() {
        val text = "字".repeat(241)

        val chunks = splitReadingSpeechIntoChunks(text)

        assertEquals(listOf(120, 120, 1), chunks.map(String::length))
        assertEquals(text, chunks.joinToString(""))
    }

    @Test
    fun hardLimitDoesNotSplitASurrogatePair() {
        val text = "字".repeat(119) + "😀" + "尾"

        val chunks = splitReadingSpeechIntoChunks(text)

        assertEquals(2, chunks.size)
        assertTrue(chunks.first().endsWith("😀"))
        assertEquals(text, chunks.joinToString(""))
    }

    @Test
    fun preferencesRoundTripAndNormalizeBounds() {
        val values = mutableMapOf<String, Any>()
        val settings = settingsStore(values)
        saveReadingPreferences(
            settings,
            ReadingPreferences(
                publishedTimeMode = ReadingPublishedTimeMode.Relative,
                relativeTimePrecision = ReadingRelativeTimePrecision.Day,
                commentCount = -2,
                readCommentAuthor = false,
                queueLimit = 500,
            ),
        )

        val loaded = loadReadingPreferences(settings)

        assertEquals(100, loaded.queueLimit)
        assertEquals(0, loaded.commentCount)
        assertEquals(ReadingPublishedTimeMode.Relative, loaded.publishedTimeMode)
        assertEquals(ReadingRelativeTimePrecision.Day, loaded.relativeTimePrecision)
        assertFalse(loaded.readCommentAuthor)
    }

    @Test
    fun oldPreferencesUseBackwardCompatibleTimeAndCommentDefaults() {
        val settings = settingsStore(mutableMapOf())
        settings.putString(READING_PREFERENCES_KEY, "{\"commentCount\":4}")

        val loaded = loadReadingPreferences(settings)

        assertEquals(ReadingPublishedTimeMode.Absolute, loaded.publishedTimeMode)
        assertEquals(ReadingRelativeTimePrecision.Second, loaded.relativeTimePrecision)
        assertTrue(loaded.readCommentAuthor)
    }

    @Test
    fun playbackSpeedNormalizesAndPersistsIndependently() {
        val values = mutableMapOf<String, Any>()
        val settings = settingsStore(values)

        assertEquals(MIN_READING_PLAYBACK_SPEED, normalizeReadingPlaybackSpeed(0.1f))
        assertEquals(MAX_READING_PLAYBACK_SPEED, normalizeReadingPlaybackSpeed(3f))
        assertEquals(DEFAULT_READING_PLAYBACK_SPEED, normalizeReadingPlaybackSpeed(Float.NaN))
        assertEquals(DEFAULT_READING_PLAYBACK_SPEED, loadReadingPlaybackSpeed(settings))

        saveReadingPlaybackSpeed(settings, 1.5f)
        saveReadingPreferences(settings, ReadingPreferences(queueLimit = 10))

        assertEquals(1.5f, values[READING_PLAYBACK_SPEED_KEY])
        assertEquals(1.5f, loadReadingPlaybackSpeed(settings))

        saveReadingPlaybackSpeed(settings, Float.POSITIVE_INFINITY)

        assertEquals(DEFAULT_READING_PLAYBACK_SPEED, loadReadingPlaybackSpeed(settings))
    }

    @Test
    fun playbackRequestAndStateDefaultToNormalSpeed() {
        val request = ReadingStartRequest(
            queue = emptyList(),
            preferences = ReadingPreferences(),
        )

        assertEquals(DEFAULT_READING_PLAYBACK_SPEED, request.playbackSpeed)
        assertEquals(DEFAULT_READING_PLAYBACK_SPEED, ReadingPlayerState().playbackSpeed)
    }

    @Test
    fun preferencesAlwaysKeepAtLeastOneEnabledField() {
        val normalized = ReadingPreferences(enabledFields = emptySet()).normalized()

        assertEquals(setOf(ReadingTemplateField.Body), normalized.enabledFields)
    }

    @Test
    fun zeroCommentsCannotBeTheOnlyEnabledField() {
        val normalized = ReadingPreferences(
            enabledFields = setOf(ReadingTemplateField.Comments),
            commentCount = 0,
        ).normalized()

        assertEquals(setOf(ReadingTemplateField.Body), normalized.enabledFields)
        assertFalse(normalized.shouldLoadComments)
    }

    private fun settingsStore(values: MutableMap<String, Any>) = SettingsStore(
        getBoolean = { key, default -> values[key] as? Boolean ?: default },
        putBoolean = { key, value -> values[key] = value },
        getString = { key, default -> values[key] as? String ?: default },
        putString = { key, value -> values[key] = value },
        getStringOrNull = { key -> values[key] as? String },
        putStringSet = { key, value -> values[key] = value },
        getStringSet = { key, default -> values[key] as? Set<String> ?: default },
        getInt = { key, default -> values[key] as? Int ?: default },
        putInt = { key, value -> values[key] = value },
        getLong = { key, default -> values[key] as? Long ?: default },
        putLong = { key, value -> values[key] = value },
        getFloat = { key, default -> values[key] as? Float ?: default },
        putFloat = { key, value -> values[key] = value },
        remove = values::remove,
    )
}
