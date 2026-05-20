package com.github.zly2006.zhihu.shared.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class ZhihuOnlineHistoryClientTest {
    @Test
    fun buildsOnlineHistoryUrl() {
        assertEquals(
            "https://api.zhihu.com/unify-consumption/read_history?offset=20&limit=10",
            zhihuOnlineHistoryUrl(offset = 20, limit = 10),
        )
    }

    @Test
    fun fetchOnlineHistoryPageDecodesSnakeCasePayload() = runTest {
        val client = onlineHistoryMockClient { url ->
            assertEquals(zhihuOnlineHistoryUrl(), url)
        }

        val page = fetchOnlineHistoryPage(client)

        assertEquals("read_history", page.data.single().cardType)
        assertEquals(
            "标题",
            page.data
                .single()
                .data.header.title,
        )
        assertEquals(
            "123",
            page.data
                .single()
                .data.extra.contentToken,
        )
        assertEquals("https://api.zhihu.com/next", page.paging?.next)
        assertFalse(page.paging?.isEnd ?: true)
    }

    @Test
    fun decodeOnlineHistoryItemsCanIgnoreInvalidEntries() {
        val data = buildJsonArray {
            add(
                buildJsonObject {
                    put("card_type", "invalid")
                },
            )
        }

        assertFailsWith<SerializationException> {
            decodeOnlineHistoryItems(data)
        }
        assertEquals(emptyList(), decodeOnlineHistoryItems(data, ignoreInvalid = true))
    }

    private fun onlineHistoryMockClient(assertUrl: (String) -> Unit): HttpClient = HttpClient(
        MockEngine { request ->
            assertUrl(request.url.toString())
            respond(
                content =
                    """
                    {
                      "data": [
                        {
                          "card_type": "read_history",
                          "data": {
                            "header": {
                              "icon": "https://example.com/icon.png",
                              "title": "标题"
                            },
                            "action": {
                              "type": "open_url",
                              "url": "zhihu://answers/123"
                            },
                            "extra": {
                              "content_token": "123",
                              "content_type": "answer",
                              "read_time": 1716120000,
                              "question_token": "456"
                            }
                          }
                        }
                      ],
                      "paging": {
                        "page": 1,
                        "is_end": false,
                        "is_start": true,
                        "totals": 100,
                        "next": "https://api.zhihu.com/next"
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
