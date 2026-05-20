package com.github.zly2006.zhihu.shared.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ZhihuReadHistoryClientTest {
    @Test
    fun buildsReadHistoryBody() {
        assertEquals(
            """{"content_token":"123","content_type":"answer"}""",
            buildZhihuReadHistoryBody(
                contentToken = "123",
                contentType = "answer",
            ),
        )
    }

    @Test
    fun addZhihuReadHistoryPostsSignedJsonBody() = runTest {
        val client = HttpClient(
            MockEngine { request ->
                assertEquals(HttpMethod.Post, request.method)
                assertEquals(ZHIHU_READ_HISTORY_ADD_URL, request.url.toString())
                assertEquals(
                    """{"content_token":"123","content_type":"answer"}""",
                    (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString(),
                )
                assertEquals(ContentType.Application.Json, request.body.contentType)
                assertEquals("101_3_3.0", request.headers["x-zse-93"])
                assertNotNull(request.headers["x-zse-96"])
                assertEquals("fetch", request.headers["x-requested-with"])
                respond(
                    content = "{}",
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

        addZhihuReadHistory(
            client = client,
            contentToken = "123",
            contentType = "answer",
            dc0 = "dc0",
        )
    }
}
