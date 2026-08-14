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

package com.github.zly2006.zhihu

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.data.Collection
import com.github.zly2006.zhihu.viewmodel.CollectionsViewModel
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollectionsViewModelInstrumentedTest {
    @Test
    fun createCollectionUsesVerifiedWebContract() = runBlocking {
        val client = jsonClient { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/api/v4/collections", request.url.encodedPath)
            assertEquals("101_3_3.0", request.headers["x-zse-93"])
            assertTrue(request.headers["x-zse-96"].orEmpty().startsWith("2.0_"))
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertEquals("测试收藏夹", body.getValue("title").jsonPrimitive.content)
            assertEquals("测试描述", body.getValue("description").jsonPrimitive.content)
            assertTrue(body.getValue("is_public").jsonPrimitive.boolean)
            respondJson(
                """{"status":100,"message":"","collection":{"id":123}}""",
            )
        }
        val viewModel = CollectionsViewModel("owner")

        assertTrue(
            viewModel.createCollection(
                environment = testEnvironment(client),
                title = "测试收藏夹",
                description = "测试描述",
                isPublic = true,
            ),
        )
        assertFalse(viewModel.isCreatingCollection)
        assertNull(viewModel.createCollectionError)
    }

    @Test
    fun deleteCollectionTargetsOnlyTheSelectedNonDefaultCollection() = runBlocking {
        var requestCount = 0
        val client = jsonClient { request ->
            requestCount++
            assertEquals(HttpMethod.Delete, request.method)
            assertEquals("/api/v4/collections/selected-id", request.url.encodedPath)
            respondJson("""{"success":true}""")
        }
        val viewModel = CollectionsViewModel("owner")
        val environment = testEnvironment(client)

        assertTrue(
            viewModel.deleteCollection(
                environment,
                Collection(id = "selected-id", title = "待删除收藏夹"),
            ),
        )
        assertFalse(
            viewModel.deleteCollection(
                environment,
                Collection(id = "default-id", title = "默认收藏夹", isDefault = true),
            ),
        )
        assertEquals(1, requestCount)
        assertEquals("默认收藏夹不能删除", viewModel.deleteCollectionError)
    }

    @Test
    fun httpSuccessWithoutDeleteConfirmationIsRejected() = runBlocking {
        val client = jsonClient { request ->
            assertEquals(HttpMethod.Delete, request.method)
            respondJson("""{"success":false,"message":"不能删除"}""")
        }
        val viewModel = CollectionsViewModel("owner")

        assertFalse(
            viewModel.deleteCollection(
                testEnvironment(client),
                Collection(id = "selected-id", title = "待删除收藏夹"),
            ),
        )
        assertEquals("不能删除", viewModel.deleteCollectionError)
    }

    private fun jsonClient(handler: MockRequestHandler) =
        HttpClient(MockEngine(handler)) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

    private fun testEnvironment(client: HttpClient) = object : ZhihuApiEnvironment {
        override fun httpClient() = client

        override fun authenticatedCookies() = mapOf("d_c0" to "test-cookie")

        override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit
    }

    private fun MockRequestHandleScope.respondJson(content: String) = respond(
        content = content,
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )
}
