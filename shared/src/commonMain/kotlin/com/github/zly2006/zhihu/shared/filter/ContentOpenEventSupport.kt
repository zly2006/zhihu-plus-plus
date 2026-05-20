package com.github.zly2006.zhihu.shared.filter

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.CollectionContent
import com.github.zly2006.zhihu.navigation.History
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question

data class TrackedContentIdentity(
    val type: String,
    val id: String,
)

data class QuestionAnswerCandidatePartition(
    val previousCandidates: List<Article>,
    val nextCandidates: List<Article>,
)

object ContentOpenFrom {
    const val ANSWER_SWITCH = "answer_switch"
    const val COLLECTION = "collection"
    const val HISTORY = "history"
    const val HOME_FEED = "home_feed"
    const val NOTIFICATION = "notification"
    const val QUESTION_FEED = "question_feed"
    const val UNKNOWN = "unknown"
}

object ContentOpenEventSupport {
    fun buildContentKey(type: String, id: String): String = "$type:$id"

    fun toTrackedContentIdentity(destination: NavDestination): TrackedContentIdentity? = when (destination) {
        is Article -> {
            val type = when (destination.type) {
                ArticleType.Answer -> CONTENT_TYPE_ANSWER
                ArticleType.Article -> CONTENT_TYPE_ARTICLE
            }
            TrackedContentIdentity(type = type, id = destination.id.toString())
        }
        is Question -> TrackedContentIdentity(type = CONTENT_TYPE_QUESTION, id = destination.questionId.toString())
        is Pin -> TrackedContentIdentity(type = CONTENT_TYPE_PIN, id = destination.id.toString())
        else -> null
    }

    fun inferOpenFrom(
        source: NavDestination?,
        target: NavDestination,
    ): String = when {
        source is Article &&
            source.type == ArticleType.Answer &&
            target is Article &&
            target.type == ArticleType.Answer -> ContentOpenFrom.ANSWER_SWITCH
        source is Question -> ContentOpenFrom.QUESTION_FEED
        source is CollectionContent -> ContentOpenFrom.COLLECTION
        source is History -> ContentOpenFrom.HISTORY
        source is Notification -> ContentOpenFrom.NOTIFICATION
        else -> ContentOpenFrom.UNKNOWN
    }

    fun filterUnopenedAnswerArticles(
        candidates: List<Article>,
        openedContentKeys: Set<String>,
        currentArticleId: Long,
        historyIds: Set<Long> = emptySet(),
    ): List<Article> = candidates.filter { article ->
        article.type == ArticleType.Answer &&
            article.id != currentArticleId &&
            article.id !in historyIds &&
            buildContentKey(CONTENT_TYPE_ANSWER, article.id.toString()) !in openedContentKeys
    }

    fun partitionQuestionAnswerCandidates(
        candidates: List<Article>,
        openedAnswerIds: Set<Long>,
        currentArticleId: Long,
        historyIds: Set<Long> = emptySet(),
        previousIds: Set<Long> = emptySet(),
        nextIds: Set<Long> = emptySet(),
    ): QuestionAnswerCandidatePartition {
        val previousCandidates = mutableListOf<Article>()
        val nextCandidates = mutableListOf<Article>()

        candidates.forEach { article ->
            if (article.type != ArticleType.Answer || article.id == currentArticleId || article.id in historyIds) {
                return@forEach
            }
            if (article.id in previousIds || article.id in nextIds) {
                return@forEach
            }
            if (article.id in openedAnswerIds) {
                previousCandidates.add(article)
            } else {
                nextCandidates.add(article)
            }
        }

        return QuestionAnswerCandidatePartition(
            previousCandidates = previousCandidates,
            nextCandidates = nextCandidates,
        )
    }

    private const val CONTENT_TYPE_ANSWER = "answer"
    private const val CONTENT_TYPE_ARTICLE = "article"
    private const val CONTENT_TYPE_QUESTION = "question"
    private const val CONTENT_TYPE_PIN = "pin"
}
