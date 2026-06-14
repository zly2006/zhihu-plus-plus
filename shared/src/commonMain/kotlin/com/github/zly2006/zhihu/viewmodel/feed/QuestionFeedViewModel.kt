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

package com.github.zly2006.zhihu.viewmodel.feed

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.zly2006.zhihu.navigation.zhihuQuestionFeedsUrl
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.target
import com.github.zly2006.zhihu.viewmodel.ContentBlocklistEnvironment
import com.github.zly2006.zhihu.viewmodel.FeedDisplayEnvironment
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import com.github.zly2006.zhihu.viewmodel.deleteSigned
import com.github.zly2006.zhihu.viewmodel.filter.fetchBlockedUserIds
import com.github.zly2006.zhihu.viewmodel.postSigned

open class QuestionFeedViewModel(
    private val questionId: Long,
) : BaseFeedViewModel() {
    var sortOrder by mutableStateOf("default")
        private set

    override val initialUrl: String
        get() = zhihuQuestionFeedsUrl(questionId, limit = 20, order = sortOrder)

    fun updateSortOrder(order: String) {
        if (sortOrder != order) {
            sortOrder = order
        }
    }

    override fun createDisplayItem(environment: FeedDisplayEnvironment, feed: Feed): FeedDisplayItem {
        val target = feed.target
        if (target is Feed.AnswerTarget) {
            return FeedDisplayItem(
                authorName = target.author?.name ?: "未知作者",
                avatarSrc = target.author?.avatarUrl,
                summary = target.excerpt,
                details = target.detailsText,
                feed = feed,
                title = "",
            )
        }
        return super.createDisplayItem(environment, feed)
    }

    suspend fun followQuestion(environment: ZhihuApiEnvironment, questionId: Long, follow: Boolean) {
        try {
            if (environment.authenticatedCookies()["d_c0"] == null) return
            val url = "https://www.zhihu.com/api/v4/questions/$questionId/followers"
            if (follow) {
                environment.postSigned(url)
            } else {
                environment.deleteSigned(url)
            }
        } catch (e: Exception) {
            environment.handleFetchFailure("QuestionFeedViewModel", e)
        }
    }

    override fun processResponse(environment: PaginationEnvironment, data: List<Feed>, rawData: kotlinx.serialization.json.JsonArray) {
        val filtered = filterBlockedAnswers(environment, data)
        super.processResponse(environment, filtered, rawData)
    }

    private fun filterBlockedAnswers(environment: ContentBlocklistEnvironment, data: List<Feed>): List<Feed> {
        val blockedUserIds = environment.fetchBlockedUserIds()
        if (blockedUserIds.isEmpty()) return data
        return data.filterNot { feed ->
            val target = feed.target
            target is Feed.AnswerTarget && target.author?.id in blockedUserIds
        }
    }
}
