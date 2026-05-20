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

package com.github.zly2006.zhihu.viewmodel.filter

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 内容曝光记录管理器。
 * 只负责维护“某个内容身份在 feed 中被看过几次、是否发生过交互”这类本地状态，
 * 真正的 feed 过滤编排在 [ContentFilterExtensions] 中完成。
 */
class ContentFilterManager private constructor(
    context: Context,
) {
    private val dao = getContentFilterDatabase(context).contentFilterDao()

    companion object {
        @Volatile
        @Suppress("ktlint")
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

    /** 记录某个内容身份在 feed 中曝光了一次。 */
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

    /** 记录某个内容身份在 feed 内发生过交互。 */
    suspend fun recordContentInteraction(targetType: String, targetId: String) {
        withContext(Dispatchers.IO) {
            val recordId = ContentViewRecord.generateId(targetType, targetId)
            dao.markAsInteracted(recordId)
        }
    }

    /** 批量查询这些内容身份是否已经出现在本地 feed 曝光记录里。 */
    suspend fun getAlreadyViewedContentIds(content: List<Pair<String, String>>): Set<String> = withContext(Dispatchers.IO) {
        val idsToCheck = content.map { (targetType, targetId) ->
            ContentViewRecord.generateId(targetType, targetId)
        }
        dao.getViewedContentIdsByIds(idsToCheck).toSet()
    }

    /** 获取曝光记录统计。 */
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

    /** 清理过期曝光记录。 */
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

    /** 清除所有曝光记录（用于测试或重置）。 */
    suspend fun clearAllData() {
        withContext(Dispatchers.IO) {
            dao.clearAllRecords()
        }
    }

    /** 重置某个内容身份的曝光记录。 */
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
