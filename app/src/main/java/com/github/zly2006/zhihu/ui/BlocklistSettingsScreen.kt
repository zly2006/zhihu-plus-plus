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

import android.content.Intent
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.viewmodel.filter.BlockedKeyword
import com.github.zly2006.zhihu.viewmodel.filter.BlockedTopic
import com.github.zly2006.zhihu.viewmodel.filter.BlockedUser
import com.github.zly2006.zhihu.viewmodel.filter.BlocklistManager
import com.github.zly2006.zhihu.viewmodel.filter.BlocklistStats
import kotlinx.coroutines.launch

object BlocklistSettingsTestTags {
    const val ROOT = "blocklistSettings:root"
    const val STATS_CARD = "blocklistSettings:statsCard"
    const val IMPORT_BUTTON = "blocklistSettings:import"
    const val EXPORT_BUTTON = "blocklistSettings:export"
    const val TAB_ROW = "blocklistSettings:tabs"
    const val FAB = "blocklistSettings:add"
    const val KEYWORD_LIST = "blocklistSettings:keywords:list"
    const val KEYWORD_CLEAR_BUTTON = "blocklistSettings:keywords:clear"
    const val USER_LIST = "blocklistSettings:users:list"
    const val USER_CLEAR_BUTTON = "blocklistSettings:users:clear"
    const val TOPIC_LIST = "blocklistSettings:topics:list"
    const val TOPIC_CLEAR_BUTTON = "blocklistSettings:topics:clear"
    const val TOPIC_CLEAR_CONFIRM = "blocklistSettings:topics:clearConfirm"
    const val TOPIC_CLEAR_DISMISS = "blocklistSettings:topics:clearDismiss"
    const val KEYWORD_DIALOG_INPUT = "blocklistSettings:keywordDialog:input"
    const val KEYWORD_DIALOG_CASE_SENSITIVE = "blocklistSettings:keywordDialog:caseSensitive"
    const val KEYWORD_DIALOG_REGEX = "blocklistSettings:keywordDialog:regex"
    const val KEYWORD_DIALOG_CONFIRM = "blocklistSettings:keywordDialog:confirm"
    const val KEYWORD_DIALOG_DISMISS = "blocklistSettings:keywordDialog:dismiss"
    const val USER_DIALOG_ID_INPUT = "blocklistSettings:userDialog:userId"
    const val USER_DIALOG_NAME_INPUT = "blocklistSettings:userDialog:userName"
    const val USER_DIALOG_CONFIRM = "blocklistSettings:userDialog:confirm"
    const val USER_DIALOG_DISMISS = "blocklistSettings:userDialog:dismiss"
    const val TOPIC_DIALOG_ID_INPUT = "blocklistSettings:topicDialog:topicId"
    const val TOPIC_DIALOG_NAME_INPUT = "blocklistSettings:topicDialog:topicName"
    const val TOPIC_DIALOG_CONFIRM = "blocklistSettings:topicDialog:confirm"
    const val TOPIC_DIALOG_DISMISS = "blocklistSettings:topicDialog:dismiss"

    fun tab(index: Int) = "blocklistSettings:tab:$index"

    fun keywordItem(keywordId: Long) = "blocklistSettings:keywords:item:$keywordId"

    fun keywordDelete(keywordId: Long) = "blocklistSettings:keywords:delete:$keywordId"

    fun userItem(userId: String) = "blocklistSettings:users:item:$userId"

    fun userDelete(userId: String) = "blocklistSettings:users:delete:$userId"

    fun topicItem(topicId: String) = "blocklistSettings:topics:item:$topicId"

    fun topicDelete(topicId: String) = "blocklistSettings:topics:delete:$topicId"
}

typealias BlocklistSettingsNlpContent = @Composable (onNavigateBack: () -> Unit) -> Unit

data class BlocklistSettingsTestConfig(
    val blockedKeywords: List<BlockedKeyword> = emptyList(),
    val blockedUsers: List<BlockedUser> = emptyList(),
    val blockedTopics: List<BlockedTopic> = emptyList(),
    val stats: BlocklistStats? = null,
    val onImportRequested: (() -> Unit)? = null,
    val onExportRequested: (() -> Unit)? = null,
    val onAddKeyword: ((String, Boolean, Boolean) -> Unit)? = null,
    val onDeleteKeyword: ((BlockedKeyword) -> Unit)? = null,
    val onClearKeywords: (() -> Unit)? = null,
    val onAddUser: ((String, String) -> Unit)? = null,
    val onDeleteUser: ((BlockedUser) -> Unit)? = null,
    val onClearUsers: (() -> Unit)? = null,
    val onAddTopic: ((String, String) -> Unit)? = null,
    val onDeleteTopic: ((BlockedTopic) -> Unit)? = null,
    val onClearTopics: (() -> Unit)? = null,
    val nlpContent: BlocklistSettingsNlpContent? = null,
)

@Composable
fun BlocklistSettingsScreen(
    innerPadding: PaddingValues,
    testConfig: BlocklistSettingsTestConfig? = null,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        context.getString(R.string.blocked_keywords),
        context.getString(R.string.nlp_smart_blocking),
        context.getString(R.string.blocked_users),
        context.getString(R.string.blocked_topics),
    )

    var loadedBlockedKeywords by remember { mutableStateOf<List<BlockedKeyword>>(emptyList()) }
    var loadedBlockedUsers by remember { mutableStateOf<List<BlockedUser>>(emptyList()) }
    var loadedBlockedTopics by remember { mutableStateOf<List<BlockedTopic>>(emptyList()) }
    var loadedStats by remember { mutableStateOf<BlocklistStats?>(null) }

    val blockedKeywords = testConfig?.blockedKeywords ?: loadedBlockedKeywords
    val blockedUsers = testConfig?.blockedUsers ?: loadedBlockedUsers
    val blockedTopics = testConfig?.blockedTopics ?: loadedBlockedTopics
    val stats = testConfig?.stats ?: loadedStats

    var showAddKeywordDialog by remember { mutableStateOf(false) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var showAddTopicDialog by remember { mutableStateOf(false) }

    // 加载数据
    fun loadData() {
        coroutineScope.launch {
            try {
                val blocklistManager = BlocklistManager.getInstance(context)
                // 只获取精确匹配的关键词
                loadedBlockedKeywords = blocklistManager
                    .getAllBlockedKeywords()
                    .filter { it.getKeywordTypeEnum() == com.github.zly2006.zhihu.viewmodel.filter.KeywordType.EXACT_MATCH }
                loadedBlockedUsers = blocklistManager.getAllBlockedUsers()
                loadedBlockedTopics = blocklistManager.getAllBlockedTopics()
                loadedStats = blocklistManager.getBlocklistStats()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, context.getString(R.string.load_data_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(context, context.getString(R.string.import_success, summary), Toast.LENGTH_LONG).show()
                    loadData()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, context.getString(R.string.import_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(testConfig) {
        if (testConfig == null) {
            loadData()
        }
    }

    Scaffold(
        floatingActionButton = {
            // 只在传统关键词、用户屏蔽和主题屏蔽标签页显示添加按钮
            if (selectedTab == 0 || selectedTab == 2 || selectedTab == 3) {
                FloatingActionButton(
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.FAB),
                    onClick = {
                        when (selectedTab) {
                            0 -> showAddKeywordDialog = true
                            2 -> showAddUserDialog = true
                            3 -> showAddTopicDialog = true
                        }
                    },
                ) {
                    Icon(Icons.Default.Add, contentDescription = context.getString(R.string.add))
                }
            }
        },
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .padding(scaffoldPadding)
                .padding(innerPadding)
                .testTag(BlocklistSettingsTestTags.ROOT)
                .fillMaxWidth(),
        ) {
            // 统计信息卡片
            stats?.let { statsData ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag(BlocklistSettingsTestTags.STATS_CARD),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Text(
                            context.getString(R.string.block_stats),
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
                                    context.getString(R.string.blocked_keywords),
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
                                    context.getString(R.string.blocked_users),
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
                                    context.getString(R.string.blocked_topics),
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
                TextButton(
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.IMPORT_BUTTON),
                    onClick = {
                        val importAction = testConfig?.onImportRequested
                        if (importAction != null) {
                            importAction()
                        } else {
                            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        }
                    },
                ) {
                    Text(context.getString(R.string.import_rules))
                }
                TextButton(
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.EXPORT_BUTTON),
                    onClick = {
                        val exportAction = testConfig?.onExportRequested
                        if (exportAction != null) {
                            exportAction()
                        } else {
                            coroutineScope.launch {
                                try {
                                    val blocklistManager = BlocklistManager.getInstance(context)
                                    val file = blocklistManager.exportAllBlocklistToJson(context)
                                    val intent = Intent().apply {
                                        action = Intent.ACTION_VIEW
                                        setDataAndType(
                                            FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.provider",
                                                file,
                                            ),
                                            "application/json",
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.view_blocklist_rules)))
                                    Toast.makeText(context, context.getString(R.string.exported_to, file.absolutePath), Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, context.getString(R.string.export_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                ) {
                    Text(context.getString(R.string.export_rules))
                }
            }

            // 标签页
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.testTag(BlocklistSettingsTestTags.TAB_ROW),
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        modifier = Modifier.testTag(BlocklistSettingsTestTags.tab(index)),
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
                        val onDeleteKeyword = testConfig?.onDeleteKeyword
                        if (onDeleteKeyword != null) {
                            onDeleteKeyword(keyword)
                        } else {
                            coroutineScope.launch {
                                try {
                                    val blocklistManager = BlocklistManager.getInstance(context)
                                    blocklistManager.removeBlockedKeyword(keyword.id)
                                    Toast.makeText(context, context.getString(R.string.deleted_keyword), Toast.LENGTH_SHORT).show()
                                    loadData()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, context.getString(R.string.delete_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    onClearAll = {
                        val onClearKeywords = testConfig?.onClearKeywords
                        if (onClearKeywords != null) {
                            onClearKeywords()
                        } else {
                            coroutineScope.launch {
                                try {
                                    val blocklistManager = BlocklistManager.getInstance(context)
                                    blocklistManager.clearAllBlockedKeywords()
                                    Toast.makeText(context, context.getString(R.string.cleared_keywords), Toast.LENGTH_SHORT).show()
                                    loadData()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, context.getString(R.string.clear_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                )
                1 -> {
                    val nlpContent = testConfig?.nlpContent
                    if (nlpContent != null) {
                        nlpContent(navigator.onNavigateBack)
                    } else {
                        NLPKeywordManagementScreen(
                            innerPadding = PaddingValues(0.dp),
                            onNavigateBack = navigator.onNavigateBack,
                        )
                    }
                }
                2 -> BlockedUsersList(
                    users = blockedUsers,
                    onDeleteUser = { user ->
                        val onDeleteUser = testConfig?.onDeleteUser
                        if (onDeleteUser != null) {
                            onDeleteUser(user)
                        } else {
                            coroutineScope.launch {
                                try {
                                    val blocklistManager = BlocklistManager.getInstance(context)
                                    blocklistManager.removeBlockedUser(user.userId)
                                    Toast.makeText(context, context.getString(R.string.deleted_user), Toast.LENGTH_SHORT).show()
                                    loadData()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, context.getString(R.string.delete_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    onClearAll = {
                        val onClearUsers = testConfig?.onClearUsers
                        if (onClearUsers != null) {
                            onClearUsers()
                        } else {
                            coroutineScope.launch {
                                try {
                                    val blocklistManager = BlocklistManager.getInstance(context)
                                    blocklistManager.clearAllBlockedUsers()
                                    Toast.makeText(context, context.getString(R.string.cleared_users), Toast.LENGTH_SHORT).show()
                                    loadData()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, context.getString(R.string.clear_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    onNavigateToUser = { user ->
                        navigator.onNavigate(
                            Person(
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
                        val onDeleteTopic = testConfig?.onDeleteTopic
                        if (onDeleteTopic != null) {
                            onDeleteTopic(topic)
                        } else {
                            coroutineScope.launch {
                                try {
                                    val blocklistManager = BlocklistManager.getInstance(context)
                                    blocklistManager.removeBlockedTopic(topic.topicId)
                                    Toast.makeText(context, context.getString(R.string.deleted_topic), Toast.LENGTH_SHORT).show()
                                    loadData()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, context.getString(R.string.delete_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    onClearAll = {
                        val onClearTopics = testConfig?.onClearTopics
                        if (onClearTopics != null) {
                            onClearTopics()
                        } else {
                            coroutineScope.launch {
                                try {
                                    val blocklistManager = BlocklistManager.getInstance(context)
                                    blocklistManager.clearAllBlockedTopics()
                                    Toast.makeText(context, context.getString(R.string.cleared_topics), Toast.LENGTH_SHORT).show()
                                    loadData()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, context.getString(R.string.clear_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                                }
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
                val onAddKeyword = testConfig?.onAddKeyword
                if (onAddKeyword != null) {
                    onAddKeyword(keyword, caseSensitive, isRegex)
                    showAddKeywordDialog = false
                } else {
                    coroutineScope.launch {
                        try {
                            val blocklistManager = BlocklistManager.getInstance(context)
                            blocklistManager.addBlockedKeyword(keyword, caseSensitive, isRegex)
                            Toast.makeText(context, context.getString(R.string.added_keyword), Toast.LENGTH_SHORT).show()
                            loadData()
                            showAddKeywordDialog = false
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, context.getString(R.string.add_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                        }
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
                val onAddTopic = testConfig?.onAddTopic
                if (onAddTopic != null) {
                    onAddTopic(topicId, topicName)
                    showAddTopicDialog = false
                } else {
                    coroutineScope.launch {
                        try {
                            val blocklistManager = BlocklistManager.getInstance(context)
                            blocklistManager.addBlockedTopic(topicId, topicName)
                            Toast.makeText(context, context.getString(R.string.added_topic), Toast.LENGTH_SHORT).show()
                            loadData()
                            showAddTopicDialog = false
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, context.getString(R.string.add_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                        }
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
                val onAddUser = testConfig?.onAddUser
                if (onAddUser != null) {
                    onAddUser(userId, userName)
                    showAddUserDialog = false
                } else {
                    coroutineScope.launch {
                        try {
                            val blocklistManager = BlocklistManager.getInstance(context)
                            blocklistManager.addBlockedUser(userId, userName)
                            Toast.makeText(context, context.getString(R.string.added_user), Toast.LENGTH_SHORT).show()
                            loadData()
                            showAddUserDialog = false
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, context.getString(R.string.add_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                        }
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
    val context = LocalContext.current
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
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.KEYWORD_CLEAR_BUTTON),
                    onClick = onClearAll,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(context.getString(R.string.clear_all))
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
                    context.getString(R.string.no_exact_keywords),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    context.getString(R.string.add_keyword_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.testTag(BlocklistSettingsTestTags.KEYWORD_LIST),
            ) {
                items(keywords, key = { it.id }) { keyword ->
                    ListItem(
                        modifier = Modifier.testTag(BlocklistSettingsTestTags.keywordItem(keyword.id)),
                        headlineContent = { Text(keyword.keyword) },
                        supportingContent = {
                            val options = mutableListOf<String>()
                            if (keyword.caseSensitive) options.add(context.getString(R.string.case_sensitive))
                            if (keyword.isRegex) options.add(context.getString(R.string.regular_expression))
                            if (options.isNotEmpty()) {
                                Text(options.joinToString(" · "))
                            } else {
                                Text(context.getString(R.string.exact_match))
                            }
                        },
                        trailingContent = {
                            IconButton(
                                modifier = Modifier.testTag(BlocklistSettingsTestTags.keywordDelete(keyword.id)),
                                onClick = { onDeleteKeyword(keyword) },
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = context.getString(R.string.delete),
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
    val context = LocalContext.current
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
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.USER_CLEAR_BUTTON),
                    onClick = onClearAll,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(context.getString(R.string.clear_all))
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
                    context.getString(R.string.no_blocked_users),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    context.getString(R.string.add_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.testTag(BlocklistSettingsTestTags.USER_LIST),
            ) {
                items(users, key = { it.userId }) { user ->
                    ListItem(
                        modifier = Modifier
                            .testTag(BlocklistSettingsTestTags.userItem(user.userId))
                            .clickable { onNavigateToUser(user) },
                        headlineContent = { Text(user.userName) },
                        supportingContent = { Text(context.getString(R.string.id_with_value, user.userId)) },
                        leadingContent = {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = context.getString(R.string.avatar),
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                            )
                        },
                        trailingContent = {
                            IconButton(
                                modifier = Modifier.testTag(BlocklistSettingsTestTags.userDelete(user.userId)),
                                onClick = { onDeleteUser(user) },
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = context.getString(R.string.delete),
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
fun AddKeywordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean, Boolean) -> Unit,
) {
    val context = LocalContext.current
    var keyword by remember { mutableStateOf("") }
    var caseSensitive by remember { mutableStateOf(false) }
    var isRegex by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.add_blocked_keyword)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text(context.getString(R.string.keyword)) },
                    placeholder = { Text(context.getString(R.string.keyword_placeholder)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(BlocklistSettingsTestTags.KEYWORD_DIALOG_INPUT),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        modifier = Modifier.testTag(BlocklistSettingsTestTags.KEYWORD_DIALOG_CASE_SENSITIVE),
                        checked = caseSensitive,
                        onCheckedChange = { caseSensitive = it },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(context.getString(R.string.case_sensitive))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        modifier = Modifier.testTag(BlocklistSettingsTestTags.KEYWORD_DIALOG_REGEX),
                        checked = isRegex,
                        onCheckedChange = { isRegex = it },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(context.getString(R.string.regular_expression))
                }
                if (isRegex) {
                    Text(
                        context.getString(R.string.regex_tip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                modifier = Modifier.testTag(BlocklistSettingsTestTags.KEYWORD_DIALOG_CONFIRM),
                onClick = {
                    if (keyword.isNotBlank()) {
                        onConfirm(keyword, caseSensitive, isRegex)
                    }
                },
                enabled = keyword.isNotBlank(),
            ) {
                Text(context.getString(R.string.add))
            }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.testTag(BlocklistSettingsTestTags.KEYWORD_DIALOG_DISMISS),
                onClick = onDismiss,
            ) {
                Text(context.getString(R.string.cancel))
            }
        },
    )
}

@Composable
fun AddUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    val context = LocalContext.current
    var userId by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.add_blocked_user)) },
        text = {
            Column {
                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    label = { Text(context.getString(R.string.user_id)) },
                    placeholder = { Text(context.getString(R.string.user_id_placeholder)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(BlocklistSettingsTestTags.USER_DIALOG_ID_INPUT),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text(context.getString(R.string.user_name)) },
                    placeholder = { Text(context.getString(R.string.user_name_placeholder)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(BlocklistSettingsTestTags.USER_DIALOG_NAME_INPUT),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    context.getString(R.string.user_id_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                modifier = Modifier.testTag(BlocklistSettingsTestTags.USER_DIALOG_CONFIRM),
                onClick = {
                    if (userId.isNotBlank()) {
                        onConfirm(userId, userName.ifBlank { userId })
                    }
                },
                enabled = userId.isNotBlank(),
            ) {
                Text(context.getString(R.string.add))
            }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.testTag(BlocklistSettingsTestTags.USER_DIALOG_DISMISS),
                onClick = onDismiss,
            ) {
                Text(context.getString(R.string.cancel))
            }
        },
    )
}

@Composable
fun BlockedTopicsList(
    topics: List<BlockedTopic>,
    onDeleteTopic: (BlockedTopic) -> Unit,
    onClearAll: () -> Unit,
) {
    val context = LocalContext.current
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
                    context.getString(R.string.no_blocked_topics),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    context.getString(R.string.add_topic_empty_hint),
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
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.TOPIC_CLEAR_BUTTON),
                    onClick = { showClearConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Text(context.getString(R.string.clear_all))
                }
            }

            LazyColumn(
                modifier = Modifier.testTag(BlocklistSettingsTestTags.TOPIC_LIST),
            ) {
                items(topics, key = { it.topicId }) { topic ->
                    ListItem(
                        modifier = Modifier.testTag(BlocklistSettingsTestTags.topicItem(topic.topicId)),
                        headlineContent = { Text(topic.topicName) },
                        supportingContent = { Text(context.getString(R.string.id_with_value, topic.topicId)) },
                        trailingContent = {
                            IconButton(
                                modifier = Modifier.testTag(BlocklistSettingsTestTags.topicDelete(topic.topicId)),
                                onClick = { onDeleteTopic(topic) },
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = context.getString(R.string.delete),
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
            title = { Text(context.getString(R.string.confirm_clear)) },
            text = { Text(context.getString(R.string.clear_all_topics_confirm)) },
            confirmButton = {
                Button(
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.TOPIC_CLEAR_CONFIRM),
                    onClick = {
                        onClearAll()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(context.getString(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.TOPIC_CLEAR_DISMISS),
                    onClick = { showClearConfirmDialog = false },
                ) {
                    Text(context.getString(R.string.cancel))
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
    val context = LocalContext.current
    var topicId by remember { mutableStateOf("") }
    var topicName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.add_blocked_topic)) },
        text = {
            Column {
                Text(
                    context.getString(R.string.add_topic_desc),
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = topicId,
                    onValueChange = { topicId = it },
                    label = { Text(context.getString(R.string.topic_id)) },
                    placeholder = { Text(context.getString(R.string.topic_id_example)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(BlocklistSettingsTestTags.TOPIC_DIALOG_ID_INPUT),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = topicName,
                    onValueChange = { topicName = it },
                    label = { Text(context.getString(R.string.topic_name)) },
                    placeholder = { Text(context.getString(R.string.topic_name_example)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(BlocklistSettingsTestTags.TOPIC_DIALOG_NAME_INPUT),
                )
            }
        },
        confirmButton = {
            Button(
                modifier = Modifier.testTag(BlocklistSettingsTestTags.TOPIC_DIALOG_CONFIRM),
                onClick = {
                    if (topicId.isNotBlank()) {
                        onConfirm(topicId, topicName.ifBlank { topicId })
                    }
                },
                enabled = topicId.isNotBlank(),
            ) {
                Text(context.getString(R.string.add))
            }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.testTag(BlocklistSettingsTestTags.TOPIC_DIALOG_DISMISS),
                onClick = onDismiss,
            ) {
                Text(context.getString(R.string.cancel))
            }
        },
    )
}
