/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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

package com.github.zly2006.zhihu.filter

import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterDao
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

data class ContentFilterStats(
    val totalRecords: Int,
    val filteredCount: Int,
    val filterRate: Float,
)

private val cleanupInterval = 7.days
private const val MAX_CONTENT_FILTER_RECORDS = 10000

suspend fun ContentFilterDao.loadFilterStats(): ContentFilterStats {
    val totalRecords = getRecordCount()
    val filteredCount = getFilteredContent().size
    return ContentFilterStats(
        totalRecords = totalRecords,
        filteredCount = filteredCount,
        filterRate = if (totalRecords > 0) filteredCount.toFloat() / totalRecords else 0f,
    )
}

suspend fun ContentFilterDao.cleanupOldData(): ContentFilterStats {
    cleanupOldRecords(
        Clock.System
            .now()
            .minus(cleanupInterval)
            .toEpochMilliseconds(),
    )
    if (getRecordCount() > MAX_CONTENT_FILTER_RECORDS) {
        clearAllRecords()
    }
    return loadFilterStats()
}

suspend fun ContentFilterDao.clearAllData(): ContentFilterStats {
    clearAllRecords()
    return loadFilterStats()
}
