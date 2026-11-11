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

package com.github.zly2006.zhihu.data

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.feed.OnlineHistoryViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class OnlineHistoryTest {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun onlineHistoryNavigationUsesVisibleFieldsAndMatchingLocalAuthorDetails() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val cached = Article(
                type = ArticleType.Answer,
                id = 123,
                title = "标题",
                authorName = "作者",
                authorBio = "本地签名",
                avatarSrc = "avatar",
                authorId = "author-id",
                authorUrlToken = "author-token",
            )
            listOf(emptyList(), listOf(cached)).forEach { local ->
                val environment = object : PaginationEnvironment {
                    override fun httpClient(): HttpClient = error("无需额外请求")

                    override fun authenticatedCookies() = emptyMap<String, String>()

                    override fun localHistory() = local

                    override suspend fun handleFetchFailure(tag: String?, error: Exception) = throw error

                    override suspend fun fetchJson(url: String, include: String) = buildJsonObject {
                        put("data", buildJsonArray { add(ZhihuJson.json.parseToJsonElement(itemJson)) })
                    }
                }
                val list = OnlineHistoryViewModel()
                list.refresh(environment)
                advanceUntilIdle()
                val route = assertIs<Article>(list.displayItems.single().navDestination)
                val model = ArticleViewModel(route, null)
                assertEquals("标题", model.title)
                assertEquals("作者", model.authorName)
                assertEquals("摘要", model.previewExcerpt)
                assertEquals(456L, model.questionId)
                assertEquals(if (local.isEmpty()) "" else "本地签名", model.authorBio)
                assertEquals(if (local.isEmpty()) "" else "avatar", model.authorAvatarSrc)
                assertEquals(if (local.isEmpty()) "" else "author-id", model.authorId)
                assertEquals("", model.content)
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun decodesOnlineHistoryItem() {
        val item = ZhihuJson.json.decodeFromString<OnlineHistoryItem>(
            itemJson,
        )

        assertEquals("read_history", item.cardType)
        assertEquals("标题", item.data.header.title)
        assertEquals("zhihu://answers/123", item.data.action.url)
        assertEquals(
            "已阅读",
            item.data.matrix
                ?.single()
                ?.data
                ?.text,
        )
        assertEquals("456", item.data.extra.questionToken)
    }

    private val itemJson =
        """
        {
          "cardType": "read_history",
          "data": {
            "header": {
              "icon": "https://example.com/icon.png",
              "title": "标题"
            },
            "content": {
              "authorName": "作者",
              "summary": "摘要",
              "coverImage": "https://example.com/cover.png"
            },
            "action": {
              "type": "open_url",
              "url": "zhihu://answers/123"
            },
            "extra": {
              "contentToken": "123",
              "contentType": "answer",
              "readTime": 1716120000,
              "questionToken": "456"
            },
            "matrix": [
              {
                "type": "text",
                "data": {
                  "text": "已阅读"
                }
              }
            ]
          }
        }
        """.trimIndent()
}
