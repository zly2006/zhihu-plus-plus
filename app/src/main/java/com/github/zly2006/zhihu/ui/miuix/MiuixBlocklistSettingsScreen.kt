/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.ui.BlocklistSettingsTestConfig
import com.github.zly2006.zhihu.ui.BlocklistSettingsTestTags
import com.github.zly2006.zhihu.ui.NLPKeywordManagementScreen
import com.github.zly2006.zhihu.viewmodel.filter.BlockedKeyword
import com.github.zly2006.zhihu.viewmodel.filter.BlockedTopic
import com.github.zly2006.zhihu.viewmodel.filter.BlockedUser
import com.github.zly2006.zhihu.viewmodel.filter.BlocklistManager
import com.github.zly2006.zhihu.viewmodel.filter.BlocklistStats
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * miuix 风格的屏蔽设置页。
 */
@Composable
fun MiuixBlocklistSettingsScreen(
    testConfig: BlocklistSettingsTestConfig? = null,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("关键词", "NLP", "用户", "主题")

    var loadedBlockedKeywords by remember { mutableStateOf<List<BlockedKeyword>>(emptyList()) }
    var loadedBlockedUsers by remember { mutableStateOf<List<BlockedUser>>(emptyList()) }
    var loadedBlockedTopics by remember { mutableStateOf<List<BlockedTopic>>(emptyList()) }
    var loadedStats by remember { mutableStateOf<BlocklistStats?>(null) }

    val blockedKeywords = testConfig?.blockedKeywords ?: loadedBlockedKeywords
    val blockedUsers = testConfig?.blockedUsers ?: loadedBlockedUsers
    val blockedTopics = testConfig?.blockedTopics ?: loadedBlockedTopics
    val stats = testConfig?.stats ?: loadedStats

    var showAddKeywordForm by remember { mutableStateOf(false) }
    var showAddUserForm by remember { mutableStateOf(false) }
    var showAddTopicForm by remember { mutableStateOf(false) }

    fun loadData() {
        coroutineScope.launch {
            try {
                val blocklistManager = BlocklistManager.getInstance(context)
                loadedBlockedKeywords = blocklistManager.getAllBlockedKeywords()
                    .filter { it.getKeywordTypeEnum() == com.github.zly2006.zhihu.viewmodel.filter.KeywordType.EXACT_MATCH }
                loadedBlockedUsers = blocklistManager.getAllBlockedUsers()
                loadedBlockedTopics = blocklistManager.getAllBlockedTopics()
                loadedStats = blocklistManager.getBlocklistStats()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "加载数据失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val blocklistManager = BlocklistManager.getInstance(context)
                    val summary = blocklistManager.importAllBlocklistFromJson(context, uri)
                    Toast.makeText(context, "导入成功：$summary", Toast.LENGTH_LONG).show()
                    loadData()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(testConfig) { if (testConfig == null) loadData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "屏蔽设置",
                navigationIcon = {
                    IconButton(onClick = { navigator.onNavigateBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag(BlocklistSettingsTestTags.ROOT),
        ) {
            // 统计卡片
            stats?.let { statsData ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .testTag(BlocklistSettingsTestTags.STATS_CARD),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("屏蔽统计", style = MiuixTheme.textStyles.title2)
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                StatItem("关键词", "${statsData.keywordCount}")
                                StatItem("用户", "${statsData.userCount}")
                                StatItem("主题", "${statsData.topicCount}")
                            }
                        }
                    }
                }
            }

            // 导入/导出
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    TextButton(
                        text = "导入规则",
                        modifier = Modifier.testTag(BlocklistSettingsTestTags.IMPORT_BUTTON),
                        onClick = {
                            val importAction = testConfig?.onImportRequested
                            if (importAction != null) importAction()
                            else importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                    )
                    TextButton(
                        text = "导出规则",
                        modifier = Modifier.testTag(BlocklistSettingsTestTags.EXPORT_BUTTON),
                        onClick = {
                            val exportAction = testConfig?.onExportRequested
                            if (exportAction != null) exportAction()
                            else coroutineScope.launch {
                                try {
                                    val mgr = BlocklistManager.getInstance(context)
                                    val file = mgr.exportAllBlocklistToJson(context)
                                    val intent = Intent().apply {
                                        action = Intent.ACTION_VIEW
                                        setDataAndType(
                                            FileProvider.getUriForFile(
                                                context, "${context.packageName}.provider", file,
                                            ), "application/json",
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(intent, "查看屏蔽规则"))
                                    Toast.makeText(context, "已导出", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                    )
                }
            }

            // 标签页
            item {
                TabRow(
                    tabs = tabs,
                    selectedTabIndex = selectedTab,
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.TAB_ROW),
                )
            }

            // 内容
            item {
                when (selectedTab) {
                    0 -> MiuixBlockedKeywordsList(
                        keywords = blockedKeywords,
                        onDelete = { kw -> handleDeleteKeyword(context, coroutineScope, testConfig, kw, ::loadData) },
                        onClearAll = { handleClearKeywords(context, coroutineScope, testConfig, ::loadData) },
                    )
                    1 -> {
                        val nlp = testConfig?.nlpContent
                        if (nlp != null) nlp(navigator.onNavigateBack)
                        else NLPKeywordManagementScreen(
                            innerPadding = PaddingValues(0.dp),
                            onNavigateBack = navigator.onNavigateBack,
                        )
                    }
                    2 -> MiuixBlockedUsersList(
                        users = blockedUsers,
                        onDelete = { u -> handleDeleteUser(context, coroutineScope, testConfig, u, ::loadData) },
                        onClearAll = { handleClearUsers(context, coroutineScope, testConfig, ::loadData) },
                        onNavigate = { u -> navigator.onNavigate(Person(u.userId, u.urlToken ?: "", u.userName)) },
                    )
                    3 -> MiuixBlockedTopicsList(
                        topics = blockedTopics,
                        onDelete = { t -> handleDeleteTopic(context, coroutineScope, testConfig, t, ::loadData) },
                        onClearAll = { handleClearTopics(context, coroutineScope, testConfig, ::loadData) },
                    )
                }
            }

            // 添加表单
            if (showAddKeywordForm && selectedTab == 0) {
                item {
                    MiuixAddKeywordForm(
                        onDismiss = { showAddKeywordForm = false },
                        onConfirm = { kw, cs, rx ->
                            handleAddKeyword(context, coroutineScope, testConfig, kw, cs, rx, ::loadData)
                            showAddKeywordForm = false
                        },
                    )
                }
            }
            if (showAddUserForm && selectedTab == 2) {
                item {
                    MiuixAddUserForm(
                        onDismiss = { showAddUserForm = false },
                        onConfirm = { id, name ->
                            handleAddUser(context, coroutineScope, testConfig, id, name, ::loadData)
                            showAddUserForm = false
                        },
                    )
                }
            }
            if (showAddTopicForm && selectedTab == 3) {
                item {
                    MiuixAddTopicForm(
                        onDismiss = { showAddTopicForm = false },
                        onConfirm = { id, name ->
                            handleAddTopic(context, coroutineScope, testConfig, id, name, ::loadData)
                            showAddTopicForm = false
                        },
                    )
                }
            }
        }
    }

    // FAB
    if (selectedTab == 0 || selectedTab == 2 || selectedTab == 3) {
        Box(modifier = Modifier.fillMaxSize()) {
            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag(BlocklistSettingsTestTags.FAB),
                onClick = {
                    when (selectedTab) {
                        0 -> showAddKeywordForm = true
                        2 -> showAddUserForm = true
                        3 -> showAddTopicForm = true
                    }
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加")
            }
        }
    }
}

// --- helpers ---

private fun handleAddKeyword(
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    config: BlocklistSettingsTestConfig?,
    keyword: String, caseSensitive: Boolean, isRegex: Boolean,
    reload: () -> Unit,
) {
    config?.onAddKeyword?.let { it(keyword, caseSensitive, isRegex); return }
    scope.launch {
        try {
            BlocklistManager.getInstance(context).addBlockedKeyword(keyword, caseSensitive, isRegex)
            Toast.makeText(context, "已添加", Toast.LENGTH_SHORT).show()
            reload()
        } catch (e: Exception) { e.printStackTrace(); Toast.makeText(context, "添加失败", Toast.LENGTH_SHORT).show() }
    }
}

private fun handleDeleteKeyword(
    context: android.content.Context, scope: kotlinx.coroutines.CoroutineScope,
    config: BlocklistSettingsTestConfig?, keyword: BlockedKeyword, reload: () -> Unit,
) {
    config?.onDeleteKeyword?.let { it(keyword); return }
    scope.launch {
        try {
            BlocklistManager.getInstance(context).removeBlockedKeyword(keyword.id)
            Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
            reload()
        } catch (e: Exception) { e.printStackTrace(); Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show() }
    }
}

private fun handleClearKeywords(
    context: android.content.Context, scope: kotlinx.coroutines.CoroutineScope,
    config: BlocklistSettingsTestConfig?, reload: () -> Unit,
) {
    config?.onClearKeywords?.let { it(); return }
    scope.launch {
        try { BlocklistManager.getInstance(context).clearAllBlockedKeywords(); reload() }
        catch (e: Exception) { e.printStackTrace() }
    }
}

private fun handleAddUser(
    context: android.content.Context, scope: kotlinx.coroutines.CoroutineScope,
    config: BlocklistSettingsTestConfig?, userId: String, userName: String, reload: () -> Unit,
) {
    config?.onAddUser?.let { it(userId, userName); return }
    scope.launch {
        try {
            BlocklistManager.getInstance(context).addBlockedUser(userId, userName)
            Toast.makeText(context, "已添加", Toast.LENGTH_SHORT).show()
            reload()
        } catch (e: Exception) { e.printStackTrace() }
    }
}

private fun handleDeleteUser(
    context: android.content.Context, scope: kotlinx.coroutines.CoroutineScope,
    config: BlocklistSettingsTestConfig?, user: BlockedUser, reload: () -> Unit,
) {
    config?.onDeleteUser?.let { it(user); return }
    scope.launch {
        try {
            BlocklistManager.getInstance(context).removeBlockedUser(user.userId)
            reload()
        } catch (e: Exception) { e.printStackTrace() }
    }
}

private fun handleClearUsers(
    context: android.content.Context, scope: kotlinx.coroutines.CoroutineScope,
    config: BlocklistSettingsTestConfig?, reload: () -> Unit,
) {
    config?.onClearUsers?.let { it(); return }
    scope.launch { try { BlocklistManager.getInstance(context).clearAllBlockedUsers(); reload() } catch (_: Exception) {} }
}

private fun handleAddTopic(
    context: android.content.Context, scope: kotlinx.coroutines.CoroutineScope,
    config: BlocklistSettingsTestConfig?, topicId: String, topicName: String, reload: () -> Unit,
) {
    config?.onAddTopic?.let { it(topicId, topicName); return }
    scope.launch {
        try {
            BlocklistManager.getInstance(context).addBlockedTopic(topicId, topicName)
            Toast.makeText(context, "已添加", Toast.LENGTH_SHORT).show()
            reload()
        } catch (e: Exception) { e.printStackTrace() }
    }
}

private fun handleDeleteTopic(
    context: android.content.Context, scope: kotlinx.coroutines.CoroutineScope,
    config: BlocklistSettingsTestConfig?, topic: BlockedTopic, reload: () -> Unit,
) {
    config?.onDeleteTopic?.let { it(topic); return }
    scope.launch {
        try { BlocklistManager.getInstance(context).removeBlockedTopic(topic.topicId); reload() }
        catch (e: Exception) { e.printStackTrace() }
    }
}

private fun handleClearTopics(
    context: android.content.Context, scope: kotlinx.coroutines.CoroutineScope,
    config: BlocklistSettingsTestConfig?, reload: () -> Unit,
) {
    config?.onClearTopics?.let { it(); return }
    scope.launch { try { BlocklistManager.getInstance(context).clearAllBlockedTopics(); reload() } catch (_: Exception) {} }
}

// --- sub-composables ---

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MiuixTheme.textStyles.footnote1)
        Text(value, style = MiuixTheme.textStyles.title1, color = MiuixTheme.colorScheme.primary)
    }
}

@Composable
private fun MiuixBlockedKeywordsList(
    keywords: List<BlockedKeyword>,
    onDelete: (BlockedKeyword) -> Unit,
    onClearAll: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        if (keywords.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = onClearAll,
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.KEYWORD_CLEAR_BUTTON),
                ) { Text("清空全部") }
            }
        }
        if (keywords.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("暂无精确匹配关键词", color = MiuixTheme.colorScheme.onSurfaceSecondary)
                Spacer(Modifier.height(8.dp))
                Text("点击 + 按钮添加", color = MiuixTheme.colorScheme.onSurfaceSecondary)
            }
        } else {
            LazyColumn(Modifier.testTag(BlocklistSettingsTestTags.KEYWORD_LIST)) {
                items(keywords, key = { it.id }) { kw ->
                    ArrowPreference(
                        modifier = Modifier.testTag(BlocklistSettingsTestTags.keywordItem(kw.id)),
                        title = kw.keyword,
                        summary = buildKwSummary(kw),
                        onClick = {},
                    )
                    Row(Modifier.fillMaxWidth().padding(end = 16.dp), horizontalArrangement = Arrangement.End) {
                        IconButton(
                            modifier = Modifier.testTag(BlocklistSettingsTestTags.keywordDelete(kw.id)),
                            onClick = { onDelete(kw) },
                        ) { Icon(Icons.Default.Delete, "删除", tint = MiuixTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}

private fun buildKwSummary(kw: BlockedKeyword): String {
    val opts = mutableListOf<String>()
    if (kw.caseSensitive) opts.add("区分大小写")
    if (kw.isRegex) opts.add("正则")
    return opts.ifEmpty { listOf("精确匹配") }.joinToString(" · ")
}

@Composable
private fun MiuixBlockedUsersList(
    users: List<BlockedUser>, onDelete: (BlockedUser) -> Unit,
    onClearAll: () -> Unit, onNavigate: (BlockedUser) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        if (users.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = onClearAll,
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.USER_CLEAR_BUTTON),
                ) { Text("清空全部") }
            }
        }
        if (users.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("暂无屏蔽用户", color = MiuixTheme.colorScheme.onSurfaceSecondary)
                Spacer(Modifier.height(8.dp))
                Text("点击 + 按钮添加", color = MiuixTheme.colorScheme.onSurfaceSecondary)
            }
        } else {
            LazyColumn(Modifier.testTag(BlocklistSettingsTestTags.USER_LIST)) {
                items(users, key = { it.userId }) { user ->
                    ArrowPreference(
                        modifier = Modifier
                            .testTag(BlocklistSettingsTestTags.userItem(user.userId))
                            .clickable { onNavigate(user) },
                        title = user.userName,
                        summary = "ID: ${user.userId}",
                        startAction = {
                            AsyncImage(user.avatarUrl, "头像",
                                modifier = Modifier.size(40.dp).clip(CircleShape))
                        },
                        onClick = {},
                    )
                    Row(Modifier.fillMaxWidth().padding(end = 16.dp), horizontalArrangement = Arrangement.End) {
                        IconButton(
                            modifier = Modifier.testTag(BlocklistSettingsTestTags.userDelete(user.userId)),
                            onClick = { onDelete(user) },
                        ) { Icon(Icons.Default.Delete, "删除", tint = MiuixTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiuixBlockedTopicsList(
    topics: List<BlockedTopic>, onDelete: (BlockedTopic) -> Unit, onClearAll: () -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        if (topics.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("暂无屏蔽主题", color = MiuixTheme.colorScheme.onSurfaceSecondary)
                Spacer(Modifier.height(8.dp))
                Text("点击 + 按钮添加", color = MiuixTheme.colorScheme.onSurfaceSecondary)
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = { showConfirm = true },
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.TOPIC_CLEAR_BUTTON),
                ) { Text("清空全部") }
            }
            LazyColumn(Modifier.testTag(BlocklistSettingsTestTags.TOPIC_LIST)) {
                items(topics, key = { it.topicId }) { topic ->
                    ArrowPreference(
                        modifier = Modifier.testTag(BlocklistSettingsTestTags.topicItem(topic.topicId)),
                        title = topic.topicName,
                        summary = "ID: ${topic.topicId}",
                        onClick = {},
                    )
                    Row(Modifier.fillMaxWidth().padding(end = 16.dp), horizontalArrangement = Arrangement.End) {
                        IconButton(
                            modifier = Modifier.testTag(BlocklistSettingsTestTags.topicDelete(topic.topicId)),
                            onClick = { onDelete(topic) },
                        ) { Icon(Icons.Default.Delete, "删除", tint = MiuixTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
    if (showConfirm) {
        MiuixConfirmDialog(
            title = "确认清空", text = "确定要清空所有屏蔽主题吗？此操作不可撤销。",
            confirmTag = BlocklistSettingsTestTags.TOPIC_CLEAR_CONFIRM,
            dismissTag = BlocklistSettingsTestTags.TOPIC_CLEAR_DISMISS,
            onConfirm = { onClearAll(); showConfirm = false },
            onDismiss = { showConfirm = false },
        )
    }
}

// --- forms (inline cards replacing AlertDialog) ---

@Composable
private fun MiuixAddKeywordForm(onDismiss: () -> Unit, onConfirm: (String, Boolean, Boolean) -> Unit) {
    var kw by remember { mutableStateOf("") }
    var caseSensitive by remember { mutableStateOf(false) }
    var isRegex by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            SmallTitle(text = "添加屏蔽关键词")
            Spacer(Modifier.height(8.dp))
            TextField(kw, { kw = it }, modifier = Modifier.fillMaxWidth()
                .testTag(BlocklistSettingsTestTags.KEYWORD_DIALOG_INPUT), label = "关键词")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    state = if (caseSensitive) ToggleableState.On else ToggleableState.Off,
                    onClick = { caseSensitive = !caseSensitive },
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.KEYWORD_DIALOG_CASE_SENSITIVE),
                )
                Spacer(Modifier.width(8.dp))
                Text("区分大小写")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    state = if (isRegex) ToggleableState.On else ToggleableState.Off,
                    onClick = { isRegex = !isRegex },
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.KEYWORD_DIALOG_REGEX),
                )
                Spacer(Modifier.width(8.dp))
                Text("正则表达式")
            }
            if (isRegex) Text("语法错误会导致该关键词无效",
                style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceSecondary)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                TextButton(
                    text = "取消",
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.KEYWORD_DIALOG_DISMISS),
                    onClick = onDismiss,
                )
                Button(
                    onClick = { if (kw.isNotBlank()) onConfirm(kw, caseSensitive, isRegex) },
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.KEYWORD_DIALOG_CONFIRM),
                    enabled = kw.isNotBlank(),
                ) { Text("添加") }
            }
        }
    }
}

@Composable
private fun MiuixAddUserForm(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var userId by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    Card(Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            SmallTitle(text = "添加屏蔽用户")
            Spacer(Modifier.height(8.dp))
            TextField(userId, { userId = it }, modifier = Modifier.fillMaxWidth()
                .testTag(BlocklistSettingsTestTags.USER_DIALOG_ID_INPUT), label = "用户ID")
            Spacer(Modifier.height(8.dp))
            TextField(userName, { userName = it }, modifier = Modifier.fillMaxWidth()
                .testTag(BlocklistSettingsTestTags.USER_DIALOG_NAME_INPUT), label = "用户名（可选）")
            Spacer(Modifier.height(8.dp))
            Text("可从用户主页URL获取用户ID", style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceSecondary)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                TextButton(text = "取消",
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.USER_DIALOG_DISMISS), onClick = onDismiss)
                Button(
                    onClick = { if (userId.isNotBlank()) onConfirm(userId, userName.ifBlank { userId }) },
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.USER_DIALOG_CONFIRM),
                    enabled = userId.isNotBlank(),
                ) { Text("添加") }
            }
        }
    }
}

@Composable
private fun MiuixAddTopicForm(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var topicId by remember { mutableStateOf("") }
    var topicName by remember { mutableStateOf("") }
    Card(Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            SmallTitle(text = "添加屏蔽主题")
            Spacer(Modifier.height(8.dp))
            Text("从知乎网页版主题链接中获取ID和名称",
                style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceSecondary)
            Spacer(Modifier.height(8.dp))
            TextField(topicId, { topicId = it }, modifier = Modifier.fillMaxWidth()
                .testTag(BlocklistSettingsTestTags.TOPIC_DIALOG_ID_INPUT), label = "主题ID")
            Spacer(Modifier.height(8.dp))
            TextField(topicName, { topicName = it }, modifier = Modifier.fillMaxWidth()
                .testTag(BlocklistSettingsTestTags.TOPIC_DIALOG_NAME_INPUT), label = "主题名称")
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                TextButton(text = "取消",
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.TOPIC_DIALOG_DISMISS), onClick = onDismiss)
                Button(
                    onClick = { if (topicId.isNotBlank()) onConfirm(topicId, topicName.ifBlank { topicId }) },
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.TOPIC_DIALOG_CONFIRM),
                    enabled = topicId.isNotBlank(),
                ) { Text("添加") }
            }
        }
    }
}

@Composable
private fun MiuixConfirmDialog(
    title: String, text: String,
    confirmTag: String, dismissTag: String,
    onConfirm: () -> Unit, onDismiss: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.padding(32.dp),
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(title, style = MiuixTheme.textStyles.title2)
                Spacer(Modifier.height(12.dp))
                Text(text, style = MiuixTheme.textStyles.body1)
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    TextButton(text = "取消", modifier = Modifier.testTag(dismissTag), onClick = onDismiss)
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.testTag(confirmTag),
                    ) { Text("清空") }
                }
            }
        }
    }
}
