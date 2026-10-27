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

package com.github.zly2006.zhihu.ui

import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PeopleColumnsResponseTest {
    @Test
    fun contributionsLoadNestedColumnsThroughProductionPagination() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val failures = mutableListOf<Exception>()
        // 2026-10-27 真实响应，包含外层 contributions_count 和内层 column。
        val response = Json.parseToJsonElement(checkNotNull(javaClass.getResource("/columns/contributions-2026-10-27.json")).readText()).jsonObject
        val environment = object : PaginationEnvironment {
            override fun httpClient(): HttpClient = error("fetchJson handles this recorded response")

            override fun authenticatedCookies(): Map<String, String> = emptyMap()

            override suspend fun fetchJson(url: String, include: String) = response.also {
                assertTrue(url.endsWith("/members/excited-vczh/column-contributions"))
            }

            override suspend fun handleFetchFailure(tag: String?, error: Exception) {
                failures += error
            }

            override fun logDecodeFailure(tag: String?, item: kotlinx.serialization.json.JsonElement, error: Exception) {
                failures += error
            }
        }
        try {
            val model = PeopleColumnContributionsViewModel(Person(id = "excited-vczh", urlToken = "excited-vczh"))
            model.loadMore(environment)
            advanceUntilIdle()
            assertEquals(listOf("vczh-nichijou"), model.allData.map { it.id })
            assertTrue(
                model.allData
                    .single()
                    .title
                    .isNotBlank(),
            )
            assertNull(model.allData.single().articlesCount)
            assertNull(model.allData.single().followers)
            assertTrue(model.isEnd)
            assertTrue(failures.isEmpty(), failures.toString())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun followingPageAppendsNestedColumnsAndPreservesCounts() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val pages = (1..2).map { index ->
            Json.parseToJsonElement(checkNotNull(javaClass.getResource("/columns/page-$index-2026-10-27.json")).readText()).jsonObject
        }
        val calls = mutableListOf<String>()
        val environment = object : PaginationEnvironment {
            override fun httpClient(): HttpClient = error("recorded response")

            override fun authenticatedCookies(): Map<String, String> = emptyMap()

            override suspend fun fetchJson(url: String, include: String) = pages[calls.size].also {
                calls += url
                assertEquals("data[*].column.articles_count", include)
            }

            override suspend fun handleFetchFailure(tag: String?, error: Exception) = throw error
        }
        try {
            val model = PeopleColumnContributionsViewModel(Person(id = "recorded-author", urlToken = "recorded-author"))
            model.loadMore(environment)
            advanceUntilIdle()
            assertEquals(10, model.allData.size)
            assertTrue(!model.isEnd)
            model.loadMore(environment)
            advanceUntilIdle()
            assertEquals(14, model.allData.size)
            assertEquals(
                14,
                model.allData
                    .map { it.id }
                    .distinct()
                    .size,
            )
            assertTrue(model.allData.all { it.articlesCount != null })
            assertTrue(model.isEnd)
            model.loadMore(environment)
            advanceUntilIdle()
            assertEquals(2, calls.size)
            assertTrue(calls.last().contains("offset=10"))
        } finally {
            Dispatchers.resetMain()
        }
    }
}
