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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.navDestination
import com.github.zly2006.zhihu.shared.data.officialBadge
import com.github.zly2006.zhihu.shared.data.target
import com.github.zly2006.zhihu.shared.filter.ContentOpenEventSupport
import com.github.zly2006.zhihu.shared.util.Log
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel.CachedAnswerContent
import com.github.zly2006.zhihu.viewmodel.CollectionItem
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import com.github.zly2006.zhihu.viewmodel.filter.ContentType
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.getOrFetchContentDetail
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
 * 回答导航器：封装回答切换的来源、历史记录和预取逻辑。
 *
 * @param sourceName 来源名称，用于 UI 标签，例如 "此问题"、"「收藏夹名称」"
 */
abstract class AnswerNavigator(
    val sourceName: String,
    protected val environment: ZhihuApiEnvironment,
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
    open suspend fun prefetchPrevious(currentArticleId: Long) = Unit

    /**
     * 从来源加载上一个回答（非历史），在 [currentAnswerIndex] == 0（即 [goToPrevious] 返回 null）时由
     * navigateToPrevious 调用。
     * 加载成功后将内容插入 history 开头（index 0），返回内容以供 pendingInitialContent。
     * 默认实现返回 null（问题导航器无此能力）。
     */
    open suspend fun loadPrevious(): CachedAnswerContent? = null

    protected fun forwardHistoryArticles(): List<Article> = if (currentAnswerIndex in answerHistory.indices) {
        answerHistory
            .drop(currentAnswerIndex + 1)
            .map(CachedAnswerContent::article)
            .distinctBy(Article::id)
    } else {
        emptyList()
    }

    /**
     * 返回当前导航位置之后的回答，不消费切换队列。
     *
     * 实现可以在快照前补充来源队列，但必须保持之后 [loadNext] 的顺序不变。已经访问过、
     * 但由当前位置返回后又成为前向历史的回答必须排在来源队列之前。
     */
    open suspend fun remainingAnswersSnapshot(
        currentArticleId: Long,
        limit: Int,
    ): List<Article> = if (limit <= 0) {
        emptyList()
    } else {
        buildList {
            addAll(forwardHistoryArticles())
            nextAnswerContent?.article?.let(::add)
        }.distinctBy(Article::id).take(limit)
    }

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
 * 从问题详情页当前回答序列中导航。
 *
 * 优先使用进入详情时已加载的前后回答队列；本地队列耗尽后，再按当前排序继续请求问题 feeds。
 */
class QuestionAnswerNavigator(
    val questionId: Long,
    initialNextAnswers: List<Article> = emptyList(),
    initialPreviousAnswers: List<Article> = emptyList(),
    initialNextUrl: String = "",
    private val order: String? = null,
    private val getAlreadyOpenedAnswerIds: suspend (List<Long>) -> Set<Long> = { answerIds ->
        ContentOpenEventSupport
            .getAlreadyOpenedContentIds(
                database = getContentFilterDatabase(),
                content = answerIds.map { ContentType.ANSWER to it.toString() },
            ).mapNotNull { key ->
                key.substringAfter(':', "").toLongOrNull()
            }.toSet()
    },
    environment: ZhihuApiEnvironment,
) : AnswerNavigator("此问题", environment) {
    private val pendingInitialNextAnswers = ArrayDeque<Article>().also { deque ->
        initialNextAnswers
            .filter { it.type == ArticleType.Answer }
            .forEach { deque.addLast(it) }
    }
    private val hasInitialNextAnswers = pendingInitialNextAnswers.isNotEmpty()
    private val destinations = ArrayDeque<Article>()
    private val previousQueue = mutableStateListOf<Article>().also { list ->
        list.addAll(initialPreviousAnswers.filter { it.type == ArticleType.Answer })
    }
    private var nextUrl: String = initialNextUrl
    private val enqueuedNextIds = mutableSetOf<Long>()
    private val enqueuedPrevIds = mutableSetOf<Long>().also { set ->
        set.addAll(previousQueue.map { it.id })
    }
    private val knownOpenedIds = mutableSetOf<Long>()
    private var initialNextAnswersProcessed = false
    private var nextSourceExhausted = false
    private val destinationsMutex = Mutex()

    override suspend fun remainingAnswersSnapshot(
        currentArticleId: Long,
        limit: Int,
    ): List<Article> = destinationsMutex.withLock {
        if (limit <= 0) return@withLock emptyList()
        ensureDestinationsLocked(currentArticleId, (limit - forwardHistoryArticles().size).coerceAtLeast(0))
        (super.remainingAnswersSnapshot(currentArticleId, limit) + destinations)
            .distinctBy(Article::id)
            .take(limit)
    }

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

    private suspend fun ensureDestinationsLocked(
        currentArticleId: Long,
        minimumCount: Int,
    ) {
        if (destinations.size >= minimumCount || nextSourceExhausted) return
        val historyIds = answerHistory.map { it.article.id }.toSet()
        val fetchedUrls = mutableSetOf<String>()
        while (destinations.size < minimumCount && !nextSourceExhausted) {
            val processingInitialNextAnswers = hasInitialNextAnswers && !initialNextAnswersProcessed
            var fetchedUrl: String? = null
            var fetchedNextUrl: String? = null
            val candidates = if (processingInitialNextAnswers) {
                pendingInitialNextAnswers.toList()
            } else {
                val url = nextUrl.ifEmpty {
                    if (hasInitialNextAnswers) {
                        nextSourceExhausted = true
                        return
                    }
                    zhihuQuestionFeedsUrl(questionId, limit = 6, order = order)
                }
                if (!fetchedUrls.add(url)) {
                    nextSourceExhausted = true
                    return
                }
                fetchedUrl = url
                val response = environment.fetchJson(url, "")
                val page = response?.let {
                    answerNavigatorPageFromJson(it) { data ->
                        data.jsonArray.mapNotNull { item ->
                            runCatching { ZhihuJson.decodeJson<Feed>(item) }.getOrNull()
                        }
                    }
                } ?: AnswerNavigatorPage(emptyList(), "")
                fetchedNextUrl = page.nextUrl
                page.items.mapNotNull { feed -> feed.target?.navDestination as? Article }
            }
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
                knownOpenedIds += getAlreadyOpenedAnswerIds(idsToLookup)
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
                    previousAnswerContent = null
                    previousQueue.add(0, article)
                }
            }
            partition.nextCandidates.forEach { article ->
                if (enqueuedNextIds.add(article.id)) {
                    destinations.addLast(article)
                }
            }
            if (processingInitialNextAnswers) {
                initialNextAnswersProcessed = true
                if (nextUrl.isEmpty()) nextSourceExhausted = true
            } else {
                nextUrl = fetchedNextUrl.orEmpty()
                if (nextUrl.isEmpty() || nextUrl == fetchedUrl) nextSourceExhausted = true
            }
        }
    }

    override suspend fun loadPrevious(): CachedAnswerContent? = destinationsMutex.withLock {
        val prefetched = previousAnswerContent
        if (prefetched != null) {
            previousAnswerContent = null
            previousQueue.removeFirstOrNull()
            return@withLock insertPrevious(prefetched)
        }
        val article = previousQueue.removeFirstOrNull() ?: return@withLock null
        val cached = try {
            fetchCached(article)
        } catch (e: Exception) {
            Log.w("QuestionAnswerNavigator", "Failed to load previous answer content", e)
            null
        }
        if (cached == null) {
            previousQueue.add(0, article)
            return@withLock null
        }
        insertPrevious(cached)
    }

    override suspend fun loadNext(): Article? = destinationsMutex.withLock {
        val prefetched = nextAnswerContent
        if (prefetched != null) {
            nextAnswerContent = null
            destinations.removeFirstOrNull()
            return@withLock prefetched.article
        }
        ensureDestinationsLocked(-1L, 1)
        destinations.removeFirstOrNull()
    }

    override suspend fun prefetchPrevious(currentArticleId: Long) = destinationsMutex.withLock {
        if (previousAnswerContent != null) return@withLock
        val article = previousQueue.firstOrNull() ?: return@withLock
        try {
            previousAnswerContent = fetchCached(article)
        } catch (e: Exception) {
            Log.w("QuestionAnswerNavigator", "Failed to pre-load previous answer content", e)
        }
    }

    override suspend fun prefetchNext(currentArticleId: Long) = destinationsMutex.withLock {
        if (nextAnswerContent != null) return@withLock
        ensureDestinationsLocked(currentArticleId, 1)
        val nextDest = destinations.firstOrNull() ?: return@withLock
        if (nextDest.type != ArticleType.Answer) return@withLock
        try {
            nextAnswerContent = fetchCached(nextDest)
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
 * @param initialNextUrl 已加载收藏夹页面的下一页地址
 */
class CollectionAnswerNavigator(
    val collectionId: String,
    collectionTitle: String,
    initialNextItems: List<CollectionItem>,
    initialPreviousItems: List<CollectionItem> = emptyList(),
    initialNextUrl: String = "https://www.zhihu.com/api/v4/collections/$collectionId/items",
    environment: ZhihuApiEnvironment,
) : AnswerNavigator("「$collectionTitle」", environment) {
    private val enqueuedNextIds = mutableSetOf<Long>()
    private val queue = ArrayDeque<Article>().also { deque ->
        initialNextItems.forEach { item ->
            val article = item.content.navDestination as? Article
            if (article?.type == ArticleType.Answer && enqueuedNextIds.add(article.id)) deque.add(article)
        }
    }

    // 逆序存储：firstOrNull() 是离当前最近的上一个回答
    private val previousQueue = mutableStateListOf<Article>().also { list ->
        initialPreviousItems.forEach { item ->
            val article = item.content.navDestination as? Article
            if (article?.type == ArticleType.Answer) list.add(article)
        }
    }

    private var nextPageUrl: String = initialNextUrl
    private var prefetchedArticle: Article? = null
    private val queueMutex = Mutex()

    override suspend fun remainingAnswersSnapshot(
        currentArticleId: Long,
        limit: Int,
    ): List<Article> = queueMutex.withLock {
        if (limit <= 0) return@withLock emptyList()
        ensureQueueLocked((limit - forwardHistoryArticles().size).coerceAtLeast(0))
        buildList {
            addAll(super.remainingAnswersSnapshot(currentArticleId, limit))
            prefetchedArticle?.let(::add)
            addAll(queue)
        }.distinctBy(Article::id).take(limit)
    }

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

    private suspend fun ensureQueueLocked(minimumCount: Int) {
        val fetchedUrls = mutableSetOf<String>()
        while (queue.size + (if (prefetchedArticle == null) 0 else 1) < minimumCount && nextPageUrl.isNotEmpty()) {
            val url = nextPageUrl
            if (!fetchedUrls.add(url)) {
                nextPageUrl = ""
                return
            }
            val response = environment.fetchJson(url, "")
            val page = response?.let {
                answerNavigatorPageFromJson(it) { data ->
                    data.jsonArray.mapNotNull { item ->
                        runCatching { ZhihuJson.decodeJson<CollectionItem>(item) }.getOrNull()
                    }
                }
            } ?: AnswerNavigatorPage(emptyList(), "")
            nextPageUrl = page.nextUrl
            page.items.forEach { item ->
                val article = item.content.navDestination as? Article
                if (article?.type == ArticleType.Answer && enqueuedNextIds.add(article.id)) queue.add(article)
            }
            if (nextPageUrl == url) nextPageUrl = ""
        }
    }

    override suspend fun loadPrevious(): CachedAnswerContent? = queueMutex.withLock {
        // 如果已预取，直接消费
        val prefetched = previousAnswerContent
        if (prefetched != null) {
            previousAnswerContent = null
            previousQueue.removeFirstOrNull() // 消费队头（prefetchPrevious 使用 firstOrNull 不弹出）
            return@withLock insertPrevious(prefetched)
        }
        val article = previousQueue.removeFirstOrNull() ?: return@withLock null
        val cached = try {
            val detail = environment.getOrFetchContentDetail(article) as? DataHolder.Answer
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
                    endorsements = detail.endorsementItems,
                    sourceLabel = sourceName,
                )
            } else {
                // 加载失败时退回 previousQueue
                previousQueue.add(0, article)
                return@withLock null
            }
        } catch (e: Exception) {
            Log.w("CollectionAnswerNavigator", "Failed to load previous answer content", e)
            previousQueue.add(0, article)
            return@withLock null
        }
        // 插入到 history 开头：insert at [0] 后 currentAnswerIndex 自动指向新条目（同为 index 0）
        // 无需调整 currentAnswerIndex
        insertPrevious(cached)
    }

    override suspend fun prefetchPrevious(currentArticleId: Long) = queueMutex.withLock {
        if (previousAnswerContent != null) return@withLock
        val article = previousQueue.firstOrNull() ?: return@withLock
        try {
            val detail = environment.getOrFetchContentDetail(article) as? DataHolder.Answer ?: return@withLock
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
                endorsements = detail.endorsementItems,
                sourceLabel = sourceName,
            )
        } catch (e: Exception) {
            Log.w("CollectionAnswerNavigator", "Failed to pre-load previous answer content", e)
        }
    }

    override suspend fun prefetchNext(currentArticleId: Long) = queueMutex.withLock {
        if (nextAnswerContent != null) return@withLock
        ensureQueueLocked(1)
        val article = prefetchedArticle ?: queue.firstOrNull() ?: return@withLock
        if (prefetchedArticle == null) {
            prefetchedArticle = queue.removeFirstOrNull()
        }
        try {
            val detail = environment.getOrFetchContentDetail(article) as? DataHolder.Answer ?: return@withLock
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
                endorsements = detail.endorsementItems,
                sourceLabel = sourceName,
            )
        } catch (e: Exception) {
            Log.w("CollectionAnswerNavigator", "Failed to pre-load next answer content", e)
        }
    }

    override suspend fun loadNext(): Article? = queueMutex.withLock {
        val cached = nextAnswerContent
        if (cached != null) {
            nextAnswerContent = null
            prefetchedArticle = null
            return@withLock cached.article
        }
        val prefetched = prefetchedArticle
        if (prefetched != null) {
            prefetchedArticle = null
            return@withLock prefetched
        }
        ensureQueueLocked(1)
        queue.removeFirstOrNull()
    }
}

/**
 * 基于回答详情中 [DataHolder.Answer.PaginationInfo] 导航。
 * 利用 nextAnswerIds 作为前进队列，prevAnswerIds 作为后退队列。
 *
 * @param questionId 问题 ID，用于保持问题上下文
 * @param initialPaginationInfo 当前回答的分页信息
 */
class PaginationInfoNavigator(
    val questionId: Long,
    initialPaginationInfo: DataHolder.Answer.PaginationInfo,
    environment: ZhihuApiEnvironment,
) : AnswerNavigator("此问题", environment) {
    // 仅用于 nextQueue 去重，与 prevQueue 无关
    private val enqueuedNextIds = mutableSetOf<Long>()

    // 前进队列（有序，无重复）
    private val nextQueue = ArrayDeque<Long>().also { queue ->
        initialPaginationInfo.nextAnswerIds.forEach { id ->
            if (enqueuedNextIds.add(id)) queue.addLast(id)
        }
    }

    // 后退队列：firstOrNull() 为最近的上一个回答
    private val prevQueue = ArrayDeque<Long>().also { it.addAll(initialPaginationInfo.prevAnswerIds) }

    // 续链用：记录最后已知的 answerId，下次 nextQueue 耗尽时从此续链
    private var lastKnownNextId: Long? = nextQueue.lastOrNull()

    // 用于 prevQueue 去重
    private val enqueuedPrevIds = mutableSetOf<Long>().also { it.addAll(initialPaginationInfo.prevAnswerIds) }
    private val nextQueueMutex = Mutex()

    override suspend fun remainingAnswersSnapshot(
        currentArticleId: Long,
        limit: Int,
    ): List<Article> = nextQueueMutex.withLock {
        if (limit <= 0) return@withLock emptyList()
        ensureNextQueueLocked((limit - forwardHistoryArticles().size).coerceAtLeast(0))
        buildList {
            addAll(super.remainingAnswersSnapshot(currentArticleId, limit))
            nextQueue.forEach { id -> add(Article(id = id, type = ArticleType.Answer)) }
        }.distinctBy(Article::id).take(limit)
    }

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

    private suspend fun ensureNextQueueLocked(minimumCount: Int) {
        val fetchedIds = mutableSetOf<Long>()
        while (nextQueue.size < minimumCount) {
            val id = lastKnownNextId ?: return
            if (!fetchedIds.add(id)) {
                lastKnownNextId = null
                return
            }
            val dest = Article(id = id, type = ArticleType.Answer)
            val detail = environment.getOrFetchContentDetail(dest) as? DataHolder.Answer
            val pagination = detail?.paginationInfo
            if (pagination == null) {
                lastKnownNextId = null
                return
            }
            pagination.nextAnswerIds.forEach { newId ->
                if (enqueuedNextIds.add(newId)) nextQueue.addLast(newId)
            }
            lastKnownNextId = pagination.nextAnswerIds.lastOrNull()
            // 续链时同步填充 prevQueue
            val historyIds = answerHistory.map { it.article.id }.toSet()
            pagination.prevAnswerIds.asReversed().forEach { newId ->
                if (enqueuedPrevIds.add(newId) && newId !in historyIds) {
                    previousAnswerContent = null
                    prevQueue.addFirst(newId)
                }
            }
        }
    }

    private suspend fun fetchCached(answerId: Long): CachedAnswerContent? {
        val dest = Article(id = answerId, type = ArticleType.Answer)
        val detail = environment.getOrFetchContentDetail(dest) as? DataHolder.Answer ?: return null
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
            endorsements = detail.endorsementItems,
            sourceLabel = sourceName,
        )
    }

    override suspend fun loadPrevious(): CachedAnswerContent? = nextQueueMutex.withLock {
        // 如果已预取，直接消费
        val prefetched = previousAnswerContent
        if (prefetched != null) {
            previousAnswerContent = null
            prevQueue.removeFirstOrNull() // 消费队头 id（prefetchPrevious 使用 firstOrNull 不弹出）
            return@withLock insertPrevious(prefetched)
        }
        val id = prevQueue.removeFirstOrNull() ?: return@withLock null
        val cached = try {
            fetchCached(id)
        } catch (e: Exception) {
            Log.w("PaginationInfoNavigator", "Failed to load previous answer content", e)
            null
        }
        if (cached == null) {
            prevQueue.addFirst(id)
            return@withLock null
        }
        insertPrevious(cached)
    }

    override suspend fun loadNext(): Article? = nextQueueMutex.withLock {
        val prefetched = nextAnswerContent
        if (prefetched != null) {
            nextAnswerContent = null
            // prefetchNext 使用 firstOrNull() 不弹出队列，此处消费时统一弹出
            val dequeued = nextQueue.removeFirstOrNull()
            if (dequeued != null && dequeued != prefetched.article.id) {
                Log.w(
                    "PaginationInfoNavigator",
                    "Queue head $dequeued != prefetched ${prefetched.article.id}, possible state mismatch",
                )
            }
            return@withLock prefetched.article
        }
        ensureNextQueueLocked(1)
        val id = nextQueue.removeFirstOrNull() ?: return@withLock null
        Article(id = id, type = ArticleType.Answer)
    }

    // currentArticleId 未使用：队列来自 API，无需过滤当前回答
    override suspend fun prefetchNext(currentArticleId: Long) = nextQueueMutex.withLock {
        if (nextAnswerContent != null) return@withLock
        ensureNextQueueLocked(1)
        val id = nextQueue.firstOrNull() ?: return@withLock
        try {
            nextAnswerContent = fetchCached(id)
        } catch (e: Exception) {
            Log.w("PaginationInfoNavigator", "Failed to pre-load next answer content", e)
        }
    }

    override suspend fun prefetchPrevious(currentArticleId: Long) = nextQueueMutex.withLock {
        if (previousAnswerContent != null) return@withLock
        val id = prevQueue.firstOrNull() ?: return@withLock
        try {
            previousAnswerContent = fetchCached(id)
        } catch (e: Exception) {
            Log.w("PaginationInfoNavigator", "Failed to pre-load previous answer content", e)
        }
    }
}
