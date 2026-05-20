package com.github.zly2006.zhihu.shared.recommendation

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.data.Feed

data class LocalContentIdentity(
    val type: String,
    val id: String,
) {
    val value: String
        get() = "$type:$id"
}

data class LocalReasonStats(
    val clicks: Int = 0,
    val likes: Int = 0,
    val dislikes: Int = 0,
)

data class LocalContentStats(
    val clicks: Int = 0,
    val likes: Int = 0,
    val dislikes: Int = 0,
)

data class LocalReasonPreference(
    val multiplier: Double,
    val explanation: String? = null,
)

data class LocalContentAffinity(
    val multiplier: Double,
    val explanation: String? = null,
)

const val LOCAL_CONTENT_TYPE_ANSWER = "answer"
const val LOCAL_CONTENT_TYPE_ARTICLE = "article"
const val LOCAL_CONTENT_TYPE_QUESTION = "question"
const val LOCAL_CONTENT_TYPE_PIN = "pin"

fun normalizeLocalContentId(type: String, id: String): String = LocalContentIdentity(type = type, id = id).value

fun parseLocalContentIdentity(
    contentId: String,
    url: String,
): LocalContentIdentity? {
    val trimmedContentId = contentId.trim()
    if (trimmedContentId.contains(':')) {
        val type = trimmedContentId.substringBefore(':').trim()
        val id = trimmedContentId.substringAfter(':').trim()
        if (type.isNotEmpty() && id.isNotEmpty()) {
            return LocalContentIdentity(type = type, id = id)
        }
    }

    return inferIdentityFromUrl(url)
}

fun LocalContentIdentity.toNavDestination(title: String): NavDestination? {
    val numericId = id.toLongOrNull() ?: return null
    return when (type) {
        LOCAL_CONTENT_TYPE_ANSWER ->
            Article(type = ArticleType.Answer, id = numericId, title = title)
        LOCAL_CONTENT_TYPE_ARTICLE ->
            Article(type = ArticleType.Article, id = numericId, title = title)
        LOCAL_CONTENT_TYPE_QUESTION ->
            Question(questionId = numericId, title = title)
        LOCAL_CONTENT_TYPE_PIN ->
            Pin(id = numericId)
        else -> null
    }
}

fun Feed.Target.toLocalContentIdentity(): LocalContentIdentity = when (this) {
    is Feed.AnswerTarget -> LocalContentIdentity(
        LOCAL_CONTENT_TYPE_ANSWER,
        id.toString(),
    )
    is Feed.ArticleTarget -> LocalContentIdentity(
        LOCAL_CONTENT_TYPE_ARTICLE,
        id.toString(),
    )
    is Feed.QuestionTarget -> LocalContentIdentity(
        LOCAL_CONTENT_TYPE_QUESTION,
        id.toString(),
    )
    is Feed.PinTarget -> LocalContentIdentity(
        LOCAL_CONTENT_TYPE_PIN,
        id.toString(),
    )
    is Feed.VideoTarget -> LocalContentIdentity("video", id.toString())
}

fun buildReasonPreference(stats: LocalReasonStats): LocalReasonPreference {
    val signal = (stats.clicks * 0.12) + (stats.likes * 0.35) - (stats.dislikes * 0.45)
    val multiplier = (1.0 + signal).coerceIn(0.55, 1.6)
    val explanation = when {
        stats.likes > 0 -> "你最近更偏好这类来源"
        stats.clicks >= 2 -> "你经常点开这类来源"
        else -> null
    }
    return LocalReasonPreference(multiplier = multiplier, explanation = explanation)
}

fun buildContentAffinity(stats: LocalContentStats): LocalContentAffinity {
    val signal = (stats.clicks * 0.08) + (stats.likes * 0.40) - (stats.dislikes * 0.70)
    val multiplier = (1.0 + signal).coerceIn(0.15, 1.8)
    val explanation = when {
        stats.likes > 0 -> "你明确喜欢过类似内容"
        stats.clicks >= 2 -> "你最近点开过类似内容"
        else -> null
    }
    return LocalContentAffinity(multiplier = multiplier, explanation = explanation)
}

fun buildLocalRecommendationReason(
    baseReason: String,
    reasonPreference: LocalReasonPreference?,
    contentAffinity: LocalContentAffinity?,
): String = listOfNotNull(
    baseReason.takeIf { it.isNotBlank() },
    contentAffinity?.explanation,
    reasonPreference?.explanation,
).joinToString(" · ")

fun stableLocalFeedId(contentId: String): String = "local_feed_${contentId.replace(':', '_')}"

private fun inferIdentityFromUrl(url: String): LocalContentIdentity? {
    val patterns = listOf(
        LOCAL_CONTENT_TYPE_ANSWER to Regex("""/(?:answer|answers)/(\d+)"""),
        LOCAL_CONTENT_TYPE_ARTICLE to Regex("""/(?:articles|p)/(\d+)"""),
        LOCAL_CONTENT_TYPE_QUESTION to Regex("""/(?:question|questions)/(\d+)"""),
        LOCAL_CONTENT_TYPE_PIN to Regex("""/(?:pin|pins)/(\d+)"""),
    )

    return patterns.firstNotNullOfOrNull { (type, regex) ->
        regex.find(url)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }?.let { id ->
            LocalContentIdentity(type = type, id = id)
        }
    }
}
