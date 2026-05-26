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

package com.github.zly2006.zhihu.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.zly2006.zhihu.shared.article.CachedAnswerContent
import com.github.zly2006.zhihu.shared.data.CollectionItem
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.navDestination
import com.github.zly2006.zhihu.shared.data.officialBadge
import com.github.zly2006.zhihu.shared.data.target
import com.github.zly2006.zhihu.shared.filter.ContentOpenEventSupport
import com.github.zly2006.zhihu.shared.util.Log
import com.github.zly2006.zhihu.viewmodel.zhihuCollectionItemsUrl

interface AnswerNavigatorRepository {
    suspend fun fetchAnswerContent(article: Article): DataHolder.Answer?

    suspend fun fetchQuestionFeeds(
        questionId: Long,
        pageUrl: String?,
    ): AnswerNavigatorPage<Feed>

    suspend fun fetchCollectionItems(pageUrl: String): AnswerNavigatorPage<CollectionItem>

    suspend fun getAlreadyOpenedAnswerIds(answerIds: List<Long>): Set<Long>
}

data class AnswerNavigatorPage<T>(
    val items: List<T>,
    val nextUrl: String,
)

/**
 * 回答导航器：封装回答切换的来源、历史记录和预取逻辑。
 *
 * @param sourceName 来源名称，用于 UI 标签，例如 "此问题"、"「收藏夹名称」"
 */
abstract class AnswerNavigator(
    val sourceName: String,
    protected val repository: AnswerNavigatorRepository,
) {
    // ── 历史记录 ──────────────────────────────────────────────────────────────

    val answerHistory = mutableStateListOf<CachedAnswerContent>()
    var currentAnswerIndex by mutableIntStateOf(-1)

    /**
     * 来源提供的上一个回答预览（history 为空时的 fallback）。
     * 子类可覆盖，用于从来源（如收藏夹）向前导航。
     */
    open val previousAnswerPreview: CachedAnswerContent?
        get() = null

    val previousAnswer: CachedAnswerContent?
        get() = if (currentAnswerIndex > 0) {
            answerHistory[currentAnswerIndex - 1]
        } else {
            previousAnswerContent ?: previousAnswerPreview
        }

    val nextAnswer: CachedAnswerContent?
        get() = if (currentAnswerIndex in 0 until answerHistory.size - 1) {
            answerHistory[currentAnswerIndex + 1]
        } else {
            nextAnswerContent
        }

    /** 将当前回答推入历史，截断前向分支。 */
    fun pushAnswer(cached: CachedAnswerContent) {
        val articleId = cached.article.id
        // 历史内导航后再次 loadArticle：只更新内容，不改变索引
        if (currentAnswerIndex in answerHistory.indices &&
            answerHistory[currentAnswerIndex].article == cached.article
        ) {
            answerHistory[currentAnswerIndex] = cached
            return
        }
        // 新回答：截断前向历史
        if (currentAnswerIndex >= 0 && currentAnswerIndex < answerHistory.size - 1) {
            val removeRange = (currentAnswerIndex + 1) until answerHistory.size
            removeRange.reversed().forEach { i -> answerHistory.removeAt(i) }
        }
        if (answerHistory.lastOrNull()?.article?.id != articleId) {
            answerHistory.add(cached)
        }
        currentAnswerIndex = answerHistory.size - 1
    }

    fun goToPrevious(): CachedAnswerContent? {
        if (currentAnswerIndex > 0) {
            currentAnswerIndex--
            return answerHistory[currentAnswerIndex]
        }
        return null
    }

    fun goToNext(): CachedAnswerContent? {
        if (currentAnswerIndex in 0 until answerHistory.size - 1) {
            currentAnswerIndex++
            return answerHistory[currentAnswerIndex]
        }
        return null
    }

    // ── 预取 ──────────────────────────────────────────────────────────────────

    /** 预取的下一个回答内容，供预览卡片使用（Compose 状态，自动触发重组）。 */
    var nextAnswerContent: CachedAnswerContent? by mutableStateOf(null)

    /** 预取的上一个回答内容（来源队列中，非历史），供预览卡片使用。 */
    var previousAnswerContent: CachedAnswerContent? by mutableStateOf(null)

    /**
     * 用户触发"下一个回答"时调用。返回导航目标 [Article]，
     * 若已无更多回答则返回 null。
     */
    abstract suspend fun loadNext(): Article?

    /**
     * 在后台预取下一个回答的内容，填充 [nextAnswerContent]。
     * [currentArticleId] 用于跳过当前已显示的回答。
     */
    abstract suspend fun prefetchNext(currentArticleId: Long)

    /**
     * 在后台预取上一个回答的内容，填充 [previousAnswerContent]，供预览卡片显示完整信息。
     * 默认不预取（问题导航器无上一个来源）。
     */
    open suspend fun prefetchPrevious(currentArticleId: Long) {}

    /**
     * 从来源加载上一个回答（非历史），在 [currentAnswerIndex] == 0（即 [goToPrevious] 返回 null）时由
     * navigateToPrevious 调用。
     * 加载成功后将内容插入 history 开头（index 0），返回内容以供 pendingInitialContent。
     * 默认实现返回 null（问题导航器无此能力）。
     */
    open suspend fun loadPrevious(): CachedAnswerContent? = null

    /**
     * 将 [cached] 插入 history 开头并返回，供 [loadPrevious] 实现共用。
     * insert at [0] 后 currentAnswerIndex 保持 0，恰好指向新插入条目。
     */
    protected fun insertPrevious(cached: CachedAnswerContent): CachedAnswerContent {
        answerHistory.add(0, cached)
        return cached
    }
}

/**
 * 从知乎问题的回答列表中导航。
 *
 * 由于默认从最高赞开始，而非上次阅读开始，不建议使用，做fallback。
 */
class QuestionAnswerNavigator(
    val questionId: Long,
    repository: AnswerNavigatorRepository,
) : AnswerNavigator("此问题", repository) {
    private val destinations = ArrayDeque<Article>()
    private val previousQueue = mutableStateListOf<Article>()
    private var nextUrl: String = ""
    private val enqueuedNextIds = mutableSetOf<Long>()
    private val enqueuedPrevIds = mutableSetOf<Long>()
    private val knownOpenedIds = mutableSetOf<Long>()

    override val previousAnswerPreview: CachedAnswerContent?
        get() {
            val article = previousQueue.firstOrNull() ?: return null
            return CachedAnswerContent(
                article = article,
                title = article.title,
                authorName = article.authorName,
                authorBio = article.authorBio,
                authorAvatarUrl = article.avatarSrc ?: "",
                content = "",
                voteUpCount = 0,
                commentCount = 0,
                sourceLabel = sourceName,
            )
        }

    private suspend fun fetchCached(article: Article): CachedAnswerContent? {
        val detail = repository.fetchAnswerContent(article) ?: return null
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
            sourceLabel = sourceName,
        )
    }

    private suspend fun ensureDestinations(currentArticleId: Long) {
        if (destinations.isNotEmpty()) return
        val historyIds = answerHistory.map { it.article.id }.toSet()
        while (destinations.isEmpty()) {
            val page = repository.fetchQuestionFeeds(
                questionId = questionId,
                pageUrl = nextUrl.ifEmpty { null },
            )
            nextUrl = page.nextUrl
            val data = page.items
            val candidates = data.mapNotNull { feed -> feed.target?.navDestination as? Article }
            val idsToLookup = candidates
                .asSequence()
                .filter { it.type == ArticleType.Answer }
                .map { it.id }
                .filter { id ->
                    id != currentArticleId &&
                        id !in historyIds &&
                        id !in enqueuedPrevIds &&
                        id !in enqueuedNextIds &&
                        id !in knownOpenedIds
                }.toList()
            if (idsToLookup.isNotEmpty()) {
                knownOpenedIds += repository.getAlreadyOpenedAnswerIds(idsToLookup)
            }
            val partition = ContentOpenEventSupport.partitionQuestionAnswerCandidates(
                candidates = candidates,
                openedAnswerIds = knownOpenedIds,
                currentArticleId = currentArticleId,
                historyIds = historyIds,
                previousIds = enqueuedPrevIds,
                nextIds = enqueuedNextIds,
            )
            partition.previousCandidates.forEach { article ->
                if (enqueuedPrevIds.add(article.id)) {
                    previousQueue.add(0, article)
                }
            }
            partition.nextCandidates.forEach { article ->
                if (enqueuedNextIds.add(article.id)) {
                    destinations.addLast(article)
                }
            }
            if (nextUrl.isEmpty()) return
        }
    }

    override suspend fun loadPrevious(): CachedAnswerContent? {
        val prefetched = previousAnswerContent
        if (prefetched != null) {
            previousAnswerContent = null
            previousQueue.removeFirstOrNull()
            return insertPrevious(prefetched)
        }
        val article = previousQueue.removeFirstOrNull() ?: return null
        val cached = try {
            fetchCached(article)
        } catch (e: Exception) {
            Log.w("QuestionAnswerNavigator", "Failed to load previous answer content", e)
            null
        }
        if (cached == null) {
            previousQueue.add(0, article)
            return null
        }
        return insertPrevious(cached)
    }

    override suspend fun loadNext(): Article? {
        if (nextAnswerContent != null) {
            val article = nextAnswerContent!!.article
            nextAnswerContent = null
            destinations.removeFirstOrNull()
            return article
        }
        ensureDestinations(-1L)
        return destinations.removeFirstOrNull()
    }

    override suspend fun prefetchPrevious(currentArticleId: Long) {
        if (previousAnswerContent != null) return
        val article = previousQueue.firstOrNull() ?: return
        try {
            previousAnswerContent = fetchCached(article)
        } catch (e: Exception) {
            Log.w("QuestionAnswerNavigator", "Failed to pre-load previous answer content", e)
        }
    }

    override suspend fun prefetchNext(currentArticleId: Long) {
        if (nextAnswerContent != null) return
        ensureDestinations(currentArticleId)
        val nextDest = destinations.firstOrNull() ?: return
        if (nextDest.type != ArticleType.Answer) return
        try {
            val detail = repository.fetchAnswerContent(nextDest) ?: return
            nextAnswerContent = CachedAnswerContent(
                article = nextDest,
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
                sourceLabel = sourceName,
            )
        } catch (e: Exception) {
            Log.w("QuestionAnswerNavigator", "Failed to pre-load next answer content", e)
        }
    }
}

/**
 * 从收藏夹中导航回答。
 *
 * @param collectionId 收藏夹 ID
 * @param collectionTitle 收藏夹名称，用于 UI 标签
 * @param initialNextItems 从当前回答之后的条目，来自 CollectionContentViewModel.allData
 * @param initialPreviousItems 从当前回答之前的条目（逆序，最近的在前），用于向前导航
 */
class CollectionAnswerNavigator(
    val collectionId: String,
    collectionTitle: String,
    initialNextItems: List<CollectionItem>,
    initialPreviousItems: List<CollectionItem> = emptyList(),
    repository: AnswerNavigatorRepository,
) : AnswerNavigator("「$collectionTitle」", repository) {
    private val queue = ArrayDeque<Article>().also { deque ->
        initialNextItems.forEach { item ->
            val article = item.content.navDestination as? Article
            if (article?.type == ArticleType.Answer) deque.add(article)
        }
    }

    // 逆序存储：firstOrNull() 是离当前最近的上一个回答
    private val previousQueue = mutableStateListOf<Article>().also { list ->
        initialPreviousItems.forEach { item ->
            val article = item.content.navDestination as? Article
            if (article?.type == ArticleType.Answer) list.add(article)
        }
    }

    private var nextPageUrl: String = zhihuCollectionItemsUrl(collectionId)
    private var prefetchedArticle: Article? = null

    override val previousAnswerPreview: CachedAnswerContent?
        get() {
            val article = previousQueue.firstOrNull() ?: return null
            return CachedAnswerContent(
                article = article,
                title = article.title,
                authorName = article.authorName,
                authorBio = article.authorBio,
                authorAvatarUrl = article.avatarSrc ?: "",
                content = "",
                voteUpCount = 0,
                commentCount = 0,
                sourceLabel = sourceName,
            )
        }

    private suspend fun ensureQueue() {
        if (queue.isNotEmpty() || prefetchedArticle != null) return
        if (nextPageUrl.isEmpty()) return
        val page = repository.fetchCollectionItems(nextPageUrl)
        nextPageUrl = page.nextUrl
        page.items.forEach { item ->
            val article = item.content.navDestination as? Article
            if (article?.type == ArticleType.Answer) queue.add(article)
        }
    }

    override suspend fun loadPrevious(): CachedAnswerContent? {
        // 如果已预取，直接消费
        val prefetched = previousAnswerContent
        if (prefetched != null) {
            previousAnswerContent = null
            previousQueue.removeFirstOrNull() // 消费队头（prefetchPrevious 使用 firstOrNull 不弹出）
            return insertPrevious(prefetched)
        }
        val article = previousQueue.removeFirstOrNull() ?: return null
        val cached = try {
            val detail = repository.fetchAnswerContent(article)
            if (detail != null) {
                CachedAnswerContent(
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
                    sourceLabel = sourceName,
                )
            } else {
                // 加载失败时退回 previousQueue
                previousQueue.add(0, article)
                return null
            }
        } catch (e: Exception) {
            Log.w("CollectionAnswerNavigator", "Failed to load previous answer content", e)
            previousQueue.add(0, article)
            return null
        }
        // 插入到 history 开头：insert at [0] 后 currentAnswerIndex 自动指向新条目（同为 index 0）
        // 无需调整 currentAnswerIndex
        return insertPrevious(cached)
    }

    override suspend fun prefetchPrevious(currentArticleId: Long) {
        if (previousAnswerContent != null) return
        val article = previousQueue.firstOrNull() ?: return
        try {
            val detail = repository.fetchAnswerContent(article) ?: return
            previousAnswerContent = CachedAnswerContent(
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
                sourceLabel = sourceName,
            )
        } catch (e: Exception) {
            Log.w("CollectionAnswerNavigator", "Failed to pre-load previous answer content", e)
        }
    }

    override suspend fun prefetchNext(currentArticleId: Long) {
        if (nextAnswerContent != null) return
        ensureQueue()
        val article = prefetchedArticle ?: queue.firstOrNull() ?: return
        if (prefetchedArticle == null) {
            prefetchedArticle = queue.removeFirstOrNull()
        }
        try {
            val detail = repository.fetchAnswerContent(article) ?: return
            nextAnswerContent = CachedAnswerContent(
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
                sourceLabel = sourceName,
            )
        } catch (e: Exception) {
            Log.w("CollectionAnswerNavigator", "Failed to pre-load next answer content", e)
        }
    }

    override suspend fun loadNext(): Article? {
        val cached = nextAnswerContent
        if (cached != null) {
            nextAnswerContent = null
            prefetchedArticle = null
            return cached.article
        }
        val prefetched = prefetchedArticle
        if (prefetched != null) {
            prefetchedArticle = null
            return prefetched
        }
        ensureQueue()
        return queue.removeFirstOrNull()
    }
}

/**
 * 基于回答详情中 [DataHolder.Answer.PaginationInfo] 导航。
 * 利用 nextAnswerIds 作为前进队列，prevAnswerIds 作为后退队列。
 * 每次加载新回答后调用 [updateFromPaginationInfo] 补充队列并去重。
 *
 * @param questionId 问题 ID，用于保持问题上下文
 * @param initialPaginationInfo 当前回答的分页信息
 */
class PaginationInfoNavigator(
    val questionId: Long,
    initialPaginationInfo: DataHolder.Answer.PaginationInfo,
    repository: AnswerNavigatorRepository,
) : AnswerNavigator("此问题", repository) {
    // 前进队列（有序，无重复）
    private val nextQueue = ArrayDeque<Long>().also { it.addAll(initialPaginationInfo.nextAnswerIds) }

    // 后退队列：firstOrNull() 为最近的上一个回答
    private val prevQueue = ArrayDeque<Long>().also { it.addAll(initialPaginationInfo.prevAnswerIds) }

    // 续链用：记录最后已知的 answerId，下次 nextQueue 耗尽时从此续链
    private var lastKnownNextId: Long? = initialPaginationInfo.nextAnswerIds.lastOrNull()

    // 仅用于 nextQueue 去重，与 prevQueue 无关
    private val enqueuedNextIds = mutableSetOf<Long>().also { it.addAll(initialPaginationInfo.nextAnswerIds) }

    // 用于 prevQueue 去重
    private val enqueuedPrevIds = mutableSetOf<Long>().also { it.addAll(initialPaginationInfo.prevAnswerIds) }

    // 仅有 id，无标题和作者信息；完整数据需 loadPrevious() 后从 answerHistory 取
    override val previousAnswerPreview: CachedAnswerContent?
        get() {
            val id = prevQueue.firstOrNull() ?: return null
            return CachedAnswerContent(
                article = Article(id = id, type = ArticleType.Answer),
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

    /**
     * 每次成功加载回答后调用，将新的 paginationInfo 中的 ids 去重后追加进队列。
     * nextAnswerIds 追加到队尾；prevAnswerIds 逆序插入队头（最近的排最前）。
     */
    fun updateFromPaginationInfo(info: DataHolder.Answer.PaginationInfo) {
        // nextQueue：追加尾部，去重
        info.nextAnswerIds.forEach { id ->
            if (enqueuedNextIds.add(id)) nextQueue.addLast(id)
        }
        lastKnownNextId = info.nextAnswerIds.lastOrNull() ?: lastKnownNextId

        // prevQueue：逆序插入头部（prevAnswerIds[0] 是最近的，应排最前）
        // 同时过滤已在 answerHistory 中的 id，避免重复导航
        val historyIds = answerHistory.map { it.article.id }.toSet()
        info.prevAnswerIds.asReversed().forEach { id ->
            if (enqueuedPrevIds.add(id) && id !in historyIds) {
                prevQueue.addFirst(id)
            }
        }
    }

    private suspend fun ensureNextQueue() {
        val id = lastKnownNextId ?: return
        if (nextQueue.isNotEmpty()) return
        val dest = Article(id = id, type = ArticleType.Answer)
        val detail = repository.fetchAnswerContent(dest) ?: return
        val pagination = detail.paginationInfo ?: return
        pagination.nextAnswerIds.forEach { newId ->
            if (enqueuedNextIds.add(newId)) nextQueue.addLast(newId)
        }
        lastKnownNextId = pagination.nextAnswerIds.lastOrNull() ?: lastKnownNextId
        // 续链时同步填充 prevQueue
        val historyIds = answerHistory.map { it.article.id }.toSet()
        pagination.prevAnswerIds.asReversed().forEach { newId ->
            if (enqueuedPrevIds.add(newId) && newId !in historyIds) {
                prevQueue.addFirst(newId)
            }
        }
    }

    private suspend fun fetchCached(answerId: Long): CachedAnswerContent? {
        val dest = Article(id = answerId, type = ArticleType.Answer)
        val detail = repository.fetchAnswerContent(dest) ?: return null
        return CachedAnswerContent(
            article = dest,
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
            sourceLabel = sourceName,
        )
    }

    override suspend fun loadPrevious(): CachedAnswerContent? {
        // 如果已预取，直接消费
        val prefetched = previousAnswerContent
        if (prefetched != null) {
            previousAnswerContent = null
            prevQueue.removeFirstOrNull() // 消费队头 id（prefetchPrevious 使用 firstOrNull 不弹出）
            return insertPrevious(prefetched)
        }
        val id = prevQueue.removeFirstOrNull() ?: return null
        val cached = try {
            fetchCached(id)
        } catch (e: Exception) {
            Log.w("PaginationInfoNavigator", "Failed to load previous answer content", e)
            null
        }
        if (cached == null) {
            prevQueue.addFirst(id)
            return null
        }
        return insertPrevious(cached)
    }

    override suspend fun loadNext(): Article? {
        if (nextAnswerContent != null) {
            val article = nextAnswerContent!!.article
            nextAnswerContent = null
            // prefetchNext 使用 firstOrNull() 不弹出队列，此处消费时统一弹出
            val dequeued = nextQueue.removeFirstOrNull()
            if (dequeued != null && dequeued != article.id) {
                Log.w("PaginationInfoNavigator", "Queue head $dequeued != prefetched ${article.id}, possible state mismatch")
            }
            return article
        }
        ensureNextQueue()
        val id = nextQueue.removeFirstOrNull() ?: return null
        return Article(id = id, type = ArticleType.Answer)
    }

    // currentArticleId 未使用：队列来自 API，无需过滤当前回答
    override suspend fun prefetchNext(currentArticleId: Long) {
        if (nextAnswerContent != null) return
        ensureNextQueue()
        val id = nextQueue.firstOrNull() ?: return
        try {
            nextAnswerContent = fetchCached(id)
        } catch (e: Exception) {
            Log.w("PaginationInfoNavigator", "Failed to pre-load next answer content", e)
        }
    }

    override suspend fun prefetchPrevious(currentArticleId: Long) {
        if (previousAnswerContent != null) return
        val id = prevQueue.firstOrNull() ?: return
        try {
            previousAnswerContent = fetchCached(id)
        } catch (e: Exception) {
            Log.w("PaginationInfoNavigator", "Failed to pre-load previous answer content", e)
        }
    }
}
