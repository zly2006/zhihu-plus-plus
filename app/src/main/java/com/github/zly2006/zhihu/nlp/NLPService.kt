package com.github.zly2006.zhihu.nlp

import com.hankcs.hanlp.HanLP
import com.hankcs.hanlp.seg.common.Term
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 关键词及其权重信息
 */
data class KeywordWithWeight(
    val keyword: String,
    val weight: Double,
)

/**
 * NLP服务，用于中文文本的关键词提取和语义相似度计算
 */
object NLPService {
    private const val MAX_KEYWORD_CANDIDATES = 60
    private const val KEYWORD_CANDIDATE_MULTIPLIER = 5
    private const val MMR_LAMBDA = 0.65
    private const val MAX_EMBEDDING_TEXT_LENGTH = 2048
    private const val MIN_KEYWORD_LENGTH = 2

    /**
     * 从文本中提取关键词
     */
    suspend fun extractKeywords(text: String, topN: Int = 5): List<String> = withContext(Dispatchers.Default) {
        extractKeywordsInternal(text, topN).map { it.keyword }
    }

    /**
     * 从文本中提取关键词及其权重
     */
    suspend fun extractKeywordsWithWeight(text: String, topN: Int = 5): List<KeywordWithWeight> = withContext(Dispatchers.Default) {
        extractKeywordsInternal(text, topN)
    }

    private suspend fun extractKeywordsInternal(text: String, topN: Int): List<KeywordWithWeight> {
        if (text.isBlank() || topN <= 0) return emptyList()
        val candidates = generateKeywordCandidates(text, topN)
        if (candidates.isEmpty()) return emptyList()

        val mmrResult = selectKeywordsWithMMR(text, candidates, topN)
        if (mmrResult.isNotEmpty()) {
            return mmrResult
        }

        // Fallback：使用HanLP结果
        val fallback = candidates.take(topN)
        val denominator = max(1, fallback.size - 1)
        return fallback.mapIndexed { index, keyword ->
            KeywordWithWeight(keyword, 1.0 - index.toDouble() / max(1, denominator))
        }
    }

    /**
     * 计算两个文本之间的语义相似度
     * @param text1 第一个文本
     * @param text2 第二个文本
     * @return 相似度分数 (0.0 - 1.0)，越高表示越相似
     */
    suspend fun calculateSimilarity(text1: String, text2: String): Double = withContext(Dispatchers.Default) {
        if (text1.isBlank() || text2.isBlank()) return@withContext 0.0

        val embeddingScore = embedPairSimilarity(text1, text2)
        embeddingScore ?: legacySimilarity(text1, text2)
    }

    /**
     * 计算段落文本与短语之间的语义相似度（优化版）
     * 适用于长文本段落与短关键词短语的匹配场景
     *
     * @param paragraphText 段落文本（较长的内容）
     * @param phraseKeywords 短语关键词（空格分隔的多个关键词）
     * @return 相似度分数 (0.0 - 1.0)
     */
    suspend fun calculatePhraseMatchScore(
        paragraphText: String,
        phraseKeywords: String,
    ): Double = withContext(Dispatchers.Default) {
        if (paragraphText.isBlank() || phraseKeywords.isBlank()) return@withContext 0.0

        val keywords = phraseKeywords
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .distinct()
        if (keywords.isEmpty()) return@withContext 0.0

        val paragraphEmbedding = encodeTextOrNull(paragraphText)
        if (paragraphEmbedding != null) {
            val keywordEmbeddings = keywords.mapNotNull { keyword ->
                encodeTextOrNull(keyword)?.let { keyword to it }
            }
            if (keywordEmbeddings.isNotEmpty()) {
                val avgScore = keywordEmbeddings
                    .map { (_, embedding) -> cosineToScore(cosineSimilarity(paragraphEmbedding, embedding)) }
                    .average()
                val coverage = keywordEmbeddings.size.toDouble() / keywords.size
                return@withContext avgScore * 0.7 + coverage * 0.3
            }
        }

        legacyPhraseMatchScore(paragraphText, keywords)
    }

    private suspend fun embedPairSimilarity(text1: String, text2: String): Double? {
        val embedding1 = encodeTextOrNull(text1)
        val embedding2 = encodeTextOrNull(text2)
        if (embedding1 == null || embedding2 == null) return null
        return cosineToScore(cosineSimilarity(embedding1, embedding2))
    }

    private fun legacySimilarity(text1: String, text2: String): Double = try {
        val terms1 = segment(text1)
        val terms2 = segment(text2)
        val words1 = terms1.filter { shouldKeepTerm(it) }.map { it.word }.toSet()
        val words2 = terms2.filter { shouldKeepTerm(it) }.map { it.word }.toSet()
        if (words1.isEmpty() || words2.isEmpty()) {
            0.0
        } else {
            val intersection = words1.intersect(words2).size
            val union = words1.union(words2).size
            intersection.toDouble() / union.toDouble()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        0.0
    }

    private fun legacyPhraseMatchScore(
        paragraphText: String,
        keywords: List<String>,
    ): Double = try {
        val paragraphKeywords = hanlpExtractKeywords(paragraphText, min(30, paragraphText.length / 10)).toSet()
        val paragraphWords = segment(paragraphText)
            .filter { shouldKeepTerm(it) }
            .map { it.word }
            .toSet()

        var totalScore = 0.0
        var matchCount = 0

        for (keyword in keywords) {
            var keywordScore = 0.0
            if (paragraphWords.contains(keyword)) {
                keywordScore = max(keywordScore, 1.0)
            }
            if (paragraphText.contains(keyword, ignoreCase = true)) {
                keywordScore = max(keywordScore, 0.85)
            }
            if (paragraphKeywords.contains(keyword)) {
                keywordScore = max(keywordScore, 0.95)
            }
            for (word in paragraphWords) {
                if (word.length >= 2 && keyword.length >= 2) {
                    if (word.contains(keyword) || keyword.contains(word)) {
                        val similarity = calculateWordSimilarity(keyword, word)
                        keywordScore = max(keywordScore, similarity * 0.75)
                    }
                }
            }
            val keywordRelatedWords = paragraphKeywords.filter { it.contains(keyword) || keyword.contains(it) }
            if (keywordRelatedWords.isNotEmpty()) {
                keywordScore = max(keywordScore, 0.7)
            }
            if (keywordScore > 0) {
                matchCount++
                totalScore += keywordScore
            }
        }

        if (matchCount == 0) {
            0.0
        } else {
            val matchRatio = matchCount.toDouble() / keywords.size
            val avgScore = totalScore / keywords.size
            when {
                matchCount == keywords.size -> avgScore * 0.7 + matchRatio * 0.3
                matchCount >= keywords.size * 0.7 -> avgScore * 0.6 + matchRatio * 0.4
                else -> avgScore * 0.5 + matchRatio * 0.5
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        0.0
    }

    private fun generateKeywordCandidates(text: String, topN: Int): List<String> {
        if (text.isBlank()) return emptyList()
        val poolSize = max(topN * KEYWORD_CANDIDATE_MULTIPLIER, topN + 3)
        val maxPoolSize = min(MAX_KEYWORD_CANDIDATES * 2, poolSize)
        return HanLP
            .extractKeyword(text, maxPoolSize)
            .mapNotNull { it?.trim() }
            .filter { it.length >= MIN_KEYWORD_LENGTH }
            .distinct()
    }

    private suspend fun selectKeywordsWithMMR(
        text: String,
        candidates: List<String>,
        topN: Int,
    ): List<KeywordWithWeight> {
        val documentEmbedding = encodeTextOrNull(text) ?: return emptyList()
        val limitedCandidates = candidates.take(min(candidates.size, MAX_KEYWORD_CANDIDATES))
        if (limitedCandidates.isEmpty()) return emptyList()

        val candidateEmbeddings = buildMap {
            for (candidate in limitedCandidates) {
                val embedding = encodeTextOrNull(candidate)
                if (embedding != null) {
                    put(candidate, embedding)
                }
            }
        }
        if (candidateEmbeddings.isEmpty()) return emptyList()

        val remaining = candidateEmbeddings.toMutableMap()
        val selected = mutableListOf<KeywordWithWeight>()

        while (selected.size < topN && remaining.isNotEmpty()) {
            var bestKey: String? = null
            var bestRelevance = 0.0
            var bestScore = Double.NEGATIVE_INFINITY

            for ((word, embedding) in remaining) {
                val relevance = cosineSimilarity(documentEmbedding, embedding)
                val redundancy = selected.maxOfOrNull { keyword ->
                    val otherEmbedding = candidateEmbeddings[keyword.keyword]
                    if (otherEmbedding != null) {
                        cosineSimilarity(embedding, otherEmbedding)
                    } else {
                        0.0
                    }
                } ?: 0.0
                val score = MMR_LAMBDA * relevance - (1 - MMR_LAMBDA) * redundancy
                if (score > bestScore) {
                    bestScore = score
                    bestKey = word
                    bestRelevance = relevance
                }
            }

            val keyword = bestKey ?: break
            selected.add(KeywordWithWeight(keyword, cosineToScore(bestRelevance)))
            remaining.remove(keyword)
        }

        return selected
    }

    private suspend fun encodeTextOrNull(text: String): FloatArray? {
        val normalized = truncateForEmbedding(text)
        if (normalized.isBlank()) return null
        return try {
            SentenceEmbeddingManager.encode(normalized)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun truncateForEmbedding(text: String): String =
        text.trim().take(MAX_EMBEDDING_TEXT_LENGTH)

    private fun cosineSimilarity(
        first: FloatArray,
        second: FloatArray,
    ): Double {
        var dot = 0.0
        var magFirst = 0.0
        var magSecond = 0.0
        val size = min(first.size, second.size)
        for (i in 0 until size) {
            val v1 = first[i].toDouble()
            val v2 = second[i].toDouble()
            dot += v1 * v2
            magFirst += v1 * v1
            magSecond += v2 * v2
        }
        val denominator = sqrt(magFirst) * sqrt(magSecond)
        if (denominator == 0.0) return 0.0
        return dot / denominator
    }

    private fun cosineToScore(value: Double): Double = ((value + 1.0) / 2.0).coerceIn(0.0, 1.0)

    private fun hanlpExtractKeywords(text: String, topN: Int): List<String> {
        if (text.isBlank() || topN <= 0) return emptyList()
        return HanLP.extractKeyword(text, topN)
    }

    /**
     * 计算两个词语之间的相似度
     */
    private fun calculateWordSimilarity(word1: String, word2: String): Double {
        if (word1 == word2) return 1.0

        // 使用编辑距离计算相似度
        val maxLen = max(word1.length, word2.length)
        if (maxLen == 0) return 1.0

        val distance = levenshteinDistance(word1, word2)
        return 1.0 - (distance.toDouble() / maxLen)
    }

    /**
     * 计算编辑距离（Levenshtein距离）
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1, // 删除
                    dp[i][j - 1] + 1, // 插入
                    dp[i - 1][j - 1] + cost, // 替换
                )
            }
        }

        return dp[m][n]
    }

    /**
     * 检查文本是否与屏蔽词列表中的任何词语相似
     * @param text 要检查的文本
     * @param blockedWords 屏蔽词列表
     * @param threshold 相似度阈值，默认0.3
     * @return 如果相似度超过阈值则返回true
     */
    suspend fun isBlockedByKeywords(
        text: String,
        blockedWords: List<String>,
        threshold: Double = 0.3,
    ): Boolean = withContext(Dispatchers.Default) {
        if (text.isBlank() || blockedWords.isEmpty()) return@withContext false

        try {
            // 提取文本关键词
            val textKeywords = extractKeywords(text, 10)
            val textContent = "$text ${textKeywords.joinToString(" ")}"

            // 检查是否与任何屏蔽词相似
            for (blockedWord in blockedWords) {
                val similarity = calculateSimilarity(textContent, blockedWord)
                if (similarity >= threshold) {
                    return@withContext true
                }
            }

            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 检查文本是否与屏蔽短语匹配，并返回匹配详情
     * @param text 要检查的文本
     * @param blockedPhrases 屏蔽短语列表（空格分隔的关键词）
     * @param threshold 相似度阈值，默认0.3
     * @return 匹配的短语及其相似度列表，按相似度降序排列
     */
    suspend fun checkBlockedPhrases(
        text: String,
        blockedPhrases: List<String>,
        threshold: Double = 0.3,
    ): List<Pair<String, Double>> = withContext(Dispatchers.Default) {
        if (text.isBlank() || blockedPhrases.isEmpty()) return@withContext emptyList()

        try {
            val matches = mutableListOf<Pair<String, Double>>()

            for (phrase in blockedPhrases) {
                val similarity = calculatePhraseMatchScore(text, phrase)
                if (similarity >= threshold) {
                    matches.add(Pair(phrase, similarity))
                }
            }

            // 按相似度降序排列
            matches.sortedByDescending { it.second }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 对文本进行分词
     */
    private fun segment(text: String): List<Term> = HanLP.segment(text)

    /**
     * 判断是否应该保留该词语（过滤停用词和标点）
     */
    private fun shouldKeepTerm(term: Term): Boolean {
        val nature = term.nature.toString()
        // 保留名词、动词、形容词等实词
        return nature.startsWith("n") ||
            // 名词
            nature.startsWith("v") ||
            // 动词
            nature.startsWith("a") ||
            // 形容词
            nature.startsWith("i") ||
            // 成语
            nature.startsWith("j") ||
            // 简称
            nature.startsWith("l") // 习用语
    }

    /**
     * 提取文本摘要（用于显示）
     * @param text 输入文本
     * @param maxLength 最大长度
     * @return 摘要文本
     */
    suspend fun extractSummary(text: String, maxLength: Int = 100): String = withContext(Dispatchers.Default) {
        if (text.length <= maxLength) return@withContext text

        try {
            // 使用HanLP的自动摘要功能
            val summary = HanLP.extractSummary(text, 1)
            if (summary.isNotEmpty()) {
                summary[0].take(maxLength)
            } else {
                text.take(maxLength) + "..."
            }
        } catch (e: Exception) {
            text.take(maxLength) + "..."
        }
    }
}
