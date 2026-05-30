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

package com.github.zly2006.zhihu.shared.filter
import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterDao
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

data class ContentFilterStats(
    val totalRecords: Int,
    val filteredCount: Int,
    val filterRate: Float,
)

data class ContentFilterMaintenance(
    val loadFilterStats: suspend () -> ContentFilterStats?,
    val cleanupOldData: suspend () -> ContentFilterStats?,
    val clearAllData: suspend () -> ContentFilterStats?,
)

@Composable
expect fun rememberContentFilterMaintenance(): ContentFilterMaintenance

private val cleanupInterval = 7.days
private const val MAX_CONTENT_FILTER_RECORDS = 10000

fun createContentFilterMaintenance(
    dao: ContentFilterDao,
): ContentFilterMaintenance {
    suspend fun loadStats(): ContentFilterStats {
        val totalRecords = dao.getRecordCount()
        val filteredCount = dao.getFilteredContent().size
        return ContentFilterStats(
            totalRecords = totalRecords,
            filteredCount = filteredCount,
            filterRate = if (totalRecords > 0) filteredCount.toFloat() / totalRecords else 0f,
        )
    }

    return ContentFilterMaintenance(
        loadFilterStats = { loadStats() },
        cleanupOldData = {
            val cutoffTime = Clock.System
                .now()
                .minus(cleanupInterval)
                .toEpochMilliseconds()
            dao.cleanupOldRecords(cutoffTime)
            if (dao.getRecordCount() > MAX_CONTENT_FILTER_RECORDS) {
                dao.clearAllRecords()
            }
            loadStats()
        },
        clearAllData = {
            dao.clearAllRecords()
            loadStats()
        },
    )
}

@Serializable
data class BlocklistBackup(
    val version: Int = 2,
    val exportTime: Long = Clock.System.now().toEpochMilliseconds(),
    val keywords: List<KeywordBackup> = emptyList(),
    val nlpKeywords: List<NlpKeywordBackup> = emptyList(),
    val users: List<UserBackup> = emptyList(),
    val topics: List<TopicBackup> = emptyList(),
)

@Serializable
data class KeywordBackup(
    val keyword: String,
    val caseSensitive: Boolean = false,
    val isRegex: Boolean = false,
)

@Serializable
data class NlpKeywordBackup(
    val keyword: String,
)

@Serializable
data class UserBackup(
    val userId: String,
    val userName: String,
    val urlToken: String = "",
    val avatarUrl: String = "",
)

@Serializable
data class TopicBackup(
    val topicId: String,
    val topicName: String,
)
