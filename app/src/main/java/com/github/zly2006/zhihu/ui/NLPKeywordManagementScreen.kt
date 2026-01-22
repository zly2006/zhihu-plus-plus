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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.nlp.BlockedKeywordRepository
import com.github.zly2006.zhihu.nlp.NLPService
import com.github.zly2006.zhihu.nlp.SentenceEmbeddingManager
import com.github.zly2006.zhihu.nlp.SentenceEmbeddingManager.ModelState
import com.github.zly2006.zhihu.viewmodel.filter.BlockedContentRecord
import com.github.zly2006.zhihu.viewmodel.filter.BlockedKeyword
import kotlinx.coroutines.launch

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
    val tabs = listOf("NLP短语管理", "被屏蔽记录")

    var blockedKeywords by remember { mutableStateOf<List<BlockedKeyword>>(emptyList()) }
    var blockedRecords by remember { mutableStateOf<List<BlockedContentRecord>>(emptyList()) }
    var extractedKeywords by remember { mutableStateOf<List<String>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var similarityThreshold by remember { mutableFloatStateOf(0.3f) }
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
                Toast.makeText(context, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(context, "请输入文本", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            isExtracting = true
                            try {
                                extractedKeywords = NLPService.extractKeywords(inputText, 10)
                                if (extractedKeywords.isEmpty()) {
                                    Toast.makeText(context, "未能提取到关键词", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "提取失败: ${e.message}", Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(context, "已添加短语: $phrase", Toast.LENGTH_SHORT).show()
                                loadData()
                                extractedKeywords = emptyList()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "添加失败: ${e.message}", Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                                loadData()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onClearAll = {
                        coroutineScope.launch {
                            try {
                                repository.clearAllKeywords()
                                Toast.makeText(context, "已清空所有NLP短语", Toast.LENGTH_SHORT).show()
                                loadData()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "清空失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modelState = modelState,
                    onLoadModel = {
                        coroutineScope.launch {
                            try {
                                SentenceEmbeddingManager.ensureModel(context)
                                Toast.makeText(context, "模型已加载", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "模型加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onUnloadModel = {
                        coroutineScope.launch {
                            SentenceEmbeddingManager.unload()
                            Toast.makeText(context, "已卸载模型", Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(context, "已删除记录", Toast.LENGTH_SHORT).show()
                                loadData()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onClearAll = {
                        coroutineScope.launch {
                            try {
                                repository.clearAllBlockedRecords()
                                Toast.makeText(context, "已清空所有记录", Toast.LENGTH_SHORT).show()
                                loadData()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "清空失败: ${e.message}", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(context, "已添加短语", Toast.LENGTH_SHORT).show()
                        loadData()
                        showAddDialog = false
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "添加失败: ${e.message}", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(context, "已更新短语", Toast.LENGTH_SHORT).show()
                        loadData()
                        showEditDialog = false
                        keywordToEdit = null
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "更新失败: ${e.message}", Toast.LENGTH_SHORT).show()
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
                        "Transformer 模型",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    val statusText = when (modelState) {
                        ModelState.Uninitialized -> "当前状态：未加载"
                        ModelState.Loading -> "当前状态：正在加载"
                        ModelState.Ready -> "当前状态：已就绪"
                        is ModelState.Error -> "当前状态：加载失败"
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
                            text = "提示：正在进行关键词任务，卸载暂不可用",
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
                            Text(if (modelState is ModelState.Loading) "加载中..." else "加载模型")
                        }
                        TextButton(
                            onClick = onUnloadModel,
                            enabled = modelState is ModelState.Ready && !isModelBusy,
                        ) {
                            Text("卸载模型")
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
                        "NLP智能屏蔽",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "基于自然语言处理技术，通过组合多个关键词成短语来表达与逻辑。例如：\"政治 敏感\" 将匹配同时包含这两个主题的内容。",
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
                        "相似度阈值: ${String.format("%.2f", similarityThreshold)}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = similarityThreshold,
                        onValueChange = onSimilarityThresholdChange,
                        valueRange = 0.1f..0.9f,
                        steps = 7,
                    )
                    Text(
                        "阈值越高，匹配越严格；阈值越低，匹配越宽松",
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
                        "提取关键词",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = onInputTextChange,
                        label = { Text("输入文本") },
                        placeholder = { Text("粘贴Feed内容或输入任意文本") },
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
                        Text("提取关键词")
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
                                "提取的关键词",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            TextButton(onClick = onAddAllKeywords) {
                                Text("组合为短语并添加")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "提示：所有关键词将用空格连接成一个短语",
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
                            "预览: ${extractedKeywords.joinToString(" ")}",
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
                            "已屏蔽短语 (${blockedKeywords.size})",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Row {
                            TextButton(onClick = onShowAddDialog) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("手动添加")
                            }
                            if (blockedKeywords.isNotEmpty()) {
                                TextButton(onClick = onClearAll) {
                                    Text("清空全部", color = MaterialTheme.colorScheme.error)
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
                        "暂无NLP屏蔽短语",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "在上方提取关键词或手动添加",
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
                            Text("包含 ${keywords.size} 个关键词")
                            Text(
                                "添加于 ${
                                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                                        .format(java.util.Date(keyword.createdTime))
                                }",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { onEditKeyword(keyword) }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "编辑",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            IconButton(onClick = { onDeleteKeyword(keyword) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "删除",
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
                            "最近100条被屏蔽记录",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        if (records.isNotEmpty()) {
                            TextButton(onClick = onClearAll) {
                                Text("清空全部", color = MaterialTheme.colorScheme.error)
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
                        "暂无屏蔽记录",
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
    val matchedKeywords = remember(record.matchedKeywords) {
        repository.parseMatchedKeywords(record.matchedKeywords)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        "${record.contentType} · ${record.authorName ?: "未知作者"}",
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
                        contentDescription = if (expanded) "收起" else "展开",
                    )
                }
            }

            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // 显示摘要
                if (!record.excerpt.isNullOrBlank()) {
                    Text(
                        "摘要",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        record.excerpt,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 显示匹配的关键词
                Text(
                    "匹配的关键词（前3个）",
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
                        Text(
                            match.keyword,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
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
                    Text("删除记录")
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
        title = { Text("手动添加NLP短语") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = phrase,
                    onValueChange = { phrase = it },
                    label = { Text("短语") },
                    placeholder = { Text("用空格分隔多个关键词，如：政治 敏感") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "提示：用空格分隔多个关键词来表达与逻辑。例如：\"政治 敏感\" 将匹配同时包含这两个主题的内容。",
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
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
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
        title = { Text("编辑NLP短语") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = phrase,
                    onValueChange = { phrase = it },
                    label = { Text("短语") },
                    placeholder = { Text("用空格分隔多个关键词") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "提示：用空格分隔多个关键词来表达与逻辑。",
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
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}
