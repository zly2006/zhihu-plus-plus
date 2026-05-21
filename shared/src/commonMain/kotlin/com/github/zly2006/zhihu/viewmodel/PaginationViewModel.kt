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

package com.github.zly2006.zhihu.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.ZhihuPaging
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import kotlin.reflect.KType

abstract class PaginationViewModel<T : Any>(
    val dataType: KType,
) : ViewModel() {
    val allData = mutableStateListOf<T>()
    val debugData: MutableList<JsonElement> = mutableListOf()
    var isLoading: Boolean by mutableStateOf(false)
        protected set
    var errorMessage: String? = null
        protected set
    var allowGuestAccess = false
    protected var lastPaging: ZhihuPaging? by mutableStateOf(null)
    open val isEnd: Boolean get() = lastPaging?.isEnd == true
    protected abstract val initialUrl: String
    private var currentJob: Job? = null
    protected open val shouldLogDecodeFailures: Boolean = true

    /**
     * Generally used fields to include in the API request.
     * This can be overridden in subclasses to include more specific fields.
     */
    open val include = "data[*].content,excerpt,headline,target.author.badge_v2"

    open fun refresh(environment: PaginationEnvironment) {
        currentJob?.cancel()
        currentJob = null
        isLoading = false
        errorMessage = null
        debugData.clear()
        allData.clear()
        lastPaging = null // 重置 lastPaging
        loadMore(environment)
    }

    open fun httpClient(environment: PaginationEnvironment): HttpClient = environment.httpClient()

    protected open fun processResponse(environment: PaginationEnvironment, data: List<T>, rawData: JsonArray) {
        debugData.addAll(rawData) // 保存原始JSON
        allData.addAll(data) // 保存未flatten的数据
    }

    protected open suspend fun fetchFeeds(environment: PaginationEnvironment) {
        try {
            val url = lastPaging?.next ?: initialUrl

            @Suppress("HttpUrlsUsage")
            val json = environment.fetchJson(url.replace("http://", "https://"), include)!!

            val jsonArray = json["data"]!!.jsonArray
            processResponse(
                environment,
                jsonArray.mapNotNull {
                    if ("type" in it.jsonObject &&
                        it.jsonObject["type"]?.jsonPrimitive?.content in listOf(
                            "invited_answer", // invalid
                            "tab_list", // invalid
                            "feed_item_index_group", // todo
                        )
                    ) {
                        return@mapNotNull null
                    }
                    try {
                        @Suppress("UNCHECKED_CAST")
                        ZhihuJson.decodeJson(serializer(dataType) as KSerializer<T>, it)
                    } catch (e: Exception) {
                        if (shouldLogDecodeFailures) {
                            environment.logDecodeFailure(this::class.simpleName, it, e)
                        }
                        null
                    }
                },
                jsonArray,
            )
            if ("paging" in json) {
                lastPaging = ZhihuJson.decodeJson(json["paging"]!!)
            }
        } catch (e: Exception) {
            if (e is java.util.concurrent.CancellationException) throw e
            environment.handleFetchFailure(this::class.simpleName, e)
        } finally {
            isLoading = false
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    open fun loadMore(environment: PaginationEnvironment) {
        if (isLoading || isEnd) return // 使用新的isEnd getter
        isLoading = true
        currentJob = viewModelScope.launch {
            try {
                fetchFeeds(environment)
            } catch (e: Exception) {
                errorHandle(e)
            }
        }
    }

    protected fun errorHandle(e: Exception) {
        if (e !is CancellationException) {
            errorMessage = e.message
            isLoading = false
        }
    }
}
