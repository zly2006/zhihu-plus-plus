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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.shared.nlp.KeywordWithWeight
import kotlin.math.roundToInt

/**
 * 按关键词屏蔽的信息流辅助弹窗。
 *
 * 弹窗展示从标题和摘要中提取的关键词，默认选择前三个，允许用户组合成一个 NLP 屏蔽短语。它用于把 Feed 卡片上的一次性“屏蔽”
 * 操作转化为可维护的规则，因此需要同时展示加载态、添加态和关键词权重详情。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlockByKeywordsDialogContent(
    showDialog: Boolean,
    feedTitle: String,
    feedExcerpt: String?,
    extractedKeywords: List<String>,
    keywordInfoList: List<KeywordWithWeight>,
    isLoading: Boolean,
    isAdding: Boolean,
    onDismiss: () -> Unit,
    onConfirmPhrase: (String) -> Unit,
) {
    var selectedKeywords by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDetailDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showDialog, extractedKeywords) {
        if (showDialog) {
            selectedKeywords = extractedKeywords.take(3).toSet()
        } else {
            selectedKeywords = emptySet()
            showDetailDialog = false
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("按关键词屏蔽") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (isLoading) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("正在提取关键词...")
                        }
                    } else if (extractedKeywords.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "未能提取到关键词",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    } else {
                        Text(
                            "从内容中提取到以下关键词，选择要屏蔽的关键词：",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "提示：选中的关键词将用空格串联成一个短语进行NLP语义匹配",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            extractedKeywords.forEach { keyword ->
                                val isSelected = keyword in selectedKeywords
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedKeywords = if (isSelected) {
                                            selectedKeywords - keyword
                                        } else {
                                            selectedKeywords + keyword
                                        }
                                    },
                                    label = { Text(keyword) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Add,
                                            contentDescription = if (isSelected) "已选中" else "添加",
                                        )
                                    },
                                )
                            }
                        }

                        if (selectedKeywords.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                ) {
                                    Text(
                                        "短语预览",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        selectedKeywords.sorted().joinToString(" "),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "提示：基于NLP语义相似度，即使用词不同，主题相似的内容也会被过滤",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = { showDetailDialog = true },
                        enabled = keywordInfoList.isNotEmpty() && !isLoading,
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "详细信息",
                            modifier = Modifier.padding(end = 4.dp),
                        )
                        Text("详细信息")
                    }

                    Button(
                        onClick = {
                            if (selectedKeywords.isNotEmpty()) {
                                onConfirmPhrase(selectedKeywords.sorted().joinToString(" "))
                            }
                        },
                        enabled = selectedKeywords.isNotEmpty() && !isLoading && !isAdding,
                    ) {
                        if (isAdding) {
                            CircularProgressIndicator(
                                modifier = Modifier.width(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text("屏蔽 (${selectedKeywords.size})")
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isAdding,
                ) {
                    Text("取消")
                }
            },
        )
    }

    if (showDetailDialog) {
        KeywordDetailDialog(
            keywordInfoList = keywordInfoList,
            feedTitle = feedTitle,
            feedExcerpt = feedExcerpt,
            onDismiss = { showDetailDialog = false },
        )
    }
}

@Composable
fun KeywordDetailDialog(
    keywordInfoList: List<KeywordWithWeight>,
    feedTitle: String,
    feedExcerpt: String?,
    onDismiss: () -> Unit,
) {
    val maxWeight = keywordInfoList.maxOfOrNull { it.weight } ?: 1.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关键词详细信息") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Text(
                            "内容预览",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            feedTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        if (!feedExcerpt.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                feedExcerpt.take(100) + if (feedExcerpt.length > 100) "..." else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "提取的关键词（按重要性排序）",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))

                keywordInfoList.forEachIndexed { index, keywordInfo ->
                    KeywordInfoItem(
                        index = index + 1,
                        keywordInfo = keywordInfo,
                        maxWeight = maxWeight,
                    )
                    if (index < keywordInfoList.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "说明：权重值由TextRank算法计算得出，数值越高表示该关键词在内容中越重要",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
    )
}

@Composable
fun KeywordInfoItem(
    index: Int,
    keywordInfo: KeywordWithWeight,
    maxWeight: Double,
) {
    val normalizedWeight = if (maxWeight > 0) (keywordInfo.weight / maxWeight).toFloat() else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "#$index",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(end = 8.dp),
                    )
                    Text(
                        keywordInfo.keyword,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Text(
                    formatKeywordWeight(keywordInfo.weight),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { normalizedWeight },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

private fun formatKeywordWeight(weight: Double): String {
    val scaled = (weight * 10_000).roundToInt()
    val sign = if (scaled < 0) "-" else ""
    val absolute = kotlin.math.abs(scaled)
    val integer = absolute / 10_000
    val fraction = (absolute % 10_000).toString().padStart(4, '0')
    return "$sign$integer.$fraction"
}
