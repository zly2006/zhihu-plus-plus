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

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.resolveContent
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.OnlineHistoryItem
import com.github.zly2006.zhihu.shared.data.ZhihuJson.decodeJson
import com.github.zly2006.zhihu.shared.data.toFeedDisplayItemNavDestinationJson
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import kotlinx.serialization.json.JsonArray

class OnlineHistoryViewModel : BaseFeedViewModel() {
    override val initialUrl: String = "https://api.zhihu.com/unify-consumption/read_history?offset=0&limit=10"
    override val shouldLogDecodeFailures: Boolean = false

    override fun processResponse(environment: PaginationEnvironment, data: List<Feed>, rawData: JsonArray) {
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
            displayItems.add(displayItem)
        }
    }
}
