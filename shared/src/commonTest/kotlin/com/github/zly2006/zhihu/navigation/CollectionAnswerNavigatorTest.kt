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

package com.github.zly2006.zhihu.navigation

import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.viewmodel.CollectionItem
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import io.ktor.client.HttpClient
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals

class CollectionAnswerNavigatorTest {
    @Test
    fun snapshotContinuesFromTheLoadedCollectionsNextPage() = runTest {
        val nextPageUrl = "https://www.zhihu.com/api/v4/collections/1/items?offset=20"
        val environment = RecordingEnvironment(collectionPage(3L, 4L))
        val navigator = CollectionAnswerNavigator(
            collectionId = "1",
            collectionTitle = "离线收藏夹",
            initialNextItems = emptyList(),
            initialNextUrl = nextPageUrl,
            environment = environment,
        )

        val expectedIds = listOf(3L, 4L)
        assertEquals(expectedIds, navigator.remainingAnswersSnapshot(currentArticleId = 2L, limit = 3).map(Article::id))
        assertEquals(expectedIds, navigator.remainingAnswersSnapshot(currentArticleId = 2L, limit = 3).map(Article::id))
        assertEquals(listOf(nextPageUrl), environment.requestedUrls)
        assertEquals(3L, navigator.loadNext()?.id)
        assertEquals(4L, navigator.loadNext()?.id)
    }

    private fun collectionPage(vararg answerIds: Long): JsonObject = buildJsonObject {
        put(
            "data",
            buildJsonArray {
                answerIds.forEach { answerId ->
                    add(
                        ZhihuJson.json.encodeToJsonElement(
                            CollectionItem.serializer(),
                            CollectionItem(
                                created = "0",
                                content = Feed.AnswerTarget(
                                    id = answerId,
                                    url = "https://www.zhihu.com/question/1/answer/$answerId",
                                    question = Feed.QuestionTarget(
                                        id = 1L,
                                        _title = "离线问题",
                                        url = "https://www.zhihu.com/question/1",
                                        type = "question",
                                    ),
                                ),
                            ),
                        ),
                    )
                }
            },
        )
        put("paging", buildJsonObject { put("next", "") })
    }

    private class RecordingEnvironment(
        private val response: JsonObject,
    ) : ZhihuApiEnvironment {
        val requestedUrls = mutableListOf<String>()

        override fun httpClient(): HttpClient = error("not used")

        override fun authenticatedCookies(): Map<String, String> = emptyMap()

        override suspend fun fetchJson(
            url: String,
            include: String,
        ): JsonObject {
            requestedUrls += url
            return response
        }

        override suspend fun handleFetchFailure(
            tag: String?,
            error: Exception,
        ): Unit = throw error
    }
}
