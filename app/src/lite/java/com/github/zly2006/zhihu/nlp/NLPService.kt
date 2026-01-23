package com.github.zly2006.zhihu.nlp

object NLPService {
    suspend fun extractKeywords(text: String, topN: Int = 5): List<String> = emptyList()

    suspend fun extractKeywordsWithWeight(text: String, topN: Int = 5): List<KeywordWithWeight> = emptyList()

    suspend fun checkBlockedPhrases(text: String, blockedPhrases: List<String>, threshold: Double = 0.8): List<Pair<String, Double>> = emptyList()

    suspend fun extractSummary(text: String, maxLength: Int = 100): String = text.take(maxLength)
}
