package com.github.zly2006.zhihu.nlp

import com.hankcs.hanlp.HanLP
import com.hankcs.hanlp.seg.common.Term
import com.hankcs.hanlp.summary.TextRankKeyword
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

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
    /**
     * 从文本中提取关键词
     * @param text 输入文本
     * @param topN 返回前N个关键词，默认5个
     * @return 关键词列表，按重要性排序
     */
    suspend fun extractKeywords(text: String, topN: Int = 5): List<String> = withContext(Dispatchers.Default) {
        if (text.isBlank()) return@withContext emptyList()

        try {
            // 使用HanLP的TextRank算法提取关键词
            val keywords = HanLP.extractKeyword(text, topN)
            keywords
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 从文本中提取关键词及其权重
     * @param text 输入文本
     * @param topN 返回前N个关键词，默认5个
     * @return 关键词及权重列表，按重要性排序
     */
    suspend fun extractKeywordsWithWeight(text: String, topN: Int = 5): List<KeywordWithWeight> = withContext(Dispatchers.Default) {
        if (text.isBlank()) return@withContext emptyList()

        try {
            // 使用TextRankKeyword获取关键词和权重
            val textRankKeyword = TextRankKeyword()
            val termAndRankList = textRankKeyword.getTermAndRank(text, topN)

            // 转换为KeywordWithWeight列表
            termAndRankList.map { (term, weight) ->
                KeywordWithWeight(term, weight.toDouble())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
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

        try {
            // 分词
            val terms1 = segment(text1)
            val terms2 = segment(text2)

            // 提取词语（过滤停用词和标点）
            val words1 = terms1.filter { shouldKeepTerm(it) }.map { it.word }.toSet()
            val words2 = terms2.filter { shouldKeepTerm(it) }.map { it.word }.toSet()

            if (words1.isEmpty() || words2.isEmpty()) return@withContext 0.0

            // 计算Jaccard相似度
            val intersection = words1.intersect(words2).size
            val union = words1.union(words2).size

            intersection.toDouble() / union.toDouble()
        } catch (e: Exception) {
            e.printStackTrace()
            0.0
        }
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

        try {
            // 将短语拆分为独立关键词
            val keywords = phraseKeywords
                .split("\\s+".toRegex())
                .filter { it.isNotBlank() }
                .distinct() // 去重
            HanLP
                .segment("")
                .first()
                .word
            if (keywords.isEmpty()) return@withContext 0.0

            // 提取段落的关键词（提取更多用于匹配）
            val paragraphKeywords = extractKeywords(paragraphText, min(30, paragraphText.length / 10)).toSet()

            // 对段落进行分词，获取所有实词
            val paragraphTerms = segment(paragraphText)
            val paragraphWords = paragraphTerms
                .filter { shouldKeepTerm(it) }
                .map { it.word }
                .toSet()

            // 计算每个短语关键词的匹配分数
            var totalScore = 0.0
            var matchCount = 0

            for (keyword in keywords) {
                var keywordScore = 0.0

                // 1. 精确匹配（权重最高）
                if (paragraphWords.contains(keyword)) {
                    keywordScore = max(keywordScore, 1.0)
                }

                // 2. 子串匹配（考虑大小写）
                if (paragraphText.contains(keyword, ignoreCase = true)) {
                    keywordScore = max(keywordScore, 0.85)
                }

                // 3. 关键词提取匹配
                if (paragraphKeywords.contains(keyword)) {
                    keywordScore = max(keywordScore, 0.95)
                }

                // 4. 模糊匹配（词语相似性）
                for (word in paragraphWords) {
                    if (word.length >= 2 && keyword.length >= 2) {
                        if (word.contains(keyword) || keyword.contains(word)) {
                            val similarity = calculateWordSimilarity(keyword, word)
                            keywordScore = max(keywordScore, similarity * 0.75)
                        }
                    }
                }

                // 5. 同义词或相关词匹配（基于关键词列表的交集）
                val keywordRelatedWords = paragraphKeywords.filter {
                    it.contains(keyword) || keyword.contains(it)
                }
                if (keywordRelatedWords.isNotEmpty()) {
                    keywordScore = max(keywordScore, 0.7)
                }

                if (keywordScore > 0) {
                    matchCount++
                    totalScore += keywordScore
                }
            }

            // 计算最终相似度：考虑匹配到的关键词比例和平均匹配分数
            if (matchCount == 0) return@withContext 0.0

            val matchRatio = matchCount.toDouble() / keywords.size
            val avgScore = totalScore / keywords.size

            // 综合评分：如果所有关键词都匹配到，给予更高的权重
            when {
                matchCount == keywords.size -> {
                    // 全部匹配：平均分数占70%，匹配比例占30%
                    avgScore * 0.7 + matchRatio * 0.3
                }
                matchCount >= keywords.size * 0.7 -> {
                    // 大部分匹配（>=70%）：平均分数占60%，匹配比例占40%
                    avgScore * 0.6 + matchRatio * 0.4
                }
                else -> {
                    // 少部分匹配：平均分数占50%，匹配比例占50%
                    avgScore * 0.5 + matchRatio * 0.5
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0.0
        }
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
