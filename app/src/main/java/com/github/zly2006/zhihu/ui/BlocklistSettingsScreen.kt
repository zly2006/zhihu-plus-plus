package com.github.zly2006.zhihu.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.LocalNavigator
import com.github.zly2006.zhihu.viewmodel.filter.BlockedKeyword
import com.github.zly2006.zhihu.viewmodel.filter.BlockedUser
import com.github.zly2006.zhihu.viewmodel.filter.BlocklistManager
import com.github.zly2006.zhihu.viewmodel.filter.BlocklistStats
import kotlinx.coroutines.launch

@Composable
fun BlocklistSettingsScreen(
    innerPadding: PaddingValues,
    onNavigateBack: () -> Unit,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("屏蔽关键词", "NLP智能屏蔽", "屏蔽用户", "屏蔽主题")

    var blockedKeywords by remember { mutableStateOf<List<BlockedKeyword>>(emptyList()) }
    var blockedUsers by remember { mutableStateOf<List<BlockedUser>>(emptyList()) }
    var blockedTopics by remember { mutableStateOf<List<com.github.zly2006.zhihu.viewmodel.filter.BlockedTopic>>(emptyList()) }
    var stats by remember { mutableStateOf<BlocklistStats?>(null) }

    var showAddKeywordDialog by remember { mutableStateOf(false) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var showAddTopicDialog by remember { mutableStateOf(false) }

    // 加载数据
    fun loadData() {
        coroutineScope.launch {
            try {
                val blocklistManager = BlocklistManager.getInstance(context)
                // 只获取精确匹配的关键词
                blockedKeywords = blocklistManager
                    .getAllBlockedKeywords()
                    .filter { it.getKeywordTypeEnum() == com.github.zly2006.zhihu.viewmodel.filter.KeywordType.EXACT_MATCH }
                blockedUsers = blocklistManager.getAllBlockedUsers()
                blockedTopics = blocklistManager.getAllBlockedTopics()
                stats = blocklistManager.getBlocklistStats()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "加载数据失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 导入文件选择器
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

    LaunchedEffect(Unit) {
        loadData()
    }

    Scaffold(
        floatingActionButton = {
            // 只在传统关键词、用户屏蔽和主题屏蔽标签页显示添加按钮
            if (selectedTab == 0 || selectedTab == 2 || selectedTab == 3) {
                FloatingActionButton(
                    onClick = {
                        when (selectedTab) {
                            0 -> showAddKeywordDialog = true
                            2 -> showAddUserDialog = true
                            3 -> showAddTopicDialog = true
                        }
                    },
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加")
                }
            }
        },
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .padding(scaffoldPadding)
                .padding(innerPadding)
                .fillMaxWidth(),
        ) {
            // 统计信息卡片
            stats?.let { statsData ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Text(
                            "屏蔽统计",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "屏蔽关键词",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    "${statsData.keywordCount}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "屏蔽用户",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    "${statsData.userCount}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "屏蔽主题",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    "${statsData.topicCount}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            // 全局导入/导出按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }) {
                    Text("导入规则")
                }
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val blocklistManager = BlocklistManager.getInstance(context)
                                val file = blocklistManager.exportAllBlocklistToJson(context)
                                Toast.makeText(context, "已导出到 ${file.absolutePath}", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                ) {
                    Text("导出规则")
                }
            }

            // 标签页
            SecondaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                    )
                }
            }

            // 内容区域
            when (selectedTab) {
                0 -> BlockedKeywordsList(
                    keywords = blockedKeywords,
                    onDeleteKeyword = { keyword ->
                        coroutineScope.launch {
                            try {
                                val blocklistManager = BlocklistManager.getInstance(context)
                                blocklistManager.removeBlockedKeyword(keyword.id)
                                Toast.makeText(context, "已删除关键词", Toast.LENGTH_SHORT).show()
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
                                val blocklistManager = BlocklistManager.getInstance(context)
                                blocklistManager.clearAllBlockedKeywords()
                                Toast.makeText(context, "已清空所有关键词", Toast.LENGTH_SHORT).show()
                                loadData()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "清空失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                )
                1 -> NLPKeywordManagementScreen(
                    innerPadding = PaddingValues(0.dp),
                    onNavigateBack = onNavigateBack,
                )
                2 -> BlockedUsersList(
                    users = blockedUsers,
                    onDeleteUser = { user ->
                        coroutineScope.launch {
                            try {
                                val blocklistManager = BlocklistManager.getInstance(context)
                                blocklistManager.removeBlockedUser(user.userId)
                                Toast.makeText(context, "已删除用户", Toast.LENGTH_SHORT).show()
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
                                val blocklistManager = BlocklistManager.getInstance(context)
                                blocklistManager.clearAllBlockedUsers()
                                Toast.makeText(context, "已清空所有用户", Toast.LENGTH_SHORT).show()
                                loadData()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "清空失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onNavigateToUser = { user ->
                        navigator.onNavigate(
                            com.github.zly2006.zhihu.Person(
                                id = user.userId,
                                urlToken = user.urlToken ?: "",
                                name = user.userName,
                            ),
                        )
                    },
                )
                3 -> BlockedTopicsList(
                    topics = blockedTopics,
                    onDeleteTopic = { topic ->
                        coroutineScope.launch {
                            try {
                                val blocklistManager = BlocklistManager.getInstance(context)
                                blocklistManager.removeBlockedTopic(topic.topicId)
                                Toast.makeText(context, "已删除主题", Toast.LENGTH_SHORT).show()
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
                                val blocklistManager = BlocklistManager.getInstance(context)
                                blocklistManager.clearAllBlockedTopics()
                                Toast.makeText(context, "已清空所有主题", Toast.LENGTH_SHORT).show()
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

    // 添加关键词对话框
    if (showAddKeywordDialog) {
        AddKeywordDialog(
            onDismiss = { showAddKeywordDialog = false },
            onConfirm = { keyword, caseSensitive, isRegex ->
                coroutineScope.launch {
                    try {
                        val blocklistManager = BlocklistManager.getInstance(context)
                        blocklistManager.addBlockedKeyword(keyword, caseSensitive, isRegex)
                        Toast.makeText(context, "已添加关键词", Toast.LENGTH_SHORT).show()
                        loadData()
                        showAddKeywordDialog = false
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "添加失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
        )
    }

    // 添加主题对话框
    if (showAddTopicDialog) {
        AddTopicDialog(
            onDismiss = { showAddTopicDialog = false },
            onConfirm = { topicId, topicName ->
                coroutineScope.launch {
                    try {
                        val blocklistManager = BlocklistManager.getInstance(context)
                        blocklistManager.addBlockedTopic(topicId, topicName)
                        Toast.makeText(context, "已添加主题", Toast.LENGTH_SHORT).show()
                        loadData()
                        showAddTopicDialog = false
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "添加失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
        )
    }

    // 添加用户对话框
    if (showAddUserDialog) {
        AddUserDialog(
            onDismiss = { showAddUserDialog = false },
            onConfirm = { userId, userName ->
                coroutineScope.launch {
                    try {
                        val blocklistManager = BlocklistManager.getInstance(context)
                        blocklistManager.addBlockedUser(userId, userName)
                        Toast.makeText(context, "已添加用户", Toast.LENGTH_SHORT).show()
                        loadData()
                        showAddUserDialog = false
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "添加失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
        )
    }
}

@Composable
fun BlockedKeywordsList(
    keywords: List<BlockedKeyword>,
    onDeleteKeyword: (BlockedKeyword) -> Unit,
    onClearAll: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (keywords.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = onClearAll,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("清空全部")
                }
            }
        }

        if (keywords.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "暂无精确匹配关键词",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "点击右下角的 + 按钮添加传统关键词屏蔽",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn {
                items(keywords) { keyword ->
                    ListItem(
                        headlineContent = { Text(keyword.keyword) },
                        supportingContent = {
                            val options = mutableListOf<String>()
                            if (keyword.caseSensitive) options.add("区分大小写")
                            if (keyword.isRegex) options.add("正则表达式")
                            if (options.isNotEmpty()) {
                                Text(options.joinToString(" · "))
                            } else {
                                Text("精确匹配")
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { onDeleteKeyword(keyword) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun BlockedUsersList(
    users: List<BlockedUser>,
    onDeleteUser: (BlockedUser) -> Unit,
    onClearAll: () -> Unit,
    onNavigateToUser: (BlockedUser) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (users.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = onClearAll,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("清空全部")
                }
            }
        }

        if (users.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "暂无屏蔽用户",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "点击右下角的 + 按钮添加",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn {
                items(users) { user ->
                    ListItem(
                        headlineContent = { Text(user.userName) },
                        supportingContent = { Text("ID: ${user.userId}") },
                        leadingContent = {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = "头像",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { onDeleteUser(user) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                        modifier = Modifier.clickable {
                            onNavigateToUser(user)
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun AddKeywordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean, Boolean) -> Unit,
) {
    var keyword by remember { mutableStateOf("") }
    var caseSensitive by remember { mutableStateOf(false) }
    var isRegex by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加屏蔽关键词") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("关键词") },
                    placeholder = { Text("输入要屏蔽的关键词") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = caseSensitive,
                        onCheckedChange = { caseSensitive = it },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("区分大小写")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = isRegex,
                        onCheckedChange = { isRegex = it },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("正则表达式")
                }
                if (isRegex) {
                    Text(
                        "提示：使用正则表达式可以实现更灵活的匹配，但语法错误会导致该关键词无效",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (keyword.isNotBlank()) {
                        onConfirm(keyword, caseSensitive, isRegex)
                    }
                },
                enabled = keyword.isNotBlank(),
            ) {
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
fun AddUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var userId by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加屏蔽用户") },
        text = {
            Column {
                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    label = { Text("用户ID") },
                    placeholder = { Text("输入用户ID") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text("用户名") },
                    placeholder = { Text("输入用户名（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "提示：可以从用户主页的URL中获取用户ID",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (userId.isNotBlank()) {
                        onConfirm(userId, userName.ifBlank { userId })
                    }
                },
                enabled = userId.isNotBlank(),
            ) {
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
fun BlockedTopicsList(
    topics: List<com.github.zly2006.zhihu.viewmodel.filter.BlockedTopic>,
    onDeleteTopic: (com.github.zly2006.zhihu.viewmodel.filter.BlockedTopic) -> Unit,
    onClearAll: () -> Unit,
) {
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (topics.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "暂无屏蔽主题",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "点击右下角的 + 按钮添加要屏蔽的主题",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            // 清空按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = { showClearConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Text("清空全部")
                }
            }

            LazyColumn {
                items(topics, key = { it.topicId }) { topic ->
                    ListItem(
                        headlineContent = { Text(topic.topicName) },
                        supportingContent = { Text("ID: ${topic.topicId}") },
                        trailingContent = {
                            IconButton(onClick = { onDeleteTopic(topic) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("确认清空") },
            text = { Text("确定要清空所有屏蔽主题吗？此操作不可撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAll()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
fun AddTopicDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var topicId by remember { mutableStateOf("") }
    var topicName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加屏蔽主题") },
        text = {
            Column {
                Text(
                    "输入要屏蔽的主题ID和名称。主题ID可以从知乎网页版主题链接中获取。",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = topicId,
                    onValueChange = { topicId = it },
                    label = { Text("主题ID") },
                    placeholder = { Text("例如: 19550517") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = topicName,
                    onValueChange = { topicName = it },
                    label = { Text("主题名称") },
                    placeholder = { Text("例如: 娱乐") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (topicId.isNotBlank()) {
                        onConfirm(topicId, topicName.ifBlank { topicId })
                    }
                },
                enabled = topicId.isNotBlank(),
            ) {
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
