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

package com.github.zly2006.zhihu.viewmodel.za

import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.navDestination
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedInteractionViewModel
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class AndroidHomeFeedViewModel :
    BaseFeedViewModel(),
    HomeFeedInteractionViewModel {
    override val initialUrl: String
        get() = "https://api.zhihu.com/topstory/recommend"

    override fun httpClient(environment: PaginationEnvironment) = environment.mobileHomeFeedHttpClient()

    public override suspend fun fetchFeeds(environment: PaginationEnvironment) {
        try {
            val response = httpClient(environment).get(lastPaging?.next ?: initialUrl)
            if (response.status.isSuccess()) {
                val jojo = response.body<JsonObject>()
                val data = jojo["data"]?.jsonArray ?: throw IllegalStateException("No data found in response")

                // 收集所有待显示的项目
                val itemsToDisplay = mutableListOf<FeedDisplayItem>()

                data
                    .map { it.jsonObject }
                    .forEach { card ->
                        try {
                            val displayItem = parseMobileHomeFeedDisplayItem(card) ?: return@forEach
                            itemsToDisplay.add(displayItem)
                        } catch (e: Exception) {
                            environment.logDecodeFailure("AndroidHomeFeedViewModel", card, e)
                        }
                    }

                // 前台先做本地已读过滤，再立即展示
                val filterResult = environment.applyHomeFeedFilters(itemsToDisplay)
                if (!filterResult.reverseBlock) {
                    withContext(Dispatchers.Main) {
                        addDisplayItems(filterResult.foregroundItems)
                    }
                }

                // 后台继续运行其余内容过滤
                val newDestinations = filterResult.foregroundItems.map { it.navDestination }.toSet()

                if (filterResult.reverseBlock) {
                    addDisplayItems(filterResult.filteredItems)
                }

                // 移除被过滤的条目，并更新已保留条目的 raw 内容
                withContext(Dispatchers.Main) {
                    displayItems.removeAll { item ->
                        if (item.navDestination !in newDestinations) return@removeAll false
                        val filteredVersion = filterResult.filteredItems.find { it.navDestination == item.navDestination }
                        item.raw = filteredVersion?.raw ?: item.raw
                        // remove if no filtered version exists, which means it was filtered out
                        filteredVersion == null
                    }
                }

                lastPaging = if ("paging" in jojo) {
                    ZhihuJson.decodeJson(jojo["paging"]!!)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            if (e !is CancellationException) {
                environment.handleMobileHomeFeedFailure(e)
            }
            throw e
        } finally {
            isLoading = false
        }
    }

    override suspend fun recordContentInteraction(environment: PaginationEnvironment, feed: Feed) {
        // Android 版本暂不记录交互
    }

    override fun onUiContentClick(environment: PaginationEnvironment, feed: Feed, item: FeedDisplayItem) {
        viewModelScope.launch(Dispatchers.Default) {
            environment.sendFeedReadStatus(feed)
        }
    }
}
