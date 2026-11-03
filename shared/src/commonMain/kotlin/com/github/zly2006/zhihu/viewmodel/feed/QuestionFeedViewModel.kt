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

package com.github.zly2006.zhihu.viewmodel.feed

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.data.navDestination
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.QuestionAnswerNavigator
import com.github.zly2006.zhihu.navigation.zhihuQuestionFeedsUrl
import com.github.zly2006.zhihu.util.formatRelativeTime
import com.github.zly2006.zhihu.viewmodel.FeedDisplayEnvironment
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import com.github.zly2006.zhihu.viewmodel.deleteSigned
import com.github.zly2006.zhihu.viewmodel.postSigned

open class QuestionFeedViewModel(
    private val questionId: Long,
) : BaseFeedViewModel() {
    var sortOrder by mutableStateOf("default")
        private set

    /**
     * 已加载的问题详情，随导航条目存活。
     *
     * 问题页头部的高度由详情和话题决定；返回时若详情尚未恢复，头部会先变矮再长回去，而列表
     * 恢复的滚动位置只记录“顶部那一项 + 项内偏移”，偏移量一旦按变矮的头部计算就会落到后面的
     * 回答上。保留详情可让返回后的头部立即保持离开前的高度。
     */
    var questionDetail by mutableStateOf<DataHolder.Question?>(null)
        internal set

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
                // 列表里带上回答时间，便于快速分辨回答的早晚；老 API 或广告可能没有时间。
                details = if (target.createdTime > 0) {
                    "${target.detailsText} · ${formatRelativeTime(target.createdTime)}"
                } else {
                    target.detailsText
                },
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
