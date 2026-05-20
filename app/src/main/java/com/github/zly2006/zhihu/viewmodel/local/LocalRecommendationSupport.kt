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
import com.github.zly2006.zhihu.shared.recommendation.applyReasonDiversity as applySharedReasonDiversity
import com.github.zly2006.zhihu.shared.recommendation.buildContentAffinity as buildSharedContentAffinity
import com.github.zly2006.zhihu.shared.recommendation.buildLocalRecommendationReason as buildSharedLocalRecommendationReason
import com.github.zly2006.zhihu.shared.recommendation.buildReasonPreference as buildSharedReasonPreference
import com.github.zly2006.zhihu.shared.recommendation.normalizeLocalContentId as normalizeSharedLocalContentId
import com.github.zly2006.zhihu.shared.recommendation.parseLocalContentIdentity as parseSharedLocalContentIdentity
import com.github.zly2006.zhihu.shared.recommendation.scoreFeedTarget as scoreSharedFeedTarget
import com.github.zly2006.zhihu.shared.recommendation.stableLocalFeedId as stableSharedLocalFeedId
import com.github.zly2006.zhihu.shared.recommendation.toLocalContentIdentity as toSharedLocalContentIdentity

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
        LOCAL_CONTENT_TYPE_ANSWER -> Article(type = ArticleType.Answer, id = numericId, title = title)
        LOCAL_CONTENT_TYPE_ARTICLE -> Article(type = ArticleType.Article, id = numericId, title = title)
        LOCAL_CONTENT_TYPE_QUESTION -> Question(questionId = numericId, title = title)
        LOCAL_CONTENT_TYPE_PIN -> Pin(id = numericId)
        else -> null
    }
}

fun Feed.Target.toLocalContentIdentity(): LocalContentIdentity = toSharedLocalContentIdentity()

fun scoreFeedTarget(target: Feed.Target): Double = scoreSharedFeedTarget(target)

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
): List<RankedLocalResult> = applySharedReasonDiversity(
    rankedResults = rankedResults,
    limit = limit,
    reasonOf = { it.result.reason },
)

fun stableLocalFeedId(contentId: String): String = stableSharedLocalFeedId(contentId)
