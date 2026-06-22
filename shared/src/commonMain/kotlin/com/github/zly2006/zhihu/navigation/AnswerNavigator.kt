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
import androidx.compose.runtime.setValue
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.navDestination
import com.github.zly2006.zhihu.shared.data.officialBadge
import com.github.zly2006.zhihu.shared.data.target
import com.github.zly2006.zhihu.shared.filter.ContentOpenEventSupport
import com.github.zly2006.zhihu.shared.util.Log
import com.github.zly2006.zhihu.ui.ArticleAnswerTransitionDirection
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel.CachedAnswerContent
import com.github.zly2006.zhihu.viewmodel.CollectionItem
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import com.github.zly2006.zhihu.viewmodel.filter.ContentType
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.getOrFetchContentDetail
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class AnswerNavigatorPage<T>(
    val items: List<T>,
    val nextUrl: String,
)

fun <T> answerNavigatorPageFromJson(
    response: JsonObject,
    decodeItems: (JsonElement) -> List<T>,
): AnswerNavigatorPage<T> {
    val data = response["data"] ?: return AnswerNavigatorPage(emptyList(), "")
    return AnswerNavigatorPage(
        items = decodeItems(data),
        nextUrl = response["paging"]
            ?.jsonObject
            ?.get("next")
            ?.jsonPrimitive
            ?.content ?: "",
    )
}

fun zhihuQuestionFeedsUrl(
    questionId: Long,
    limit: Int,
    order: String? = null,
): String = zhihuQuestionFeedsUrl(questionId.toString(), limit, order)

fun zhihuQuestionFeedsUrl(
    questionId: String,
    limit: Int,
    order: String? = null,
): String =
    buildString {
        append("https://www.zhihu.com/api/v4/questions/")
        append(questionId)
        append("/feeds?limit=")
        append(limit)
        if (order != null) {
            append("&order=")
            append(order)
        }
    }

/**
 * 回答导航器：基于 [AnswerSwitchSession] 的序列 + 游标，按扩展模式拉取邻居正文。
 */
abstract class AnswerNavigator(
    val session: AnswerSwitchSession,
    protected val environment: ZhihuApiEnvironment,
) {
    val sourceName: String get() = session.sourceName
    val queueRevision: Int get() = session.revision
    val hasNextCandidate: Boolean get() = session.hasNextCandidate
    val hasPreviousCandidate: Boolean get() = session.hasPreviousCandidate
    val previousAnswer: CachedAnswerContent? get() = session.previousAnswer
    val nextAnswer: CachedAnswerContent? get() = session.nextAnswer

    var neighborNextSlot: NeighborSlot?
        get() = session.neighborId(+1)?.let { id -> slotFromEntry(id) }
        private set(value) = Unit

    var neighborPrevSlot: NeighborSlot?
        get() = session.neighborId(-1)?.let { id -> slotFromEntry(id) }
        private set(value) = Unit

    private fun slotFromEntry(answerId: Long): NeighborSlot {
        val entry = session.entryById[answerId]
        val meta = entry?.listMeta ?: Article(id = answerId, type = ArticleType.Answer)
        return when (entry?.phase) {
            NeighborPhase.Ready -> entry.cached?.let(NeighborSlot::ready)
                ?: NeighborSlot.previewFrom(meta, sourceName)
            NeighborPhase.Loading -> NeighborSlot.loadingFrom(meta, sourceName)
            NeighborPhase.Preview, NeighborPhase.None, null -> NeighborSlot.previewFrom(meta, sourceName)
        }
    }

    fun resolveNextForNavigation(): CachedAnswerContent? = session.resolveNextForNavigation()

    fun resolvePreviousForNavigation(): CachedAnswerContent? = session.resolvePreviousForNavigation()

    suspend fun loadNextCached(): CachedAnswerContent? {
        resolveNextForNavigation()?.let { return it }
        return loadNextCachedFromExtension()
    }

    suspend fun loadPreviousCached(): CachedAnswerContent? {
        resolvePreviousForNavigation()?.let { return it }
        return loadPreviousCachedFromExtension()
    }

    fun pushAnswer(cached: CachedAnswerContent) {
        session.putEntry(cached)
        session.alignCursor(cached.article.id)
    }

    suspend fun onPageSettled(
        articleId: Long,
        direction: ArticleAnswerTransitionDirection?,
        paginationInfo: DataHolder.Answer.PaginationInfo?,
        schedulePrefetch: suspend (Long) -> Unit,
    ) {
        session.alignCursor(articleId)
        if (session.extensionMode == AnswerSwitchExtensionMode.PaginationOnly && paginationInfo != null) {
            session.mergePagination(paginationInfo)
        }
        schedulePrefetch(articleId)
    }

    abstract suspend fun prefetchNext(currentArticleId: Long)

    open suspend fun prefetchPrevious(currentArticleId: Long) = Unit

    protected abstract suspend fun loadNextCachedFromExtension(): CachedAnswerContent?

    protected open suspend fun loadPreviousCachedFromExtension(): CachedAnswerContent? = null

    protected suspend fun fetchCached(article: Article): CachedAnswerContent? {
        val detail = environment.getOrFetchContentDetail(article) as? DataHolder.Answer ?: return null
        return CachedAnswerContent(
            article = article,
            title = detail.question.title,
            authorName = detail.author.name,
            authorBio = detail.author.headline,
            authorAvatarUrl = detail.author.avatarUrl,
            authorBadge = detail.author.badgeV2.officialBadge(),
            content = detail.content,
            voteUpCount = detail.voteupCount,
            commentCount = detail.commentCount,
            createdAt = detail.createdTime,
            updatedAt = detail.updatedTime,
            ipInfo = detail.ipInfo,
            endorsements = detail.endorsementItems,
            sourceLabel = sourceName,
        )
    }

    protected suspend fun fetchCached(answerId: Long): CachedAnswerContent? =
        fetchCached(Article(id = answerId, type = ArticleType.Answer))

    /** 测试与兼容：取序列中下一个 id。 */
    suspend fun loadNext(): Article? {
        if (session.cursor >= session.orderedIds.lastIndex) {
            val anchorId = session.orderedIds.getOrNull(session.cursor) ?: 0L
            ensureRightExtension(anchorId)
        }
        val id = session.moveToNext() ?: return null
        return Article(id = id, type = ArticleType.Answer)
    }

    protected open suspend fun ensureRightExtension(currentArticleId: Long): Boolean = false
}

class QuestionAnswerNavigator(
    val questionId: Long,
    session: AnswerSwitchSession,
    environment: ZhihuApiEnvironment,
    private val getAlreadyOpenedAnswerIds: suspend (List<Long>) -> Set<Long> = { answerIds ->
        ContentOpenEventSupport
            .getAlreadyOpenedContentIds(
                database = getContentFilterDatabase(),
                content = answerIds.map { ContentType.ANSWER to it.toString() },
            ).mapNotNull { key ->
                key.substringAfter(':', "").toLongOrNull()
            }.toSet()
    },
) : AnswerNavigator(session, environment) {
    private val knownOpenedIds = mutableSetOf<Long>()

    companion object {
        fun fromQuestionList(
            questionId: Long,
            orderedArticles: List<Article>,
            cursorIndex: Int,
            feedsNextUrl: String,
            order: String?,
            environment: ZhihuApiEnvironment,
            getAlreadyOpenedAnswerIds: suspend (List<Long>) -> Set<Long> = defaultOpenedIdsLookup(),
        ): QuestionAnswerNavigator {
            val ids = orderedArticles.map { it.id }
            val session = AnswerSwitchSession(
                sourceName = "此问题",
                extensionMode = AnswerSwitchExtensionMode.QuestionFeeds,
                initialOrderedIds = ids,
                initialCursor = cursorIndex.coerceIn(0, maxOf(0, ids.size - 1)),
                questionId = questionId,
                feedsNextUrl = feedsNextUrl,
                feedsOrder = order,
            )
            orderedArticles.forEach { session.registerListMeta(it) }
            return QuestionAnswerNavigator(
                questionId = questionId,
                session = session,
                environment = environment,
                getAlreadyOpenedAnswerIds = getAlreadyOpenedAnswerIds,
            )
        }

        fun defaultOpenedIdsLookup(): suspend (List<Long>) -> Set<Long> = { answerIds ->
            ContentOpenEventSupport
                .getAlreadyOpenedContentIds(
                    database = getContentFilterDatabase(),
                    content = answerIds.map { ContentType.ANSWER to it.toString() },
                ).mapNotNull { key ->
                    key.substringAfter(':', "").toLongOrNull()
                }.toSet()
        }
    }

    override suspend fun ensureRightExtension(currentArticleId: Long): Boolean {
        val url = session.feedsNextUrl.ifEmpty {
            zhihuQuestionFeedsUrl(questionId, limit = 6, order = session.feedsOrder)
        }
        val response = environment.fetchJson(url, "")
        val page = response?.let {
            answerNavigatorPageFromJson(it) { data ->
                data.jsonArray.mapNotNull { item ->
                    runCatching { ZhihuJson.decodeJson<Feed>(item) }.getOrNull()
                }
            }
        } ?: AnswerNavigatorPage(emptyList(), "")
        session.feedsNextUrl = page.nextUrl
        val candidates = page.items
            .mapNotNull { feed -> feed.target?.navDestination as? Article }
            .filter { it.type == ArticleType.Answer }
        if (candidates.isEmpty()) return false
        val idsToLookup = candidates.map { it.id }.filter { id ->
            id != currentArticleId && id !in session.orderedIds && id !in knownOpenedIds
        }
        if (idsToLookup.isNotEmpty()) {
            knownOpenedIds += getAlreadyOpenedAnswerIds(idsToLookup)
        }
        val partition = ContentOpenEventSupport.partitionQuestionAnswerCandidates(
            candidates = candidates,
            openedAnswerIds = knownOpenedIds,
            currentArticleId = currentArticleId,
            historyIds = session.orderedIds.toSet(),
            previousIds = emptySet(),
            nextIds = session.orderedIds.toSet(),
        )
        val prevIds = partition.previousCandidates.map { it.id }
        val nextIds = partition.nextCandidates.map { it.id }
        partition.previousCandidates.forEach { session.registerListMeta(it) }
        partition.nextCandidates.forEach { session.registerListMeta(it) }
        session.mergeAtEnds(prevIds, nextIds)
        return prevIds.isNotEmpty() || nextIds.isNotEmpty()
    }

    override suspend fun prefetchNext(currentArticleId: Long) {
        if (session.neighborId(+1) == null) {
            ensureRightExtension(currentArticleId)
        }
        val nextId = session.neighborId(+1) ?: return
        val entry = session.entryById[nextId]
        if (entry?.phase == NeighborPhase.Ready && entry.cached != null) return
        session.markNeighborLoading(nextId, isNext = true)
        try {
            fetchCached(nextId)?.let { session.markNeighborReady(it) }
        } catch (e: Exception) {
            Log.w("QuestionAnswerNavigator", "Failed to pre-load next answer content", e)
        }
    }

    override suspend fun prefetchPrevious(currentArticleId: Long) {
        val prevId = session.neighborId(-1) ?: return
        val entry = session.entryById[prevId]
        if (entry?.phase == NeighborPhase.Ready && entry.cached != null) return
        session.markNeighborLoading(prevId, isNext = false)
        try {
            fetchCached(prevId)?.let { session.markNeighborReady(it) }
        } catch (e: Exception) {
            Log.w("QuestionAnswerNavigator", "Failed to pre-load previous answer content", e)
        }
    }

    override suspend fun loadNextCachedFromExtension(): CachedAnswerContent? {
        if (session.neighborId(+1) == null) {
            ensureRightExtension(session.orderedIds[session.cursor])
        }
        val nextId = session.neighborId(+1) ?: return null
        return fetchCached(nextId)?.also { session.markNeighborReady(it) }
    }

    override suspend fun loadPreviousCachedFromExtension(): CachedAnswerContent? {
        val prevId = session.neighborId(-1) ?: return null
        return fetchCached(prevId)?.also { session.markNeighborReady(it) }
    }
}

class CollectionAnswerNavigator(
    val collectionId: String,
    session: AnswerSwitchSession,
    environment: ZhihuApiEnvironment,
) : AnswerNavigator(session, environment) {
    companion object {
        fun fromCollectionList(
            collectionId: String,
            collectionTitle: String,
            orderedArticles: List<Article>,
            cursorIndex: Int,
            nextPageUrl: String,
            environment: ZhihuApiEnvironment,
        ): CollectionAnswerNavigator {
            val ids = orderedArticles.map { it.id }
            val session = AnswerSwitchSession(
                sourceName = "「$collectionTitle」",
                extensionMode = AnswerSwitchExtensionMode.QuestionFeeds,
                initialOrderedIds = ids,
                initialCursor = cursorIndex.coerceIn(0, maxOf(0, ids.size - 1)),
                collectionId = collectionId,
                collectionNextPageUrl = nextPageUrl,
            )
            orderedArticles.forEach { session.registerListMeta(it) }
            return CollectionAnswerNavigator(collectionId, session, environment)
        }
    }

    override suspend fun ensureRightExtension(currentArticleId: Long): Boolean {
        val url = session.collectionNextPageUrl
        if (url.isEmpty()) return false
        val response = environment.fetchJson(url, "")
        val page = response?.let {
            answerNavigatorPageFromJson(it) { data ->
                data.jsonArray.mapNotNull { item ->
                    runCatching { ZhihuJson.decodeJson<CollectionItem>(item) }.getOrNull()
                }
            }
        } ?: AnswerNavigatorPage(emptyList(), "")
        session.collectionNextPageUrl = page.nextUrl
        val articles = page.items
            .mapNotNull { item ->
                item.content.navDestination as? Article
            }.filter { it.type == ArticleType.Answer }
        val newIds = articles.map { it.id }.filter { it !in session.orderedIds }
        if (newIds.isEmpty()) return false
        articles.forEach { session.registerListMeta(it) }
        session.mergeAtEnds(emptyList(), newIds)
        return true
    }

    override suspend fun prefetchNext(currentArticleId: Long) {
        if (session.neighborId(+1) == null) ensureRightExtension(currentArticleId)
        val nextId = session.neighborId(+1) ?: return
        if (session.entryById[nextId]?.phase == NeighborPhase.Ready) return
        session.markNeighborLoading(nextId, isNext = true)
        try {
            fetchCached(nextId)?.let { session.markNeighborReady(it) }
        } catch (e: Exception) {
            Log.w("CollectionAnswerNavigator", "Failed to pre-load next answer content", e)
        }
    }

    override suspend fun prefetchPrevious(currentArticleId: Long) {
        val prevId = session.neighborId(-1) ?: return
        if (session.entryById[prevId]?.phase == NeighborPhase.Ready) return
        session.markNeighborLoading(prevId, isNext = false)
        try {
            fetchCached(prevId)?.let { session.markNeighborReady(it) }
        } catch (e: Exception) {
            Log.w("CollectionAnswerNavigator", "Failed to pre-load previous answer content", e)
        }
    }

    override suspend fun loadNextCachedFromExtension(): CachedAnswerContent? {
        if (session.neighborId(+1) == null) ensureRightExtension(session.orderedIds[session.cursor])
        val nextId = session.neighborId(+1) ?: return null
        return fetchCached(nextId)?.also { session.markNeighborReady(it) }
    }

    override suspend fun loadPreviousCachedFromExtension(): CachedAnswerContent? {
        val prevId = session.neighborId(-1) ?: return null
        return fetchCached(prevId)?.also { session.markNeighborReady(it) }
    }
}

/** 直达回答详情：仅 paginationInfo 拓展，不使用问题 feeds。 */
class PaginationAnswerNavigator(
    session: AnswerSwitchSession,
    environment: ZhihuApiEnvironment,
) : AnswerNavigator(session, environment) {
    companion object {
        fun forDirectEntry(
            answerId: Long,
            questionId: Long?,
            environment: ZhihuApiEnvironment,
        ): PaginationAnswerNavigator {
            val session = AnswerSwitchSession(
                sourceName = "此问题",
                extensionMode = AnswerSwitchExtensionMode.PaginationOnly,
                initialOrderedIds = listOf(answerId),
                initialCursor = 0,
                questionId = questionId,
            )
            return PaginationAnswerNavigator(session, environment)
        }
    }

    override suspend fun prefetchNext(currentArticleId: Long) {
        val nextId = session.neighborId(+1) ?: return
        if (session.entryById[nextId]?.phase == NeighborPhase.Ready) return
        session.markNeighborLoading(nextId, isNext = true)
        try {
            fetchCached(nextId)?.let { session.markNeighborReady(it) }
        } catch (e: Exception) {
            Log.w("PaginationAnswerNavigator", "Failed to pre-load next answer content", e)
        }
    }

    override suspend fun prefetchPrevious(currentArticleId: Long) {
        val prevId = session.neighborId(-1) ?: return
        if (session.entryById[prevId]?.phase == NeighborPhase.Ready) return
        session.markNeighborLoading(prevId, isNext = false)
        try {
            fetchCached(prevId)?.let { session.markNeighborReady(it) }
        } catch (e: Exception) {
            Log.w("PaginationAnswerNavigator", "Failed to pre-load previous answer content", e)
        }
    }

    override suspend fun loadNextCachedFromExtension(): CachedAnswerContent? {
        val nextId = session.neighborId(+1) ?: return null
        return fetchCached(nextId)?.also { session.markNeighborReady(it) }
    }

    override suspend fun loadPreviousCachedFromExtension(): CachedAnswerContent? {
        val prevId = session.neighborId(-1) ?: return null
        return fetchCached(prevId)?.also { session.markNeighborReady(it) }
    }
}

/** @deprecated 使用 [PaginationAnswerNavigator] */
@Suppress("ktlint:standard:comment-wrapping")
typealias PaginationInfoNavigator = PaginationAnswerNavigator

val PaginationAnswerNavigator.questionId: Long?
    get() = session.questionId
