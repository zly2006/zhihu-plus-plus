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
