/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.account.rememberZhihuAccountStore
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Collections
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.OnlineHistory
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.requestLoginNavigation
import com.github.zly2006.zhihu.platform.rememberSettingBoolean
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.reading.isReadingPlayerSupported
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.AccountSettingsAccountState
import com.github.zly2006.zhihu.ui.miuix.components.MiuixConfirmDialog
import com.github.zly2006.zhihu.ui.rememberAccountSettingsAccountState
import com.github.zly2006.zhihu.ui.subscreens.SystemUpdateState
import com.github.zly2006.zhihu.ui.subscreens.rememberSystemUpdateState
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun MiuixAccountSettingScreen(
    innerPadding: PaddingValues = PaddingValues(0.dp),
    unreadCount: Int = 0,
    showUnreadBadge: Boolean = true,
    testAccountData: AccountSettingsAccountState? = null,
) {
    val navigator = LocalNavigator.current
    val environment = rememberPaginationEnvironment(allowGuestAccess = false)
    val readingPlayerSupported = isReadingPlayerSupported
    val accountStore = rememberZhihuAccountStore()
    val settings = rememberSettingsStore()
    val accountState by rememberAccountSettingsAccountState()
    val data = testAccountData ?: accountState
    val userMessages = rememberUserMessageSink()
    val updateState by rememberSystemUpdateState().collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    val blurEnabled = rememberSettingBoolean("blurEnabled", true, settings)
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    val scrollBehavior = MiuixScrollBehavior()

    // 登录后拉取 /me 资料（头像、用户名等），对齐 M3 账号页。否则登录后头像为空。
    LaunchedEffect(data.login) {
        if (testAccountData == null && data.login) {
            try {
                accountStore.client.refreshAndSaveProfile()
            } catch (e: Exception) {
                userMessages.showShortMessage("获取用户信息失败")
            }
        }
    }

    LaunchedEffect(updateState) {
        when (val state = updateState) {
            is SystemUpdateState.UpdateAvailable ->
                userMessages.showShortMessage(
                    "发现新${if (state.isNightly) "Nightly版本" else "正式版本"} ${state.version}",
                )
            is SystemUpdateState.Error -> userMessages.showLongMessage("检查更新失败: ${state.message}")
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(backdrop),
                color = backdrop.getMiuixAppBarColor(),
                title = "账号设置",
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + innerPadding.calculateBottomPadding(),
            ),
        ) {
            item { Spacer(Modifier.height(12.dp)) }

            // ── 用户信息 ──
            item {
                Card(
                    Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(16.dp)),
                ) {
                    if (data.login) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navigator.onNavigate(Person(id = data.id, urlToken = data.urlToken ?: "", name = data.username)) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(data.avatarUrl, "头像", modifier = Modifier.size(56.dp).clip(CircleShape))
                            Spacer(Modifier.width(12.dp))
                            Text(data.username, style = MiuixTheme.textStyles.title3, modifier = Modifier.weight(1f))
                        }
                    } else {
                        ArrowPreference(
                            title = "登录知乎",
                            summary = "登录后体验完整功能",
                            startAction = { Icon(Icons.AutoMirrored.Filled.Login, null, tint = MiuixTheme.colorScheme.primary) },
                            onClick = {
                                requestLoginNavigation()
                            },
                        )
                    }
                }
            }

            // ── 快捷入口 ──
            if (data.login) {
                item { SmallTitle(text = "快捷入口") }
                item {
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                    ) {
                        ArrowPreference(
                            title = "收藏夹",
                            onClick = { data.urlToken?.let { navigator.onNavigate(Collections(it)) } },
                            startAction = { Icon(Icons.Default.BookmarkBorder, null) },
                        )
                        ArrowPreference(
                            title = "关注订阅",
                            onClick = { navigator.onNavigate(Person(id = data.id, urlToken = data.urlToken ?: "", name = data.username, jumpTo = "关注订阅")) },
                            startAction = { Icon(Icons.Default.Groups, null) },
                        )
                        ArrowPreference(
                            title = "通知",
                            summary = if (showUnreadBadge && unreadCount > 0) "$unreadCount 条未读" else null,
                            onClick = { navigator.onNavigate(Notification) },
                            startAction = { Icon(Icons.Default.Notifications, null) },
                        )
                        ArrowPreference(
                            title = "浏览历史",
                            onClick = { navigator.onNavigate(OnlineHistory) },
                            startAction = { Icon(Icons.Default.History, null) },
                        )
                    }
                }
            }

            // 设置项搜索入口，对标 M3 账号页顶部的搜索条。
            item {
                Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    ArrowPreference(
                        title = "搜索设置项",
                        onClick = { navigator.onNavigate(Account.SettingsSearch) },
                        startAction = { Icon(Icons.Default.Search, null) },
                    )
                }
            }

            // ── 设置 ──
            item { SmallTitle(text = "设置") }
            item {
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                ) {
                    if (data.login) {
                        ArrowPreference(
                            title = "身份管理",
                            summary = "创建马甲号或切换当前账号",
                            onClick = { navigator.onNavigate(Account.IdentityManagement) },
                            startAction = { Icon(Icons.Default.SwitchAccount, null) },
                        )
                    }
                    ArrowPreference(
                        title = "外观与阅读体验",
                        summary = "主题颜色、字体大小等",
                        onClick = { navigator.onNavigate(Account.AppearanceSettings()) },
                        startAction = { Icon(Icons.Default.Palette, null) },
                    )
                    if (readingPlayerSupported) {
                        ArrowPreference(
                            title = "朗读与播放",
                            summary = "朗读内容、播放队列与条目过渡",
                            onClick = { navigator.onNavigate(Account.ReadingSettings) },
                            startAction = { Icon(Icons.AutoMirrored.Filled.VolumeUp, null) },
                        )
                    }
                    ArrowPreference(
                        title = "推荐系统与内容过滤",
                        summary = "推荐、智能过滤、关键词屏蔽等",
                        onClick = { navigator.onNavigate(Account.RecommendSettings()) },
                        startAction = { Icon(Icons.Default.FilterAlt, null) },
                    )
                    ArrowPreference(
                        title = "系统与更新",
                        summary = "GitHub、更新设置等",
                        onClick = { navigator.onNavigate(Account.SystemAndUpdateSettings()) },
                        startAction = { Icon(Icons.Default.Settings, null) },
                    )
                    if (settings.getBoolean("developer", false)) {
                        ArrowPreference(
                            title = "开发者选项",
                            onClick = { navigator.onNavigate(Account.DeveloperSettings) },
                            startAction = { Icon(Icons.Default.Code, null) },
                        )
                    }
                }
            }

            // ── 关于 ──
            item { SmallTitle(text = "关于") }
            item {
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                ) {
                    ArrowPreference(
                        title = "关于",
                        summary = "关于zhihu++",
                        onClick = { navigator.onNavigate(Account.About) },
                        startAction = { Icon(Icons.Default.Info, null) },
                    )
                    ArrowPreference(
                        title = "开源许可",
                        summary = "查看第三方组件许可证",
                        onClick = { navigator.onNavigate(Account.OpenSourceLicenses) },
                        startAction = { Icon(Icons.Default.Info, null) },
                    )
                }
            }

            // ── 退出登录 ──
            if (data.login) {
                item {
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                    ) {
                        ArrowPreference(
                            title = "退出登录",
                            onClick = { showLogoutDialog = true },
                            startAction = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MiuixTheme.colorScheme.error) },
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    MiuixConfirmDialog(
        show = showLogoutDialog,
        title = "退出登录",
        summary = if (accountStore.accounts.size > 1) {
            "确定退出并移除当前登录账号吗？退出后会自动切换到另一个已保存账号。"
        } else {
            "确定要退出登录吗？"
        },
        confirmText = "退出",
        onConfirm = {
            accountStore.clear()
            showLogoutDialog = false
        },
        onDismiss = { showLogoutDialog = false },
    )
}
