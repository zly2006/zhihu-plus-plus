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

import com.github.zly2006.zhihu.shared.recommendation.LocalContentAffinity
import com.github.zly2006.zhihu.shared.recommendation.LocalContentStats
import com.github.zly2006.zhihu.shared.recommendation.LocalReasonPreference
import com.github.zly2006.zhihu.shared.recommendation.LocalReasonStats
import com.github.zly2006.zhihu.shared.recommendation.buildContentAffinity
import com.github.zly2006.zhihu.shared.recommendation.buildReasonPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * 用户行为分析器，用于分析用户行为并优化推荐
 */
class UserBehaviorAnalyzer(
    private val dao: LocalContentDao,
) {
    data class RecommendationBehaviorProfile(
        val reasonPreferences: Map<CrawlingReason, LocalReasonPreference>,
        val contentAffinities: Map<String, LocalContentAffinity>,
    )

    /**
     * 记录用户行为
     */
    suspend fun recordBehavior(
        contentId: String,
        action: String,
        duration: Long? = null,
    ) {
        withContext(Dispatchers.Default) {
            val behavior = UserBehavior(
                contentId = contentId,
                action = action,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                duration = duration,
            )
            dao.insertBehavior(behavior)
        }
    }

    suspend fun recordContentOpened(contentId: String, reason: CrawlingReason) {
        recordBehavior(contentId, "click")
        recordBehavior(contentId, "click:${reason.name}")
    }

    suspend fun recordRecommendationFeedback(contentId: String, reason: CrawlingReason, feedback: Double) {
        val action = if (feedback >= 0.0) "like" else "dislike"
        recordBehavior(contentId, action)
        recordBehavior(contentId, "$action:${reason.name}")
    }

    suspend fun buildBehaviorProfile(): RecommendationBehaviorProfile = withContext(Dispatchers.Default) {
        val recentBehaviors = dao.getBehaviorsSince(
            Clock.System.now().toEpochMilliseconds() - 30 * 24 * 60 * 60 * 1000L,
        )

        val reasonStats = mutableMapOf<CrawlingReason, LocalReasonStats>()
        val contentStats = mutableMapOf<String, LocalContentStats>()

        recentBehaviors.forEach { behavior ->
            val baseAction = behavior.action.substringBefore(':')
            val reasonToken = behavior.action.substringAfter(':', "")

            if (reasonToken.isNotEmpty()) {
                val reason = runCatching { CrawlingReason.valueOf(reasonToken) }.getOrNull() ?: return@forEach
                val current = reasonStats[reason] ?: LocalReasonStats()
                reasonStats[reason] = when (baseAction) {
                    "click" -> current.copy(clicks = current.clicks + 1)
                    "like" -> current.copy(likes = current.likes + 1)
                    "dislike" -> current.copy(dislikes = current.dislikes + 1)
                    else -> current
                }
            } else {
                val current = contentStats[behavior.contentId] ?: LocalContentStats()
                contentStats[behavior.contentId] = when (baseAction) {
                    "click" -> current.copy(clicks = current.clicks + 1)
                    "like" -> current.copy(likes = current.likes + 1)
                    "dislike" -> current.copy(dislikes = current.dislikes + 1)
                    else -> current
                }
            }
        }

        RecommendationBehaviorProfile(
            reasonPreferences = CrawlingReason.entries.associateWith { reason ->
                buildReasonPreference(reasonStats[reason] ?: LocalReasonStats())
            },
            contentAffinities = contentStats.mapValues { (_, stats) ->
                buildContentAffinity(stats)
            },
        )
    }
}
