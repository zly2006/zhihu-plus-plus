package com.github.zly2006.zhihu.navigator

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.ArticleType
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.util.signFetchRequest
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel.CachedAnswerContent
import com.github.zly2006.zhihu.viewmodel.CollectionContentViewModel
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 回答导航器：封装回答切换的来源、历史记录和预取逻辑。
 *
 * @param sourceName 来源名称，用于 UI 标签，例如 "此问题"、"「收藏夹名称」"
 */
abstract class AnswerNavigator(
    val sourceName: String,
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
        get() = if (currentAnswerIndex > 0) answerHistory[currentAnswerIndex - 1] else previousAnswerPreview

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

    /**
     * 用户触发"下一个回答"时调用。返回导航目标 [Article]，
     * 若已无更多回答则返回 null。
     */
    abstract suspend fun loadNext(context: Context): Article?

    /**
     * 在后台预取下一个回答的内容，填充 [nextAnswerContent]。
     * [currentArticleId] 用于跳过当前已显示的回答。
     */
    abstract suspend fun prefetchNext(context: Context, currentArticleId: Long)

    /**
     * 从来源加载上一个回答（非历史），在 history 为空时由 navigateToPrevious 调用。
     * 加载成功后将内容插入 history 开头（index 0），返回内容以供 pendingInitialContent。
     * 默认实现返回 null（问题导航器无此能力）。
     */
    open suspend fun loadPrevious(context: Context): CachedAnswerContent? = null
}

/**
 * 从知乎问题的回答列表中导航。
 */
class QuestionAnswerNavigator(
    val questionId: Long,
) : AnswerNavigator("此问题") {
    private val destinations = ArrayDeque<Feed>()
    private var nextUrl: String = ""

    private suspend fun ensureDestinations(context: Context, currentArticleId: Long) {
        if (destinations.isNotEmpty()) return
        val url = nextUrl.ifEmpty { "https://www.zhihu.com/api/v4/questions/$questionId/feeds?limit=2" }
        val jojo = AccountData.fetchGet(context, url) { signFetchRequest(context) } ?: return
        val data = AccountData.decodeJson<List<Feed>>(jojo["data"] ?: return)
        nextUrl = jojo["paging"]
            ?.jsonObject
            ?.get("next")
            ?.jsonPrimitive
            ?.content ?: ""
        destinations.addAll(
            data.filter { feed ->
                val dest = feed.target?.navDestination
                dest is Article && dest.id != currentArticleId
            },
        )
    }

    override suspend fun loadNext(context: Context): Article? {
        if (nextAnswerContent != null) {
            val article = nextAnswerContent!!.article
            nextAnswerContent = null
            destinations.removeFirstOrNull()
            return article
        }
        ensureDestinations(context, -1L)
        return (destinations.removeFirstOrNull()?.target?.navDestination as? Article)
    }

    override suspend fun prefetchNext(context: Context, currentArticleId: Long) {
        if (nextAnswerContent != null) return
        ensureDestinations(context, currentArticleId)
        val nextFeed = destinations.firstOrNull() ?: return
        val nextDest = nextFeed.target?.navDestination as? Article ?: return
        if (nextDest.type != ArticleType.Answer) return
        try {
            val detail = DataHolder.getContentDetail(context, nextDest) as? DataHolder.Answer ?: return
            nextAnswerContent = CachedAnswerContent(
                article = nextDest,
                title = detail.question.title,
                authorName = detail.author.name,
                authorBio = detail.author.headline,
                authorAvatarUrl = detail.author.avatarUrl,
                content = detail.content,
                voteUpCount = detail.voteupCount,
                commentCount = detail.commentCount,
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
    initialNextItems: List<CollectionContentViewModel.CollectionItem>,
    initialPreviousItems: List<CollectionContentViewModel.CollectionItem> = emptyList(),
) : AnswerNavigator("「$collectionTitle」") {
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

    private var nextPageUrl: String = "https://www.zhihu.com/api/v4/collections/$collectionId/items"
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

    private suspend fun ensureQueue(context: Context) {
        if (queue.isNotEmpty() || prefetchedArticle != null) return
        if (nextPageUrl.isEmpty()) return
        val jojo = AccountData.fetchGet(context, nextPageUrl) { signFetchRequest(context) } ?: return
        val items = AccountData.decodeJson<List<CollectionContentViewModel.CollectionItem>>(jojo["data"] ?: return)
        nextPageUrl = jojo["paging"]
            ?.jsonObject
            ?.get("next")
            ?.jsonPrimitive
            ?.content ?: ""
        items.forEach { item ->
            val article = item.content.navDestination as? Article
            if (article?.type == ArticleType.Answer) queue.add(article)
        }
    }

    override suspend fun loadPrevious(context: Context): CachedAnswerContent? {
        val article = previousQueue.removeFirstOrNull() ?: return null
        val cached = try {
            val detail = DataHolder.getContentDetail(context, article) as? DataHolder.Answer
            if (detail != null) {
                CachedAnswerContent(
                    article = article,
                    title = detail.question.title,
                    authorName = detail.author.name,
                    authorBio = detail.author.headline,
                    authorAvatarUrl = detail.author.avatarUrl,
                    content = detail.content,
                    voteUpCount = detail.voteupCount,
                    commentCount = detail.commentCount,
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
        // 插入到 history 开头，使向后导航可从 goToPrevious() 返回
        answerHistory.add(0, cached)
        // currentAnswerIndex 从 0 变为指向同一条目（insertedAt=0，原index+1）
        if (currentAnswerIndex >= 0) currentAnswerIndex++
        return cached
    }

    override suspend fun prefetchNext(context: Context, currentArticleId: Long) {
        if (nextAnswerContent != null) return
        ensureQueue(context)
        val article = prefetchedArticle ?: queue.firstOrNull() ?: return
        if (prefetchedArticle == null) {
            prefetchedArticle = queue.removeFirstOrNull()
        }
        try {
            val detail = DataHolder.getContentDetail(context, article) as? DataHolder.Answer ?: return
            nextAnswerContent = CachedAnswerContent(
                article = article,
                title = detail.question.title,
                authorName = detail.author.name,
                authorBio = detail.author.headline,
                authorAvatarUrl = detail.author.avatarUrl,
                content = detail.content,
                voteUpCount = detail.voteupCount,
                commentCount = detail.commentCount,
                sourceLabel = sourceName,
            )
        } catch (e: Exception) {
            Log.w("CollectionAnswerNavigator", "Failed to pre-load next answer content", e)
        }
    }

    override suspend fun loadNext(context: Context): Article? {
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
        ensureQueue(context)
        return queue.removeFirstOrNull()
    }
}
