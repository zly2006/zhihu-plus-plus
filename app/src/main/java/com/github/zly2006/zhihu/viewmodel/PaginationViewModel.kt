package com.github.zly2006.zhihu.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.signFetchRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import kotlin.reflect.KType

abstract class PaginationViewModel<T : Any>(
    val dataType: KType
) : ViewModel() {
    val allData = mutableStateListOf<T>()
    val debugData: MutableList<JsonElement> = mutableListOf()
    var isLoading: Boolean by mutableStateOf(false)
        protected set
    var errorMessage: String? = null
        protected set
    protected var lastPaging: Paging? by mutableStateOf(null)
    open val isEnd: Boolean get() = lastPaging?.is_end == true
    protected abstract val initialUrl: String

    @Suppress("PropertyName")
    @Serializable
    class Paging(
        val page: Int = -1,
        val is_end: Boolean,
        val next: String,
    )

    open fun refresh(context: Context) {
        if (isLoading) return
        errorMessage = null
        debugData.clear()
        allData.clear()
        lastPaging = null  // 重置 lastPaging
        loadMore(context)
    }

    fun httpClient(context: Context): HttpClient {
        if (context is MainActivity) {
            return context.httpClient
        }
        return AccountData.httpClient(context)
    }

    protected open fun processResponse(data: List<T>, rawData: JsonArray) {
        debugData.addAll(rawData) // 保存原始JSON
        allData.addAll(data) // 保存未flatten的数据
    }

    protected open suspend fun fetchFeeds(context: Context) {
        try {
            val url = lastPaging?.next ?: initialUrl
            val httpClient = httpClient(context)

            val response = httpClient.get(url) {
                signFetchRequest(context)
            }

            if (response.status == HttpStatusCode.Companion.OK) {
                val json = response.body<JsonObject>()
                val jsonArray = json["data"]!!.jsonArray
                processResponse(
                    jsonArray.mapNotNull {
                        if ("type" in it.jsonObject && it.jsonObject["type"]?.jsonPrimitive?.content in listOf(
                                "invited_answer", // invalid
                                "tab_list", // invalid
                                "feed_item_index_group" // todo
                        )) {
                            return@mapNotNull null
                        }
                        try {
                            @Suppress("UNCHECKED_CAST")
                            AccountData.json.decodeFromJsonElement(
                                serializer(dataType) as KSerializer<T>,
                                it
                            )
                        } catch (e: Exception) {
                            Log.e(this::class.simpleName, "Failed to decode item: $it", e)
                            null
                        }
                    },
                    jsonArray
                )
                if ("paging" in json) {
                    lastPaging = AccountData.decodeJson(json["paging"]!!)
                }
            } else {
                context.mainExecutor.execute {
                    Toast.makeText(context, "获取数据失败: ${response.status}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(this::class.simpleName, "Failed to fetch feeds", e)
            context.mainExecutor.execute {
                Toast.makeText(context, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } finally {
            isLoading = false
        }
    }

    open fun loadMore(context: Context) {
        if (isLoading || isEnd) return  // 使用新的isEnd getter
        isLoading = true
        viewModelScope.launch {
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
