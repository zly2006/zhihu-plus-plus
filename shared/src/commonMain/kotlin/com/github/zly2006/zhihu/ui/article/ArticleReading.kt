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

package com.github.zly2006.zhihu.ui.article

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.reading.ReadingContentType
import com.github.zly2006.zhihu.reading.ReadingPlayerController
import com.github.zly2006.zhihu.reading.ReadingPreferences
import com.github.zly2006.zhihu.reading.ReadingQueueItem
import com.github.zly2006.zhihu.reading.ReadingQueueSourceRegistry
import com.github.zly2006.zhihu.reading.ReadingStartRequest
import com.github.zly2006.zhihu.util.Log
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import kotlinx.coroutines.CancellationException

/**
 * 当前正文页对应的朗读条目：既用于判断"这篇内容有没有可朗读字段"，也是连续朗读队列的首项。
 */
fun articleReadingQueueItem(
    article: Article,
    viewModel: ArticleViewModel,
): ReadingQueueItem = ReadingQueueItem(
    contentType = when (article.type) {
        ArticleType.Answer -> ReadingContentType.Answer
        ArticleType.Article -> ReadingContentType.Article
    },
    id = article.id,
    title = viewModel.title,
    author = viewModel.authorName,
    questionId = viewModel.questionId.takeIf { it > 0 },
    bodyHtml = viewModel.content.takeIf(String::isNotBlank),
    publishedAt = viewModel.createdAt,
    updatedAt = viewModel.updatedAt,
    voteUpCount = viewModel.voteUpCount,
    commentCount = viewModel.commentCount,
)

/**
 * 从当前正文页开始连续朗读。
 *
 * 队列先取来源列表（信息流、收藏夹等）里当前条目之后的内容；回答页的首页来源混杂无关内容，改用同题回答顺序，
 * 不足 [ReadingPreferences.queueLimit] 时再用 [answerQueueFallbackProvider] 或 ViewModel 已知的后续回答补齐。
 *
 * M3 [ArticleActionsMenu] 与 miuix 正文页共用这套队列语义和播放速度，改动只应发生在这里。
 */
suspend fun startArticleReading(
    article: Article,
    viewModel: ArticleViewModel,
    preferences: ReadingPreferences,
    playbackSpeed: Float,
    player: ReadingPlayerController,
    answerQueueFallbackProvider: (suspend (limit: Int) -> List<Article>)?,
) {
    val current = articleReadingQueueItem(article, viewModel)
    // Home feed mixes unrelated content; the question navigator owns answer order.
    val useQuestionAnswerOrder = article.type == ArticleType.Answer &&
        article.readingQueueSourceId?.startsWith("home:") == true
    val originQueue = ReadingQueueSourceRegistry.queueStartingAt(
        current = current,
        sourceId = article.readingQueueSourceId.takeUnless { useQuestionAnswerOrder },
        limit = preferences.queueLimit,
    )
    val queue = if (
        article.type == ArticleType.Answer &&
        (useQuestionAnswerOrder || originQueue.size < preferences.queueLimit) &&
        preferences.queueLimit > 1
    ) {
        val fallbackAfterCurrent = try {
            val fallbackArticles = answerQueueFallbackProvider
                ?.invoke(preferences.queueLimit - 1)
                ?: viewModel.answerNextIds.map { answerId ->
                    Article(
                        type = ArticleType.Answer,
                        id = answerId,
                        title = viewModel.title,
                    )
                }
            fallbackArticles.map { fallback ->
                ReadingQueueItem(
                    contentType = ReadingContentType.Answer,
                    id = fallback.id,
                    title = fallback.title
                        .takeUnless { it == "loading..." }
                        .orEmpty()
                        .ifBlank { viewModel.title },
                    author = fallback.authorName
                        .takeUnless { it == "loading..." }
                        .orEmpty(),
                    questionId = viewModel.questionId.takeIf { it > 0 },
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w("ArticleReading", "Failed to load the remaining reading queue", error)
            emptyList()
        }
        ReadingQueueSourceRegistry.queueStartingAt(
            current = current,
            sourceId = article.readingQueueSourceId.takeUnless { useQuestionAnswerOrder },
            limit = preferences.queueLimit,
            fallbackAfterCurrent = fallbackAfterCurrent,
        )
    } else {
        originQueue
    }
    player.start(
        ReadingStartRequest(
            queue = queue,
            preferences = preferences,
            sourceId = article.readingQueueSourceId,
            playbackSpeed = playbackSpeed,
        ),
    )
}
