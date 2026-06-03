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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.AccountSettingsAccountState
import com.github.zly2006.zhihu.ui.rememberAccountSettingsPlatformRuntime
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
    testAccountData: AccountSettingsAccountState? = null,
) {
    val navigator = LocalNavigator.current
    val runtime = rememberAccountSettingsPlatformRuntime()
    val settings = rememberSettingsStore()
    val accountState by runtime.accountState
    val data = testAccountData ?: accountState
    val blurEnabled = remember { settings.getBoolean("blurEnabled", true) }
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    val scrollBehavior = MiuixScrollBehavior()

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
                            IconButton(onClick = { runtime.requestQrLoginScan() }) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "扫码登录", tint = MiuixTheme.colorScheme.onSurface)
                            }
                        }
                    } else {
                        ArrowPreference(
                            title = "登录知乎",
                            summary = "登录后体验完整功能",
                            startAction = { Icon(Icons.AutoMirrored.Filled.Login, null, tint = MiuixTheme.colorScheme.primary) },
                            onClick = { runtime.requestLogin() },
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
                            summary = if (unreadCount > 0) "$unreadCount 条未读" else null,
                            onClick = { navigator.onNavigate(Notification) },
                            startAction = { Icon(Icons.Default.Notifications, null) },
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
                            onClick = { runtime.logout() },
                            startAction = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MiuixTheme.colorScheme.error) },
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
