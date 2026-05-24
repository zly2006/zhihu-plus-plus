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

import com.github.zly2006.zhihu.shared.recommendation.LocalReasonPreference
import com.github.zly2006.zhihu.shared.recommendation.buildLocalRecommendationReason
import com.github.zly2006.zhihu.shared.recommendation.parseLocalContentIdentity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock

data class RankedLocalResult(
    val result: CrawlingResult,
    val finalScore: Double,
    val reasonDisplay: String,
)

internal suspend fun collectCandidateResults(
    dao: LocalContentDao,
    limit: Int,
): List<CrawlingResult> = withContext(Dispatchers.IO) {
    CrawlingReason.entries
        .flatMap { reason ->
            dao.getResultsByReason(reason).take(limit * 2)
        }.mapNotNull { candidate ->
            val identity = parseLocalContentIdentity(candidate.contentId, candidate.url) ?: return@mapNotNull null
            if (candidate.contentId == identity.value) {
                candidate
            } else {
                candidate.copy(contentId = identity.value)
            }
        }.distinctBy { it.contentId }
}

internal suspend fun ensurePendingTasks(dao: LocalContentDao) {
    val tasks = mutableListOf<CrawlingTask>()

    CrawlingReason.entries.forEach { reason ->
        val pendingCount = dao.getTaskCountByReasonAndStatus(reason, CrawlingStatus.NotStarted)
        val inProgressCount = dao.getTaskCountByReasonAndStatus(reason, CrawlingStatus.InProgress)

        if (pendingCount + inProgressCount < 2) {
            repeat(3 - pendingCount - inProgressCount) {
                tasks.add(createTaskForReason(reason))
            }
        }
    }

    if (tasks.isNotEmpty()) {
        dao.insertTasks(tasks)
    }
}

internal fun rankCandidate(
    candidate: CrawlingResult,
    behaviorProfile: UserBehaviorAnalyzer.RecommendationBehaviorProfile,
): RankedLocalResult? {
    val identity = parseLocalContentIdentity(candidate.contentId, candidate.url) ?: return null
    val normalizedResult = if (candidate.contentId == identity.value) {
        candidate
    } else {
        candidate.copy(contentId = identity.value)
    }

    val reasonPreference = behaviorProfile.reasonPreferences[normalizedResult.reason] ?: LocalReasonPreference(1.0)
    val contentAffinity = behaviorProfile.contentAffinities[normalizedResult.contentId]
    val reasonWeight = getDefaultWeight(normalizedResult.reason) * reasonPreference.multiplier
    val contentWeight = contentAffinity?.multiplier ?: 1.0
    val freshnessWeight = getFreshnessWeight(normalizedResult.createdAt)
    val finalScore = normalizedResult.score * reasonWeight * contentWeight * freshnessWeight

    return RankedLocalResult(
        result = normalizedResult,
        finalScore = finalScore,
        reasonDisplay = buildLocalRecommendationReason(
            baseReason = getReasonDisplayText(normalizedResult.reason),
            reasonPreference = reasonPreference,
            contentAffinity = contentAffinity,
        ),
    )
}

internal fun getFreshnessWeight(
    createdAt: Long,
    nowMillis: Long = Clock.System.now().toEpochMilliseconds(),
): Double {
    val ageMillis = (nowMillis - createdAt).coerceAtLeast(0L)
    val ageHours = ageMillis / (60.0 * 60.0 * 1000.0)
    return when {
        ageHours < 12 -> 1.08
        ageHours < 48 -> 1.0
        ageHours < 168 -> 0.92
        else -> 0.82
    }
}

internal fun getDefaultWeight(reason: CrawlingReason): Double = when (reason) {
    CrawlingReason.Following -> 1.2
    CrawlingReason.Trending -> 1.0
    CrawlingReason.UpvotedQuestion -> 0.95
    CrawlingReason.FollowingUpvote -> 0.88
    CrawlingReason.CollaborativeFiltering -> 0.8
}

internal fun getReasonDisplayText(reason: CrawlingReason): String = when (reason) {
    CrawlingReason.Following -> "关注用户的最新动态"
    CrawlingReason.Trending -> "热门推荐"
    CrawlingReason.FollowingUpvote -> "关注用户点赞的内容"
    CrawlingReason.UpvotedQuestion -> "相关问题的优质回答"
    CrawlingReason.CollaborativeFiltering -> "相似用户喜欢的内容"
}

internal fun createTaskForReason(reason: CrawlingReason): CrawlingTask {
    val (url, priority) = when (reason) {
        CrawlingReason.Following -> "https://api.zhihu.com/moments_v3?feed_type=recommend" to 8
        CrawlingReason.Trending -> "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=20" to 7
        CrawlingReason.UpvotedQuestion -> "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=15" to 6
        CrawlingReason.FollowingUpvote -> "https://www.zhihu.com/api/v3/feed/topstory/recommend?action_feed=True&limit=15" to 5
        CrawlingReason.CollaborativeFiltering -> "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=10" to 4
    }

    return CrawlingTask(
        url = url,
        reason = reason,
        priority = priority,
    )
}
