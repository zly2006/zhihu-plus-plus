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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
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
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.utils.overScrollVertical

/**
 * miuix 风格的屏蔽设置页。
 *
 * 跟 [com.github.zly2006.zhihu.ui.BlocklistSettingsScreen] 语义对等，
 * 但 UI 用 miuix 风格。
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

    var loadedKeywords by remember { mutableStateOf<List<BlockedKeyword>>(emptyList()) }
    var loadedUsers by remember { mutableStateOf<List<BlockedUser>>(emptyList()) }
    var loadedTopics by remember { mutableStateOf<List<BlockedTopic>>(emptyList()) }
    var loadedStats by remember { mutableStateOf<BlocklistStats?>(null) }

    val keywords = testConfig?.blockedKeywords ?: loadedKeywords
    val users = testConfig?.blockedUsers ?: loadedUsers
    val topics = testConfig?.blockedTopics ?: loadedTopics
    val stats = testConfig?.stats ?: loadedStats

    var showAddKeywordForm by remember { mutableStateOf(false) }
    var showAddUserForm by remember { mutableStateOf(false) }
    var showAddTopicForm by remember { mutableStateOf(false) }
    var showTopicClearConfirm by remember { mutableStateOf(false) }

    fun loadData() {
        coroutineScope.launch {
            try {
                val mgr = BlocklistManager.getInstance(context)
                loadedKeywords = mgr.getAllBlockedKeywords()
                    .filter { it.getKeywordTypeEnum() == com.github.zly2006.zhihu.viewmodel.filter.KeywordType.EXACT_MATCH }
                loadedUsers = mgr.getAllBlockedUsers()
                loadedTopics = mgr.getAllBlockedTopics()
                loadedStats = mgr.getBlocklistStats()
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
                    val summary = BlocklistManager.getInstance(context).importAllBlocklistFromJson(context, uri)
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

    val showFab = selectedTab == 0 || selectedTab == 2 || selectedTab == 3
    val backdrop = rememberMiuixBlurBackdrop(true)
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(backdrop),
                color = backdrop.getMiuixAppBarColor(),
                title = "屏蔽设置",
                navigationIcon = {
                    IconButton(onClick = { navigator.onNavigateBack() }) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回",
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.FAB),
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
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                .padding(innerPadding)
                .testTag(BlocklistSettingsTestTags.ROOT),
        ) {
            // 统计卡片
            stats?.let { statsData ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
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

            // 导入/导出（跟 stats card 同 padding）
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
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

            // 标签页
            TabRow(
                tabs = tabs,
                selectedTabIndex = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.testTag(BlocklistSettingsTestTags.TAB_ROW),
            )

            // 内容区（仅此区域滚动）
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            ) {
                when (selectedTab) {
                    0 -> KeywordsTab(
                        keywords = keywords,
                        testConfig = testConfig,
                        onReload = ::loadData,
                        showAddForm = showAddKeywordForm,
                        onDismissForm = { showAddKeywordForm = false },
                    )
                    1 -> {
                        val nlp = testConfig?.nlpContent
                        if (nlp != null) nlp(navigator.onNavigateBack)
                        else NLPKeywordManagementScreen(
                            innerPadding = PaddingValues(0.dp),
                            onNavigateBack = navigator.onNavigateBack,
                        )
                    }
                    2 -> UsersTab(
                        users = users,
                        testConfig = testConfig,
                        onReload = ::loadData,
                        onNavigate = { u -> navigator.onNavigate(Person(u.userId, u.urlToken ?: "", u.userName)) },
                        showAddForm = showAddUserForm,
                        onDismissForm = { showAddUserForm = false },
                    )
                    3 -> TopicsTab(
                        topics = topics,
                        testConfig = testConfig,
                        onReload = ::loadData,
                        showAddForm = showAddTopicForm,
                        onDismissForm = { showAddTopicForm = false },
                        showClearConfirm = showTopicClearConfirm,
                        onShowClearConfirm = { showTopicClearConfirm = it },
                    )
                }
            }
        }
    }
}

// --- helper ---

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MiuixTheme.textStyles.footnote1)
        Text(value, style = MiuixTheme.textStyles.title1, color = MiuixTheme.colorScheme.primary)
    }
}

// --- tabs ---

@Composable
private fun KeywordsTab(
    keywords: List<BlockedKeyword>,
    testConfig: BlocklistSettingsTestConfig?,
    onReload: () -> Unit,
    showAddForm: Boolean,
    onDismissForm: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (keywords.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = {
                            testConfig?.onClearKeywords?.let { it(); return@Button }
                            coroutineScope.launch {
                                try { BlocklistManager.getInstance(context).clearAllBlockedKeywords(); onReload() }
                                catch (e: Exception) { e.printStackTrace() }
                            }
                        },
                        modifier = Modifier.testTag(BlocklistSettingsTestTags.KEYWORD_CLEAR_BUTTON),
                    ) { Text("清空全部") }
                }
            }
            items(keywords, key = { it.id }) { kw ->
                ArrowPreference(
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.keywordItem(kw.id)),
                    title = kw.keyword,
                    summary = kwSummary(kw),
                    onClick = {},
                    endActions = {
                        IconButton(
                            modifier = Modifier.testTag(BlocklistSettingsTestTags.keywordDelete(kw.id)),
                            onClick = {
                                testConfig?.onDeleteKeyword?.let { it(kw); return@IconButton }
                                coroutineScope.launch {
                                    try { BlocklistManager.getInstance(context).removeBlockedKeyword(kw.id); onReload() }
                                    catch (e: Exception) { e.printStackTrace() }
                                }
                            },
                        ) {
                            Icon(Icons.Default.Delete, "删除", tint = MiuixTheme.colorScheme.error)
                        }
                    },
                )
            }
        } else {
            item {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("暂无精确匹配关键词", color = MiuixTheme.colorScheme.onSurfaceSecondary)
                    Spacer(Modifier.height(8.dp))
                    Text("点击 + 按钮添加", color = MiuixTheme.colorScheme.onSurfaceSecondary)
                }
            }
        }

        if (showAddForm) {
            item {
                AddKeywordForm(
                    onDismiss = onDismissForm,
                    onConfirm = { kw, cs, rx ->
                        testConfig?.onAddKeyword?.let { it(kw, cs, rx); onDismissForm(); return@AddKeywordForm }
                        coroutineScope.launch {
                            try {
                                BlocklistManager.getInstance(context).addBlockedKeyword(kw, cs, rx)
                                Toast.makeText(context, "已添加", Toast.LENGTH_SHORT).show()
                                onReload(); onDismissForm()
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun UsersTab(
    users: List<BlockedUser>,
    testConfig: BlocklistSettingsTestConfig?,
    onReload: () -> Unit,
    onNavigate: (BlockedUser) -> Unit,
    showAddForm: Boolean,
    onDismissForm: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (users.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = {
                            testConfig?.onClearUsers?.let { it(); return@Button }
                            coroutineScope.launch {
                                try { BlocklistManager.getInstance(context).clearAllBlockedUsers(); onReload() }
                                catch (e: Exception) { e.printStackTrace() }
                            }
                        },
                        modifier = Modifier.testTag(BlocklistSettingsTestTags.USER_CLEAR_BUTTON),
                    ) { Text("清空全部") }
                }
            }
            items(users, key = { it.userId }) { user ->
                ArrowPreference(
                    modifier = Modifier
                        .testTag(BlocklistSettingsTestTags.userItem(user.userId))
                        .clickable { onNavigate(user) },
                    title = user.userName,
                    summary = "ID: ${user.userId}",
                    startAction = {
                        AsyncImage(user.avatarUrl, "头像", modifier = Modifier.size(40.dp).clip(CircleShape))
                    },
                    onClick = {},
                    endActions = {
                        IconButton(
                            modifier = Modifier.testTag(BlocklistSettingsTestTags.userDelete(user.userId)),
                            onClick = {
                                testConfig?.onDeleteUser?.let { it(user); return@IconButton }
                                coroutineScope.launch {
                                    try { BlocklistManager.getInstance(context).removeBlockedUser(user.userId); onReload() }
                                    catch (e: Exception) { e.printStackTrace() }
                                }
                            },
                        ) {
                            Icon(Icons.Default.Delete, "删除", tint = MiuixTheme.colorScheme.error)
                        }
                    },
                )
            }
        } else {
            item {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("暂无屏蔽用户", color = MiuixTheme.colorScheme.onSurfaceSecondary)
                    Spacer(Modifier.height(8.dp))
                    Text("点击 + 按钮添加", color = MiuixTheme.colorScheme.onSurfaceSecondary)
                }
            }
        }

        if (showAddForm) {
            item {
                AddUserForm(
                    onDismiss = onDismissForm,
                    onConfirm = { id, name ->
                        testConfig?.onAddUser?.let { it(id, name); onDismissForm(); return@AddUserForm }
                        coroutineScope.launch {
                            try {
                                BlocklistManager.getInstance(context).addBlockedUser(id, name)
                                Toast.makeText(context, "已添加", Toast.LENGTH_SHORT).show()
                                onReload(); onDismissForm()
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun TopicsTab(
    topics: List<BlockedTopic>,
    testConfig: BlocklistSettingsTestConfig?,
    onReload: () -> Unit,
    showAddForm: Boolean,
    onDismissForm: () -> Unit,
    showClearConfirm: Boolean,
    onShowClearConfirm: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (topics.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("暂无屏蔽主题", color = MiuixTheme.colorScheme.onSurfaceSecondary)
                        Spacer(Modifier.height(8.dp))
                        Text("点击 + 按钮添加", color = MiuixTheme.colorScheme.onSurfaceSecondary)
                    }
                }
            } else {
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = { onShowClearConfirm(true) },
                            modifier = Modifier.testTag(BlocklistSettingsTestTags.TOPIC_CLEAR_BUTTON),
                        ) { Text("清空全部") }
                    }
                }
                items(topics, key = { it.topicId }) { topic ->
                    ArrowPreference(
                        modifier = Modifier.testTag(BlocklistSettingsTestTags.topicItem(topic.topicId)),
                        title = topic.topicName,
                        summary = "ID: ${topic.topicId}",
                        onClick = {},
                        endActions = {
                            IconButton(
                                modifier = Modifier.testTag(BlocklistSettingsTestTags.topicDelete(topic.topicId)),
                                onClick = {
                                    testConfig?.onDeleteTopic?.let { it(topic); return@IconButton }
                                    coroutineScope.launch {
                                        try { BlocklistManager.getInstance(context).removeBlockedTopic(topic.topicId); onReload() }
                                        catch (e: Exception) { e.printStackTrace() }
                                    }
                                },
                            ) {
                                Icon(Icons.Default.Delete, "删除", tint = MiuixTheme.colorScheme.error)
                            }
                        },
                    )
                }
            }

            if (showAddForm) {
                item {
                    AddTopicForm(
                        onDismiss = onDismissForm,
                        onConfirm = { id, name ->
                            testConfig?.onAddTopic?.let { it(id, name); onDismissForm(); return@AddTopicForm }
                            coroutineScope.launch {
                                try {
                                    BlocklistManager.getInstance(context).addBlockedTopic(id, name)
                                    Toast.makeText(context, "已添加", Toast.LENGTH_SHORT).show()
                                    onReload(); onDismissForm()
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                        },
                    )
                }
            }
        }

        if (showClearConfirm) {
            ConfirmDialog(
                title = "确认清空",
                text = "确定要清空所有屏蔽主题吗？此操作不可撤销。",
                confirmTag = BlocklistSettingsTestTags.TOPIC_CLEAR_CONFIRM,
                dismissTag = BlocklistSettingsTestTags.TOPIC_CLEAR_DISMISS,
                onConfirm = {
                    testConfig?.onClearTopics?.let { it(); onShowClearConfirm(false); return@ConfirmDialog }
                    coroutineScope.launch {
                        try { BlocklistManager.getInstance(context).clearAllBlockedTopics(); onReload() }
                        catch (e: Exception) { e.printStackTrace() }
                    }
                    onShowClearConfirm(false)
                },
                onDismiss = { onShowClearConfirm(false) },
            )
        }
    }
}

// --- forms ---

@Composable
private fun AddKeywordForm(onDismiss: () -> Unit, onConfirm: (String, Boolean, Boolean) -> Unit) {
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
                TextButton(text = "取消", modifier = Modifier.testTag(BlocklistSettingsTestTags.KEYWORD_DIALOG_DISMISS), onClick = onDismiss)
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
private fun AddUserForm(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
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
                TextButton(text = "取消", modifier = Modifier.testTag(BlocklistSettingsTestTags.USER_DIALOG_DISMISS), onClick = onDismiss)
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
private fun AddTopicForm(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
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
                TextButton(text = "取消", modifier = Modifier.testTag(BlocklistSettingsTestTags.TOPIC_DIALOG_DISMISS), onClick = onDismiss)
                Button(
                    onClick = { if (topicId.isNotBlank()) onConfirm(topicId, topicName.ifBlank { topicId }) },
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.TOPIC_DIALOG_CONFIRM),
                    enabled = topicId.isNotBlank(),
                ) { Text("添加") }
            }
        }
    }
}

// --- dialog ---

@Composable
private fun ConfirmDialog(
    title: String, text: String,
    confirmTag: String, dismissTag: String,
    onConfirm: () -> Unit, onDismiss: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(modifier = Modifier.padding(32.dp)) {
            Column(Modifier.padding(24.dp)) {
                Text(title, style = MiuixTheme.textStyles.title2)
                Spacer(Modifier.height(12.dp))
                Text(text, style = MiuixTheme.textStyles.body1)
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    TextButton(text = "取消", modifier = Modifier.testTag(dismissTag), onClick = onDismiss)
                    Button(onClick = onConfirm, modifier = Modifier.testTag(confirmTag)) { Text("清空") }
                }
            }
        }
    }
}

// --- utils ---

private fun kwSummary(kw: BlockedKeyword): String {
    val opts = mutableListOf<String>()
    if (kw.caseSensitive) opts.add("区分大小写")
    if (kw.isRegex) opts.add("正则")
    return opts.ifEmpty { listOf("精确匹配") }.joinToString(" · ")
}
