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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.nlp.BlockedKeywordRepository
import com.github.zly2006.zhihu.shared.nlp.KeywordAnalyzerCore
import com.github.zly2006.zhihu.shared.nlp.KeywordWithWeight
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.viewmodel.filter.AndroidContentFilterRuntime
import kotlinx.coroutines.launch

/**
 * NLP关键词提取和屏蔽对话框
 * 从Feed内容中提取关键词，让用户选择要屏蔽的关键词
 */
@Composable
actual fun BlockByKeywordsDialog(
    showDialog: Boolean,
    feedTitle: String,
    feedExcerpt: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val context = LocalContext.current
    val userMessages = rememberUserMessageSink()
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { BlockedKeywordRepository(context) }

    var extractedKeywords by remember { mutableStateOf<List<String>>(emptyList()) }
    var keywordInfoList by remember { mutableStateOf<List<KeywordWithWeight>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isAdding by remember { mutableStateOf(false) }

    LaunchedEffect(showDialog, feedTitle, feedExcerpt) {
        if (showDialog) {
            isLoading = true
            try {
                val keywordsWithWeight = KeywordAnalyzerCore.extractFromFeedWithWeight(
                    title = feedTitle,
                    excerpt = feedExcerpt,
                    content = null,
                    topN = 10,
                    extractor = AndroidContentFilterRuntime.keywordWeightExtractor,
                )

                keywordInfoList = keywordsWithWeight
                extractedKeywords = keywordsWithWeight.take(8).map { it.keyword }
            } catch (e: Exception) {
                e.printStackTrace()
                userMessages.showShortMessage("提取关键词失败: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    BlockByKeywordsDialogContent(
        showDialog = showDialog,
        feedTitle = feedTitle,
        feedExcerpt = feedExcerpt,
        extractedKeywords = extractedKeywords,
        keywordInfoList = keywordInfoList,
        isLoading = isLoading,
        isAdding = isAdding,
        onDismiss = onDismiss,
        onConfirmPhrase = { phrase ->
            isAdding = true
            coroutineScope.launch {
                try {
                    repository.addNLPPhrase(phrase)
                    userMessages.showShortMessage("已添加NLP屏蔽短语: $phrase")
                    onConfirm()
                } catch (e: Exception) {
                    e.printStackTrace()
                    userMessages.showShortMessage("添加失败: ${e.message}")
                } finally {
                    isAdding = false
                }
            }
        },
    )
}
