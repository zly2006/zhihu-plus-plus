package com.github.zly2006.zhihu.shared.data

import com.github.zly2006.zhihu.shared.util.ZHIHU_WEB_ZSE93
import com.github.zly2006.zhihu.shared.util.signZhihuFetchRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

const val ZHIHU_READ_HISTORY_ADD_URL = "https://www.zhihu.com/api/v4/read_history/add"
const val ZHIHU_LAST_READ_TOUCH_URL = "https://www.zhihu.com/lastread/touch"

fun buildZhihuReadHistoryBody(
    contentToken: String,
    contentType: String,
): String = buildJsonObject {
    put("content_token", contentToken)
    put("content_type", contentType)
}.toString()

fun encodeZhihuLastReadTouchItems(items: List<List<String>>): String =
    ZhihuJson.json.encodeToString(items)

suspend fun addZhihuReadHistory(
    client: HttpClient,
    contentToken: String,
    contentType: String,
    dc0: String,
    zse93: String = ZHIHU_WEB_ZSE93,
) {
    val body = buildZhihuReadHistoryBody(contentToken, contentType)
    client.post(ZHIHU_READ_HISTORY_ADD_URL) {
        contentType(ContentType.Application.Json)
        setBody(body)
        signZhihuFetchRequest(
            zse93 = zse93,
            dc0 = dc0,
            body = body,
        )
    }
}
