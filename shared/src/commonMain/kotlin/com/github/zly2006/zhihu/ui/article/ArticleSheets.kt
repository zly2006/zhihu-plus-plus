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

package com.github.zly2006.zhihu.ui.article

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.ui.components.MyModalBottomSheet
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArticleSummarySheet(
    showDialog: Boolean,
    summaryText: String,
    loading: Boolean,
    errorMessage: String?,
    onDismissRequest: () -> Unit,
    onRetryRequest: () -> Unit,
) {
    if (!showDialog) return
    val scrollState = rememberScrollState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    MyModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(scrollState),
        ) {
            Text("总结本文", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                if (loading && summaryText.isBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("正在生成总结...")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (summaryText.isNotBlank()) {
                    SelectionContainer { Text(summaryText) }
                }
                if (!errorMessage.isNullOrBlank()) {
                    if (summaryText.isNotBlank()) Spacer(modifier = Modifier.height(12.dp))
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismissRequest) { Text("关闭") }
                Spacer(modifier = Modifier.width(8.dp))
                if (!loading) {
                    TextButton(onClick = onRetryRequest) { Text("重新总结") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AigcFlagSheet(
    showDialog: Boolean,
    viewModel: ArticleViewModel,
    onDismissRequest: () -> Unit,
    onSubmitRequest: () -> Unit,
) {
    if (!showDialog) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    MyModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val canSubmitAigcFlag = viewModel.aigcVoteAvailable &&
                !viewModel.aigcVoteLoading &&
                !viewModel.aigcFlagged &&
                viewModel.aigcVoterName.isNotBlank() &&
                (
                    viewModel.aigcCreditBypassAvailable ||
                        (viewModel.aigcVoteCredit > 0 && viewModel.isAigcFlagEvidenceReady())
                )
            Text(
                text = "标记疑似 AIGC",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "每浏览 20 篇内容获得 1 点投票积分，最多保留 ${viewModel.aigcVoteCap} 点。标记会上传当前正文 HTML、编辑时间和投票人身份，服务端按内容版本统计。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (!viewModel.aigcVoteAvailable) {
                    "AIGC 标记未启用"
                } else if (viewModel.aigcVoterName.isBlank()) {
                    "未登录，无法记名投票"
                } else {
                    "投票人：${viewModel.aigcVoterName}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (viewModel.aigcCreditBypassAvailable) {
                    "积分 ${viewModel.aigcVoteCredit}/${viewModel.aigcVoteCap} · 当前账号可免积分标记"
                } else {
                    "积分 ${viewModel.aigcVoteCredit}/${viewModel.aigcVoteCap} · 进度 ${viewModel.aigcVoteProgress}/20"
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (viewModel.aigcEffectiveFlagCount > 0) {
                    "已有 ${viewModel.aigcEffectiveFlagCount} 个有效标记"
                } else {
                    "当前还没有有效标记"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (viewModel.aigcNamedVoters.isNotEmpty()) {
                Text(
                    text = "记名投票：" + viewModel.aigcNamedVoters.joinToString("、") { voter ->
                        if (voter.creditBypassed) "${voter.voterName}（免积分）" else voter.voterName
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            viewModel.aigcVoteError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismissRequest) { Text("关闭") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onSubmitRequest,
                    enabled = canSubmitAigcFlag,
                ) {
                    Text(
                        when {
                            !viewModel.aigcVoteAvailable -> "未启用"
                            viewModel.aigcFlagged -> "已标记"
                            viewModel.aigcVoteLoading -> "提交中"
                            viewModel.aigcVoterName.isBlank() -> "需登录"
                            viewModel.aigcCreditBypassAvailable -> "免积分标记"
                            viewModel.aigcVoteCredit <= 0 -> "积分不足"
                            !viewModel.isAigcFlagEvidenceReady() -> "继续阅读"
                            else -> "消耗 1 点标记"
                        },
                    )
                }
            }
        }
    }
}
