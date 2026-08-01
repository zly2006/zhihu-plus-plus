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

package com.chloemlla.zhplus.viewmodel.comment

import com.chloemlla.zhplus.navigation.Article
import com.chloemlla.zhplus.navigation.ArticleType
import com.chloemlla.zhplus.shared.data.ZhihuJson
import com.chloemlla.zhplus.viewmodel.ZhihuApiEnvironment
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class RootCommentAnchorTest {
    @Test
    fun rootCommentAnchorNeedsOnlyItsOwnDetail() = runTest {
        val requests = mutableListOf<String>()
        val environment = commentEnvironment(
            responses = mapOf("root" to commentJson(id = "root")),
            requests = requests,
        )

        val anchor = RootCommentViewModel(article).resolveCommentAnchor("root", environment)

        assertEquals(listOf("root"), requests)
        assertEquals("root", anchor.target.id)
        assertEquals("root", anchor.root.id)
    }

    @Test
    fun replyAnchorResolvesToItsRootComment() = runTest {
        val requests = mutableListOf<String>()
        val environment = commentEnvironment(
            responses = mapOf(
                "reply" to commentJson(id = "reply", replyRootCommentId = "root"),
                "root" to commentJson(id = "root"),
            ),
            requests = requests,
        )

        val anchor = RootCommentViewModel(article).resolveCommentAnchor("reply", environment)

        assertEquals(listOf("reply", "root"), requests)
        assertEquals("reply", anchor.target.id)
        assertEquals("root", anchor.root.id)
    }

    private val article = Article(
        type = ArticleType.Answer,
        id = 42,
    )

    private fun commentEnvironment(
        responses: Map<String, JsonObject>,
        requests: MutableList<String>,
    ) = object : ZhihuApiEnvironment {
        override fun httpClient() = HttpClient(MockEngine { error("Unexpected HTTP request") })

        override fun authenticatedCookies() = emptyMap<String, String>()

        override suspend fun fetchJson(url: String, include: String): JsonObject? {
            val commentId = url.substringAfterLast('/')
            requests += commentId
            return responses[commentId]
        }

        override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit
    }

    private fun commentJson(
        id: String,
        replyRootCommentId: String? = null,
    ): JsonObject = ZhihuJson.json
        .parseToJsonElement(
            """
            {
              "id": "$id",
              "type": "comment",
              "resource_type": "answer",
              "url": "https://www.zhihu.com/api/v4/comment_v5/comment/$id",
              "content": "<p>测试评论</p>",
              "created_time": 1750000000,
              "is_delete": false,
              "collapsed": false,
              "reviewing": false,
              ${replyRootCommentId?.let { "\"reply_root_comment_id\": \"$it\"," }.orEmpty()}
              "author": {
                "id": "author-id",
                "url_token": "author-token",
                "name": "作者",
                "avatar_url": "https://example.invalid/avatar.png",
                "avatar_url_template": "",
                "is_org": false,
                "type": "people",
                "url": "https://www.zhihu.com/people/author-token",
                "user_type": "people",
                "headline": "",
                "gender": 0,
                "is_advertiser": false
              }
            }
            """.trimIndent(),
        ).jsonObject
}
