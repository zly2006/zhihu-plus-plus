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

    /**
     * 获取用户偏好分析（简化版本）
     */
    suspend fun getUserPreferences(): Map<CrawlingReason, Double> = withContext(Dispatchers.IO) {
        val recentBehaviors = dao.getBehaviorsByActionSince(
            "like",
            System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L,
        )

        val preferenceWeights = mutableMapOf<CrawlingReason, Double>()

        // 基于用户行为调整推荐权重
        recentBehaviors.forEach { behavior ->
            // 简化的偏好分析，实际可以更复杂
            when (behavior.action) {
                "like" -> {
                    CrawlingReason.entries.forEach { reason ->
                        preferenceWeights[reason] = (preferenceWeights[reason] ?: 0.0) + 0.1
                    }
                }
                "share" -> {
                    CrawlingReason.entries.forEach { reason ->
                        preferenceWeights[reason] = (preferenceWeights[reason] ?: 0.0) + 0.2
                    }
                }
            }
        }

        preferenceWeights
    }
}
