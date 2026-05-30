package com.github.zly2006.zhihu.shared.data

import com.github.zly2006.zhihu.shared.util.signZhihuFetchRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray

const val ZHIHU_HOT_LIST_TOTAL_URL = "https://www.zhihu.com/api/v3/feed/topstory/hot-lists/total"
const val ZHIHU_HOT_LIST_INCLUDE = "data[*].content,excerpt,headline,target.author.badge_v2"

fun zhihuHotListUrl(
    limit: Int = 50,
    mobile: Boolean = true,
): String = "https://www.zhihu.com/api/v3/feed/topstory/hot-lists/total?limit=$limit&mobile=$mobile"

data class ZhihuHotListPage(
    val data: List<Feed>,
    val rawData: JsonArray,
    val paging: ZhihuPaging?,
)

suspend fun fetchHotListPage(
    client: HttpClient,
    cookies: Map<String, String> = emptyMap(),
    url: String = zhihuHotListUrl(),
    include: String = ZHIHU_HOT_LIST_INCLUDE,
): ZhihuHotListPage {
    val response = client
        .get(url.replace("http://", "https://")) {
            if (include.isNotEmpty()) {
                parameter("include", include)
            }
            cookies["d_c0"]?.let { dc0 ->
                signZhihuFetchRequest(dc0 = dc0)
            }
        }.body<JsonObject>()

    val rawData = response["data"]?.jsonArray ?: JsonArray(emptyList())
    return ZhihuHotListPage(
        data = decodeHotListFeeds(rawData),
        rawData = rawData,
        paging = response["paging"]?.let { ZhihuJson.decodeJson<ZhihuPaging>(it) },
    )
}

fun decodeHotListFeeds(
    data: JsonArray,
    ignoreInvalid: Boolean = true,
): List<Feed> = data.mapNotNull { item ->
    runCatching {
        ZhihuJson.decodeJson<Feed>(item)
    }.getOrElse { error ->
        if (ignoreInvalid) {
            null
        } else {
            throw error
        }
    }
}
