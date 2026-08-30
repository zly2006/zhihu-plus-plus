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
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
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
import com.github.zly2006.zhihu.account.SwitchAccountRequest
import com.github.zly2006.zhihu.account.ZhihuAccountProfileSnapshot
import com.github.zly2006.zhihu.account.ZhihuAccountStore
import com.github.zly2006.zhihu.account.ZhihuIdentityAccount
import com.github.zly2006.zhihu.account.ZhihuIdentityAccountListResponse
import com.github.zly2006.zhihu.account.ZhihuIdentityChangeResult
import com.github.zly2006.zhihu.account.ZhihuIdentityProfile
import com.github.zly2006.zhihu.account.ZhihuIdentityToken
import com.github.zly2006.zhihu.account.ZhihuSavedAccount
import com.github.zly2006.zhihu.account.applyIdentityHeaders
import com.github.zly2006.zhihu.account.identitySuccessBody
import com.github.zly2006.zhihu.account.rememberZhihuAccountStore
import com.github.zly2006.zhihu.data.ZhihuJson
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.requestLoginNavigation
import com.github.zly2006.zhihu.platform.platformName
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.ui.components.SettingItem
import com.github.zly2006.zhihu.ui.components.SettingItemGroup
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject

const val IDENTITY_MANAGEMENT_SCREEN_TAG = "identityManagement.screen"
const val IDENTITY_MANAGEMENT_CREATE_TAG = "identityManagement.create"
const val IDENTITY_MANAGEMENT_RETRY_TAG = "identityManagement.retry"
const val IDENTITY_MANAGEMENT_CREATE_CONFIRM_TAG = "identityManagement.createConfirm"

expect val isIdentityManagementSupported: Boolean

data class IdentityManagementState(
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

private suspend fun completeIdentityChange(
    accountStore: ZhihuAccountStore,
    body: String,
    expectedAccountId: String? = null,
): ZhihuIdentityChangeResult {
    val token = ZhihuJson.decodeJson<ZhihuIdentityToken>(ZhihuJson.json.parseToJsonElement(body))
    check(token.accessToken.isNotBlank()) { "服务器未返回新账号凭证" }
    check(token.cookie["z_c0"].isNullOrBlank().not()) { "服务器未返回新账号 Cookie" }
    val oldSession = accountStore.session
    val newCookies = oldSession.cookies.toMutableMap().apply { putAll(token.cookie) }
    val client = accountStore.client.temporaryHttpClient(newCookies)
    return try {
        val response = client.get("https://api.zhihu.com/people/self") {
            applyIdentityHeaders(
                oldSession.copy(
                    mobileAccessToken = token.accessToken,
                    mobileTokenType = token.tokenType,
                ),
            )
        }
        val rawProfile = ZhihuJson.json
            .parseToJsonElement(
                response.identitySuccessBody("初始化新账号"),
            ).jsonObject
        val profile = ZhihuJson.decodeJson<ZhihuIdentityProfile>(rawProfile)
        check(profile.id.isNotBlank() && profile.name.isNotBlank()) { "服务器返回的账号资料不完整" }
        check(expectedAccountId == null || profile.id == expectedAccountId) { "服务器返回的账号与目标账号不一致" }
        val nextSession = oldSession.copy(
            login = true,
            username = profile.name,
            cookies = newCookies,
            profile = ZhihuAccountProfileSnapshot(
                id = profile.id,
                name = profile.name,
                urlToken = profile.urlToken,
                userType = profile.userType,
                avatarUrl = profile.avatarUrl,
            ),
            self = ZhihuJson.snakeCaseToCamelCase(rawProfile),
            mobileAccessToken = token.accessToken,
            mobileRefreshToken = token.refreshToken,
            mobileTokenType = token.tokenType,
            mobileTokenExpiresAt = token.expiresAt,
        )
        accountStore.replaceSession(nextSession)
        ZhihuIdentityChangeResult(
            account = ZhihuIdentityAccount(
                id = profile.id,
                urlToken = profile.urlToken,
                name = profile.name,
                avatarUrl = profile.avatarUrl,
                isActive = true,
                canCreateSubAccount = profile.canCreateSubAccount,
                accountType = profile.accountType,
                subAccountControlStatus = profile.subAccountControlStatus,
            ),
            session = nextSession,
        )
    } finally {
        client.close()
    }
}

/**
 * “身份管理”页面。
 *
 * 账号列表来自 `/people/account/list`。切换和创建都会签发一套新的会话凭证，并由账户 store 原子替换当前会话和
 * 与它绑定的客户端。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityManagementScreen() {
    if (!isIdentityManagementSupported) {
        error("$platformName 暂不支持身份管理")
    }
    val navigator = LocalNavigator.current
    val accountStore = rememberZhihuAccountStore()
    val userMessages = rememberUserMessageSink()
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
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
        if (!isIdentityManagementSupported) return
        if (state.switchingToAccountId != null || state.creating) return
        state = state.copy(loading = true, errorMessage = null)
        state = try {
            state.copy(
                accounts = ZhihuJson
                    .decodeJson<ZhihuIdentityAccountListResponse>(
                        ZhihuJson.json.parseToJsonElement(
                            accountStore.client
                                .httpClient()
                                .get("https://api.zhihu.com/people/account/list") {
                                    applyIdentityHeaders(accountStore.session)
                                }.identitySuccessBody("获取身份列表"),
                        ),
                    ).data,
                currentAccountId = accountStore.session.profile
                    ?.id
                    .orEmpty(),
                loading = false,
            )
        } catch (e: Exception) {
            state.copy(loading = false, errorMessage = e.message ?: "获取身份列表失败")
        }
    }

    LaunchedEffect(accountStore) {
        refresh()
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
                                refresh()
                            }
                        },
                    )
                }
            }

            if (state.accounts.isNotEmpty()) {
                SettingItemGroup(
                    title = "当前手机号下的账号",
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
                val otherAccounts = savedAccounts.accounts.filterNot { it.id == savedAccounts.activeAccountId }
                SettingItemGroup(
                    title = "其他登录账号",
                ) {
                    otherAccounts.forEach { account ->
                        SettingItem(
                            title = { Text(account.session.profile?.name ?: account.session.username) },
                            description = { Text("切换到这个登录账号") },
                            icon = { Icon(Icons.Default.SwitchAccount, null) },
                            endAction = {
                                IconButton(onClick = { removeLoginAccount = account }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "移除登录账号")
                                }
                            },
                            onClick = { switchLoginAccount = account },
                        )
                    }
                    SettingItem(
                        title = { Text("添加其他手机号登录账号") },
                        icon = { Icon(Icons.AutoMirrored.Filled.Login, null) },
                        onClick = ::requestLoginNavigation,
                    )
                }

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
                Text("将切换到“${account.name}”。之后的推荐、内容和互动行为都属于该账号。")
            },
            confirmButton = {
                TextButton(
                    enabled = !state.busy,
                    onClick = {
                        switchTarget = null
                        coroutineScope.launch {
                            runCatching {
                                check(!state.busy) { "另一个账号操作正在进行" }
                                state = state.copy(switchingToAccountId = account.id, errorMessage = null)
                                try {
                                    require(account.id.isNotBlank()) { "目标账号不能为空" }
                                    val response = accountStore.client.httpClient().post(
                                        "https://api.zhihu.com/account/switch",
                                    ) {
                                        applyIdentityHeaders(accountStore.session)
                                        contentType(ContentType.Application.Json)
                                        setBody(SwitchAccountRequest(account.id))
                                    }
                                    val result = completeIdentityChange(
                                        accountStore,
                                        response.identitySuccessBody("切换账号"),
                                        account.id,
                                    )
                                    state = state.copy(
                                        currentAccountId = result.account.id,
                                        switchingToAccountId = null,
                                    )
                                } catch (e: Exception) {
                                    state = state.copy(
                                        switchingToAccountId = null,
                                        errorMessage = e.message ?: "切换账号失败",
                                    )
                                    throw e
                                }
                            }.onSuccess {
                                userMessages.showShortMessage("已切换到 ${account.name}")
                                navigator.onNavigateBack()
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
                                check(!state.busy) { "另一个账号操作正在进行" }
                                check(state.canCreateSubAccount) { "当前账号暂不能创建新账号" }
                                state = state.copy(creating = true, errorMessage = null)
                                try {
                                    val response = accountStore.client.httpClient().post(
                                        "https://api.zhihu.com/account/sub/register",
                                    ) {
                                        applyIdentityHeaders(accountStore.session)
                                    }
                                    val result = completeIdentityChange(
                                        accountStore,
                                        response.identitySuccessBody("创建新账号"),
                                    )
                                    state = state.copy(
                                        accounts = state.accounts + result.account,
                                        currentAccountId = result.account.id,
                                        creating = false,
                                    )
                                } catch (e: Exception) {
                                    state = state.copy(
                                        creating = false,
                                        errorMessage = e.message ?: "创建新账号失败",
                                    )
                                    throw e
                                }
                            }.onSuccess {
                                userMessages.showLongMessage("新账号已创建并初始化，正在重新加载")
                                navigator.onNavigateBack()
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

    switchLoginAccount?.let { account ->
        AlertDialog(
            onDismissRequest = { switchLoginAccount = null },
            title = { Text("切换登录账号") },
            text = { Text("将切换到“${account.session.profile?.name ?: account.session.username}”。") },
            confirmButton = {
                TextButton(
                    onClick = {
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
                ) {
                    Text("切换")
                }
            },
            dismissButton = {
                TextButton(onClick = { switchLoginAccount = null }) {
                    Text("取消")
                }
            },
        )
    }

    removeLoginAccount?.let { account ->
        AlertDialog(
            onDismissRequest = { removeLoginAccount = null },
            title = { Text("移除登录账号") },
            text = { Text("将从本机删除“${account.session.profile?.name ?: account.session.username}”的登录凭据，不会注销知乎账号。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        accountStore.removeAccount(account.id)
                        removeLoginAccount = null
                    },
                ) {
                    Text("移除")
                }
            },
            dismissButton = {
                TextButton(onClick = { removeLoginAccount = null }) {
                    Text("取消")
                }
            },
        )
    }
}
