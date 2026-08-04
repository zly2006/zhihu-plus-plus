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

package com.github.zly2006.zhihu.viewmodel

import com.github.zly2006.zhihu.util.ZhihuMessageBodyEncryptor
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationViewModelTest {
    @Test
    fun marksOnlyTheRequestedMobileNotificationCategoryAsRead() = runTest {
        val requests = mutableListOf<Pair<HttpMethod, String>>()
        val client = HttpClient(
            MockEngine { request ->
                requests += request.method to request.url.toString()
                respond("", HttpStatusCode.NoContent)
            },
        )
        val environment = object : MobileHomeFeedEnvironment {
            override fun httpClient() = client

            override fun mobileHomeFeedHttpClient() = client

            override fun authenticatedCookies() = emptyMap<String, String>()

            override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit
        }
        val viewModel = NotificationViewModel()
        MobileNotificationCategory.entries.forEach { category ->
            viewModel.categoryUnreadCounts[category] = 1
        }

        for ((index, category) in MobileNotificationCategory.entries.withIndex()) {
            assertTrue(viewModel.markCategoryAsRead(category, environment))
            assertEquals(0, viewModel.categoryUnreadCounts.getValue(category))
            assertEquals(MobileNotificationCategory.entries.size - index - 1, viewModel.unreadCount)
        }

        assertEquals(
            MobileNotificationCategory.entries.map { category ->
                HttpMethod.Post to category.readAllUrl
            },
            requests,
        )
    }

    @Test
    fun encryptsPrivateMessageFormAndAddsServerMessage() = runTest {
        var requestMethod: HttpMethod? = null
        var requestUrl = ""
        var requestContentType = ""
        var requestEncryptionHeader = ""
        var requestBody = ""
        val client = HttpClient(
            MockEngine { request ->
                requestMethod = request.method
                requestUrl = request.url.toString()
                val body = request.body as OutgoingContent.ByteArrayContent
                requestContentType = body.contentType?.toString().orEmpty()
                requestEncryptionHeader = request.headers["X-Zse-93"].orEmpty()
                requestBody = body.bytes().decodeToString()
                respond(
                    content =
                        """
                        {
                          "id":"sent-message",
                          "type":"message",
                          "content_type":0,
                          "content":"测试 & 消息",
                          "created_time":1785743000,
                          "sender":{"id":"me","name":"我"},
                          "receiver":{"id":"peer-id","name":"关注者"}
                        }
                        """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        )
        val environment = object : MobileHomeFeedEnvironment {
            override fun httpClient() = client

            override fun mobileHomeFeedHttpClient() = client

            override fun authenticatedCookies() = emptyMap<String, String>()

            override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit
        }
        val viewModel = PrivateMessageViewModel("peer-id")

        assertTrue(viewModel.sendMessage("测试 & 消息", environment))

        assertEquals(HttpMethod.Post, requestMethod)
        assertEquals("https://api.zhihu.com/messages", requestUrl)
        assertTrue(requestContentType.startsWith(ContentType.Application.FormUrlEncoded.toString()))
        assertEquals("101_1_1.0", requestEncryptionHeader)
        assertEquals(
            "iysSOrmMn/PKkw/O3kD3zj8Zek4IHXEc68IeyNqGB11qQkNt9MTqRTLNSVgELiW3" +
                "TEqBuzSn0sFSH+2mf9H1JNqm4Wpvdlz4c+5Zh4dD65OLN4k32e2ArRDBoLJMj48abfwSVd0Daz9a4aMjncI6cA==",
            requestBody,
        )
        assertEquals("sent-message", viewModel.allData.single().id)
        assertFalse(viewModel.isSending)
    }

    @Test
    fun messageBodyEncryptionMatchesDeidentifiedProtocolVector() {
        assertEquals("14RJeQ+vLOS4ihOY/LtYCg==", ZhihuMessageBodyEncryptor.encrypt("hello"))
    }

    @Test
    fun failedPrivateMessageSendKeepsListAndExposesServerReason() = runTest {
        val client = HttpClient(
            MockEngine {
                respond(
                    content = """{"error":{"code":40380,"message":"暂时不能向对方发送私信"}}""",
                    status = HttpStatusCode.Forbidden,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        )
        val environment = object : MobileHomeFeedEnvironment {
            override fun httpClient() = client

            override fun mobileHomeFeedHttpClient() = client

            override fun authenticatedCookies() = emptyMap<String, String>()

            override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit
        }
        val viewModel = PrivateMessageViewModel("peer-id")

        assertFalse(viewModel.sendMessage("保留的草稿", environment))

        assertTrue(viewModel.allData.isEmpty())
        assertEquals("暂时不能向对方发送私信", viewModel.errorMessage)
        assertFalse(viewModel.isSending)
    }
}
