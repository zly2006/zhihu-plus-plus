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

package com.github.zly2006.zhihu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.nlp.BlockedKeywordRepository
import com.github.zly2006.zhihu.shared.nlp.KeywordAnalyzerCore
import com.github.zly2006.zhihu.viewmodel.filter.AndroidContentFilterRuntime

/**
 * NLP关键词提取和屏蔽对话框
 * 从Feed内容中提取关键词，让用户选择要屏蔽的关键词
 */
@Composable
actual fun rememberBlockByKeywordsRuntime(): BlockByKeywordsRuntime {
    val context = LocalContext.current
    val repository = remember { BlockedKeywordRepository(context) }
    return remember(repository) {
        BlockByKeywordsRuntime(
            extractKeywords = { title, excerpt ->
                KeywordAnalyzerCore.extractFromFeedWithWeight(
                    title = title,
                    excerpt = excerpt,
                    content = null,
                    topN = 10,
                    extractor = AndroidContentFilterRuntime.keywordWeightExtractor,
                )
            },
            addNlpPhrase = { phrase ->
                repository.addNLPPhrase(phrase)
            },
        )
    }
}
