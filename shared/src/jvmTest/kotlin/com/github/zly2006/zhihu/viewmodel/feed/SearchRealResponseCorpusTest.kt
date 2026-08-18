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

import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.ZhihuJson
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs

/** Search fixtures come from zhurl production responses with only person identity fields anonymized. */
class SearchRealResponseCorpusTest {
    @Test
    fun realZhihuSearchResponsesExerciseEveryTabAndDecodeFailureGuard() = runTest {
        val general = CorpusSearchViewModel()
        val generalFeeds = general.decode(SearchTab.General, fixture("search-general-sanitized.json"))
        general.process(generalFeeds)
        assertEquals(
            "如何评价<em>kotlin</em>？",
            general.displayItems
                .single()
                .feed
                ?.target
                ?.title,
        )

        val people = CorpusSearchViewModel()
        people.decode(SearchTab.People, fixture("search-people-sanitized.json"))
        val person = assertIs<SearchEntity.Person>(people.entities.single()).person
        assertEquals("redacted-person-token", person.urlToken)
        assertEquals("<em>脱敏用户</em>", person.name)

        val topic = CorpusSearchViewModel()
        topic.decode(SearchTab.Topic, fixture("search-topic-sanitized.json"))
        val topicEntity = assertIs<SearchEntity.Topic>(topic.entities.single())
        assertEquals("Kotlin", topicEntity.topic.name)
        assertFalse(topicEntity.isFollowing)
        assertFalse(
            topic.setTopicFollowing(environment(HttpStatusCode.InternalServerError), topicEntity.id, true).isSuccess,
        )
        assertFalse(assertIs<SearchEntity.Topic>(topic.entities.single()).isFollowing)

        val drift = assertFailsWith<IllegalStateException> {
            CorpusSearchViewModel().decode(SearchTab.Topic, fixture("search-people-sanitized.json"))
        }
        assertEquals("服务端返回了 1 条话题搜索结果，但均无法解码", drift.message)
    }

    private fun fixture(name: String) =
        checkNotNull(javaClass.getResource("/search/$name")).readText()

    private class CorpusSearchViewModel : SearchViewModel("kotlin") {
        private val testEnvironment = environment()

        override fun refresh(environment: PaginationEnvironment) = Unit

        fun decode(
            tab: SearchTab,
            response: String,
        ): List<Feed> {
            selectTab(testEnvironment, tab)
            return decodePage(
                testEnvironment,
                ZhihuJson.json
                    .parseToJsonElement(response)
                    .jsonObject
                    .getValue("data")
                    .jsonArray,
            )
        }

        fun process(feeds: List<Feed>) = processResponse(testEnvironment, feeds, JsonArray(emptyList()))
    }

    private companion object {
        fun environment(status: HttpStatusCode = HttpStatusCode.OK) = object : PaginationEnvironment {
            override fun httpClient() = HttpClient(MockEngine { respond("{}", status) })

            override fun authenticatedCookies() = mapOf("d_c0" to "test")

            override suspend fun fetchJson(
                url: String,
                include: String,
            ): JsonObject? = null

            override suspend fun handleFetchFailure(
                tag: String?,
                error: Exception,
            ) = Unit
        }
    }
}
