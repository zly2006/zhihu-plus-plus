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
import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.shared.filter.ContentFilterStats
import com.github.zly2006.zhihu.shared.filter.createContentFilterMaintenance
import com.github.zly2006.zhihu.viewmodel.ContentBlocklistEnvironment

typealias FilterStats = ContentFilterStats

/**
 * 内容曝光记录管理器。
 * 只负责维护“某个内容身份在 feed 中被看过几次、是否发生过交互”这类本地状态，
 * 真正的 feed 过滤编排由上层过滤流水线完成。
 */
class ContentFilterManager(
    private val dao: ContentFilterDao,
) {
    private val maintenance = createContentFilterMaintenance(dao)

    /** 记录某个内容身份在 feed 中曝光了一次。 */
    suspend fun recordContentView(targetType: String, targetId: String) {
        val recordId = ContentViewRecord.generateId(targetType, targetId)
        val existingRecord = dao.getViewRecord(recordId)

        if (existingRecord != null) {
            dao.incrementViewCount(recordId)
        } else {
            val newRecord = ContentViewRecord(
                id = recordId,
                targetType = targetType,
                targetId = targetId,
                viewCount = 1,
            )
            dao.insertOrUpdateViewRecord(newRecord)
        }
    }

    /** 记录某个内容身份在 feed 内发生过交互。 */
    suspend fun recordContentInteraction(targetType: String, targetId: String) {
        val recordId = ContentViewRecord.generateId(targetType, targetId)
        dao.markAsInteracted(recordId)
    }

    /** 批量查询这些内容身份是否已经出现在本地 feed 曝光记录里。 */
    suspend fun getAlreadyViewedContentIds(content: List<Pair<String, String>>): Set<String> {
        val idsToCheck = content.map { (targetType, targetId) ->
            ContentViewRecord.generateId(targetType, targetId)
        }
        return dao.getViewedContentIdsByIds(idsToCheck).toSet()
    }

    /** 清理过期曝光记录。 */
    suspend fun cleanupOldData() {
        maintenance.cleanupOldData()
    }

    /** 清除所有曝光记录（用于测试或重置）。 */
    suspend fun clearAllData() {
        maintenance.clearAllData()
    }
}

fun ContentBlocklistEnvironment.fetchBlockedUserIds(): Set<String> = blockedUserIds()

@Composable
expect fun rememberBlockedFeedRecordDao(): BlockedFeedRecordDao
