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

package com.github.zly2006.zhihu.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.nlp.BlockedKeywordRepository
import com.github.zly2006.zhihu.nlp.ModelState
import com.github.zly2006.zhihu.nlp.NLPService
import com.github.zly2006.zhihu.nlp.SentenceEmbeddingManager
import com.github.zly2006.zhihu.viewmodel.filter.BlockedContentRecord
import com.github.zly2006.zhihu.viewmodel.filter.BlockedKeyword
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterExtensions
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * NLP关键词管理界面
 * 支持从文本中提取关键词，并添加到屏蔽列表
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NLPKeywordManagementScreen(
    innerPadding: PaddingValues,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { BlockedKeywordRepository(context) }
    val modelState by SentenceEmbeddingManager.state.collectAsState()

    var isExtracting by remember { mutableStateOf(false) }

    LaunchedEffect(context) {
        SentenceEmbeddingManager.setDefaultContext(context.applicationContext)
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(stringResource(R.string.nlp_phrase_management), stringResource(R.string.blocked_records))

    var blockedKeywords by remember { mutableStateOf<List<BlockedKeyword>>(emptyList()) }
    var blockedRecords by remember { mutableStateOf<List<BlockedContentRecord>>(emptyList()) }
    var extractedKeywords by remember { mutableStateOf<List<String>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var similarityThreshold by remember { mutableFloatStateOf(ContentFilterExtensions.getNLPSimilarityThreshold(context).toFloat()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var keywordToEdit by remember { mutableStateOf<BlockedKeyword?>(null) }
    var phraseInput by remember { mutableStateOf("") }

    // 加载已屏蔽的关键词和记录
    fun loadData() {
        coroutineScope.launch {
            try {
                blockedKeywords = repository.getNLPSemanticKeywords()
                blockedRecords = repository.getRecentBlockedRecords(100)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, context.getString(R.string.load_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Scaffold { scaffoldPadding ->
        Column(
            modifier = Modifier
                .padding(scaffoldPadding)
                .padding(innerPadding)
                .fillMaxWidth(),
        ) {
            // 标签页
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                    )
                }
            }

            when (selectedTab) {
                0 -> NLPPhraseManagementTab(
                    blockedKeywords = blockedKeywords,
                    extractedKeywords = extractedKeywords,
                    inputText = inputText,
                    similarityThreshold = similarityThreshold,
                    onInputTextChange = { inputText = it },
                    onSimilarityThresholdChange = { similarityThreshold = it },
                    onExtractKeywords = {
                        coroutineScope.launch {
                            if (inputText.isBlank()) {
                                Toast.makeText(context, context.getString(R.string.input_text_required), Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            isExtracting = true
                            try {
                                extractedKeywords = NLPService.extractKeywords(inputText, 10)
                                if (extractedKeywords.isEmpty()) {
                                    Toast.makeText(context, context.getString(R.string.no_keywords_extracted), Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, context.getString(R.string.keyword_extract_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
                            } finally {
                                isExtracting = false
                            }
                        }
                    },
                    onAddAllKeywords = {
                        coroutineScope.launch {
                            try {
                                // 将所有关键词组合成一个短语
                                val phrase = extractedKeywords.joinToString(" ")
                                repository.addNLPPhrase(phrase)
                                Toast.makeText(context, context.getString(R.string.nlp_phrase_added, phrase), Toast.LENGTH_SHORT).show()
                                loadData()
                                extractedKeywords = emptyList()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, context.getString(R.string.add_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onShowAddDialog = { showAddDialog = true },
                    onEditKeyword = { keyword ->
                        keywordToEdit = keyword
                        phraseInput = keyword.keyword
                        showEditDialog = true
                    },
                    onDeleteKeyword = { keyword ->
                        coroutineScope.launch {
                            try {
                                repository.deleteKeyword(keyword)
                                Toast.makeText(context, context.getString(R.string.deleted), Toast.LENGTH_SHORT).show()
                                loadData()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, context.getString(R.string.delete_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onClearAll = {
                        coroutineScope.launch {
                            try {
                                repository.clearAllKeywords()
                                Toast.makeText(context, context.getString(R.string.all_nlp_phrases_cleared), Toast.LENGTH_SHORT).show()
                                loadData()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, context.getString(R.string.clear_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modelState = modelState,
                    onLoadModel = {
                        coroutineScope.launch {
                            try {
                                SentenceEmbeddingManager.ensureModel(context)
                                Toast.makeText(context, context.getString(R.string.model_loaded), Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, context.getString(R.string.model_load_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onUnloadModel = {
                        coroutineScope.launch {
                            SentenceEmbeddingManager.unload()
                            Toast.makeText(context, context.getString(R.string.model_unloaded), Toast.LENGTH_SHORT).show()
                        }
                    },
                    isModelBusy = isExtracting,
                )
                1 -> BlockedRecordsTab(
                    records = blockedRecords,
                    repository = repository,
                    onDeleteRecord = { record ->
                        coroutineScope.launch {
                            try {
                                repository.deleteBlockedRecord(record.id)
                                Toast.makeText(context, context.getString(R.string.record_deleted), Toast.LENGTH_SHORT).show()
                                loadData()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, context.getString(R.string.delete_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onClearAll = {
                        coroutineScope.launch {
                            try {
                                repository.clearAllBlockedRecords()
                                Toast.makeText(context, context.getString(R.string.all_records_cleared), Toast.LENGTH_SHORT).show()
                                loadData()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, context.getString(R.string.clear_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                )
            }
        }
    }

    // 手动添加短语对话框
    if (showAddDialog) {
        AddPhraseDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { phrase ->
                coroutineScope.launch {
                    try {
                        repository.addNLPPhrase(phrase)
                        Toast.makeText(context, context.getString(R.string.phrase_added), Toast.LENGTH_SHORT).show()
                        loadData()
                        showAddDialog = false
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, context.getString(R.string.add_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
                    }
                }
            },
        )
    }

    // 编辑短语对话框
    if (showEditDialog && keywordToEdit != null) {
        EditPhraseDialog(
            initialPhrase = phraseInput,
            onDismiss = {
                showEditDialog = false
                keywordToEdit = null
            },
            onConfirm = { newPhrase ->
                coroutineScope.launch {
                    try {
                        val updated = keywordToEdit!!.copy(keyword = newPhrase.trim())
                        repository.updateKeyword(updated)
                        Toast.makeText(context, context.getString(R.string.phrase_updated), Toast.LENGTH_SHORT).show()
                        loadData()
                        showEditDialog = false
                        keywordToEdit = null
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, context.getString(R.string.update_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NLPPhraseManagementTab(
    blockedKeywords: List<BlockedKeyword>,
    extractedKeywords: List<String>,
    inputText: String,
    similarityThreshold: Float,
    onInputTextChange: (String) -> Unit,
    onSimilarityThresholdChange: (Float) -> Unit,
    onExtractKeywords: () -> Unit,
    onAddAllKeywords: () -> Unit,
    onShowAddDialog: () -> Unit,
    onEditKeyword: (BlockedKeyword) -> Unit,
    onDeleteKeyword: (BlockedKeyword) -> Unit,
    onClearAll: () -> Unit,
    modelState: ModelState,
    onLoadModel: () -> Unit,
    onUnloadModel: () -> Unit,
    isModelBusy: Boolean,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        stringResource(R.string.transformer_model),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    val statusText = when (modelState) {
                        ModelState.Uninitialized -> stringResource(R.string.model_status_not_loaded)
                        ModelState.Loading -> stringResource(R.string.model_status_loading)
                        ModelState.Ready -> stringResource(R.string.model_status_ready)
                        is ModelState.Error -> stringResource(R.string.model_status_load_failed)
                        is ModelState.Downloading -> stringResource(R.string.model_status_downloading, (modelState.progress * 100).toInt())
                    }
                    Text(statusText, style = MaterialTheme.typography.bodyMedium)
                    if (modelState is ModelState.Error) {
                        Text(
                            text = modelState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (isModelBusy) {
                        Text(
                            text = stringResource(R.string.model_busy_unload_disabled),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = onLoadModel,
                            enabled = modelState !is ModelState.Loading && modelState !is ModelState.Ready,
                        ) {
                            Text(if (modelState is ModelState.Loading) stringResource(R.string.loading) else stringResource(R.string.load_model))
                        }
                        TextButton(
                            onClick = onUnloadModel,
                            enabled = modelState is ModelState.Ready && !isModelBusy,
                        ) {
                            Text(stringResource(R.string.unload_model))
                        }
                    }
                }
            }
        }

        // 说明卡片
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        stringResource(R.string.nlp_smart_blocking),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.nlp_smart_blocking_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        // 相似度阈值设置
        item {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        stringResource(R.string.similarity_threshold_value, String.format("%.2f", similarityThreshold)),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = similarityThreshold,
                        onValueChange = onSimilarityThresholdChange,
                        valueRange = 0.1f..0.9f,
                        steps = 15,
                    )
                    Text(
                        stringResource(R.string.similarity_threshold_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // 文本输入和关键词提取
        item {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        stringResource(R.string.extract_keywords),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = onInputTextChange,
                        label = { Text(stringResource(R.string.input_text)) },
                        placeholder = { Text(stringResource(R.string.input_text_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onExtractKeywords,
                        enabled = inputText.isNotBlank(),
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(stringResource(R.string.extract_keywords))
                    }
                }
            }
        }

        // 提取的关键词
        if (extractedKeywords.isNotEmpty()) {
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.extracted_keywords),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            TextButton(onClick = onAddAllKeywords) {
                                Text(stringResource(R.string.combine_phrase_add))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.nlp_join_keywords_tip),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            extractedKeywords.forEach { keyword ->
                                FilterChip(
                                    selected = true,
                                    onClick = { },
                                    label = { Text(keyword) },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.preview_format, extractedKeywords.joinToString(" ")),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        // 已屏蔽的短语列表
        item {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.blocked_phrases_count, blockedKeywords.size),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Row {
                            TextButton(onClick = onShowAddDialog) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.manual_add))
                            }
                            if (blockedKeywords.isNotEmpty()) {
                                TextButton(onClick = onClearAll) {
                                    Text(stringResource(R.string.clear_all), color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (blockedKeywords.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.no_nlp_phrases),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.nlp_phrases_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(blockedKeywords) { keyword ->
                ListItem(
                    headlineContent = {
                        Text(
                            keyword.keyword,
                            fontWeight = FontWeight.Medium,
                        )
                    },
                    supportingContent = {
                        Column {
                            val keywords = keyword.getNLPKeywords()
                            val addedAt = SimpleDateFormat(
                                "yyyy-MM-dd HH:mm",
                                Locale.getDefault(),
                            ).format(Date(keyword.createdTime))
                            Text(stringResource(R.string.contains_keyword_count, keywords.size))
                            Text(
                                stringResource(
                                    R.string.added_at,
                                    addedAt,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { onEditKeyword(keyword) }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.edit),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            IconButton(onClick = { onDeleteKeyword(keyword) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun BlockedRecordsTab(
    records: List<BlockedContentRecord>,
    repository: BlockedKeywordRepository,
    onDeleteRecord: (BlockedContentRecord) -> Unit,
    onClearAll: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.recent_blocked_records),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        if (records.isNotEmpty()) {
                            TextButton(onClick = onClearAll) {
                                Text(stringResource(R.string.clear_all), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        if (records.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.no_blocked_feed_records),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(records) { record ->
                BlockedRecordItem(
                    record = record,
                    repository = repository,
                    onDelete = { onDeleteRecord(record) },
                )
            }
        }
    }
}

@Composable
fun BlockedRecordItem(
    record: BlockedContentRecord,
    repository: BlockedKeywordRepository,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val unknownAuthor = stringResource(R.string.unknown_author)
    val matchedKeywords = remember(record.matchedKeywords) {
        repository.parseMatchedKeywords(record.matchedKeywords)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .clickable { expanded = !expanded }
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        record.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.record_content_author, record.contentType, record.authorName ?: unknownAuthor),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        java.text
                            .SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                            .format(java.util.Date(record.blockedTime)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                    )
                }
            }

            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // 显示摘要
                if (!record.excerpt.isNullOrBlank()) {
                    Text(
                        stringResource(R.string.summary),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    SelectionContainer {
                        Text(
                            record.excerpt,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 显示匹配的关键词
                Text(
                    stringResource(R.string.matched_keywords_top3),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                matchedKeywords.forEach { match ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        SelectionContainer {
                            Text(
                                match.keyword,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Text(
                            String.format("%.2f%%", match.similarity * 100),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onDelete,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.delete_record))
                }
            }
        }
    }
}

@Composable
fun AddPhraseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var phrase by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_nlp_phrase_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = phrase,
                    onValueChange = { phrase = it },
                    label = { Text(stringResource(R.string.phrase)) },
                    placeholder = { Text(stringResource(R.string.phrase_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.add_phrase_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (phrase.isNotBlank()) {
                        onConfirm(phrase)
                    }
                },
                enabled = phrase.isNotBlank(),
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
fun EditPhraseDialog(
    initialPhrase: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var phrase by remember { mutableStateOf(initialPhrase) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_nlp_phrase_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = phrase,
                    onValueChange = { phrase = it },
                    label = { Text(stringResource(R.string.phrase)) },
                    placeholder = { Text(stringResource(R.string.phrase_placeholder_short)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.edit_phrase_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (phrase.isNotBlank()) {
                        onConfirm(phrase)
                    }
                },
                enabled = phrase.isNotBlank(),
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
