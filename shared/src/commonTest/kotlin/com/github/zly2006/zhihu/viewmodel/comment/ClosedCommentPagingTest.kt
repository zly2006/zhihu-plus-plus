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

package com.github.zly2006.zhihu.viewmodel.comment

import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.ZhihuJson
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.viewmodel.CommentItem
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClosedCommentPagingTest {
    @Test
    fun closedCommentSectionStopsLoadMore() = runTest {
        val requests = mutableListOf<String>()
        val environment = commentListEnvironment(requests, statusType = 1, statusText = "评论区已关闭")
        val viewModel = FetchableCommentViewModel(Article(type = ArticleType.Answer, id = 66419687738))

        viewModel.fetch(environment)
        viewModel.loadMore(environment)
        viewModel.loadMore(environment)

        assertEquals(1, requests.size)
        assertEquals("评论区已关闭", viewModel.commentClosedMessage)
    }

    @Test
    fun replyDisabledSectionDoesNotStopPaging() = runTest {
        val requests = mutableListOf<String>()
        val viewModel = FetchableCommentViewModel(Article(type = ArticleType.Answer, id = 1))

        viewModel.fetch(commentListEnvironment(requests, statusType = 0, canReply = false))

        assertNull(viewModel.commentClosedMessage)
        assertEquals(false, viewModel.isEnd)
    }
}

/**
 * `RootCommentViewModel` 是 final，不能为了测试改成 open。关闭判断在 `BaseCommentViewModel`，
 * 这里只补齐抽象方法并调用 protected 的 `fetchFeeds`。第一页不能走 `loadMore`：它会进入 `viewModelScope`，
 * commonTest 没有 Main dispatcher；关闭后的再次 `loadMore` 会在启动协程前返回。
 */
private class FetchableCommentViewModel(
    article: Article,
) : BaseCommentViewModel(article) {
    override val initialUrl: String = "https://www.zhihu.com/api/v4/comment_v5/answers/${article.id}/root_comment"

    override fun createCommentItem(comment: DataHolder.Comment, article: NavDestination) = CommentItem(comment, null)

    override fun submitComment(
        content: NavDestination,
        commentText: String,
        environment: ZhihuApiEnvironment,
        replyToCommentId: String?,
        onSuccess: () -> Unit,
    ) = Unit

    suspend fun fetch(environment: PaginationEnvironment) = fetchFeeds(environment)
}

private fun commentListEnvironment(
    requests: MutableList<String>,
    statusType: Int,
    statusText: String = "",
    canReply: Boolean = true,
): PaginationEnvironment = object : PaginationEnvironment {
    override fun httpClient() = HttpClient(MockEngine { error("Unexpected HTTP request") })

    override fun authenticatedCookies() = emptyMap<String, String>()

    override suspend fun fetchJson(url: String, include: String): JsonObject {
        requests += url
        val offset = requests.size
        return ZhihuJson.json.parseToJsonElement(
            """
            {
              "comment_status": {"type": $statusType, "text": "$statusText", "induce_text": ""},
              "edit_status": {"can_reply": $canReply, "toast": ""},
              "data": [],
              "paging": {
                "is_end": false,
                "is_start": ${offset == 1},
                "next": "https://www.zhihu.com/api/v4/comment_v5/answers/1/root_comment?offset=cursor-$offset",
                "totals": 164
              }
            }
            """.trimIndent(),
        ) as JsonObject
    }

    override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit
}
