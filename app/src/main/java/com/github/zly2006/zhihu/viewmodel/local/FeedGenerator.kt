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

package com.github.zly2006.zhihu.viewmodel.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 推荐内容生成器，负责从爬虫结果生成LocalFeed
 */
class FeedGenerator(
    private val context: Context,
) {
    private val database by lazy { LocalContentDatabase.getDatabase(context) }
    private val dao by lazy { database.contentDao() }

    /**
     * 从爬虫结果生成推荐内容
     */
    suspend fun generateFeedFromResult(result: CrawlingResult, reasonDisplay: String): LocalFeed = withContext(Dispatchers.IO) {
        val stableId = generateFeedId(result)
        val existingFeed = dao.getFeedById(stableId)
        val feed = LocalFeed(
            id = stableId,
            resultId = result.id,
            title = result.title,
            summary = result.summary,
            reasonDisplay = reasonDisplay,
            navDestination = generateNavDestination(result),
            userFeedback = existingFeed?.userFeedback ?: 0.0,
            createdAt = existingFeed?.createdAt ?: System.currentTimeMillis(),
        )

        dao.insertFeed(feed)
        feed
    }

    /**
     * 批量生成推荐内容
     */
    suspend fun generateFeedsFromResults(
        results: List<CrawlingResult>,
        reasonDisplay: String,
    ): List<LocalFeed> = withContext(Dispatchers.IO) {
        results.map { result ->
            generateFeedFromResult(result, reasonDisplay)
        }
    }

    private fun generateFeedId(result: CrawlingResult): String = stableLocalFeedId(result.contentId)

    private fun generateNavDestination(result: CrawlingResult): String? = parseLocalContentIdentity(result.contentId, result.url)?.value

    /**
     * 根据推荐原因获取理由显示文本
     */
    fun getReasonDisplayText(reason: CrawlingReason): String = when (reason) {
        CrawlingReason.Following -> "关注用户的最新动态"
        CrawlingReason.Trending -> "热门推荐"
        CrawlingReason.FollowingUpvote -> "关注用户点赞的内容"
        CrawlingReason.UpvotedQuestion -> "相关问题的优质回答"
        CrawlingReason.CollaborativeFiltering -> "相似用户喜欢的内容"
    }
}
