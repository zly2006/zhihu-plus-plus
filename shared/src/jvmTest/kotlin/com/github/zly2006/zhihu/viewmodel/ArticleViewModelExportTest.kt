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

import com.github.zly2006.zhihu.account.ZhihuAccountClient
import com.github.zly2006.zhihu.account.ZhihuRequestProfile
import com.github.zly2006.zhihu.data.ArticleContentPreview
import com.github.zly2006.zhihu.data.CommonFeed
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.data.Person
import com.github.zly2006.zhihu.data.navDestination
import com.github.zly2006.zhihu.data.prepareNavigation
import com.github.zly2006.zhihu.data.toFeedDisplayItemNavDestinationJson
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ArticleViewModelExportTest {
    @Test
    fun listNavigationSeedsFirstFrameWithoutTreatingExcerptAsFullContent() {
        val author = Person(
            id = "author-id",
            url = "",
            userType = "people",
            urlToken = "author-token",
            name = "列表作者",
            headline = "作者简介",
            avatarUrl = "https://example.com/avatar.png",
        )
        val targets = listOf(
            Feed.AnswerTarget(
                id = 123,
                url = "",
                author = author,
                question = Feed.QuestionTarget(id = 456, _title = "列表问题", url = "", type = "question"),
                excerpt = "列表回答摘要 &amp; 后续内容",
                content = "<p data-pid=\"first\">列表中的正文片段</p><p data-pid=\"second\"><strong>第二段正文</strong></p>",
            ),
            Feed.ArticleTarget(id = 789, url = "", author = author, title = "列表文章", excerpt = "文章摘要", content = "<p>文章第一段</p><p>文章第二段</p>"),
        )
        targets.forEach { target ->
            val route = FeedDisplayItem(
                title = target.title,
                summary = target.excerpt,
                details = "",
                feed = CommonFeed(id = target.title, target = target),
            ).prepareNavigation("list-source") as Article
            val previewHtml = when (target) {
                is Feed.AnswerTarget -> target.content
                is Feed.ArticleTarget -> target.content
                else -> error("Unexpected target")
            }
            assertFalse(route.toFeedDisplayItemNavDestinationJson().contains(if (target is Feed.AnswerTarget) "列表中的正文片段" else "文章第一段"))
            // Also exercise the serialized navigation used by stored lists.
            val restored = FeedDisplayItem(
                title = target.title,
                summary = target.excerpt,
                details = "",
                feed = null,
                navDestinationJson = route.toFeedDisplayItemNavDestinationJson(),
            ).navDestination as Article
            val model = ArticleViewModel(restored, null)
            assertEquals(target.title, model.title)
            assertEquals(author.name, model.authorName)
            assertEquals(author.headline, model.authorBio)
            assertEquals(author.avatarUrl, model.authorAvatarSrc)
            assertEquals(author.id, model.authorId)
            assertEquals(author.urlToken, model.authorUrlToken)
            assertEquals(if (target is Feed.AnswerTarget) 456L else 0L, model.questionId)
            assertEquals(target.excerpt, model.previewExcerpt)
            assertEquals(previewHtml, model.previewContent)
            assertEquals("list-source", restored.readingQueueSourceId)
            assertEquals("", model.content)
            assertEquals("", ArticleViewModel(restored, null).previewContent)
        }
    }

    @Test
    fun directLinksDoNotDisplayLegacyLoadingPlaceholders() {
        val model = ArticleViewModel(Article(type = ArticleType.Answer, id = 1), null)
        assertEquals("", model.title)
        assertEquals("", model.authorName)
        assertEquals("", model.authorBio)
        assertEquals("", model.previewExcerpt)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun detailResponseReplacesListMetadataAndKeepsFullBodySeparate() = runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val requested = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val client = HttpClient(MockEngine { respond("{}") })
        try {
            val route = Article(type = ArticleType.Article, id = 123, title = "列表标题", authorName = "列表作者", excerpt = "列表摘要")
            val previewHtml = "<p data-pid=\"preview\">列表中的已加载正文</p>"
            val model = ArticleViewModel(ArticleContentPreview.attach(route, previewHtml), client)
            val environment = object : ArticleLoadEnvironment {
                override val accountClient: ZhihuAccountClient? = null

                override fun httpClient(profile: ZhihuRequestProfile) = client

                override fun authenticatedCookies() = emptyMap<String, String>()

                override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit

                override suspend fun fetchJson(url: String, include: String): JsonObject {
                    requested.complete(Unit)
                    release.await()
                    return Json
                        .parseToJsonElement(
                            """{
                          "id":123,"type":"article","title":"详情标题","content":"<p>完整正文</p>","excerpt":"详情摘要",
                          "created":1710000000,"updated":1710000600,"url":"","voteup_count":42,"comment_count":7,
                          "can_comment":{"status":true,"reason":""},
                          "author":{"id":"detail-author","url_token":"detail-token","name":"详情作者","headline":"详情简介",
                          "avatar_url":"https://example.com/detail.png","gender":0,"is_advertiser":false,"is_org":false,
                          "type":"people","url":"","user_type":"people"}
                        }""",
                        ).jsonObject
                }
            }
            model.loadArticle(environment)
            withTimeout(5_000) { requested.await() }
            assertEquals("列表标题", model.title)
            assertEquals("列表作者", model.authorName)
            assertEquals("", model.content)
            assertEquals(true, model.isLoading)
            assertEquals(previewHtml, model.previewContent)
            release.complete(Unit)
            withTimeout(5_000) { while (model.isLoading) delay(10) }
            assertEquals("详情标题", model.title)
            assertEquals("详情作者", model.authorName)
            assertEquals("detail-author", model.authorId)
            assertEquals("<p>完整正文</p>", model.content)
            assertEquals("", model.previewContent)
            assertEquals(42, model.voteUpCount)
            assertEquals(7, model.commentCount)
        } finally {
            release.complete(Unit)
            client.close()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun convert_to_markdown_should_reuse_md_ast_html_conversion() {
        val viewModel = ArticleViewModel(
            article = Article(type = ArticleType.Answer, id = 1L),
            httpClient = null,
        )
        viewModel.title = "导出标题"
        viewModel.authorName = "导出作者"
        viewModel.authorBio = "导出简介"
        viewModel.content =
            """
            <p>正文包含脚注<sup data-draft-type="reference" data-numero="1" data-text="参考资料" data-url="https://example.com/ref">[1]</sup> 和 <a href="https://link.zhihu.com/?target=https%3A%2F%2Fexample.com%2Ftarget">链接</a>。</p>
            <a class="video-box" href="https://link.zhihu.com/?target=https%3A//www.zhihu.com/video/2029631316597973958" data-lens-id="2029631316597973958">
              <img src="https://example.com/cover.jpg" />
            </a>
            """.trimIndent()

        assertEquals(
            """
            # 导出标题

            **作者**: 导出作者

            **简介**: 导出简介

            ---

            正文包含脚注[^1] 和 [链接](https://example.com/target)。

            [视频](https://www.zhihu.com/video/2029631316597973958)

            [^1]: 参考资料[https://example.com/ref](https://example.com/ref)
            """.trimIndent(),
            viewModel.convertToMarkdown(),
        )
    }
}
