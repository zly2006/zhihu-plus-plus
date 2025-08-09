package com.github.zly2006.zhihu.viewmodel.filter

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 内容过滤管理器
 * 负责记录内容展示次数、用户交互，并提供过滤逻辑
 */
class ContentFilterManager private constructor(
    context: Context,
) {
    private val database = ContentFilterDatabase.getDatabase(context)
    private val dao = database.contentFilterDao()

    companion object {
        @Volatile
        private var INSTANCE: ContentFilterManager? = null

        // 数据清理配置
        private val CLEANUP_INTERVAL_DAYS = 7L // 清理7天前的数据
        private val MAX_RECORDS = 10000 // 最大记录数

        fun getInstance(context: Context): ContentFilterManager = INSTANCE ?: synchronized(this) {
            val instance = ContentFilterManager(context.applicationContext)
            INSTANCE = instance
            instance
        }
    }

    /**
     * 记录内容展示
     */
    suspend fun recordContentView(targetType: String, targetId: String) {
        withContext(Dispatchers.IO) {
            val recordId = ContentViewRecord.generateId(targetType, targetId)
            val existingRecord = dao.getViewRecord(recordId)

            if (existingRecord != null) {
                // 更新现有记录
                dao.incrementViewCount(recordId)
            } else {
                // 创建新记录
                val newRecord = ContentViewRecord(
                    id = recordId,
                    targetType = targetType,
                    targetId = targetId,
                    viewCount = 1,
                )
                dao.insertOrUpdateViewRecord(newRecord)
            }
        }
    }

    /**
     * 记录用户交互（点击、点赞等）
     */
    suspend fun recordContentInteraction(targetType: String, targetId: String) {
        withContext(Dispatchers.IO) {
            val recordId = ContentViewRecord.generateId(targetType, targetId)
            dao.markAsInteracted(recordId)
        }
    }

    /**
     * 检查内容是否应该被过滤
     */
    suspend fun shouldFilterContent(targetType: String, targetId: String): Boolean = withContext(Dispatchers.IO) {
        val recordId = ContentViewRecord.generateId(targetType, targetId)
        val record = dao.getViewRecord(recordId)

        record?.let {
            it.viewCount > ContentViewRecord.MAX_VIEW_COUNT_WITHOUT_INTERACTION && !it.hasInteraction
        } ?: false
    }

    /**
     * 批量检查内容是否应该被过滤
     */
    suspend fun filterContentList(content: List<Pair<String, String>>): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val filteredIds = dao.getFilteredContentIds().toSet()
        content.filter { (targetType, targetId) ->
            val recordId = ContentViewRecord.generateId(targetType, targetId)
            recordId !in filteredIds
        }
    }

    /**
     * 获取统计信息
     */
    suspend fun getFilterStats(): FilterStats = withContext(Dispatchers.IO) {
        val totalRecords = dao.getRecordCount()
        val filteredContent = dao.getFilteredContent()
        val filteredCount = filteredContent.size

        FilterStats(
            totalRecords = totalRecords,
            filteredCount = filteredCount,
            filterRate = if (totalRecords > 0) filteredCount.toFloat() / totalRecords else 0f,
        )
    }

    /**
     * 清理过期数据
     */
    suspend fun cleanupOldData() {
        withContext(Dispatchers.IO) {
            val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(CLEANUP_INTERVAL_DAYS)
            dao.cleanupOldRecords(cutoffTime)

            // 如果记录数量仍然过多，可以考虑进一步清理
            val recordCount = dao.getRecordCount()
            if (recordCount > MAX_RECORDS) {
                // 这里可以实现更激进的清理策略，比如只保留最近的记录
                // 暂时使用简单的全部清理
                dao.clearAllRecords()
            }
        }
    }

    /**
     * 清除所有数据（用于测试或重置）
     */
    suspend fun clearAllData() {
        withContext(Dispatchers.IO) {
            dao.clearAllRecords()
        }
    }

    /**
     * 重置特定内容的记录
     */
    suspend fun resetContentRecord(targetType: String, targetId: String) {
        withContext(Dispatchers.IO) {
            val recordId = ContentViewRecord.generateId(targetType, targetId)
            val record = ContentViewRecord(
                id = recordId,
                targetType = targetType,
                targetId = targetId,
                viewCount = 0,
                hasInteraction = false,
            )
            dao.insertOrUpdateViewRecord(record)
        }
    }
}

/**
 * 过滤统计信息
 */
data class FilterStats(
    val totalRecords: Int,
    val filteredCount: Int,
    val filterRate: Float,
)
