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

import com.chloemlla.zhplus.navigation.Article
import com.chloemlla.zhplus.navigation.resolveContent
import com.chloemlla.zhplus.shared.data.Feed
import com.chloemlla.zhplus.shared.data.FeedDisplayItem
import com.chloemlla.zhplus.shared.data.OnlineHistoryDeletePair
import com.chloemlla.zhplus.shared.data.OnlineHistoryItem
import com.chloemlla.zhplus.shared.data.ZhihuJson.decodeJson
import com.chloemlla.zhplus.shared.data.toFeedDisplayItemNavDestinationJson
import com.chloemlla.zhplus.viewmodel.PaginationEnvironment
import com.chloemlla.zhplus.viewmodel.deleteOnlineHistoryItem
import kotlinx.serialization.json.JsonArray

class OnlineHistoryViewModel : BaseFeedViewModel() {
    override val initialUrl: String = "https://api.zhihu.com/unify-consumption/read_history?offset=0&limit=10"
    override val shouldLogDecodeFailures: Boolean = false
    private val deletionPairs = mutableMapOf<FeedDisplayItem, OnlineHistoryDeletePair>()

    override fun processResponse(environment: PaginationEnvironment, data: List<Feed>, rawData: JsonArray) {
        if (displayItems.isEmpty()) {
            deletionPairs.clear()
        }
        val response = rawData.mapNotNull { item ->
            runCatching { decodeJson<OnlineHistoryItem>(item) }.getOrNull()
        }
        val localHistory = environment.localHistory()

        response.forEach { item ->
            val navDest = try {
                resolveContent(item.data.action.url)
            } catch (e: Exception) {
                null
            }

            val detailsText = item.data.matrix
                ?.firstOrNull()
                ?.data
                ?.text ?: item.data.extra.contentType

            val matchedItem = localHistory.firstOrNull {
                it == navDest
            }
            val displayItem = FeedDisplayItem(
                title = item.data.header.title,
                summary = item.data.content?.summary ?: "",
                details = detailsText,
                feed = null,
                navDestinationJson = navDest?.toFeedDisplayItemNavDestinationJson(),
                avatarSrc = when (matchedItem) {
                    is Article -> matchedItem.avatarSrc
                    else -> null
                },
                authorName = item.data.content?.authorName,
            )
            deletionPairs[displayItem] = OnlineHistoryDeletePair(
                contentToken = item.data.extra.contentToken,
                contentType = item.data.extra.contentType,
            )
            displayItems.add(displayItem)
        }
    }

    suspend fun deleteItem(environment: PaginationEnvironment, item: FeedDisplayItem) {
        val pair = checkNotNull(deletionPairs[item]) { "在线历史记录缺少删除标识" }
        environment.deleteOnlineHistoryItem(pair)
        displayItems.remove(item)
        deletionPairs.remove(item)
    }
}
