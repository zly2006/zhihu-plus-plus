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

package com.github.zly2006.zhihu.test

import android.content.Context
import com.github.zly2006.zhihu.data.AccountData
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import java.util.concurrent.CopyOnWriteArrayList

object ZhihuMockApi {
    data class RecordedRequest(
        val method: HttpMethod,
        val url: String,
    )

    private data class Route(
        val predicate: (HttpRequestData) -> Boolean,
        val responder: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    )

    private val routes = CopyOnWriteArrayList<Route>()
    private val requests = CopyOnWriteArrayList<RecordedRequest>()
    private var enabled = true

    fun install(enabled: Boolean = true) {
        this.enabled = enabled
        if (!enabled) {
            AccountData.overrideHttpClientFactoryForTesting(null)
            routes.clear()
            requests.clear()
            return
        }
        AccountData.overrideHttpClientFactoryForTesting { context: Context, cookies ->
            AccountData.createConfiguredHttpClient(
                context = context,
                cookies = cookies,
                engine = MockEngine { request ->
                    requests += RecordedRequest(request.method, request.url.toString())
                    val route = routes.firstOrNull { it.predicate(request) }
                    if (route != null) {
                        route.responder(this, request)
                    } else {
                        jsonResponse(
                            status = HttpStatusCode.NotFound,
                            body = """{"error":"No mock route registered","url":"${request.url}","method":"${request.method.value}"}""",
                        )
                    }
                },
            )
        }
    }

    fun isEnabled(): Boolean = enabled

    fun reset() {
        if (!enabled) return
        routes.clear()
        requests.clear()
        installDefaultRoutes()
    }

    fun recordedRequests(): List<RecordedRequest> = requests.toList()

    fun requestCount(method: HttpMethod? = null, urlSubstring: String? = null): Int = requests.count { request ->
        (method == null || request.method == method) &&
            (urlSubstring == null || request.url.contains(urlSubstring))
    }

    fun mockJson(
        method: HttpMethod,
        url: String,
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) {
        routes.add(
            0,
            Route(
                predicate = { request -> request.method == method && request.url.toString() == url },
                responder = { jsonResponse(status, body) },
            ),
        )
    }

    fun mockJsonPrefix(
        method: HttpMethod,
        urlPrefix: String,
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) {
        routes.add(
            0,
            Route(
                predicate = { request -> request.method == method && request.url.toString().startsWith(urlPrefix) },
                responder = { jsonResponse(status, body) },
            ),
        )
    }

    private fun installDefaultRoutes() {
        val emptyFeedResponse =
            """
            {
              "data": [],
              "paging": {
                "is_end": true,
                "is_start": true,
                "totals": 0,
                "next": ""
              }
            }
            """.trimIndent()
        mockJson(
            method = HttpMethod.Get,
            url = "https://api.github.com/repos/zly2006/zhihu-plus-plus/releases/latest",
            body =
                """
                {
                  "tag_name": "0.0.0",
                  "body": "",
                  "assets": [
                    {
                      "name": "zhihu-lite.apk",
                      "content_type": "application/vnd.android.package-archive",
                      "browser_download_url": "https://example.invalid/zhihu-lite.apk"
                    }
                  ]
                }
                """.trimIndent(),
        )
        mockJson(
            method = HttpMethod.Get,
            url = "https://redenmc.com/api/zhihu/releases/latest",
            body =
                """
                {
                  "tag_name": "0.0.0",
                  "body": "",
                  "assets": [
                    {
                      "name": "zhihu-lite.apk",
                      "content_type": "application/vnd.android.package-archive",
                      "browser_download_url": "https://example.invalid/zhihu-lite.apk",
                      "cn_download_url": "https://example.invalid/zhihu-lite-cn.apk"
                    }
                  ]
                }
                """.trimIndent(),
        )
        mockJson(
            method = HttpMethod.Get,
            url = "https://api.zhihu.com/topstory/recommend?include=data%5B%2A%5D.content%2Cexcerpt%2Cheadline",
            body = emptyFeedResponse,
        )
        mockJson(
            method = HttpMethod.Get,
            url = "https://www.zhihu.com/api/v3/feed/topstory/hot-lists/total?limit=50&mobile=true&include=data%5B%2A%5D.content%2Cexcerpt%2Cheadline",
            body = emptyFeedResponse,
        )
        mockJson(
            method = HttpMethod.Get,
            url = "https://www.zhihu.com/api/v4/notifications/v2/recent?limit=20&include=data%5B%2A%5D.content%2Cexcerpt%2Cheadline",
            body = emptyFeedResponse,
        )
        mockJson(
            method = HttpMethod.Get,
            url = "https://api.zhihu.com/unify-consumption/read_history?offset=0&limit=10&include=data%5B%2A%5D.content%2Cexcerpt%2Cheadline",
            body = emptyFeedResponse,
        )
        mockJson(
            method = HttpMethod.Get,
            url = "https://api.zhihu.com/moments_v3?feed_type=recommend&include=data%5B%2A%5D.content%2Cexcerpt%2Cheadline",
            body = emptyFeedResponse,
        )
        mockJson(
            method = HttpMethod.Get,
            url = "https://www.zhihu.com/api/v3/moments?limit=10&desktop=true&include=data%5B%2A%5D.content%2Cexcerpt%2Cheadline",
            body = emptyFeedResponse,
        )
        mockJson(
            method = HttpMethod.Get,
            url = "http://localhost?include=data%5B%2A%5D.content%2Cexcerpt%2Cheadline",
            body = emptyFeedResponse,
        )
        mockJson(
            method = HttpMethod.Get,
            url = "https://api.zhihu.com/moments/recent?type=raw",
            body = """{"data":[]}""",
        )
        mockJson(
            method = HttpMethod.Get,
            url = "https://www.zhihu.com/api/v4/me",
            body =
                """
                {
                  "id": "android-test-user-id",
                  "url": "https://www.zhihu.com/people/android-test-user",
                  "user_type": "people",
                  "url_token": "android-test-user",
                  "name": "AndroidTestUser",
                  "headline": "androidTest mocked /me",
                  "avatar_url": ""
                }
                """.trimIndent(),
        )
        mockJson(
            method = HttpMethod.Get,
            url = "https://www.zhihu.com/api/v4/search/hot_search",
            body = """{"hot_search_queries":[]}""",
        )
        mockJson(
            method = HttpMethod.Post,
            url = "https://www.zhihu.com/api/v4/read_history/add",
            body = """{}""",
        )
        mockJson(
            method = HttpMethod.Post,
            url = "https://www.zhihu.com/lastread/touch",
            body = """{}""",
        )
        mockJsonPrefix(
            method = HttpMethod.Post,
            urlPrefix = "https://www.zhihu.com/api/v4/notifications/",
            body = """{}""",
        )
    }

    private suspend fun MockRequestHandleScope.jsonResponse(
        status: HttpStatusCode,
        body: String,
    ) = respond(
        content = ByteReadChannel(body),
        status = status,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )
}
