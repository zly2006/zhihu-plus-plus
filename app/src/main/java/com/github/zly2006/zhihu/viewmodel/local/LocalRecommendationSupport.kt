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

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.recommendation.LOCAL_CONTENT_TYPE_ANSWER
import com.github.zly2006.zhihu.shared.recommendation.LOCAL_CONTENT_TYPE_ARTICLE
import com.github.zly2006.zhihu.shared.recommendation.LOCAL_CONTENT_TYPE_PIN
import com.github.zly2006.zhihu.shared.recommendation.LOCAL_CONTENT_TYPE_QUESTION
import kotlin.math.ceil
import com.github.zly2006.zhihu.shared.recommendation.buildContentAffinity as buildSharedContentAffinity
import com.github.zly2006.zhihu.shared.recommendation.buildLocalRecommendationReason as buildSharedLocalRecommendationReason
import com.github.zly2006.zhihu.shared.recommendation.buildReasonPreference as buildSharedReasonPreference
import com.github.zly2006.zhihu.shared.recommendation.normalizeLocalContentId as normalizeSharedLocalContentId
import com.github.zly2006.zhihu.shared.recommendation.parseLocalContentIdentity as parseSharedLocalContentIdentity
import com.github.zly2006.zhihu.shared.recommendation.stableLocalFeedId as stableSharedLocalFeedId

typealias LocalContentIdentity = com.github.zly2006.zhihu.shared.recommendation.LocalContentIdentity
typealias LocalReasonStats = com.github.zly2006.zhihu.shared.recommendation.LocalReasonStats
typealias LocalContentStats = com.github.zly2006.zhihu.shared.recommendation.LocalContentStats
typealias LocalReasonPreference = com.github.zly2006.zhihu.shared.recommendation.LocalReasonPreference
typealias LocalContentAffinity = com.github.zly2006.zhihu.shared.recommendation.LocalContentAffinity

data class RankedLocalResult(
    val result: CrawlingResult,
    val finalScore: Double,
    val reasonDisplay: String,
)

fun normalizeLocalContentId(type: String, id: String): String = normalizeSharedLocalContentId(type, id)

fun parseLocalContentIdentity(
    contentId: String,
    url: String,
): LocalContentIdentity? = parseSharedLocalContentIdentity(contentId, url)

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

fun buildReasonPreference(stats: LocalReasonStats): LocalReasonPreference = buildSharedReasonPreference(stats)

fun buildContentAffinity(stats: LocalContentStats): LocalContentAffinity = buildSharedContentAffinity(stats)

fun buildLocalRecommendationReason(
    baseReason: String,
    reasonPreference: LocalReasonPreference?,
    contentAffinity: LocalContentAffinity?,
): String = buildSharedLocalRecommendationReason(
    baseReason = baseReason,
    reasonPreference = reasonPreference,
    contentAffinity = contentAffinity,
)

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

fun stableLocalFeedId(contentId: String): String = stableSharedLocalFeedId(contentId)
