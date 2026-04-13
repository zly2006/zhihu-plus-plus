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

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.ui.IHomeFeedViewModel
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.util.signFetchRequest
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterExtensions
import com.github.zly2006.zhihu.viewmodel.filter.ContentType
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

class HomeFeedViewModel :
    BaseFeedViewModel(),
    IHomeFeedViewModel {
    private val reportedTouchedItems = hashSetOf<Pair<String, String>>()

    override val initialUrl: String
//        get() = "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=10"
        get() = "https://api.zhihu.com/topstory/recommend"

    init {
        allowGuestAccess = true
    }

    public override suspend fun fetchFeeds(context: Context) {
        markItemsAsTouched(context)
        super.fetchFeeds(context)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun processResponse(context: Context, data: List<Feed>, rawData: JsonArray) {
        allData.addAll(data)
        debugData.addAll(rawData)

        viewModelScope.launch {
            val newItems = data
                .flatten()
                .map { feed -> createDisplayItem(context, feed) }

            val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            // 前台先做本地已读过滤，再立即展示
            val foregroundFilteredItems = ContentFilterExtensions.applyForegroundReadFilterToDisplayItems(context, newItems)
            if (!preferences.getBoolean("reverseBlock", false)) {
                withContext(Dispatchers.Main) {
                    addDisplayItems(foregroundFilteredItems)
                }
            }

            // 后台继续运行其余内容过滤
            val filteredItems = ContentFilterExtensions.applyContentFilterToDisplayItems(context, foregroundFilteredItems)
            val newDestinations = foregroundFilteredItems.map { it.navDestination }.toSet()

            if (preferences.getBoolean("reverseBlock", false)) {
                addDisplayItems(filteredItems)
            }

            // 移除被过滤的条目，并更新已保留条目的 raw 内容
            withContext(Dispatchers.Main) {
                displayItems.removeAll { item ->
                    if (item.navDestination !in newDestinations) return@removeAll false
                    val filteredVersion = filteredItems.find { it.navDestination == item.navDestination }
                    item.raw = filteredVersion?.raw ?: item.raw
                    // remove if no filtered version exists, which means it was filtered out
                    filteredVersion == null
                }
            }
        }
    }

    /**
     * 记录用户与内容的交互行为
     * 应该在用户点击、点赞等操作时调用
     */
    override suspend fun recordContentInteraction(context: Context, feed: Feed) {
        withContext(Dispatchers.IO) {
            try {
                when (val target = feed.target) {
                    is Feed.AnswerTarget -> {
                        ContentFilterExtensions.recordContentInteraction(
                            context,
                            ContentType.ANSWER,
                            target.id.toString(),
                        )
                    }
                    is Feed.ArticleTarget -> {
                        ContentFilterExtensions.recordContentInteraction(
                            context,
                            ContentType.ARTICLE,
                            target.id.toString(),
                        )
                    }
                    is Feed.QuestionTarget -> {
                        ContentFilterExtensions.recordContentInteraction(
                            context,
                            ContentType.QUESTION,
                            target.id.toString(),
                        )
                    }
                    else -> {
                        // 其他类型暂不处理
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeFeedViewModel", "Failed to record content interaction", e)
            }
        }
    }

    /**
     * 记录用户点击内容
     * 在viewModelScope中运行，使用viewModelScope代替GlobalScope
     */
    override fun onUiContentClick(context: Context, feed: Feed, item: BaseFeedViewModel.FeedDisplayItem) {
        viewModelScope.launch(Dispatchers.IO) {
            sendReadStatusToServer(context, feed)
            recordContentInteraction(context, feed)
        }
    }

    private suspend fun markItemsAsTouched(
        context: Context,
        httpClient: HttpClient = AccountData.httpClient(context),
    ) {
        try {
            val currentTouchItems = displayItems
                .asSequence()
                .filterNot { it.isFiltered }
                .mapNotNull { it.feed?.target }
                .mapNotNull { target ->
                    when (target) {
                        is Feed.AnswerTarget -> "answer" to target.id.toString()
                        is Feed.ArticleTarget -> "article" to target.id.toString()
                        is Feed.PinTarget -> "pin" to target.id.toString()
                        else -> null
                    }
                }.toList()
            val untouchedItemSet = currentTouchItems - reportedTouchedItems

            if (untouchedItemSet.isNotEmpty()) {
                val response = httpClient
                    .post("https://www.zhihu.com/lastread/touch") {
                        header("x-requested-with", "fetch")
                        signFetchRequest()
                        setBody(
                            MultiPartFormDataContent(
                                formData {
                                    val payload = untouchedItemSet.map { (type, id) ->
                                        listOf(type, id, "touch")
                                    }
                                    append(
                                        "items",
                                        Json.encodeToString(payload),
                                    )
                                },
                            ),
                        )
                    }
                if (response.status.isSuccess()) {
                    reportedTouchedItems.addAll(untouchedItemSet)
                } else {
                    Log.e("Browse-Touch", response.bodyAsText())
                }
            }
        } catch (e: Exception) {
            Log.e("FeedViewModel", "Failed to mark items as touched", e)
        }
    }

    override fun refresh(context: Context) {
        super.refresh(context)
        reportedTouchedItems.clear()
    }
}
