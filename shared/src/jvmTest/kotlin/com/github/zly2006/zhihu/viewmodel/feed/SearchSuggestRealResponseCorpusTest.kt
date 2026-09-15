/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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

import com.github.zly2006.zhihu.data.ZhihuJson
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Search suggest fixtures come from real `www.zhihu.com/api/v4/search/suggest` responses with only session tracking fields redacted. */
class SearchSuggestRealResponseCorpusTest {
    @Test
    fun realSuggestResponsesDecodeIntoItems() = runTest {
        val items = fetchSearchSuggest(environment(respondFixture = "search-suggest-2-sanitized.json"), "AI")
        assertEquals(10, items.size)
        assertTrue(items.all { it.query.isNotBlank() })
        // 真实响应包含带徽标图的建议（hot 热搜火焰、koc 盐选"小说"标签）与普通建议
        assertTrue(items.any { it.iconUrl.isNotEmpty() && it.label == "hot" })
        assertTrue(items.any { it.iconUrl.isEmpty() && it.label.isEmpty() })
    }

    @Test
    fun productionUrlConstructionKeepsSingleEncodedQueryParameter() = runTest {
        val requestedUrls = mutableListOf<String>()
        val env = environment(respondFixture = "search-suggest-1-sanitized.json") { requestedUrls += it }
        fetchSearchSuggest(env, "知乎")
        // 生产 fetchJson 会把 include 也放进去，这里只断言 q 参数恰好出现一次且编码正确
        assertTrue(requestedUrls.isNotEmpty())
        val query = URLBuilder(requestedUrls.last()).parameters
        assertEquals(listOf("知乎"), query.getAll("q"))
    }

    @Test
    fun failedRequestYieldsEmptySuggestListInsteadOfThrowing() = runTest {
        // 模拟真实失败形态：网关返回 HTML 错误页而非 JSON，生产 fetchZhihuAuthenticatedJson 解码时抛异常，
        // fetchSearchSuggest 必须容错返回空列表而不是让异常穿透到 UI 协程。
        val items = fetchSearchSuggest(
            environment(respondFixture = null, statusCode = HttpStatusCode.InternalServerError),
            "知乎",
        )
        assertTrue(items.isEmpty())
    }

    @Test
    fun fixtureDecodesAllItemsEvenWhenOneEntryIsMalformed() {
        val raw = fixture("search-suggest-2-sanitized.json")
        val json = ZhihuJson.json.decodeFromString(JsonObject.serializer(), raw)
        val items = json["suggest"]!!.jsonArray.mapNotNull { element ->
            runCatching { ZhihuJson.decodeJson<SearchSuggestItem>(element) }.getOrNull()
        }
        assertEquals(10, items.size)
    }

    @Test
    fun fixtureWithDuplicateQueriesIsHandledByIndexKey() {
        // search-suggest-5（"你好"语料）含两条同名 "你好迷人"（一条 km_general 小说渠道带徽标，一条普通），
        // UI 的 LazyColumn key 必须容忍重复 query。
        val raw = fixture("search-suggest-5-sanitized.json")
        val json = ZhihuJson.json.decodeFromString(JsonObject.serializer(), raw)
        val queries = json["suggest"]!!
            .jsonArray
            .mapNotNull { element ->
                runCatching { ZhihuJson.decodeJson<SearchSuggestItem>(element) }.getOrNull()
            }.map(SearchSuggestItem::query)
        assertTrue(queries.size != queries.toSet().size, "夹具应包含重复建议词")
        assertEquals(2, queries.count { it == "你好迷人" })
        // 渠道徽标项（小说标签）必须保留 iconUrl 供 UI 展示
        val novel = json["suggest"]!!
            .jsonArray
            .map {
                ZhihuJson.decodeJson<SearchSuggestItem>(it)
            }.first { it.query == "你好迷人" && it.iconUrl.isNotEmpty() }
        assertEquals("koc", novel.label)
    }

    private fun fixture(name: String) = checkNotNull(javaClass.getResource("/search/$name")).readText()

    private fun environment(
        respondFixture: String?,
        statusCode: HttpStatusCode = HttpStatusCode.OK,
        onRequest: (String) -> Unit = {},
    ) = object : PaginationEnvironment {
        override fun httpClient() = HttpClient(
            MockEngine { request ->
                onRequest(request.url.toString())
                val body = respondFixture?.let(::fixture) ?: "<html><body>500 Internal Server Error</body></html>"
                respond(
                    body,
                    statusCode,
                    headers = headersOf(
                        HttpHeaders.ContentType,
                        if (respondFixture == null) "text/html" else "application/json",
                    ),
                )
            },
        ) {
            install(ContentNegotiation) {
                json(ZhihuJson.json)
            }
        }

        override fun authenticatedCookies() = mapOf("d_c0" to "test-dc0")

        override suspend fun handleFetchFailure(
            tag: String?,
            error: Exception,
        ) = Unit
    }
}
