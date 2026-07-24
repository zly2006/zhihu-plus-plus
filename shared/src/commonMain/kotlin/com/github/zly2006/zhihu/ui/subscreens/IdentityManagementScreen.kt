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

package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.shared.account.ZhihuIdentityAccount
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.ui.components.SettingItem
import com.github.zly2006.zhihu.ui.components.SettingItemGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

const val IDENTITY_MANAGEMENT_SCREEN_TAG = "identityManagement.screen"
const val IDENTITY_MANAGEMENT_CREATE_TAG = "identityManagement.create"
const val IDENTITY_MANAGEMENT_RETRY_TAG = "identityManagement.retry"
const val IDENTITY_MANAGEMENT_CREATE_CONFIRM_TAG = "identityManagement.createConfirm"

data class IdentityManagementState(
    val supported: Boolean = true,
    val accounts: List<ZhihuIdentityAccount> = emptyList(),
    val currentAccountId: String = "",
    val loading: Boolean = false,
    val switchingToAccountId: String? = null,
    val creating: Boolean = false,
    val errorMessage: String? = null,
) {
    val busy: Boolean
        get() = loading || switchingToAccountId != null || creating

    val canCreateSubAccount: Boolean
        get() = accounts.size < 2 &&
            accounts.any {
                it.canCreateSubAccount &&
                    it.subAccountControlStatus == 0
            }
}

data class IdentityManagementRuntime(
    val state: StateFlow<IdentityManagementState>,
    val refresh: suspend () -> Unit,
    val switchAccount: suspend (targetAccountId: String) -> Unit,
    val createSubAccount: suspend () -> Unit,
    val reloadApplication: () -> Unit,
)

@Composable
expect fun rememberIdentityManagementRuntime(): IdentityManagementRuntime

fun unsupportedIdentityManagementRuntime(message: String): IdentityManagementRuntime {
    val state = MutableStateFlow(
        IdentityManagementState(
            supported = false,
            errorMessage = message,
        ),
    )
    return IdentityManagementRuntime(
        state = state,
        refresh = {},
        switchAccount = { throw UnsupportedOperationException(message) },
        createSubAccount = { throw UnsupportedOperationException(message) },
        reloadApplication = {},
    )
}

/**
 * “身份管理”页面。
 *
 * 账号列表来自 `/people/account/list`。切换和创建都会签发一套新的会话凭证，平台 runtime 保存凭证后由
 * [IdentityManagementRuntime.reloadApplication] 重建主壳，确保推荐流、历史、通知和个人资料不会继续复用旧账号缓存。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityManagementScreen() {
    val navigator = LocalNavigator.current
    val runtime = rememberIdentityManagementRuntime()
    val state by runtime.state.collectAsState()
    val userMessages = rememberUserMessageSink()
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var switchTarget by remember { mutableStateOf<ZhihuIdentityAccount?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var acceptedCreateRules by remember { mutableStateOf(false) }

    LaunchedEffect(runtime) {
        runtime.refresh()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag(IDENTITY_MANAGEMENT_SCREEN_TAG)
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeTopAppBar(
                title = { Text("身份管理") },
                navigationIcon = {
                    IconButton(
                        onClick = navigator.onNavigateBack,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(vertical = 16.dp),
        ) {
            if (!state.supported) {
                SettingItemGroup {
                    SettingItem(
                        title = { Text("当前平台暂不支持身份管理") },
                        description = { Text(state.errorMessage ?: "请在 Android 客户端使用此功能") },
                        icon = { Icon(Icons.Default.ErrorOutline, null) },
                    )
                }
                return@Column
            }

            if (state.loading && state.accounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.errorMessage?.let { errorMessage ->
                SettingItemGroup {
                    SettingItem(
                        title = { Text("加载失败") },
                        description = { Text(errorMessage) },
                        icon = { Icon(Icons.Default.ErrorOutline, null) },
                        endAction = { Icon(Icons.Default.Refresh, contentDescription = "重试") },
                        modifier = Modifier.testTag(IDENTITY_MANAGEMENT_RETRY_TAG),
                        enabled = !state.busy,
                        onClick = {
                            coroutineScope.launch {
                                runtime.refresh()
                            }
                        },
                    )
                }
            }

            if (state.accounts.isNotEmpty()) {
                SettingItemGroup(
                    title = "当前手机号下的账号",
                    footer = {
                        Text("两个账号共用当前手机号，但昵称、主页、内容、推荐和互动数据相互独立。")
                    },
                ) {
                    state.accounts.forEachIndexed { index, account ->
                        val isCurrent = account.id == state.currentAccountId
                        val isSwitching = account.id == state.switchingToAccountId
                        SettingItem(
                            title = { Text(account.name) },
                            description = {
                                Text(
                                    when (account.accountType) {
                                        1 -> "主账号"
                                        2 -> "马甲号"
                                        else -> "知乎账号"
                                    },
                                )
                            },
                            icon = {
                                AsyncImage(
                                    model = account.avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                )
                            },
                            endAction = {
                                when {
                                    isSwitching -> CircularProgressIndicator(Modifier.size(20.dp))
                                    isCurrent -> Text(
                                        "当前登录",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                    else -> Icon(Icons.Default.SwitchAccount, contentDescription = "切换")
                                }
                            },
                            modifier = Modifier.testTag("identityManagement.account.$index"),
                            enabled = !state.busy,
                            onClick = if (isCurrent) {
                                null
                            } else {
                                { switchTarget = account }
                            },
                        )
                    }
                }
            } else if (!state.loading && state.errorMessage == null) {
                SettingItemGroup {
                    SettingItem(
                        title = { Text("未找到可管理的账号") },
                        description = { Text("请确认当前登录状态后重试") },
                        icon = { Icon(Icons.Default.ErrorOutline, null) },
                    )
                }
            }

            if (state.accounts.isNotEmpty()) {
                SettingItemGroup(
                    title = "新账号",
                    footer = {
                        Text(
                            if (state.canCreateSubAccount) {
                                "新账号会先使用系统昵称完成初始化。昵称修改受知乎次数限制，本客户端不会自动改名。"
                            } else if (state.accounts.size >= 2) {
                                "当前手机号下已经存在主账号和马甲号。"
                            } else {
                                "当前登录账号暂不满足创建新账号的条件。"
                            },
                        )
                    },
                ) {
                    SettingItem(
                        title = { Text("创建新账号") },
                        description = { Text("共用当前手机号，数据相互独立") },
                        icon = { Icon(Icons.Default.Add, null) },
                        modifier = Modifier.testTag(IDENTITY_MANAGEMENT_CREATE_TAG),
                        enabled = state.canCreateSubAccount && !state.busy,
                        endAction = {
                            if (state.creating) {
                                CircularProgressIndicator(Modifier.size(20.dp))
                            }
                        },
                        onClick = {
                            acceptedCreateRules = false
                            showCreateDialog = true
                        },
                    )
                }
            }
        }
    }

    switchTarget?.let { account ->
        AlertDialog(
            onDismissRequest = {
                if (!state.busy) switchTarget = null
            },
            title = { Text("切换账号") },
            text = {
                Text("将切换到“${account.name}”并重新加载应用。之后的推荐、内容和互动行为都属于该账号。")
            },
            confirmButton = {
                TextButton(
                    enabled = !state.busy,
                    onClick = {
                        switchTarget = null
                        coroutineScope.launch {
                            runCatching {
                                runtime.switchAccount(account.id)
                            }.onSuccess {
                                userMessages.showShortMessage("已切换到 ${account.name}")
                                runtime.reloadApplication()
                            }.onFailure {
                                userMessages.showLongMessage(it.message ?: "切换账号失败")
                            }
                        }
                    },
                ) {
                    Text("切换")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.busy,
                    onClick = { switchTarget = null },
                ) {
                    Text("取消")
                }
            },
        )
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!state.busy) showCreateDialog = false
            },
            title = { Text("新账号使用规则") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "您即将创建的新账号，与当前账号共用同一手机号，为两个相互独立的账号身份。" +
                            "新账号拥有独立的昵称、头像与个人主页，账号之间的内容、数据、互动行为完全隔离。" +
                            "两个账号均需遵守社区规范与用户协议，任一账号存在违规行为，可能影响同一手机号下其他账号的使用权限。",
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !state.busy) {
                                acceptedCreateRules = !acceptedCreateRules
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = acceptedCreateRules,
                            enabled = !state.busy,
                            onCheckedChange = { acceptedCreateRules = it },
                        )
                        Text("我已知悉创建机会有限，创建后将立即切换")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag(IDENTITY_MANAGEMENT_CREATE_CONFIRM_TAG),
                    enabled = acceptedCreateRules && !state.busy,
                    onClick = {
                        showCreateDialog = false
                        coroutineScope.launch {
                            runCatching {
                                runtime.createSubAccount()
                            }.onSuccess {
                                userMessages.showLongMessage("新账号已创建并初始化，正在重新加载")
                                runtime.reloadApplication()
                            }.onFailure {
                                userMessages.showLongMessage(it.message ?: "创建新账号失败")
                            }
                        }
                    },
                ) {
                    Text("同意并创建")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.busy,
                    onClick = { showCreateDialog = false },
                ) {
                    Text("取消")
                }
            },
        )
    }
}
