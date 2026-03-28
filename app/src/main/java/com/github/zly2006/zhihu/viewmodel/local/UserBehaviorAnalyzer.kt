package com.github.zly2006.zhihu.viewmodel.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 用户行为分析器，用于分析用户行为并优化推荐
 */
class UserBehaviorAnalyzer(
    private val context: Context,
) {
    private val database by lazy { LocalContentDatabase.getDatabase(context) }
    private val dao by lazy { database.contentDao() }

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
        withContext(Dispatchers.IO) {
            val behavior = UserBehavior(
                contentId = contentId,
                action = action,
                timestamp = System.currentTimeMillis(),
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

    suspend fun buildBehaviorProfile(): RecommendationBehaviorProfile = withContext(Dispatchers.IO) {
        val recentBehaviors = dao.getBehaviorsSince(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L)

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

    /**
     * 获取用户偏好分析（简化版本）
     */
    suspend fun getUserPreferences(): Map<CrawlingReason, Double> = withContext(Dispatchers.IO) {
        buildBehaviorProfile().reasonPreferences.mapValues { (_, preference) -> preference.multiplier }
    }
}
