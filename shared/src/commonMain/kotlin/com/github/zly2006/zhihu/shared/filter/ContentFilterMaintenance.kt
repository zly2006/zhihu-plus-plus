package com.github.zly2006.zhihu.shared.filter

import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterDao
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
