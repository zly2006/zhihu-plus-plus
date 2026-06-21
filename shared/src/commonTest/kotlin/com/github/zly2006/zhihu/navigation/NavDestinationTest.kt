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

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NavDestinationTest {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun serializesSearchDestinationFromCommonCode() {
        val destination: NavDestination = Search(query = "kmp")

        val decoded = json.decodeFromString<NavDestination>(
            json.encodeToString<NavDestination>(destination),
        )

        assertEquals(destination, decoded)
    }

    @Test
    fun resolvesZhihuAnswerUrlFromCommonCode() {
        val destination = resolveContent("https://www.zhihu.com/question/1/answer/42")

        val article = assertIs<Article>(destination)
        assertEquals(ArticleType.Answer, article.type)
        assertEquals(42L, article.id)
    }

    @Test
    fun articleNavigationIdentityIgnoresMutableDisplayFields() {
        val article = Article(type = ArticleType.Answer, id = 42L, title = "loading...")
        val hash = article.hashCode()
        val key = article.toString()

        article.title = "loaded title"
        article.authorName = "loaded author"
        article.avatarSrc = "https://example.invalid/avatar.png"

        assertEquals(Article(type = ArticleType.Answer, id = 42L), article)
        assertEquals(hash, article.hashCode())
        assertEquals(key, article.toString())
    }

    @Test
    fun personNavigationIdentitySurvivesProfileBackfillAndSerialization() {
        val route = Person(
            id = Person.EMPTY_ID,
            urlToken = "initial-token",
            name = "loading...",
        )
        val hash = route.hashCode()
        val key = route.toString()

        route.id = "loaded-id"
        route.urlToken = "loaded-token"

        assertEquals(hash, route.hashCode())
        assertEquals(key, route.toString())

        val decoded = assertIs<Person>(
            json.decodeFromString<NavDestination>(
                json.encodeToString<NavDestination>(route),
            ),
        )
        assertEquals(route, decoded)
        assertEquals(key, decoded.toString())
    }
}
