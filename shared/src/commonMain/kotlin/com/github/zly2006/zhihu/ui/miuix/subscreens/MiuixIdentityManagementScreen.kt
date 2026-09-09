/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.subscreens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwitchAccount
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
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.account.ZhihuIdentityAccount
import com.github.zly2006.zhihu.account.ZhihuSavedAccount
import com.github.zly2006.zhihu.account.createSubIdentityAccount
import com.github.zly2006.zhihu.account.fetchIdentityAccounts
import com.github.zly2006.zhihu.account.rememberZhihuAccountStore
import com.github.zly2006.zhihu.account.switchIdentityAccount
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.requestLoginNavigation
import com.github.zly2006.zhihu.platform.rememberSettingBoolean
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.miuix.components.MiuixConfirmDialog
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import com.github.zly2006.zhihu.ui.subscreens.IDENTITY_MANAGEMENT_CREATE_CONFIRM_TAG
import com.github.zly2006.zhihu.ui.subscreens.IDENTITY_MANAGEMENT_CREATE_TAG
import com.github.zly2006.zhihu.ui.subscreens.IDENTITY_MANAGEMENT_RETRY_TAG
import com.github.zly2006.zhihu.ui.subscreens.IDENTITY_MANAGEMENT_SCREEN_TAG
import com.github.zly2006.zhihu.ui.subscreens.IdentityManagementState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 身份管理页的 miuix 版本，对标 M3 [com.github.zly2006.zhihu.ui.subscreens.IdentityManagementScreen]。
 *
 * 账号列表、切换和创建都走 `account` 包里的同一套协议实现，两套外观共享同一份状态语义；
 * 这里只负责渲染和二次确认。
 */
@Composable
fun MiuixIdentityManagementScreen() {
    val navigator = LocalNavigator.current
    val accountStore = rememberZhihuAccountStore()
    val userMessages = rememberUserMessageSink()
    val coroutineScope = rememberCoroutineScope()
    val settings = rememberSettingsStore()
    val blurEnabled = rememberSettingBoolean("blurEnabled", true, settings)
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    val scrollBehavior = MiuixScrollBehavior()

    var state by remember(accountStore) {
        mutableStateOf(
            IdentityManagementState(
                currentAccountId = accountStore.session.profile
                    ?.id
                    .orEmpty(),
                loading = true,
            ),
        )
    }
    var switchTarget by remember { mutableStateOf<ZhihuIdentityAccount?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var acceptedCreateRules by remember { mutableStateOf(false) }
    var switchLoginAccount by remember { mutableStateOf<ZhihuSavedAccount?>(null) }
    var removeLoginAccount by remember { mutableStateOf<ZhihuSavedAccount?>(null) }
    val savedAccounts by accountStore.accountsState.collectAsState()

    suspend fun refresh() {
        if (state.switchingToAccountId != null || state.creating) return
        state = state.copy(loading = true, errorMessage = null)
        state = try {
            state.copy(
                accounts = accountStore.fetchIdentityAccounts(),
                currentAccountId = accountStore.session.profile
                    ?.id
                    .orEmpty(),
                loading = false,
            )
        } catch (e: Exception) {
            state.copy(loading = false, errorMessage = e.message ?: "获取身份列表失败")
        }
    }

    LaunchedEffect(accountStore) { refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(backdrop),
                color = backdrop.getMiuixAppBarColor(),
                title = "身份管理",
                navigationIcon = {
                    IconButton(onClick = { navigator.onNavigateBack() }) {
                        Icon(MiuixIconsEmbedded.Back, "返回", tint = MiuixTheme.colorScheme.onBackground)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .testTag(IDENTITY_MANAGEMENT_SCREEN_TAG),
            contentPadding = innerPadding,
        ) {
            item { Spacer(Modifier.size(12.dp)) }

            if (state.loading && state.accounts.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            state.errorMessage?.let { errorMessage ->
                item {
                    Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                        ArrowPreference(
                            modifier = Modifier.testTag(IDENTITY_MANAGEMENT_RETRY_TAG),
                            title = "加载失败",
                            summary = errorMessage,
                            startAction = { Icon(Icons.Default.ErrorOutline, null) },
                            endActions = { Icon(Icons.Default.Refresh, "重试") },
                            enabled = !state.busy,
                            onClick = { coroutineScope.launch { refresh() } },
                        )
                    }
                }
            }

            if (state.accounts.isNotEmpty()) {
                item { SmallTitle(text = "当前手机号下的账号") }
                item {
                    Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                        state.accounts.forEachIndexed { index, account ->
                            val isCurrent = account.id == state.currentAccountId
                            val isSwitching = account.id == state.switchingToAccountId
                            ArrowPreference(
                                modifier = Modifier.testTag("identityManagement.account.$index"),
                                title = account.name,
                                summary = when (account.accountType) {
                                    1 -> "主账号"
                                    2 -> "马甲号"
                                    else -> "知乎账号"
                                },
                                startAction = {
                                    AsyncImage(
                                        model = account.avatarUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(CircleShape),
                                    )
                                },
                                endActions = {
                                    when {
                                        isSwitching -> CircularProgressIndicator(Modifier.size(20.dp))
                                        isCurrent -> Text("当前登录", color = MiuixTheme.colorScheme.primary)
                                        else -> Icon(Icons.Default.SwitchAccount, "切换")
                                    }
                                },
                                enabled = !state.busy && !isCurrent,
                                onClick = { switchTarget = account },
                            )
                        }
                    }
                }

                item { SmallTitle(text = "其他登录账号") }
                item {
                    Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                        savedAccounts.accounts
                            .filterNot { it.id == savedAccounts.activeAccountId }
                            .forEach { account ->
                                ArrowPreference(
                                    title = account.session.profile?.name ?: account.session.username,
                                    summary = "切换到这个登录账号",
                                    startAction = { Icon(Icons.Default.SwitchAccount, null) },
                                    endActions = {
                                        IconButton(onClick = { removeLoginAccount = account }) {
                                            Icon(Icons.Default.DeleteOutline, "移除登录账号")
                                        }
                                    },
                                    onClick = { switchLoginAccount = account },
                                )
                            }
                        ArrowPreference(
                            title = "添加其他手机号登录账号",
                            startAction = { Icon(Icons.AutoMirrored.Filled.Login, null) },
                            onClick = ::requestLoginNavigation,
                        )
                    }
                }

                item { SmallTitle(text = "新账号") }
                item {
                    Card(Modifier.padding(horizontal = 12.dp)) {
                        ArrowPreference(
                            modifier = Modifier.testTag(IDENTITY_MANAGEMENT_CREATE_TAG),
                            title = "创建新账号",
                            startAction = { Icon(Icons.Default.Add, null) },
                            endActions = {
                                if (state.creating) CircularProgressIndicator(Modifier.size(20.dp))
                            },
                            enabled = state.canCreateSubAccount && !state.busy,
                            onClick = {
                                acceptedCreateRules = false
                                showCreateDialog = true
                            },
                        )
                    }
                }
                item {
                    Text(
                        text = when {
                            state.canCreateSubAccount ->
                                "新账号会先使用系统昵称完成初始化。昵称修改受知乎次数限制，本客户端不会自动改名。"
                            state.accounts.size >= 2 -> "当前手机号下已经存在主账号和马甲号。"
                            else -> "当前登录账号暂不满足创建新账号的条件。"
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.footnote1,
                    )
                }
            } else if (!state.loading && state.errorMessage == null) {
                item {
                    Card(Modifier.padding(horizontal = 12.dp)) {
                        ArrowPreference(
                            title = "未找到可管理的账号",
                            summary = "请确认当前登录状态后重试",
                            startAction = { Icon(Icons.Default.ErrorOutline, null) },
                        )
                    }
                }
            }

            item { Spacer(Modifier.size(24.dp)) }
        }
    }

    val pendingSwitchTarget = switchTarget
    MiuixConfirmDialog(
        show = pendingSwitchTarget != null,
        title = "切换账号",
        summary = pendingSwitchTarget
            ?.let {
                "将切换到“${it.name}”。之后的推荐、内容和互动行为都属于该账号。"
            }.orEmpty(),
        confirmText = "切换",
        onConfirm = {
            val account = pendingSwitchTarget ?: return@MiuixConfirmDialog
            switchTarget = null
            coroutineScope.launch {
                state = state.copy(switchingToAccountId = account.id, errorMessage = null)
                try {
                    val result = accountStore.switchIdentityAccount(account.id)
                    state = state.copy(currentAccountId = result.account.id, switchingToAccountId = null)
                    userMessages.showShortMessage("已切换到 ${account.name}")
                    navigator.onNavigateBack()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    state = state.copy(switchingToAccountId = null, errorMessage = e.message ?: "切换账号失败")
                    userMessages.showLongMessage(e.message ?: "切换账号失败")
                }
            }
        },
        onDismiss = { if (!state.busy) switchTarget = null },
    )

    WindowDialog(
        show = showCreateDialog,
        title = "新账号使用规则",
        onDismissRequest = { if (!state.busy) showCreateDialog = false },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "您即将创建的新账号，与当前账号共用同一手机号，为两个相互独立的账号身份。" +
                    "新账号拥有独立的昵称、头像与个人主页，账号之间的内容、数据、互动行为完全隔离。" +
                    "两个账号均需遵守社区规范与用户协议，任一账号存在违规行为，可能影响同一手机号下其他账号的使用权限。",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !state.busy) { acceptedCreateRules = !acceptedCreateRules },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    state = if (acceptedCreateRules) ToggleableState.On else ToggleableState.Off,
                    onClick = { acceptedCreateRules = !acceptedCreateRules },
                    enabled = !state.busy,
                )
                Spacer(Modifier.size(8.dp))
                Text("我已知悉创建机会有限，创建后将立即切换")
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    text = "取消",
                    onClick = { showCreateDialog = false },
                    enabled = !state.busy,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        showCreateDialog = false
                        coroutineScope.launch {
                            state = state.copy(creating = true, errorMessage = null)
                            try {
                                val result = accountStore.createSubIdentityAccount()
                                state = state.copy(
                                    accounts = state.accounts + result.account,
                                    currentAccountId = result.account.id,
                                    creating = false,
                                )
                                userMessages.showLongMessage("新账号已创建并初始化，正在重新加载")
                                navigator.onNavigateBack()
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                state = state.copy(creating = false, errorMessage = e.message ?: "创建新账号失败")
                                userMessages.showLongMessage(e.message ?: "创建新账号失败")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).testTag(IDENTITY_MANAGEMENT_CREATE_CONFIRM_TAG),
                    enabled = acceptedCreateRules && !state.busy,
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) {
                    Text("同意并创建")
                }
            }
        }
    }

    val pendingSwitchLogin = switchLoginAccount
    MiuixConfirmDialog(
        show = pendingSwitchLogin != null,
        title = "切换登录账号",
        summary = pendingSwitchLogin
            ?.let {
                "将切换到“${it.session.profile?.name ?: it.session.username}”。"
            }.orEmpty(),
        confirmText = "切换",
        onConfirm = {
            val account = pendingSwitchLogin ?: return@MiuixConfirmDialog
            switchLoginAccount = null
            coroutineScope.launch {
                try {
                    if (!accountStore.switchAccount(account.id)) {
                        userMessages.showLongMessage("登录凭据已失效，请重新添加这个账号")
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    userMessages.showLongMessage(e.message ?: "切换登录账号失败")
                }
            }
        },
        onDismiss = { switchLoginAccount = null },
    )

    val pendingRemoveLogin = removeLoginAccount
    MiuixConfirmDialog(
        show = pendingRemoveLogin != null,
        title = "移除登录账号",
        summary = pendingRemoveLogin
            ?.let {
                "将从本机删除“${it.session.profile?.name ?: it.session.username}”的登录凭据，不会注销知乎账号。"
            }.orEmpty(),
        confirmText = "移除",
        onConfirm = {
            pendingRemoveLogin?.let { accountStore.removeAccount(it.id) }
            removeLoginAccount = null
        },
        onDismiss = { removeLoginAccount = null },
    )
}
