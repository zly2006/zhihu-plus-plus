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

package com.github.zly2006.zhihu.nlp

import com.github.zly2006.zhihu.nlp.NLPService
import com.github.zly2006.zhihu.shared.nlp.KeywordAnalyzerCore
import com.github.zly2006.zhihu.shared.nlp.KeywordWithWeight

/**
 * 关键词分析器
 * 提供完整的关键词提取、去重、过滤逻辑
 */
object KeywordAnalyzer {
    /**
     * 从Feed内容中提取关键词及权重
     * @param title 标题（必填）
     * @param excerpt 摘要
     * @param content 正文内容
     * @param topN 返回前N个关键词
     * @return 去重和过滤后的关键词及权重列表
     */
    suspend fun extractFromFeedWithWeight(
        title: String,
        excerpt: String? = null,
        content: String? = null,
        topN: Int = 10,
    ): List<KeywordWithWeight> = KeywordAnalyzerCore.extractFromFeedWithWeight(
        title = title,
        excerpt = excerpt,
        content = content,
        topN = topN,
        extractor = NLPService::extractKeywordsWithWeight,
    )
}
