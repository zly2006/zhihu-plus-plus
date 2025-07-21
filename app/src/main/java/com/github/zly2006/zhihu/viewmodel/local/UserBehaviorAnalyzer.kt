package com.github.zly2006.zhihu.viewmodel.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 用户行为分析器，用于分析用户行为并优化推荐
 */
class UserBehaviorAnalyzer(private val context: Context) {
    private val database by lazy { LocalContentDatabase.getDatabase(context) }
    private val dao by lazy { database.contentDao() }

    /**
     * 记录用户行为
     */
    suspend fun recordBehavior(
        feedId: String,
        actionType: UserActionType,
        duration: Long = 0L
    ) {
        withContext(Dispatchers.IO) {
            val behavior = UserBehavior(
                id = "behavior_${System.currentTimeMillis()}_${feedId}",
                feedId = feedId,
                actionType = actionType,
                duration = duration,
                value = calculateBehaviorValue(actionType, duration)
            )
            dao.insertBehavior(behavior)

            // 更新LocalFeed的相关统计
            updateFeedStats(feedId, actionType)
        }
    }

    /**
     * 计算行为权重值
     */
    private fun calculateBehaviorValue(actionType: UserActionType, duration: Long): Double {
        return when (actionType) {
            UserActionType.LONG_READ -> {
                // 根据阅读时长动态计算权重
                val minutes = duration / 60000.0
                when {
                    minutes > 5 -> 5.0  // 超过5分钟认为是深度阅读
                    minutes > 2 -> 3.0  // 2-5分钟是认真阅读
                    minutes > 0.5 -> 2.0 // 30秒-2分钟是一般阅读
                    else -> 1.0
                }
            }
            UserActionType.QUICK_SKIP -> {
                // 快速跳过的负权重
                if (duration < 3000) -1.0 else 0.1
            }
            else -> actionType.weight
        }
    }

    /**
     * 更新Feed统计信息
     */
    private suspend fun updateFeedStats(feedId: String, actionType: UserActionType) {
        val feed = dao.getFeedById(feedId) ?: return

        val updatedFeed = when (actionType) {
            UserActionType.VIEW -> feed.copy(
                viewCount = feed.viewCount + 1,
                lastViewed = System.currentTimeMillis()
            )
            UserActionType.BOOKMARK -> feed.copy(isBookmarked = true)
            else -> feed
        }

        dao.updateFeed(updatedFeed)
    }

    /**
     * 获取用户偏好分析（简化版本）
     */
    suspend fun getUserPreferences(): UserPreferences {
        return withContext(Dispatchers.IO) {
            try {
                val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
                val recentBehaviors = dao.getBehaviorsSince(thirtyDaysAgo)

                // 简化的偏好分析：直接按推荐原因统计
                val reasonWeights = mutableMapOf<String, Double>()
                val contentTypeWeights = mutableMapOf<String, Double>()

                recentBehaviors.forEach { behavior ->
                    val feed = dao.getFeedById(behavior.feedId)
                    feed?.let {
                        // 累计推荐原因权重
                        val reason = it.reasonDisplay ?: "未知"
                        reasonWeights[reason] = (reasonWeights[reason] ?: 0.0) + behavior.value

                        // 累计内容类型权重
                        val result = dao.getResultById(it.resultId)
                        result?.let { r ->
                            contentTypeWeights[r.contentType] = (contentTypeWeights[r.contentType] ?: 0.0) + behavior.value
                        }
                    }
                }

                UserPreferences(
                    reasonWeights = reasonWeights,
                    contentTypeWeights = contentTypeWeights
                )
            } catch (e: Exception) {
                // 返回默认偏好
                UserPreferences(
                    reasonWeights = emptyMap(),
                    contentTypeWeights = emptyMap()
                )
            }
        }
    }
}

/**
 * 用户偏好数据类
 */
data class UserPreferences(
    val reasonWeights: Map<String, Double>,
    val contentTypeWeights: Map<String, Double>
)
