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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.util.Log
import com.github.zly2006.zhihu.viewmodel.filter.BlockedKeyword
import com.github.zly2006.zhihu.viewmodel.filter.BlockedMcnOrganization
import com.github.zly2006.zhihu.viewmodel.filter.BlockedTopic
import com.github.zly2006.zhihu.viewmodel.filter.BlockedUser
import com.github.zly2006.zhihu.viewmodel.filter.BlocklistStats
import com.github.zly2006.zhihu.viewmodel.filter.KeywordType
import com.github.zly2006.zhihu.viewmodel.filter.rememberBlocklistManager
import kotlinx.coroutines.launch

typealias BlocklistSettingsNlpContent = @Composable (onNavigateBack: () -> Unit) -> Unit

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
    const val MCN_LIST = "blocklistSettings:mcn:list"
    const val MCN_CLEAR_BUTTON = "blocklistSettings:mcn:clear"
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
    const val MCN_DIALOG_INPUT = "blocklistSettings:mcnDialog:organizationName"
    const val MCN_DIALOG_CONFIRM = "blocklistSettings:mcnDialog:confirm"
    const val MCN_DIALOG_DISMISS = "blocklistSettings:mcnDialog:dismiss"

    fun tab(index: Int) = "blocklistSettings:tab:$index"

    fun keywordItem(keywordId: Long) = "blocklistSettings:keywords:item:$keywordId"

    fun keywordDelete(keywordId: Long) = "blocklistSettings:keywords:delete:$keywordId"

    fun userItem(userId: String) = "blocklistSettings:users:item:$userId"

    fun userDelete(userId: String) = "blocklistSettings:users:delete:$userId"

    fun topicItem(topicId: String) = "blocklistSettings:topics:item:$topicId"

    fun topicDelete(topicId: String) = "blocklistSettings:topics:delete:$topicId"

    fun mcnItem(organizationName: String) = "blocklistSettings:mcn:item:$organizationName"

    fun mcnDelete(organizationName: String) = "blocklistSettings:mcn:delete:$organizationName"
}

data class BlocklistSettingsTestConfig(
    val blockedKeywords: List<BlockedKeyword> = emptyList(),
    val blockedUsers: List<BlockedUser> = emptyList(),
    val blockedTopics: List<BlockedTopic> = emptyList(),
    val blockedMcnOrganizations: List<BlockedMcnOrganization> = emptyList(),
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
    val onAddMcnOrganization: ((String) -> Unit)? = null,
    val onDeleteMcnOrganization: ((BlockedMcnOrganization) -> Unit)? = null,
    val onClearMcnOrganizations: (() -> Unit)? = null,
    val nlpContent: BlocklistSettingsNlpContent? = null,
)

/**
 * 屏蔽列表管理页。
 *
 * 页面用 tab 管理关键词、NLP 智能屏蔽短语、用户和主题四类规则，并展示统计、添加、删除和清空操作。Lite variant 可能没有
 * NLP 内容区，因此 [nlpContent] 需要作为可空插槽传入；新增屏蔽类型时要同步数据管理、设置页入口和 Feed 卡片菜单。
 */
@Composable
fun BlocklistSettingsScreen(
    nlpContent: BlocklistSettingsNlpContent? = null,
    testConfig: BlocklistSettingsTestConfig? = null,
) {
    val navigator = LocalNavigator.current
    val userMessages = rememberUserMessageSink()
    val runtime = rememberBlocklistSettingsPlatformRuntime(userMessages)
    val blocklistManager = rememberBlocklistManager()
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("屏蔽关键词", "NLP智能屏蔽", "屏蔽用户", "屏蔽主题", "屏蔽MCN")

    var loadedBlockedKeywords by remember { mutableStateOf<List<BlockedKeyword>>(emptyList()) }
    var loadedBlockedUsers by remember { mutableStateOf<List<BlockedUser>>(emptyList()) }
    var loadedBlockedTopics by remember { mutableStateOf<List<BlockedTopic>>(emptyList()) }
    var loadedBlockedMcnOrganizations by remember { mutableStateOf<List<BlockedMcnOrganization>>(emptyList()) }
    var loadedStats by remember { mutableStateOf<BlocklistStats?>(null) }

    val blockedKeywords = testConfig?.blockedKeywords ?: loadedBlockedKeywords
    val blockedUsers = testConfig?.blockedUsers ?: loadedBlockedUsers
    val blockedTopics = testConfig?.blockedTopics ?: loadedBlockedTopics
    val blockedMcnOrganizations = testConfig?.blockedMcnOrganizations ?: loadedBlockedMcnOrganizations
    val stats = testConfig?.stats ?: loadedStats

    var showAddKeywordDialog by remember { mutableStateOf(false) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var showAddTopicDialog by remember { mutableStateOf(false) }
    var showAddMcnDialog by remember { mutableStateOf(false) }

    // 加载数据
    fun loadData() {
        coroutineScope.launch {
            try {
                // 只获取精确匹配的关键词
                loadedBlockedKeywords = blocklistManager
                    .getAllBlockedKeywords()
                    .filter { it.getKeywordTypeEnum() == KeywordType.EXACT_MATCH }
                loadedBlockedUsers = blocklistManager.getAllBlockedUsers()
                loadedBlockedTopics = blocklistManager.getAllBlockedTopics()
                loadedBlockedMcnOrganizations = blocklistManager.getAllBlockedMcnOrganizations()
                loadedStats = blocklistManager.getBlocklistStats()
            } catch (e: Exception) {
                Log.e("BlocklistSettingsScreen", "Blocklist settings action failed", e)
                userMessages.showShortMessage("加载数据失败: ${e.message}")
            }
        }
    }

    LaunchedEffect(testConfig) {
        if (testConfig == null) {
            loadData()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            // 只在可手动添加规则的标签页显示添加按钮
            if (selectedTab == 0 || selectedTab == 2 || selectedTab == 3 || selectedTab == 4) {
                FloatingActionButton(
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.FAB),
                    onClick = {
                        when (selectedTab) {
                            0 -> showAddKeywordDialog = true
                            2 -> showAddUserDialog = true
                            3 -> showAddTopicDialog = true
                            4 -> showAddMcnDialog = true
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
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "屏蔽MCN",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    "${statsData.mcnOrganizationCount}",
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
                            runtime.requestImport { summary ->
                                userMessages.showLongMessage("导入成功：$summary")
                                loadData()
                            }
                        }
                    },
                ) {
                    Text("导入规则")
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
                                    userMessages.showLongMessage(runtime.exportRules())
                                } catch (e: Exception) {
                                    Log.e("BlocklistSettingsScreen", "Blocklist settings action failed", e)
                                    userMessages.showShortMessage("导出失败: ${e.message}")
                                }
                            }
                        }
                    },
                ) {
                    Text("导出规则")
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
                                    blocklistManager.removeBlockedKeyword(keyword.id)
                                    userMessages.showShortMessage("已删除关键词")
                                    loadData()
                                } catch (e: Exception) {
                                    Log.e("BlocklistSettingsScreen", "Blocklist settings action failed", e)
                                    userMessages.showShortMessage("删除失败: ${e.message}")
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
                                    blocklistManager.clearAllBlockedKeywords()
                                    userMessages.showShortMessage("已清空所有关键词")
                                    loadData()
                                } catch (e: Exception) {
                                    Log.e("BlocklistSettingsScreen", "Blocklist settings action failed", e)
                                    userMessages.showShortMessage("清空失败: ${e.message}")
                                }
                            }
                        }
                    },
                )
                1 -> {
                    val actualNlpContent = testConfig?.nlpContent ?: nlpContent
                    if (actualNlpContent != null) {
                        actualNlpContent(navigator.onNavigateBack)
                    } else {
                        Text("AI features are not available on this platform.")
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
                                    blocklistManager.removeBlockedUser(user.userId)
                                    userMessages.showShortMessage("已删除用户")
                                    loadData()
                                } catch (e: Exception) {
                                    Log.e("BlocklistSettingsScreen", "Blocklist settings action failed", e)
                                    userMessages.showShortMessage("删除失败: ${e.message}")
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
                                    blocklistManager.clearAllBlockedUsers()
                                    userMessages.showShortMessage("已清空所有用户")
                                    loadData()
                                } catch (e: Exception) {
                                    Log.e("BlocklistSettingsScreen", "Blocklist settings action failed", e)
                                    userMessages.showShortMessage("清空失败: ${e.message}")
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
                                    blocklistManager.removeBlockedTopic(topic.topicId)
                                    userMessages.showShortMessage("已删除主题")
                                    loadData()
                                } catch (e: Exception) {
                                    Log.e("BlocklistSettingsScreen", "Blocklist settings action failed", e)
                                    userMessages.showShortMessage("删除失败: ${e.message}")
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
                                    blocklistManager.clearAllBlockedTopics()
                                    userMessages.showShortMessage("已清空所有主题")
                                    loadData()
                                } catch (e: Exception) {
                                    Log.e("BlocklistSettingsScreen", "Blocklist settings action failed", e)
                                    userMessages.showShortMessage("清空失败: ${e.message}")
                                }
                            }
                        }
                    },
                )
                4 -> BlockedMcnOrganizationsList(
                    organizations = blockedMcnOrganizations,
                    onDeleteOrganization = { organization ->
                        val onDeleteOrganization = testConfig?.onDeleteMcnOrganization
                        if (onDeleteOrganization != null) {
                            onDeleteOrganization(organization)
                        } else {
                            coroutineScope.launch {
                                try {
                                    blocklistManager.removeBlockedMcnOrganization(organization.organizationName)
                                    userMessages.showShortMessage("已删除MCN机构")
                                    loadData()
                                } catch (e: Exception) {
                                    Log.e("BlocklistSettingsScreen", "Blocklist settings action failed", e)
                                    userMessages.showShortMessage("删除失败: ${e.message}")
                                }
                            }
                        }
                    },
                    onClearAll = {
                        val onClearOrganizations = testConfig?.onClearMcnOrganizations
                        if (onClearOrganizations != null) {
                            onClearOrganizations()
                        } else {
                            coroutineScope.launch {
                                try {
                                    blocklistManager.clearAllBlockedMcnOrganizations()
                                    userMessages.showShortMessage("已清空所有MCN机构")
                                    loadData()
                                } catch (e: Exception) {
                                    Log.e("BlocklistSettingsScreen", "Blocklist settings action failed", e)
                                    userMessages.showShortMessage("清空失败: ${e.message}")
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
                            blocklistManager.addBlockedKeyword(keyword, caseSensitive, isRegex)
                            userMessages.showShortMessage("已添加关键词")
                            loadData()
                            showAddKeywordDialog = false
                        } catch (e: Exception) {
                            Log.e("BlocklistSettingsScreen", "Blocklist settings action failed", e)
                            userMessages.showShortMessage("添加失败: ${e.message}")
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
                            blocklistManager.addBlockedTopic(topicId, topicName)
                            userMessages.showShortMessage("已添加主题")
                            loadData()
                            showAddTopicDialog = false
                        } catch (e: Exception) {
                            Log.e("BlocklistSettingsScreen", "Blocklist settings action failed", e)
                            userMessages.showShortMessage("添加失败: ${e.message}")
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
                            blocklistManager.addBlockedUser(userId, userName)
                            userMessages.showShortMessage("已添加用户")
                            loadData()
                            showAddUserDialog = false
                        } catch (e: Exception) {
                            Log.e("BlocklistSettingsScreen", "Blocklist settings action failed", e)
                            userMessages.showShortMessage("添加失败: ${e.message}")
                        }
                    }
                }
            },
        )
    }

    if (showAddMcnDialog) {
        AddMcnOrganizationDialog(
            onDismiss = { showAddMcnDialog = false },
            onConfirm = { organizationName ->
                val onAddOrganization = testConfig?.onAddMcnOrganization
                if (onAddOrganization != null) {
                    onAddOrganization(organizationName)
                    showAddMcnDialog = false
                } else {
                    coroutineScope.launch {
                        try {
                            blocklistManager.addBlockedMcnOrganization(organizationName)
                            userMessages.showShortMessage("已添加MCN机构")
                            loadData()
                            showAddMcnDialog = false
                        } catch (e: Exception) {
                            Log.e("BlocklistSettingsScreen", "Blocklist settings action failed", e)
                            userMessages.showShortMessage("添加失败: ${e.message}")
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
            LazyColumn(
                modifier = Modifier.testTag(BlocklistSettingsTestTags.KEYWORD_LIST),
            ) {
                items(keywords, key = { it.id }) { keyword ->
                    ListItem(
                        modifier = Modifier.testTag(BlocklistSettingsTestTags.keywordItem(keyword.id)),
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
                            IconButton(
                                modifier = Modifier.testTag(BlocklistSettingsTestTags.keywordDelete(keyword.id)),
                                onClick = { onDeleteKeyword(keyword) },
                            ) {
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
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.USER_CLEAR_BUTTON),
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
            LazyColumn(
                modifier = Modifier.testTag(BlocklistSettingsTestTags.USER_LIST),
            ) {
                items(users, key = { it.userId }) { user ->
                    ListItem(
                        modifier = Modifier
                            .testTag(BlocklistSettingsTestTags.userItem(user.userId))
                            .clickable { onNavigateToUser(user) },
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
                            IconButton(
                                modifier = Modifier.testTag(BlocklistSettingsTestTags.userDelete(user.userId)),
                                onClick = { onDeleteUser(user) },
                            ) {
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
                    Text("区分大小写")
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
                modifier = Modifier.testTag(BlocklistSettingsTestTags.KEYWORD_DIALOG_CONFIRM),
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
            TextButton(
                modifier = Modifier.testTag(BlocklistSettingsTestTags.KEYWORD_DIALOG_DISMISS),
                onClick = onDismiss,
            ) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(BlocklistSettingsTestTags.USER_DIALOG_ID_INPUT),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text("用户名") },
                    placeholder = { Text("输入用户名（可选）") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(BlocklistSettingsTestTags.USER_DIALOG_NAME_INPUT),
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
                modifier = Modifier.testTag(BlocklistSettingsTestTags.USER_DIALOG_CONFIRM),
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
            TextButton(
                modifier = Modifier.testTag(BlocklistSettingsTestTags.USER_DIALOG_DISMISS),
                onClick = onDismiss,
            ) {
                Text("取消")
            }
        },
    )
}

@Composable
fun BlockedMcnOrganizationsList(
    organizations: List<BlockedMcnOrganization>,
    onDeleteOrganization: (BlockedMcnOrganization) -> Unit,
    onClearAll: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (organizations.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.MCN_CLEAR_BUTTON),
                    onClick = onClearAll,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("清空全部")
                }
            }
        }

        if (organizations.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "暂无屏蔽MCN机构",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "点击右下角的 + 按钮添加要屏蔽的机构名称",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.testTag(BlocklistSettingsTestTags.MCN_LIST),
            ) {
                items(organizations, key = { it.organizationName }) { organization ->
                    ListItem(
                        modifier = Modifier.testTag(BlocklistSettingsTestTags.mcnItem(organization.organizationName)),
                        headlineContent = { Text(organization.organizationName) },
                        supportingContent = { Text("屏蔽该 MCN 机构旗下用户发布的内容") },
                        trailingContent = {
                            IconButton(
                                modifier = Modifier.testTag(
                                    BlocklistSettingsTestTags.mcnDelete(organization.organizationName),
                                ),
                                onClick = { onDeleteOrganization(organization) },
                            ) {
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
fun AddMcnOrganizationDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var organizationName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加屏蔽MCN机构") },
        text = {
            Column {
                OutlinedTextField(
                    value = organizationName,
                    onValueChange = { organizationName = it },
                    label = { Text("机构名称") },
                    placeholder = { Text("例如：杭州亚序") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(BlocklistSettingsTestTags.MCN_DIALOG_INPUT),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "提示：可输入完整机构名或关键名称，例如「杭州亚序」。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                modifier = Modifier.testTag(BlocklistSettingsTestTags.MCN_DIALOG_CONFIRM),
                onClick = {
                    if (organizationName.isNotBlank()) {
                        onConfirm(organizationName.trim())
                    }
                },
                enabled = organizationName.isNotBlank(),
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.testTag(BlocklistSettingsTestTags.MCN_DIALOG_DISMISS),
                onClick = onDismiss,
            ) {
                Text("取消")
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
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.TOPIC_CLEAR_BUTTON),
                    onClick = { showClearConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Text("清空全部")
                }
            }

            LazyColumn(
                modifier = Modifier.testTag(BlocklistSettingsTestTags.TOPIC_LIST),
            ) {
                items(topics, key = { it.topicId }) { topic ->
                    ListItem(
                        modifier = Modifier.testTag(BlocklistSettingsTestTags.topicItem(topic.topicId)),
                        headlineContent = { Text(topic.topicName) },
                        supportingContent = { Text("ID: ${topic.topicId}") },
                        trailingContent = {
                            IconButton(
                                modifier = Modifier.testTag(BlocklistSettingsTestTags.topicDelete(topic.topicId)),
                                onClick = { onDeleteTopic(topic) },
                            ) {
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
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.TOPIC_CLEAR_CONFIRM),
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
                TextButton(
                    modifier = Modifier.testTag(BlocklistSettingsTestTags.TOPIC_CLEAR_DISMISS),
                    onClick = { showClearConfirmDialog = false },
                ) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(BlocklistSettingsTestTags.TOPIC_DIALOG_ID_INPUT),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = topicName,
                    onValueChange = { topicName = it },
                    label = { Text("主题名称") },
                    placeholder = { Text("例如: 娱乐") },
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
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.testTag(BlocklistSettingsTestTags.TOPIC_DIALOG_DISMISS),
                onClick = onDismiss,
            ) {
                Text("取消")
            }
        },
    )
}
