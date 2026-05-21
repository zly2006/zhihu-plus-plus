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

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.BuildConfig
import com.github.zly2006.zhihu.LoginActivity
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.AccountData.json
import com.github.zly2006.zhihu.shared.data.ZhihuPaging
import com.github.zly2006.zhihu.shared.util.HttpStatusException
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.util.signFetchRequest
import com.github.zly2006.zhihu.viewmodel.feed.OnlineHistoryViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
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

    protected open fun paginationEnvironment(context: Context): PaginationEnvironment =
        AndroidPaginationEnvironment(context)

    /**
     * Generally used fields to include in the API request.
     * This can be overridden in subclasses to include more specific fields.
     */
    open val include = "data[*].content,excerpt,headline,target.author.badge_v2"

    open fun refresh(context: Context) {
        currentJob?.cancel()
        currentJob = null
        isLoading = false
        errorMessage = null
        debugData.clear()
        allData.clear()
        lastPaging = null // 重置 lastPaging
        loadMore(context)
    }

    open fun httpClient(context: Context): HttpClient {
        // 检查是否启用推荐内容时登录设置
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val loginForRecommendation = preferences.getBoolean("loginForRecommendation", true)
        if (allowGuestAccess && !loginForRecommendation) {
            return HttpClient {
                install(HttpCache)
                install(ContentNegotiation) {
                    json(json)
                }
                install(UserAgent) {
                    agent = AccountData.data.userAgent
                }
            }
        }
        if (context is MainActivity) {
            return context.httpClient
        }
        return AccountData.httpClient(context)
    }

    protected open fun processResponse(context: Context, data: List<T>, rawData: JsonArray) {
        debugData.addAll(rawData) // 保存原始JSON
        allData.addAll(data) // 保存未flatten的数据
    }

    protected open suspend fun fetchFeeds(context: Context) {
        val environment = paginationEnvironment(context)
        try {
            val url = lastPaging?.next ?: initialUrl

            @Suppress("HttpUrlsUsage")
            val json = environment.fetchJson(url.replace("http://", "https://"), include)!!

            val jsonArray = json["data"]!!.jsonArray
            processResponse(
                context,
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
                        AccountData.decodeJson(serializer(dataType) as KSerializer<T>, it)
                    } catch (e: Exception) {
                        if (this !is OnlineHistoryViewModel) {
                            // Note: 小特判一下，懒得写了
                            environment.logDecodeFailure(this::class.simpleName, it, e)
                        }
                        null
                    }
                },
                jsonArray,
            )
            if ("paging" in json) {
                lastPaging = AccountData.decodeJson(json["paging"]!!)
            }
        } catch (e: Exception) {
            if (e is java.util.concurrent.CancellationException) throw e
            environment.handleFetchFailure(this::class.simpleName, e)
        } finally {
            isLoading = false
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    open fun loadMore(context: Context) {
        if (isLoading || isEnd) return // 使用新的isEnd getter
        isLoading = true
        currentJob = viewModelScope.launch {
            try {
                fetchFeeds(context)
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

    private class AndroidPaginationEnvironment(
        private val context: Context,
    ) : PaginationEnvironment {
        override suspend fun fetchJson(
            url: String,
            include: String,
        ): JsonObject? =
            fetchWithClient(context, url) {
                url {
                    if (include.isNotEmpty()) {
                        parameters["include"] = include
                    }
                }
                signFetchRequest()
            }

        override fun logDecodeFailure(
            tag: String?,
            item: JsonElement,
            error: Exception,
        ) {
            Log.e(tag, "Failed to decode item: $item", error)
        }

        override suspend fun handleFetchFailure(
            tag: String?,
            error: Exception,
        ) {
            if (error is HttpStatusException && BuildConfig.DEBUG) {
                Log.e(tag, "Response: ${error.bodyText}", error)
                if (tryShowLoginExpiredDialog(error)) {
                    return
                }
                showDebugErrorDialog(error)
            }
            Log.e(tag, "Failed to fetch feeds", error)
            context.mainExecutor.execute {
                Toast.makeText(context, "加载失败: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        private fun tryShowLoginExpiredDialog(error: HttpStatusException): Boolean {
            try {
                val jojo = json.parseToJsonElement(error.bodyText)
                if (jojo.jsonObject["error"]!!
                        .jsonObject["code"]!!
                        .jsonPrimitive.int == 100 &&
                    jojo.jsonObject["error"]!!
                        .jsonObject["message"]!!
                        .jsonPrimitive.content == "ERR_TICKET_NOT_EXIST"
                ) {
                    context.mainExecutor.execute {
                        if (context.canSafelyShowDialog()) {
                            AlertDialog
                                .Builder(context)
                                .setTitle("登录已过期")
                                .setMessage("请重新登录以继续使用完整功能。")
                                .setPositiveButton("重新登录") { _, _ ->
                                    AccountData.delete(context)
                                    context.startActivity(Intent(context, LoginActivity::class.java))
                                }.setNegativeButton("取消", null)
                                .show()
                        }
                    }
                    return true
                }
            } catch (_: Exception) {
            }
            return false
        }

        private fun showDebugErrorDialog(error: HttpStatusException) {
            context.mainExecutor.execute {
                if (context.canSafelyShowDialog()) {
                    AlertDialog
                        .Builder(context)
                        .setTitle("错误 ${error.status}")
                        .setMessage(error.bodyText)
                        .setNeutralButton("复制curl") { _, _ ->
                            val curl = error.dumpedCurlRequest
                            context.clipboardManager
                                .setPrimaryClip(
                                    ClipData.newPlainText(
                                        "curl",
                                        curl,
                                    ),
                                )
                            Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                        }.show()
                }
            }
        }
    }

    companion object {
        private suspend fun fetchWithClient(
            context: Context,
            url: String,
            block: suspend HttpRequestBuilder.() -> Unit,
        ): JsonObject? = AccountData.fetchGet(context, url) {
            block()
        }
    }
}

private fun Context.canSafelyShowDialog(): Boolean {
    val activity = this as? Activity ?: return false
    if (activity.isFinishing || activity.isDestroyed) return false
    val lifecycleOwner = activity as? LifecycleOwner ?: return true
    return lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
}
