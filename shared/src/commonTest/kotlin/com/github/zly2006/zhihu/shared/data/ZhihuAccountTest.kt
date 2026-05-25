package com.github.zly2006.zhihu.shared.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

    @Test
    fun fetchVerifiedSessionKeepsProfileAndRawSelf() = runTest {
        val cookies = mutableMapOf("z_c0" to "token", "d_c0" to "dc0")
        val client = mockClient(
            status = HttpStatusCode.OK,
            body = """{"id":"1","name":"Alice","url_token":"alice-token","user_type":"people","avatar_url":"https://example.com/avatar.jpg"}""",
            cookies = cookies,
        )

        val session = fetchVerifiedZhihuSession(client, cookies, "test-agent")

        requireNotNull(session)
        assertEquals(true, session.login)
        assertEquals("Alice", session.username)
        assertEquals(cookies, session.cookies)
        assertEquals("test-agent", session.userAgent)
        assertEquals("1", session.profile?.id)
        assertEquals("alice-token", session.profile?.urlToken)
        assertEquals(
            "https://example.com/avatar.jpg",
            session.self
                ?.jsonObject
                ?.get("avatar_url")
                ?.jsonPrimitive
                ?.content,
        )
    }

    private fun mockClient(
        status: HttpStatusCode,
        body: String,
        cookies: MutableMap<String, String> = mutableMapOf("_xsrf" to "token"),
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
            cookies = cookies,
            userAgent = "test-agent",
        )
    }
}
