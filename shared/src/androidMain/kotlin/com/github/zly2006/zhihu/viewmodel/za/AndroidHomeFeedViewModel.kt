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

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.AccountData.json
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.navDestination
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.androidContext
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedInteractionViewModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.appendAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val ZHIHU_PP_ANDROID_HEADERS = createClientPlugin("ZhihuPPAndroidHeaders", { }) {
    onRequest { request, _ ->
        request.headers.appendAll(AccountData.ANDROID_HEADERS)
    }
}

class AndroidHomeFeedViewModel :
    BaseFeedViewModel(),
    HomeFeedInteractionViewModel {
    override val initialUrl: String
        get() = "https://api.zhihu.com/topstory/recommend"

    override fun httpClient(environment: PaginationEnvironment): HttpClient {
        val context = environment.androidContext()
        // 检查是否启用推荐内容时登录设置
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val loginForRecommendation = preferences.getBoolean("loginForRecommendation", true)

        return HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
            install(UserAgent) {
                agent = AccountData.ANDROID_USER_AGENT
            }
            install(ZHIHU_PP_ANDROID_HEADERS)
            if (loginForRecommendation) {
                install(HttpCookies) {
                    storage = AccountData.cookieStorage(context, null)
                }
            }
        }
    }

    public override suspend fun fetchFeeds(environment: PaginationEnvironment) {
        val context = environment.androidContext()
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
                            Log.e("AndroidHomeFeedViewModel", "Failed to process card: $card", e)
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
                    AccountData.decodeJson(jojo["paging"]!!)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            if (e !is CancellationException) {
                Log.e(this::class.simpleName, "Failed to fetch feeds", e)
                context.mainExecutor.execute {
                    Toast.makeText(context, "安卓端推荐加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
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
        viewModelScope.launch(Dispatchers.IO) {
            environment.sendFeedReadStatus(feed)
        }
    }
}
