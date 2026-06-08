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

package com.github.zly2006.zhihu.shared.nlp

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class KeywordAnalyzerCoreTest {
    @Test
    fun extractsWeightedFeedKeywordsThroughInjectedExtractor() = runTest {
        val seenTexts = mutableListOf<String>()
        val result = KeywordAnalyzerCore.extractFromFeedWithWeight(
            title = "人工智能",
            excerpt = "机器学习",
            content = "深度学习正文",
            topN = 3,
            extractor = KeywordWeightExtractor { text, topN ->
                seenTexts += text
                assertEquals(9, topN)
                listOf(
                    KeywordWithWeight("人工智能", 0.4),
                    KeywordWithWeight("人工智能", 0.9),
                    KeywordWithWeight("机器学习", 0.8),
                    KeywordWithWeight("的", 1.0),
                    KeywordWithWeight("123", 0.7),
                    KeywordWithWeight("A", 0.6),
                )
            },
        )

        assertEquals("人工智能 人工智能 人工智能 机器学习 深度学习正文", seenTexts.single())
        assertEquals(
            listOf(
                KeywordWithWeight("人工智能", 0.9),
                KeywordWithWeight("机器学习", 0.8),
            ),
            result,
        )
    }

    @Test
    fun blankTitleSkipsExtractor() = runTest {
        var extractorCalled = false

        val result = KeywordAnalyzerCore.extractFromFeedWithWeight(
            title = " ",
            extractor = KeywordWeightExtractor { _, _ ->
                extractorCalled = true
                emptyList()
            },
        )

        assertEquals(emptyList(), result)
        assertFalse(extractorCalled)
    }
}
