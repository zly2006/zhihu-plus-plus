/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
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
import kotlin.test.assertNull

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
    fun resolvesMobileArticleUrlFromCommonCode() {
        val destination = resolveContent("https://www.zhihu.com/p/123")

        val article = assertIs<Article>(destination)
        assertEquals(ArticleType.Article, article.type)
        assertEquals(123L, article.id)
    }

    @Test
    fun resolvesZhihuAdSourceToAnswerFromCommonCode() {
        val destination = resolveContent(
            "https://www.zhihu.com/market/paid_column/example.html" +
                "?source=https%3A%2F%2Fwww.zhihu.com%2Fappview%2Fv2%2Fanswer%2F3309625617%3Futm_campaign%3Dad",
        )

        val article = assertIs<Article>(destination)
        assertEquals(ArticleType.Answer, article.type)
        assertEquals(3309625617L, article.id)
    }

    @Test
    fun resolvesZhihuRedirectTargetToAdSourceFromCommonCode() {
        val destination = resolveContent(
            "https://link.zhihu.com/?target=" +
                "https%3A%2F%2Fwww.zhihu.com%2Fmarket%2Fpaid_column%2Fexample.html%3Fsource%3D" +
                "https%253A%252F%252Fwww.zhihu.com%252Fappview%252Fv2%252Fanswer%252F3309625617",
        )

        val article = assertIs<Article>(destination)
        assertEquals(ArticleType.Answer, article.type)
        assertEquals(3309625617L, article.id)
    }

    @Test
    fun resolvesLaterZhihuAdSourceWhenEarlierTargetIsExternalFromCommonCode() {
        val destination = resolveContent(
            "https://www.zhihu.com/market/paid_column/example.html" +
                "?url=https%3A%2F%2Fexample.com%2Flanding" +
                "&source=https%3A%2F%2Fwww.zhihu.com%2Fappview%2Fv2%2Farticle%2F123456",
        )

        val article = assertIs<Article>(destination)
        assertEquals(ArticleType.Article, article.type)
        assertEquals(123456L, article.id)
    }

    @Test
    fun resolvesZhihuRedirectTargetUrlToArticleFromCommonCode() {
        val destination = resolveContent(
            "https://link.zhihu.com/?target_url=https%3A%2F%2Fwww.zhihu.com%2Fp%2F987654321",
        )

        val article = assertIs<Article>(destination)
        assertEquals(ArticleType.Article, article.type)
        assertEquals(987654321L, article.id)
    }

    @Test
    fun ignoresExternalRedirectTargetFromCommonCode() {
        assertNull(resolveContent("https://link.zhihu.com/?target=https%3A%2F%2Fexample.com%2Fquestion%2F1"))
    }

    @Test
    fun ignoresUngroundedZhihuRedirectParametersFromCommonCode() {
        assertNull(
            resolveContent(
                "https://www.zhihu.com/not-a-content-page" +
                    "?source=https%3A%2F%2Fwww.zhihu.com%2Fappview%2Fv2%2Fanswer%2F3309625617",
            ),
        )
        assertNull(
            resolveContent(
                "https://xg.zhihu.com/ad" +
                    "?target=https%3A%2F%2Fwww.zhihu.com%2Fp%2F987654321",
            ),
        )
    }
}
