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

package com.github.zly2006.zhihu.viewmodel.filter

import com.github.zly2006.zhihu.shared.util.Log
import kotlinx.serialization.json.Json

fun interface KeywordSemanticMatcher {
    suspend fun checkBlockedPhrases(
        text: String,
        blockedPhrases: List<String>,
        threshold: Double,
    ): List<Pair<String, Double>>
}

/**
 * 屏蔽词核心服务。
 * 负责屏蔽词和 NLP 屏蔽记录的数据库语义；具体语义相似度算法通过 [semanticMatcher] 注入。
 */
class BlockedKeywordService(
    private val keywordDao: BlockedKeywordDao,
    private val recordDao: BlockedContentRecordDao,
    private val semanticMatcher: KeywordSemanticMatcher?,
) {
    /**
     * 检查内容快照是否应该被 NLP 语义屏蔽。
     * 使用标题、摘要和正文纯文本参与匹配。
     * @param title 标题
     * @param excerpt 摘要
     * @param content 正文内容，当前保留给后续扩展
     * @param threshold 相似度阈值，默认 0.8
     * @return Pair<是否屏蔽, 匹配的关键词列表>
     */
    suspend fun checkNLPBlockingWithWeight(
        title: String,
        excerpt: String?,
        content: String?,
        threshold: Double = 0.8,
    ): Pair<Boolean, List<MatchedKeywordInfo>> {
        if (title.isBlank()) return Pair(false, emptyList())

        val nlpKeywords = keywordDao
            .getAllKeywords()
            .filter { it.getKeywordTypeEnum() == KeywordType.NLP_SEMANTIC }
        if (nlpKeywords.isEmpty()) return Pair(false, emptyList())

        // 获取所有 NLP 短语
        val phrases = nlpKeywords.map { it.keyword }

        val weightedText = buildString {
            append(title)
            append(" ")
            if (!excerpt.isNullOrBlank()) {
                append(excerpt)
                append(" ")
            }
            if (!content.isNullOrBlank()) {
                append(content)
                append(" ")
            }
        }

        val matches = semanticMatcher?.checkBlockedPhrases(weightedText, phrases, threshold)
            ?: emptyList()

        val matchedInfos = matches.map { (phrase, similarity) ->
            MatchedKeywordInfo(phrase, similarity)
        }

        return Pair(matches.isNotEmpty(), matchedInfos)
    }

    /**
     * 记录被NLP屏蔽的内容
     */
    suspend fun recordBlockedContent(
        contentId: String,
        contentType: String,
        title: String,
        excerpt: String?,
        authorName: String?,
        authorId: String?,
        matchedKeywords: List<MatchedKeywordInfo>,
    ) {
        try {
            val top3Matches = matchedKeywords.sortedByDescending { it.similarity }.take(3)
            val record = BlockedContentRecord(
                contentId = contentId,
                contentType = contentType,
                title = title,
                excerpt = excerpt ?: "",
                authorName = authorName,
                authorId = authorId,
                blockReason = "NLP语义匹配",
                matchedKeywords = Json.encodeToString(
                    kotlinx.serialization.builtins.ListSerializer(MatchedKeywordInfo.serializer()),
                    top3Matches,
                ),
            )
            recordDao.insertRecord(record)
            // 维护记录数量限制
            recordDao.maintainRecordLimit()
        } catch (e: Exception) {
            Log.e("BlockedKeywordService", "Failed to save blocked content record", e)
        }
    }
}
