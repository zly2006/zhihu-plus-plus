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

package com.github.zly2006.zhihu.viewmodel.local

import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import kotlin.math.ceil

data class LocalContentIdentity(
    val type: String,
    val id: String,
) {
    val value: String
        get() = "$type:$id"

    fun toNavDestination(title: String): NavDestination? {
        val numericId = id.toLongOrNull() ?: return null
        return when (type) {
            CONTENT_TYPE_ANSWER -> Article(type = ArticleType.Answer, id = numericId, title = title)
            CONTENT_TYPE_ARTICLE -> Article(type = ArticleType.Article, id = numericId, title = title)
            CONTENT_TYPE_QUESTION -> Question(questionId = numericId, title = title)
            CONTENT_TYPE_PIN -> Pin(id = numericId)
            else -> null
        }
    }
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

data class RankedLocalResult(
    val result: CrawlingResult,
    val finalScore: Double,
    val reasonDisplay: String,
)

private const val CONTENT_TYPE_ANSWER = "answer"
private const val CONTENT_TYPE_ARTICLE = "article"
private const val CONTENT_TYPE_QUESTION = "question"
private const val CONTENT_TYPE_PIN = "pin"

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

fun Feed.Target.toLocalContentIdentity(): LocalContentIdentity = when (this) {
    is Feed.AnswerTarget -> LocalContentIdentity(CONTENT_TYPE_ANSWER, id.toString())
    is Feed.ArticleTarget -> LocalContentIdentity(CONTENT_TYPE_ARTICLE, id.toString())
    is Feed.QuestionTarget -> LocalContentIdentity(CONTENT_TYPE_QUESTION, id.toString())
    is Feed.PinTarget -> LocalContentIdentity(CONTENT_TYPE_PIN, id.toString())
    is Feed.VideoTarget -> LocalContentIdentity("video", id.toString())
}

fun scoreFeedTarget(target: Feed.Target): Double {
    var score = 1.0

    when (target) {
        is Feed.AnswerTarget -> {
            score += (target.voteupCount / 100.0).coerceAtMost(5.0)
            score += (target.commentCount / 50.0).coerceAtMost(2.0)
        }

        is Feed.ArticleTarget -> {
            score += (target.voteupCount / 100.0).coerceAtMost(5.0)
            score += (target.commentCount / 50.0).coerceAtMost(2.0)
        }

        is Feed.VideoTarget -> {
            score += (target.voteCount / 100.0).coerceAtMost(5.0)
            score += (target.commentCount / 50.0).coerceAtMost(2.0)
        }

        is Feed.PinTarget -> {
            score += (target.favoriteCount / 50.0).coerceAtMost(3.0)
            score += (target.commentCount / 20.0).coerceAtMost(1.0)
        }

        is Feed.QuestionTarget -> {
            score += (target.answerCount / 10.0).coerceAtMost(2.0)
            score += (target.followerCount / 100.0).coerceAtMost(3.0)
            score += (target.commentCount / 20.0).coerceAtMost(1.0)
        }
    }

    val contentLength = target.excerpt?.length ?: 0
    score += when {
        contentLength in 100..500 -> 1.0
        contentLength in 50..99 || contentLength in 501..1000 -> 0.5
        else -> 0.0
    }

    return score.coerceIn(0.1, 10.0)
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

fun applyReasonDiversity(
    rankedResults: List<RankedLocalResult>,
    limit: Int,
): List<RankedLocalResult> {
    if (limit <= 0 || rankedResults.isEmpty()) {
        return emptyList()
    }
    if (rankedResults.size <= limit) {
        return rankedResults
    }

    val selected = mutableListOf<RankedLocalResult>()
    val reasonCounts = mutableMapOf<CrawlingReason, Int>()
    val maxPerReason = ceil(limit * 0.5).toInt().coerceAtLeast(1)
    val diversityTarget = rankedResults
        .map { it.result.reason }
        .distinct()
        .size
        .coerceAtMost(limit)
        .coerceAtMost(3)

    rankedResults.forEach { ranked ->
        if (selected.size >= diversityTarget) return@forEach
        if (selected.none { it.result.reason == ranked.result.reason }) {
            selected.add(ranked)
            reasonCounts[ranked.result.reason] = 1
        }
    }

    rankedResults.forEach { ranked ->
        if (selected.size >= limit || ranked in selected) return@forEach
        val currentCount = reasonCounts[ranked.result.reason] ?: 0
        if (currentCount < maxPerReason) {
            selected.add(ranked)
            reasonCounts[ranked.result.reason] = currentCount + 1
        }
    }

    if (selected.size < limit) {
        rankedResults.forEach { ranked ->
            if (selected.size >= limit || ranked in selected) return@forEach
            selected.add(ranked)
        }
    }

    return selected.take(limit)
}

fun stableLocalFeedId(contentId: String): String = "local_feed_${contentId.replace(':', '_')}"

private fun inferIdentityFromUrl(url: String): LocalContentIdentity? {
    val patterns = listOf(
        CONTENT_TYPE_ANSWER to Regex("""/(?:answer|answers)/(\d+)"""),
        CONTENT_TYPE_ARTICLE to Regex("""/(?:articles|p)/(\d+)"""),
        CONTENT_TYPE_QUESTION to Regex("""/(?:question|questions)/(\d+)"""),
        CONTENT_TYPE_PIN to Regex("""/(?:pin|pins)/(\d+)"""),
    )

    return patterns.firstNotNullOfOrNull { (type, regex) ->
        regex.find(url)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }?.let { id ->
            LocalContentIdentity(type = type, id = id)
        }
    }
}
