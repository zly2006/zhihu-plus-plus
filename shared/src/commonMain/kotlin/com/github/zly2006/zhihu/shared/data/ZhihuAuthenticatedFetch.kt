package com.github.zly2006.zhihu.shared.data

import com.github.zly2006.zhihu.shared.util.raiseForStatus
import com.github.zly2006.zhihu.util.ZhihuCredentialRefresher
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.time.Clock

suspend fun fetchZhihuAuthenticatedJson(
    client: HttpClient,
    url: String,
    lastRefreshMillis: Long,
    updateLastRefreshMillis: (Long) -> Unit,
    nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    block: suspend HttpRequestBuilder.() -> Unit = {},
): JsonObject? {
    val response = client.request(url) {
        block()
    }
    if (response.status == HttpStatusCode.NoContent) {
        return null
    }
    val body = response.body<JsonElement>()
    if (response.status != HttpStatusCode.Unauthorized) return body as? JsonObject

    if (nowMillis() - lastRefreshMillis < 10_000) {
        return body as? JsonObject
    }
    val refreshToken = ZhihuCredentialRefresher.fetchRefreshToken(client)
    ZhihuCredentialRefresher.refreshZhihuToken(refreshToken, client)
    val refreshedAt = nowMillis()
    updateLastRefreshMillis(refreshedAt)
    val retryResponse = client.request(url) {
        block()
    }
    val retryBody = retryResponse.raiseForStatus().body<JsonElement>()
    return retryBody as? JsonObject
}
