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

package com.github.zly2006.zhihu.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel.CachedAnswerContent

enum class AnswerSwitchExtensionMode {
    /** 问题页 / 收藏夹列表：仅 feeds 种子与 feeds 续页，忽略 paginationInfo。 */
    QuestionFeeds,

    /** 首页 / 搜索等直达回答：仅 paginationInfo 两端拓展，不调用问题 feeds。 */
    PaginationOnly,
}

data class AnswerEntryCache(
    val listMeta: Article? = null,
    var phase: NeighborPhase = NeighborPhase.Preview,
    var cached: CachedAnswerContent? = null,
)

/**
 * 回答切换会话：有序 id 序列 + 游标；仅在左右端拓展，中段保序。
 */
class AnswerSwitchSession(
    val sourceName: String,
    val extensionMode: AnswerSwitchExtensionMode,
    initialOrderedIds: List<Long> = emptyList(),
    initialCursor: Int = 0,
    val questionId: Long? = null,
    var feedsNextUrl: String = "",
    val feedsOrder: String? = null,
    val collectionId: String? = null,
    var collectionNextPageUrl: String = "",
) {
    val orderedIds = mutableStateListOf<Long>().also { list ->
        list.addAll(initialOrderedIds)
    }

    var cursor by mutableIntStateOf(initialCursor.coerceIn(0, maxOf(0, initialOrderedIds.size - 1)))

    var revision by mutableIntStateOf(0)
        private set

    val entryById = mutableMapOf<Long, AnswerEntryCache>()

    private fun bumpRevision() {
        revision++
    }

    val hasNextCandidate: Boolean
        get() = when {
            cursor < orderedIds.lastIndex -> true
            extensionMode == AnswerSwitchExtensionMode.QuestionFeeds &&
                (questionId != null || collectionNextPageUrl.isNotEmpty()) -> true
            else -> false
        }

    val hasPreviousCandidate: Boolean
        get() = cursor > 0

    fun alignCursor(articleId: Long) {
        val index = orderedIds.indexOf(articleId)
        if (index >= 0) {
            cursor = index
            bumpRevision()
        }
    }

    fun mergeAtEnds(prevIds: List<Long>, nextIds: List<Long>) {
        prevIds.asReversed().forEach { id ->
            if (id !in orderedIds) {
                orderedIds.add(0, id)
                cursor++
            }
        }
        nextIds.forEach { id ->
            if (id !in orderedIds) {
                orderedIds.add(id)
            }
        }
        bumpRevision()
    }

    fun mergePagination(pagination: DataHolder.Answer.PaginationInfo) {
        if (extensionMode != AnswerSwitchExtensionMode.PaginationOnly) return
        mergeAtEnds(pagination.prevAnswerIds, pagination.nextAnswerIds)
    }

    fun putEntry(cached: CachedAnswerContent, listMeta: Article? = null) {
        val id = cached.article.id
        val existing = entryById[id]
        entryById[id] = AnswerEntryCache(
            listMeta = listMeta ?: existing?.listMeta ?: cached.article,
            phase = NeighborPhase.Ready,
            cached = cached,
        )
    }

    fun registerListMeta(article: Article) {
        val existing = entryById[article.id]
        entryById[article.id] = AnswerEntryCache(
            listMeta = article,
            phase = existing?.phase ?: NeighborPhase.Preview,
            cached = existing?.cached,
        )
    }

    fun neighborId(offset: Int): Long? = orderedIds.getOrNull(cursor + offset)

    fun moveToNext(): Long? {
        if (cursor < orderedIds.lastIndex) {
            cursor++
            bumpRevision()
            return orderedIds[cursor]
        }
        return null
    }

    fun moveToPrevious(): Long? {
        if (cursor > 0) {
            cursor--
            bumpRevision()
            return orderedIds[cursor]
        }
        return null
    }

    fun displayContentFor(answerId: Long): CachedAnswerContent? {
        val entry = entryById[answerId]
        entry?.cached?.let { return it }
        val meta = entry?.listMeta
        if (meta != null) {
            return CachedAnswerContent(
                article = meta,
                title = meta.title,
                authorName = meta.authorName,
                authorBio = meta.authorBio,
                authorAvatarUrl = meta.avatarSrc ?: "",
                content = "",
                voteUpCount = 0,
                commentCount = 0,
                sourceLabel = sourceName,
            )
        }
        return CachedAnswerContent(
            article = Article(id = answerId, type = ArticleType.Answer),
            title = "加载中...",
            authorName = "",
            authorBio = "",
            authorAvatarUrl = "",
            content = "",
            voteUpCount = 0,
            commentCount = 0,
            sourceLabel = sourceName,
        )
    }

    val previousAnswer: CachedAnswerContent?
        get() = neighborId(-1)?.let { displayContentFor(it) }

    val nextAnswer: CachedAnswerContent?
        get() = neighborId(+1)?.let { displayContentFor(it) }

    fun markNeighborLoading(answerId: Long, isNext: Boolean) {
        val entry = entryById[answerId]
        val meta = entry?.listMeta ?: Article(id = answerId, type = ArticleType.Answer)
        entryById[answerId] = AnswerEntryCache(
            listMeta = meta,
            phase = NeighborPhase.Loading,
            cached = entry?.cached,
        )
        bumpRevision()
    }

    fun markNeighborReady(cached: CachedAnswerContent) {
        putEntry(cached)
        bumpRevision()
    }

    fun resolveNextForNavigation(): CachedAnswerContent? {
        val id = neighborId(+1) ?: return null
        return displayContentFor(id)
    }

    fun resolvePreviousForNavigation(): CachedAnswerContent? {
        val id = neighborId(-1) ?: return null
        return displayContentFor(id)
    }

    fun advanceAfterNavigation(direction: ArticleSwitchDirection) {
        when (direction) {
            ArticleSwitchDirection.NEXT -> moveToNext()
            ArticleSwitchDirection.PREVIOUS -> moveToPrevious()
        }
    }
}

enum class ArticleSwitchDirection {
    NEXT,
    PREVIOUS,
}
