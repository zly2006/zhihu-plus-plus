package com.github.zly2006.zhihu.viewmodel

import android.app.AlertDialog
import android.content.ClipData
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.github.zly2006.zhihu.BuildConfig
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.AccountData.json
import com.github.zly2006.zhihu.ui.HttpStatusException
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.util.signFetchRequest
import io.ktor.client.HttpClient
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
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
    protected var lastPaging: Paging? by mutableStateOf(null)
    open val isEnd: Boolean get() = lastPaging?.isEnd == true
    protected abstract val initialUrl: String

    /**
     * Generally used fields to include in the API request.
     * This can be overridden in subclasses to include more specific fields.
     */
    open val include = "data[*].content,excerpt,headline"

    @Serializable
    class Paging(
        val page: Int = -1,
        val isEnd: Boolean,
        val next: String,
        val prev: String? = null,
    )

    open fun refresh(context: Context) {
        if (isLoading) return
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
        try {
            val url = lastPaging?.next ?: initialUrl
            val json = AccountData.fetchGet(context, url) {
                url {
                    parameters.append("include", include)
                }
                signFetchRequest(context)
            }

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
                        Log.e(this::class.simpleName, "Failed to decode item: $it", e)
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
            if (e is HttpStatusException && BuildConfig.DEBUG) {
                Log.e(this::class.simpleName, "Response: ${e.bodyText}", e)
                context.mainExecutor.execute {
                    AlertDialog
                        .Builder(context)
                        .setTitle("错误 ${e.status}")
                        .setMessage(e.bodyText)
                        .setNeutralButton("复制curl") { _, _ ->
                            val curl = e.dumpedCurlRequest
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
            Log.e(this::class.simpleName, "Failed to fetch feeds", e)
            context.mainExecutor.execute {
                Toast.makeText(context, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } finally {
            isLoading = false
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    open fun loadMore(context: Context) {
        if (isLoading || isEnd) return // 使用新的isEnd getter
        isLoading = true
        GlobalScope.launch {
            try {
                fetchFeeds(context)
            } catch (e: Exception) {
                errorHandle(e)
            }
        }
    }

    protected fun errorHandle(e: Exception) {
        errorMessage = e.message
        isLoading = false
    }
}
