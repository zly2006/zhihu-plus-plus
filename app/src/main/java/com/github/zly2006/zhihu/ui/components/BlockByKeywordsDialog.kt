package com.github.zly2006.zhihu.ui.components

import android.widget.Toast
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.nlp.BlockedKeywordRepository
import com.github.zly2006.zhihu.nlp.KeywordAnalyzer
import com.github.zly2006.zhihu.nlp.KeywordWithWeight
import kotlinx.coroutines.launch

/**
 * NLP关键词提取和屏蔽对话框
 * 从Feed内容中提取关键词，让用户选择要屏蔽的关键词
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlockByKeywordsDialog(
    showDialog: Boolean,
    feedTitle: String,
    feedExcerpt: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { BlockedKeywordRepository(context) }

    var extractedKeywords by remember { mutableStateOf<List<String>>(emptyList()) }
    var keywordInfoList by remember { mutableStateOf<List<KeywordWithWeight>>(emptyList()) }
    var selectedKeywords by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(false) }
    var isAdding by remember { mutableStateOf(false) }
    var showDetailDialog by remember { mutableStateOf(false) }

    // 提取关键词（使用KeywordAnalyzer，标题加权+去重+过滤）
    LaunchedEffect(showDialog, feedTitle, feedExcerpt) {
        if (showDialog) {
            isLoading = true
            try {
                // 使用KeywordAnalyzer提取关键词，自动处理标题加权、去重、过滤
                val keywordsWithWeight = KeywordAnalyzer.extractFromFeedWithWeight(
                    title = feedTitle,
                    excerpt = feedExcerpt,
                    content = null,
                    topN = 10,
                )

                keywordInfoList = keywordsWithWeight
                extractedKeywords = keywordsWithWeight.take(8).map { it.keyword } // 显示前8个用于选择

                // 默认选中前3个关键词
                selectedKeywords = extractedKeywords.take(3).toSet()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "提取关键词失败: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
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
                                    leadingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "已选中",
                                            )
                                        }
                                    } else {
                                        {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "添加",
                                            )
                                        }
                                    },
                                )
                            }
                        }

                        // 显示短语预览
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
                    // 详细信息按钮
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
                                isAdding = true
                                coroutineScope.launch {
                                    try {
                                        // 将选中的关键词用空格串联成一个短语
                                        val phrase = selectedKeywords.sorted().joinToString(" ")
                                        repository.addNLPPhrase(phrase)
                                        Toast
                                            .makeText(
                                                context,
                                                "已添加NLP屏蔽短语: $phrase",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        onConfirm()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast
                                            .makeText(
                                                context,
                                                "添加失败: ${e.message}",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                    } finally {
                                        isAdding = false
                                    }
                                }
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

    // 详细信息对话框
    if (showDetailDialog) {
        KeywordDetailDialog(
            keywordInfoList = keywordInfoList,
            feedTitle = feedTitle,
            feedExcerpt = feedExcerpt,
            onDismiss = { showDetailDialog = false },
        )
    }
}

/**
 * 关键词详细信息对话框
 * 显示前10个关键词及其真实权重
 */
@Composable
fun KeywordDetailDialog(
    keywordInfoList: List<KeywordWithWeight>,
    feedTitle: String,
    feedExcerpt: String?,
    onDismiss: () -> Unit,
) {
    // 找到最大权重用于归一化显示
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
                // Feed内容预览
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

                // 关键词列表
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

/**
 * 单个关键词信息项
 */
@Composable
fun KeywordInfoItem(
    index: Int,
    keywordInfo: KeywordWithWeight,
    maxWeight: Double,
) {
    // 归一化权重用于显示进度条（相对于最大权重）
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
                    String.format("%.4f", keywordInfo.weight),
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
