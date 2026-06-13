/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Collections
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.OnlineHistory
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.theme.AppTokens
import com.github.zly2006.zhihu.ui.AccountSettingsAccountState
import com.github.zly2006.zhihu.ui.rememberAccountSettingsPlatformRuntime
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowBottomSheet

@Composable
fun MiuixAccountSheet(
    show: Boolean,
    unreadCount: Int = 0,
    showUnreadBadge: Boolean = true,
    onDismiss: () -> Unit,
    testAccountData: AccountSettingsAccountState? = null,
) {
    // Only mount WindowBottomSheet when show is true, clean up on dispose
    if (!show) return

    val navigator = LocalNavigator.current
    val runtime = rememberAccountSettingsPlatformRuntime()
    val settings = rememberSettingsStore()
    val accountState by runtime.accountState
    val data = testAccountData ?: accountState

    WindowBottomSheet(
        show = true,
        onDismissRequest = onDismiss,
        title = "账号设置",
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().scrollEndHaptic().overScrollVertical(),
        ) {
            // ── 用户信息 ──
            if (data.login) {
                item {
                    Card(
                        modifier = Modifier.padding(bottom = 12.dp),
                        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    navigator.onNavigate(Person(id = data.id, urlToken = data.urlToken ?: "", name = data.username))
                                }.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(data.avatarUrl, "头像", modifier = Modifier.size(56.dp).clip(CircleShape))
                            Spacer(Modifier.width(12.dp))
                            Text(data.username, style = AppTokens.text.titleMedium, modifier = Modifier.weight(1f))
                            // 扫码登录：协助电脑端登录，扫到知乎登录二维码后打开 WebView 确认（与 M3 账号页一致）
                            IconButton(onClick = { runtime.requestQrLoginScan() }) {
                                Icon(MiuixIconsEmbedded.Scan, contentDescription = "扫码登录", tint = MiuixTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            } else {
                item {
                    SmallTitle(text = "账号", insideMargin = PaddingValues(16.dp, 8.dp))
                    Card(
                        modifier = Modifier.padding(bottom = 12.dp),
                        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
                    ) {
                        ArrowPreference(
                            title = "登录知乎",
                            onClick = {
                                onDismiss()
                                runtime.requestLogin()
                            },
                            startAction = { Icon(MiuixIconsEmbedded.ContactsCircle, null) },
                        )
                    }
                }
            }

            // ── 快捷入口 ──
            if (data.login) {
                item {
                    SmallTitle(text = "快捷入口", insideMargin = PaddingValues(16.dp, 8.dp))
                    Card(
                        modifier = Modifier.padding(bottom = 12.dp),
                        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
                    ) {
                        ArrowPreference(title = "收藏夹", onClick = {
                            onDismiss()
                            data.urlToken?.let { navigator.onNavigate(Collections(it)) }
                        }, startAction = { Icon(MiuixIconsEmbedded.Favorites, null) })
                        ArrowPreference(title = "关注订阅", onClick = {
                            onDismiss()
                            navigator.onNavigate(Person(id = data.id, urlToken = data.urlToken ?: "", name = data.username, jumpTo = "关注订阅"))
                        }, startAction = { Icon(MiuixIconsEmbedded.ContactsBook, null) })
                        ArrowPreference(title = "通知", summary = if (showUnreadBadge && unreadCount > 0) "$unreadCount 条未读" else null, onClick = {
                            onDismiss()
                            navigator.onNavigate(Notification)
                        }, startAction = { Icon(MiuixIconsEmbedded.Messages, null) })
                        ArrowPreference(title = "浏览历史", onClick = {
                            onDismiss()
                            navigator.onNavigate(OnlineHistory)
                        }, startAction = { Icon(MiuixIconsEmbedded.Recent, null) })
                    }
                }
            }

            // ── 设置 ──
            item {
                SmallTitle(text = "设置", insideMargin = PaddingValues(16.dp, 8.dp))
                Card(
                    modifier = Modifier.padding(bottom = 12.dp),
                    colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
                ) {
                    ArrowPreference(title = "外观与阅读体验", summary = "主题颜色、字体大小等", onClick = {
                        onDismiss()
                        navigator.onNavigate(Account.AppearanceSettings())
                    }, startAction = { Icon(MiuixIconsEmbedded.Theme, null) })
                    ArrowPreference(title = "推荐系统与内容过滤", summary = "推荐、智能过滤、关键词屏蔽等", onClick = {
                        onDismiss()
                        navigator.onNavigate(Account.RecommendSettings())
                    }, startAction = { Icon(MiuixIconsEmbedded.Filter, null) })
                    ArrowPreference(title = "系统与更新", summary = "GitHub、更新设置等", onClick = {
                        onDismiss()
                        navigator.onNavigate(Account.SystemAndUpdateSettings)
                    }, startAction = { Icon(MiuixIconsEmbedded.Settings, null) })
                    if (settings.getBoolean("developer", false)) {
                        ArrowPreference(title = "开发者选项", onClick = {
                            onDismiss()
                            navigator.onNavigate(Account.DeveloperSettings)
                        }, startAction = { Icon(MiuixIconsEmbedded.File, null) })
                    }
                }
            }

            // ── 关于 ──
            item {
                Card(
                    modifier = Modifier.padding(bottom = 12.dp),
                    colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
                ) {
                    ArrowPreference(title = "关于", onClick = {
                        onDismiss()
                        navigator.onNavigate(Account.About)
                    }, startAction = { Icon(MiuixIconsEmbedded.Info, null) })
                    ArrowPreference(title = "开源许可", summary = "查看第三方组件许可证", onClick = {
                        onDismiss()
                        navigator.onNavigate(Account.OpenSourceLicenses)
                    }, startAction = { Icon(MiuixIconsEmbedded.Info, null) })
                }
            }

            // ── 退出登录 ──
            if (data.login) {
                item {
                    Card(
                        modifier = Modifier.padding(bottom = 12.dp),
                        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
                    ) {
                        ArrowPreference(
                            title = "退出登录",
                            onClick = {
                                onDismiss()
                                runtime.logout()
                            },
                            startAction = { Icon(MiuixIconsEmbedded.Close, null, tint = MiuixTheme.colorScheme.error) },
                        )
                    }
                }
            }
        }
    }
}
