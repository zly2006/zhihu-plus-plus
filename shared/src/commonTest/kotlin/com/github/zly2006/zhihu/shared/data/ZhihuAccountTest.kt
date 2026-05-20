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
import kotlin.test.assertNull

class ZhihuAccountTest {
    @Test
    fun fetchVerifiedProfileDecodesSnakeCaseResponse() = runTest {
        val client = mockClient(
            status = HttpStatusCode.OK,
            body = """{"id":"1","name":"Alice","url_token":"alice-token","user_type":"people"}""",
        )

        val profile = fetchVerifiedZhihuProfile(client)

        assertEquals("Alice", profile?.name)
        assertEquals("alice-token", profile?.urlToken)
        assertEquals("people", profile?.userType)
    }

    @Test
    fun fetchVerifiedProfileReturnsNullForUnauthorizedResponse() = runTest {
        val client = mockClient(
            status = HttpStatusCode.Unauthorized,
            body = """{"error":"unauthorized"}""",
        )

        assertNull(fetchVerifiedZhihuProfile(client))
    }

    private fun mockClient(
        status: HttpStatusCode,
        body: String,
    ): HttpClient = HttpClient(
        MockEngine { request ->
            assertEquals(ZHIHU_ME_URL, request.url.toString())
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        },
    ) {
        installZhihuCommonClientConfig(
            cookies = mutableMapOf("_xsrf" to "token"),
            userAgent = "test-agent",
        )
    }
}
