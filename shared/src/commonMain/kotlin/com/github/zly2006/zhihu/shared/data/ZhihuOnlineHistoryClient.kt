package com.github.zly2006.zhihu.shared.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

const val ZHIHU_ONLINE_HISTORY_URL = "https://api.zhihu.com/unify-consumption/read_history"
const val ZHIHU_CLEAR_ONLINE_HISTORY_URL = "https://api.zhihu.com/read_history/batch_del"

@Serializable
data class OnlineHistoryPage(
    val data: List<OnlineHistoryItem>,
    val paging: ZhihuPaging? = null,
)

fun zhihuOnlineHistoryUrl(
    offset: Int = 0,
    limit: Int = 10,
): String = URLBuilder("https://api.zhihu.com")
    .apply {
        appendPathSegments("unify-consumption", "read_history")
        parameters.append("offset", offset.toString())
        parameters.append("limit", limit.toString())
    }.buildString()

fun buildZhihuClearOnlineHistoryBody(): JsonObject = buildJsonObject {
    put("pairs", JsonArray(emptyList()))
    put("clear", true)
}

fun encodeZhihuClearOnlineHistoryBody(): String =
    ZhihuJson.json.encodeToString(JsonObject.serializer(), buildZhihuClearOnlineHistoryBody())

suspend fun fetchOnlineHistoryPage(
    client: HttpClient,
    url: String = zhihuOnlineHistoryUrl(),
): OnlineHistoryPage = decodeOnlineHistoryPage(client.get(url).body())

fun decodeOnlineHistoryPage(response: JsonObject): OnlineHistoryPage {
    val data = response["data"]
        ?.jsonArray
        ?.let(::decodeOnlineHistoryItems)
        .orEmpty()
    val paging = response["paging"]?.let { ZhihuJson.decodeJson<ZhihuPaging>(it.jsonObject) }
    return OnlineHistoryPage(data = data, paging = paging)
}

fun decodeOnlineHistoryItems(
    data: JsonArray,
    ignoreInvalid: Boolean = false,
): List<OnlineHistoryItem> = data.mapNotNull { item ->
    runCatching {
        ZhihuJson.decodeJson<OnlineHistoryItem>(item)
    }.getOrElse { error ->
        if (ignoreInvalid) {
            null
        } else {
            throw error
        }
    }
}
