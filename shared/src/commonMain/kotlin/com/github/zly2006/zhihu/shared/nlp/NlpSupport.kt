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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun interface KeywordWeightExtractor {
    suspend fun extractKeywordsWithWeight(text: String, topN: Int): List<KeywordWithWeight>
}

object KeywordAnalyzerCore {
    suspend fun extractFromFeedWithWeight(
        title: String,
        excerpt: String? = null,
        content: String? = null,
        topN: Int = 10,
        extractor: KeywordWeightExtractor,
    ): List<KeywordWithWeight> = withContext(Dispatchers.Default) {
        if (title.isBlank()) return@withContext emptyList()

        runCatching {
            val weightedText = buildWeightedText(title, excerpt, content)
            val keywordsWithWeight = extractor.extractKeywordsWithWeight(weightedText, topN * 3)
            processKeywordsWithWeight(keywordsWithWeight, topN)
        }.getOrElse {
            emptyList()
        }
    }

    fun buildWeightedText(
        title: String,
        excerpt: String?,
        content: String?,
    ): String = buildString {
        repeat(3) {
            append(title)
            append(" ")
        }

        if (!excerpt.isNullOrBlank()) {
            append(excerpt)
            append(" ")
        }

        if (!content.isNullOrBlank()) {
            append(content.take(500))
        }
    }

    fun processKeywordsWithWeight(
        keywordsWithWeight: List<KeywordWithWeight>,
        topN: Int,
    ): List<KeywordWithWeight> {
        val keywordMap = mutableMapOf<String, Double>()
        for ((keyword, weight) in keywordsWithWeight) {
            val current = keywordMap[keyword] ?: 0.0
            if (weight > current) {
                keywordMap[keyword] = weight
            }
        }

        return keywordMap
            .asSequence()
            .filter { (keyword, _) -> keyword.length >= 2 }
            .filter { (keyword, _) -> !isStopWord(keyword) }
            .filter { (keyword, _) -> !isNumberOnly(keyword) }
            .map { (keyword, weight) -> KeywordWithWeight(keyword, weight) }
            .sortedByDescending { it.weight }
            .take(topN)
            .toList()
    }

    fun isStopWord(word: String): Boolean = word in stopWords

    fun isNumberOnly(word: String): Boolean = word.all { it.isDigit() }

    private val stopWords = setOf(
        "的",
        "了",
        "在",
        "是",
        "我",
        "有",
        "和",
        "就",
        "不",
        "人",
        "都",
        "一",
        "一个",
        "上",
        "也",
        "很",
        "到",
        "说",
        "要",
        "去",
        "你",
        "会",
        "着",
        "没有",
        "看",
        "好",
        "自己",
        "这",
        "个",
        "们",
        "那",
        "来",
        "么",
        "吗",
        "时",
        "大",
        "地",
        "为",
        "子",
        "中",
        "你们",
        "对",
        "生",
        "能",
        "而",
        "得",
        "与",
        "她",
        "他",
        "以",
        "及",
        "于",
        "用",
        "就是",
        "比",
        "啊",
        "呢",
        "吧",
        "呀",
        "嘛",
        "哦",
        "哪",
        "什么",
        "怎么",
    )
}

data class KeywordWithWeight(
    val keyword: String,
    val weight: Double,
)

// 汇总调试信息，便于断点查看
data class NlpDebugTrace(
    val normalizedText: String,
    val candidateKeywords: List<String> = emptyList(),
    val segments: List<SegmentDebugInfo> = emptyList(),
    val mmrIterations: MutableList<KeywordSelectionDebug> = mutableListOf(),
    val selectedKeywords: MutableList<KeywordWithWeight> = mutableListOf(),
)

data class SegmentDebugInfo(
    val index: Int,
    val text: String,
    val length: Int,
    val embeddingDim: Int?,
    val embeddingPreview: String,
)

data class KeywordSelectionDebug(
    val iteration: Int,
    val candidate: String,
    val relevance: Double,
    val redundancy: Double,
    val score: Double,
)

data class SegmentMatchDebug(
    val index: Int,
    val segmentTextPreview: String,
    val segmentLength: Int,
    val embeddingDim: Int?,
    val score: Double?,
)

data class PhraseMatchResult(
    val phrase: String,
    val normalizedPhrase: String,
    val keywords: List<String>,
    val phraseEmbeddingDim: Int?,
    val bestScore: Double,
    val segmentDebug: List<SegmentMatchDebug>,
)
