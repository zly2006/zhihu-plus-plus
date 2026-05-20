package com.github.zly2006.zhihu.shared.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class ZhihuHotListClientTest {
    @Test
    fun buildsDefaultHotListUrl() {
        assertEquals(
            "https://www.zhihu.com/api/v3/feed/topstory/hot-lists/total?limit=50&mobile=true",
            zhihuHotListUrl(),
        )
    }

    @Test
    fun buildsCustomHotListUrl() {
        assertEquals(
            "https://www.zhihu.com/api/v3/feed/topstory/hot-lists/total?limit=20&mobile=false",
            zhihuHotListUrl(limit = 20, mobile = false),
        )
    }

    @Test
    fun fetchHotListPageRequestsIncludeAndDecodesPaging() = runTest {
        val client = hotListMockClient { include ->
            assertEquals(ZHIHU_HOT_LIST_INCLUDE, include)
        }

        val page = fetchHotListPage(client)
        val paging = page.paging ?: error("Expected paging")

        assertEquals(emptyList(), page.data)
        assertFalse(paging.isEnd)
        assertEquals("https://example.com/next", paging.next)
    }

    @Test
    fun createsQuestionDisplayItem() {
        val feed = HotListFeed(
            id = "hot-1",
            detailText = "热度 100 万",
            target = Feed.QuestionTarget(
                id = 1,
                _title = "问题标题",
                url = "https://www.zhihu.com/question/1",
                type = "question",
                answerCount = 10,
                followerCount = 20,
                excerpt = "问题摘要",
            ),
        )

        val item = feed.toDisplayItem(enableQualityFilter = false)

        assertEquals("问题标题", item.title)
        assertEquals("问题摘要", item.summary)
        assertEquals("问题 · 20 关注 · 10 回答 · 热度 100 万", item.details)
        assertNull(item.authorName)
    }

    private fun hotListMockClient(assertInclude: (String?) -> Unit): HttpClient = HttpClient(
        MockEngine { request ->
            assertEquals(zhihuHotListUrl(), "${request.url.protocol.name}://${request.url.host}${request.url.encodedPath}?limit=50&mobile=true")
            assertInclude(request.url.parameters["include"])
            respond(
                content =
                    """
                    {
                      "data": [],
                      "paging": {
                        "page": 1,
                        "is_end": false,
                        "is_start": true,
                        "totals": 100,
                        "next": "https://example.com/next"
                      }
                    }
                    """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        },
    ) {
        installZhihuCommonClientConfig(
            cookies = mutableMapOf(),
            userAgent = "test-agent",
        )
    }
}
