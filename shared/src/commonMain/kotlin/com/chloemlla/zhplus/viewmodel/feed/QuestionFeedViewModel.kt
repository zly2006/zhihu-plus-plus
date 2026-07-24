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

package com.chloemlla.zhplus.viewmodel.feed

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.chloemlla.zhplus.navigation.Article
import com.chloemlla.zhplus.navigation.ArticleType
import com.chloemlla.zhplus.navigation.QuestionAnswerNavigator
import com.chloemlla.zhplus.navigation.zhihuQuestionFeedsUrl
import com.chloemlla.zhplus.shared.data.Feed
import com.chloemlla.zhplus.shared.data.FeedDisplayItem
import com.chloemlla.zhplus.shared.data.navDestination
import com.chloemlla.zhplus.shared.data.target
import com.chloemlla.zhplus.viewmodel.FeedDisplayEnvironment
import com.chloemlla.zhplus.viewmodel.PaginationEnvironment
import com.chloemlla.zhplus.viewmodel.ZhihuApiEnvironment
import com.chloemlla.zhplus.viewmodel.deleteSigned
import com.chloemlla.zhplus.viewmodel.postSigned

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

    fun createAnswerNavigatorFor(
        item: FeedDisplayItem,
        environment: ZhihuApiEnvironment,
    ): QuestionAnswerNavigator? {
        val destination = item.navDestination as? Article ?: return null
        if (destination.type != ArticleType.Answer) return null
        val index = displayItems.indexOfFirst { it.stableKey == item.stableKey }
        if (index < 0) return null
        return QuestionAnswerNavigator(
            questionId = questionId,
            initialNextAnswers = displayItems
                .drop(index + 1)
                .mapNotNull { it.navDestination as? Article },
            initialPreviousAnswers = displayItems
                .take(index)
                .asReversed()
                .mapNotNull { it.navDestination as? Article },
            initialNextUrl = lastPaging?.next.orEmpty(),
            order = sortOrder,
            environment = environment,
        )
    }

    suspend fun followQuestion(environment: ZhihuApiEnvironment, follow: Boolean) {
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
        val blockedUserIds = environment.blockedUserIds()
        val filtered = if (blockedUserIds.isEmpty()) {
            data
        } else {
            data.filterNot { feed ->
                val target = feed.target
                target is Feed.AnswerTarget && target.author?.id in blockedUserIds
            }
        }
        super.processResponse(environment, filtered, rawData)
    }
}
