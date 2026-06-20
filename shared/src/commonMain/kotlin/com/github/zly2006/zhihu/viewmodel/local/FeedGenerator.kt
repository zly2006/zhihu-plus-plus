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

package com.github.zly2006.zhihu.viewmodel.local

import com.github.zly2006.zhihu.shared.recommendation.parseLocalContentIdentity
import com.github.zly2006.zhihu.shared.recommendation.stableLocalFeedId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * 推荐内容生成器，负责从爬虫结果生成LocalFeed
 */
class FeedGenerator(
    private val dao: LocalContentDao,
) {
    /**
     * 从爬虫结果生成推荐内容
     */
    suspend fun generateFeedFromResult(result: CrawlingResult, reasonDisplay: String): LocalFeed = withContext(Dispatchers.Default) {
        val stableId = stableLocalFeedId(result.contentId)
        val existingFeed = dao.getFeedById(stableId)
        val feed = LocalFeed(
            id = stableId,
            resultId = result.id,
            title = result.title,
            summary = result.summary,
            reasonDisplay = reasonDisplay,
            navDestination = parseLocalContentIdentity(result.contentId, result.url)?.value,
            userFeedback = existingFeed?.userFeedback ?: 0.0,
            createdAt = existingFeed?.createdAt ?: Clock.System.now().toEpochMilliseconds(),
        )

        dao.insertFeed(feed)
        feed
    }
}
