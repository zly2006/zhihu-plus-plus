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
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Collections
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.OnlineHistory
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.shared.platform.rememberSettingBoolean
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.AccountSettingsAccountState
import com.github.zly2006.zhihu.ui.rememberAccountLoginRequester
import com.github.zly2006.zhihu.ui.rememberAccountLogoutAction
import com.github.zly2006.zhihu.ui.rememberAccountProfileRefresher
import com.github.zly2006.zhihu.ui.rememberAccountQrLoginRequester
import com.github.zly2006.zhihu.ui.rememberAccountSettingsAccountState
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
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
    val refreshProfile = rememberAccountProfileRefresher()
    val requestLogin = rememberAccountLoginRequester()
    val requestQrLoginScan = rememberAccountQrLoginRequester()
    val logout = rememberAccountLogoutAction()
    val settings = rememberSettingsStore()
    val accountState by rememberAccountSettingsAccountState()
    val data = testAccountData ?: accountState
    val userMessages = rememberUserMessageSink()
    val blurEnabled = rememberSettingBoolean("blurEnabled", true, settings)
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    val scrollBehavior = MiuixScrollBehavior()

    // 登录后拉取 /me 资料（头像、用户名等），对齐 M3 账号页。否则登录后头像为空。
    LaunchedEffect(data.login) {
        if (testAccountData == null && data.login) {
            try {
                refreshProfile()
            } catch (e: Exception) {
                userMessages.showShortMessage("获取用户信息失败")
            }
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
                            // 扫码登录：协助电脑端登录，扫到知乎登录二维码后打开 WebView 确认（与 M3 账号页一致）
                            IconButton(onClick = { requestQrLoginScan() }) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "扫码登录", tint = MiuixTheme.colorScheme.onSurface)
                            }
                        }
                    } else {
                        ArrowPreference(
                            title = "登录知乎",
                            summary = "登录后体验完整功能",
                            startAction = { Icon(Icons.AutoMirrored.Filled.Login, null, tint = MiuixTheme.colorScheme.primary) },
                            onClick = { requestLogin() },
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

            // ── 设置 ──
            item { SmallTitle(text = "设置") }
            item {
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                ) {
                    ArrowPreference(
                        title = "外观与阅读体验",
                        summary = "主题颜色、字体大小等",
                        onClick = { navigator.onNavigate(Account.AppearanceSettings()) },
                        startAction = { Icon(Icons.Default.Palette, null) },
                    )
                    ArrowPreference(
                        title = "推荐系统与内容过滤",
                        summary = "推荐、智能过滤、关键词屏蔽等",
                        onClick = { navigator.onNavigate(Account.RecommendSettings()) },
                        startAction = { Icon(Icons.Default.FilterAlt, null) },
                    )
                    ArrowPreference(
                        title = "系统与更新",
                        summary = "GitHub、更新设置等",
                        onClick = { navigator.onNavigate(Account.SystemAndUpdateSettings) },
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
                            onClick = { logout() },
                            startAction = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MiuixTheme.colorScheme.error) },
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
