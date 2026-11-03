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
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
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

    @Test
    fun privateMessagePollingFetchesUntilOverlapAndPreservesHistory() = runTest {
        val requestedUrls = mutableListOf<String>()
        val client = HttpClient(
            MockEngine { request ->
                requestedUrls += request.url.toString()
                val content = if (request.url.parameters["after_id"] == null) {
                    privateMessagePage(
                        ids = listOf("new-2" to 300L, "new-1" to 200L),
                        next = "https://api.zhihu.com/messages?sender_id=peer-id&after_id=200&limit=20",
                    )
                } else {
                    privateMessagePage(
                        ids = listOf("old-2" to 100L, "old-1" to 50L),
                        isEnd = true,
                    )
                }
                respond(
                    content = content,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        ) {
            install(ContentNegotiation) { json() }
        }
        val environment = object : MobileHomeFeedEnvironment {
            override fun httpClient() = client

            override fun mobileHomeFeedHttpClient() = client

            override fun authenticatedCookies() = emptyMap<String, String>()

            override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit
        }
        val viewModel = PrivateMessageViewModel("peer-id")
        viewModel.allData += listOf(
            privateMessage("old-2", 100L),
            privateMessage("old-1", 50L),
        )

        viewModel.refreshLatest(environment)

        assertEquals(
            listOf("new-2", "new-1", "old-2", "old-1"),
            viewModel.allData.map { it.id },
        )
        assertEquals(2, requestedUrls.size)
    }

    @Test
    fun privateMessagePollingPreservesServerOrderWithinTheSameSecond() = runTest {
        var poll = 0
        val client = HttpClient(
            MockEngine {
                val ids = if (poll++ == 0) {
                    listOf("old-4" to 100L, "old-3" to 100L, "old-2" to 100L, "old-1" to 100L)
                } else {
                    listOf("new-2" to 100L, "new-1" to 100L, "old-4" to 100L, "old-3" to 100L)
                }
                respond(
                    privateMessagePage(ids, next = "https://api.zhihu.com/messages?after_id=history"),
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        ) {
            install(ContentNegotiation) { json() }
        }
        val environment = privateMessageEnvironment(client)
        val viewModel = PrivateMessageViewModel("peer-id")

        viewModel.refreshLatest(environment)
        repeat(2) { viewModel.refreshLatest(environment) }

        assertEquals(listOf("new-2", "new-1", "old-4", "old-3", "old-2", "old-1"), viewModel.allData.map { it.id })
    }

    @Test
    fun privateMessagePollingBridgesBacklogAfterSendingBeforePolling() = runTest {
        val requestedPages = mutableListOf<String?>()
        var sent = false
        val client = HttpClient(
            MockEngine { request ->
                val content = if (request.method == HttpMethod.Post) {
                    sent = true
                    """{"id":"sent","created_time":400}"""
                } else {
                    val afterId = request.url.parameters["after_id"]
                    requestedPages += afterId
                    when {
                        !sent -> privateMessagePage(listOf("old" to 100L), next = "https://api.zhihu.com/messages?after_id=history")
                        afterId == "middle" -> privateMessagePage(
                            listOf("new-2" to 200L, "new-1" to 100L),
                            next = "https://api.zhihu.com/messages?after_id=overlap",
                        )
                        afterId == "overlap" -> privateMessagePage(listOf("old" to 100L), isEnd = true)
                        else -> privateMessagePage(
                            listOf("sent" to 400L, "new-3" to 300L),
                            next = "https://api.zhihu.com/messages?after_id=middle",
                        )
                    }
                }
                respond(content, headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
            },
        ) {
            install(ContentNegotiation) { json() }
        }
        val environment = privateMessageEnvironment(client)
        val viewModel = PrivateMessageViewModel("peer-id")
        viewModel.refreshLatest(environment)
        assertTrue(viewModel.sendMessage("消息", environment))
        assertEquals("sent", viewModel.allData.first().id)
        requestedPages.clear()

        viewModel.refreshLatest(environment)

        assertEquals(listOf(null, "middle", "overlap"), requestedPages)
        assertEquals(listOf("sent", "new-3", "new-2", "new-1", "old"), viewModel.allData.map { it.id })
        requestedPages.clear()
        viewModel.refreshLatest(environment)
        assertEquals(listOf<String?>(null), requestedPages)
    }

    @Test
    fun privateMessagePollingKeepsConcurrentSendAheadOfItsOlderSnapshot() = runTest {
        val pollStarted = CompletableDeferred<Unit>()
        val completePoll = CompletableDeferred<Unit>()
        var polls = 0
        val client = HttpClient(
            MockEngine { request ->
                val content = when {
                    request.method == HttpMethod.Post -> """{"id":"sent","created_time":100}"""
                    polls++ == 0 -> privateMessagePage(listOf("old" to 100L), isEnd = true)
                    else -> {
                        pollStarted.complete(Unit)
                        completePoll.await()
                        privateMessagePage(listOf("new" to 100L, "old" to 100L), isEnd = true)
                    }
                }
                respond(content, headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
            },
        ) {
            install(ContentNegotiation) { json() }
        }
        val environment = privateMessageEnvironment(client)
        val viewModel = PrivateMessageViewModel("peer-id")
        viewModel.refreshLatest(environment)
        val polling = launch { viewModel.refreshLatest(environment) }
        pollStarted.await()

        assertTrue(viewModel.sendMessage("消息", environment))
        completePoll.complete(Unit)
        polling.join()

        assertEquals(listOf("sent", "new", "old"), viewModel.allData.map { it.id })
    }

    @Test
    fun privateMessagePollingRetriesTheGapAfterAMiddlePageFails() = runTest {
        var sent = false
        var failMiddlePage = true
        val client = HttpClient(
            MockEngine { request ->
                val content = when {
                    request.method == HttpMethod.Post -> {
                        sent = true
                        """{"id":"sent","created_time":300}"""
                    }
                    !sent -> privateMessagePage(listOf("old" to 100L), isEnd = true)
                    request.url.parameters["after_id"] == "middle" -> {
                        check(!failMiddlePage) { "中间页暂时失败" }
                        privateMessagePage(listOf("new-1" to 150L, "old" to 100L), isEnd = true)
                    }
                    else -> privateMessagePage(
                        listOf("sent" to 300L, "new-2" to 200L),
                        next = "https://api.zhihu.com/messages?after_id=middle",
                    )
                }
                respond(content, headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
            },
        ) {
            install(ContentNegotiation) { json() }
        }
        val environment = privateMessageEnvironment(client)
        val viewModel = PrivateMessageViewModel("peer-id")
        viewModel.refreshLatest(environment)
        assertTrue(viewModel.sendMessage("消息", environment))

        viewModel.refreshLatest(environment)

        assertEquals(listOf("sent", "old"), viewModel.allData.map { it.id })
        failMiddlePage = false
        viewModel.refreshLatest(environment)
        assertEquals(listOf("sent", "new-2", "new-1", "old"), viewModel.allData.map { it.id })
    }

    private fun privateMessageEnvironment(client: HttpClient) = object : MobileHomeFeedEnvironment {
        override fun httpClient() = client

        override fun mobileHomeFeedHttpClient() = client

        override fun authenticatedCookies() = emptyMap<String, String>()

        override suspend fun handleFetchFailure(tag: String?, error: Exception) = throw error
    }

    private fun privateMessage(id: String, createdTime: Long) =
        com.github.zly2006.zhihu.data.ZhihuPrivateMessage(
            id = id,
            createdTime = createdTime,
        )

    private fun privateMessagePage(
        ids: List<Pair<String, Long>>,
        next: String = "",
        isEnd: Boolean = false,
    ): String =
        """
        {
          "data": [${ids.joinToString(",") { (id, createdTime) ->
            """{"id":"$id","type":"message","content":"$id","created_time":$createdTime}"""
        }}],
          "paging": {"is_end":$isEnd,"next":"$next"}
        }
        """.trimIndent()
}
