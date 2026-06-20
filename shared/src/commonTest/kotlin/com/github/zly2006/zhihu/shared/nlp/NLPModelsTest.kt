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

package com.github.zly2006.zhihu.shared.nlp

import kotlin.test.Test
import kotlin.test.assertEquals

class NLPModelsTest {
    @Test
    fun debugTraceDefaultsMutableCollectionsForRuntimeTracing() {
        val trace = NlpDebugTrace(normalizedText = "文本")

        trace.mmrIterations += KeywordSelectionDebug(
            iteration = 1,
            candidate = "关键词",
            relevance = 0.8,
            redundancy = 0.2,
            score = 0.6,
        )
        trace.selectedKeywords += KeywordWithWeight("关键词", 0.6)

        assertEquals("文本", trace.normalizedText)
        assertEquals("关键词", trace.mmrIterations.single().candidate)
        assertEquals(KeywordWithWeight("关键词", 0.6), trace.selectedKeywords.single())
    }
}
