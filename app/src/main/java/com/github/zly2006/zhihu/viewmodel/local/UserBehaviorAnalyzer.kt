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
     * 优化：使用更高效的计算方式
     */
    suspend fun getUserPreferences(): Map<CrawlingReason, Double> = withContext(Dispatchers.IO) {
        val recentBehaviors = dao.getBehaviorsByActionSince(
            "like",
            System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L,
        )

        // 优化：使用 groupingBy 和 fold 来减少重复遍历
        val actionWeights = mapOf(
            "like" to 0.1,
            "share" to 0.2
        )
        
        // 计算每个原因的总权重
        return@withContext recentBehaviors
            .groupingBy { it.action }
            .eachCount()
            .entries
            .fold(mutableMapOf<CrawlingReason, Double>()) { acc, (action, count) ->
                val weight = actionWeights[action] ?: 0.0
                val totalWeight = weight * count
                CrawlingReason.entries.forEach { reason ->
                    acc[reason] = (acc[reason] ?: 0.0) + totalWeight
                }
                acc
            }
    }
}
