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

package com.github.zly2006.zhihu.viewmodel.feed

import com.github.zly2006.zhihu.shared.data.CommonFeed
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.MomentsFeed
import com.github.zly2006.zhihu.viewmodel.ContentInteractionEnvironment
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.http.parseUrlEncodedParameters
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class RecommendationFeedbackPosterTest {
    @Test
    fun batchesFiveTouchesAndFlushesReadImmediately() = runTest {
        val payloads = mutableListOf<String>()
        val environment = environment(payloads)
        val poster = RecommendationFeedbackPoster("https://api.zhihu.com/lastread/touch")

        poster.touch(environment, (1..4).map { listOf("a", it.toString()) })
        assertEquals(emptyList(), payloads)

        poster.touch(environment, listOf(listOf("p", "5")))
        poster.touch(environment, listOf(listOf("q", "6")))
        poster.read(environment, listOf("a", "7"))

        assertEquals(
            listOf(
                listOf(
                    listOf("t", "a", "1"),
                    listOf("t", "a", "2"),
                    listOf("t", "a", "3"),
                    listOf("t", "a", "4"),
                    listOf("t", "p", "5"),
                ),
                listOf(
                    listOf("t", "q", "6"),
                    listOf("r", "a", "7"),
                ),
            ),
            payloads.map(::decodeTargets),
        )
    }

    @Test
    fun flushesOldPendingTouchesOnNextSettledList() = runTest {
        var nowMillis = 0L
        val payloads = mutableListOf<String>()
        val poster = RecommendationFeedbackPoster(
            endpoint = "https://api.zhihu.com/moments/lastread",
            nowMillis = { nowMillis },
        )
        val environment = environment(payloads)

        poster.touch(environment, listOf(listOf("brief-1")))
        nowMillis = 120_001L
        poster.touch(environment, listOf(listOf("brief-1")))

        assertEquals(listOf(listOf(listOf("t", "brief-1"))), payloads.map(::decodeTargets))
    }

    @Test
    fun doesNotPostTheSameTouchOrReadEventTwice() = runTest {
        val payloads = mutableListOf<String>()
        val environment = environment(payloads)
        val poster = RecommendationFeedbackPoster("https://api.zhihu.com/moments/lastread")
        val targets = (1..5).map { listOf("brief-$it") }

        poster.touch(environment, targets)
        poster.touch(environment, targets)
        poster.read(environment, targets.first())
        poster.read(environment, targets.first())

        assertEquals(
            listOf(
                targets.map { listOf("t") + it },
                listOf(listOf("r", "brief-1")),
            ),
            payloads.map(::decodeTargets),
        )
    }

    @Test
    fun mapsWebFeedTargetToOfficialShortTypeAndId() {
        val item = FeedDisplayItem(
            title = "问题",
            summary = null,
            details = "",
            feed = CommonFeed(
                target = Feed.QuestionTarget(
                    id = 42,
                    _title = "问题",
                    url = "https://www.zhihu.com/question/42",
                    type = "question",
                ),
            ),
        )

        assertEquals(listOf("q", "42"), item.topStoryFeedbackTarget())
    }

    @Test
    fun homeFeedRecordsOnlyItemsFromTheSettledVisibleSet() = runTest {
        val recordedTitles = mutableListOf<String>()
        val environment = environment(mutableListOf(), recordedTitles)
        val viewModel = HomeFeedViewModel()
        val visibleItem = followDynamicItem(1, "visible")
        val offscreenItem = followDynamicItem(2, "offscreen")
        viewModel.addDisplayItems(listOf(visibleItem, offscreenItem))

        viewModel.reportVisibleItems(environment, setOf(visibleItem.stableKey))

        assertEquals(listOf(visibleItem.title), recordedTitles)
    }

    @Test
    fun bothFollowViewModelsReportBriefsWhenVisibleItemsSettle() = runTest {
        val payloads = mutableListOf<String>()
        val environment = environment(payloads)
        val dynamicViewModel = FollowViewModel()
        val recommendViewModel = FollowRecommendViewModel()
        val dynamicItems = (1L..5L).map { id -> followDynamicItem(id, "dynamic-$id") }
        val recommendItems = (6L..10L).map { id -> followRecommendItem(id, "recommend-$id") }
        dynamicViewModel.addDisplayItems(dynamicItems)
        recommendViewModel.addDisplayItems(recommendItems)

        dynamicViewModel.reportVisibleItems(environment, dynamicItems.mapTo(mutableSetOf()) { it.stableKey })
        recommendViewModel.reportVisibleItems(environment, recommendItems.mapTo(mutableSetOf()) { it.stableKey })
        dynamicViewModel.reportVisibleItems(environment, dynamicItems.mapTo(mutableSetOf()) { it.stableKey })
        recommendViewModel.reportVisibleItems(environment, recommendItems.mapTo(mutableSetOf()) { it.stableKey })

        assertEquals(
            listOf(
                (1L..5L).map { id -> listOf("t", "dynamic-$id") },
                (6L..10L).map { id -> listOf("t", "recommend-$id") },
            ),
            payloads.map(::decodeTargets),
        )
    }

    @Test
    fun followFeedbackUsesOnlyRealNonBlankBriefs() {
        assertEquals(listOf("dynamic"), followDynamicItem(1, "dynamic").momentsFeedbackTarget())
        assertEquals(listOf("recommend"), followRecommendItem(2, "recommend").momentsFeedbackTarget())
        assertEquals(null, followDynamicItem(3, "<none>").momentsFeedbackTarget())
        assertEquals(null, followRecommendItem(4, "").momentsFeedbackTarget())
    }

    private fun environment(
        payloads: MutableList<String>,
        recordedTitles: MutableList<String> = mutableListOf(),
    ): ContentInteractionEnvironment {
        val client = HttpClient(
            MockEngine { request ->
                val body = when (val content = request.body) {
                    is TextContent -> content.text
                    is OutgoingContent.ByteArrayContent -> content.bytes().decodeToString()
                    else -> error("不支持的请求体：${content::class}")
                }
                payloads += body.parseUrlEncodedParameters()["targets"]!!
                respond(
                    content = "{\"success\":true}",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )
        return object : ContentInteractionEnvironment {
            override fun httpClient() = client

            override fun authenticatedCookies() = mapOf("d_c0" to "test-cookie")

            override suspend fun recordContentView(item: FeedDisplayItem) {
                recordedTitles += item.title
            }

            override suspend fun handleFetchFailure(
                tag: String?,
                error: Exception,
            ): Unit = throw error
        }
    }

    private fun followDynamicItem(
        id: Long,
        brief: String,
    ) = FeedDisplayItem(
        title = "动态 $id",
        summary = null,
        details = "",
        feed = CommonFeed(
            id = "dynamic-$id",
            brief = brief,
            target = questionTarget(id),
        ),
    )

    private fun followRecommendItem(
        id: Long,
        brief: String,
    ) = FeedDisplayItem(
        title = "推荐 $id",
        summary = null,
        details = "",
        feed = MomentsFeed(
            id = "recommend-$id",
            brief = brief,
            target = questionTarget(id),
            targetType = "question",
        ),
    )

    private fun questionTarget(id: Long) = Feed.QuestionTarget(
        id = id,
        _title = "问题 $id",
        url = "https://www.zhihu.com/question/$id",
        type = "question",
    )

    private fun decodeTargets(value: String): List<List<String>> = Json
        .parseToJsonElement(value)
        .jsonArray
        .map { target -> target.jsonArray.map { it.jsonPrimitive.content } }
}
