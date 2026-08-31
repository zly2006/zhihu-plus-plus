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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.theme.AppTokens
import com.github.zly2006.zhihu.ui.AccountSettingsAccountState
import com.github.zly2006.zhihu.ui.rememberAccountSettingsAccountState
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
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
    val environment = rememberPaginationEnvironment(allowGuestAccess = false)
    val accountStore = rememberZhihuAccountStore()
    val userMessages = rememberUserMessageSink()
    val settings = rememberSettingsStore()
    val accountState by rememberAccountSettingsAccountState()
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
                                requestLoginNavigation()
                            },
                            startAction = { Icon(Icons.AutoMirrored.Filled.Login, null) },
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
                        }, startAction = { Icon(Icons.Default.BookmarkBorder, null) })
                        ArrowPreference(title = "关注订阅", onClick = {
                            onDismiss()
                            navigator.onNavigate(Person(id = data.id, urlToken = data.urlToken ?: "", name = data.username, jumpTo = "关注订阅"))
                        }, startAction = { Icon(Icons.Default.Groups, null) })
                        ArrowPreference(title = "通知", summary = if (showUnreadBadge && unreadCount > 0) "$unreadCount 条未读" else null, onClick = {
                            onDismiss()
                            navigator.onNavigate(Notification)
                        }, startAction = { Icon(Icons.Default.Notifications, null) })
                        ArrowPreference(title = "浏览历史", onClick = {
                            onDismiss()
                            navigator.onNavigate(OnlineHistory)
                        }, startAction = { Icon(Icons.Default.History, null) })
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
                    }, startAction = { Icon(Icons.Default.Palette, null) })
                    ArrowPreference(title = "推荐系统与内容过滤", summary = "推荐、智能过滤、关键词屏蔽等", onClick = {
                        onDismiss()
                        navigator.onNavigate(Account.RecommendSettings())
                    }, startAction = { Icon(Icons.Default.FilterAlt, null) })
                    ArrowPreference(title = "系统与更新", summary = "GitHub、更新设置等", onClick = {
                        onDismiss()
                        navigator.onNavigate(Account.SystemAndUpdateSettings())
                    }, startAction = { Icon(Icons.Default.Settings, null) })
                    if (settings.getBoolean("developer", false)) {
                        ArrowPreference(title = "开发者选项", onClick = {
                            onDismiss()
                            navigator.onNavigate(Account.DeveloperSettings)
                        }, startAction = { Icon(Icons.Default.Code, null) })
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
                    }, startAction = { Icon(Icons.Default.Info, null) })
                    ArrowPreference(title = "开源许可", summary = "查看第三方组件许可证", onClick = {
                        onDismiss()
                        navigator.onNavigate(Account.OpenSourceLicenses)
                    }, startAction = { Icon(Icons.Default.Info, null) })
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
                                accountStore.clear()
                            },
                            startAction = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MiuixTheme.colorScheme.error) },
                        )
                    }
                }
            }
        }
    }
}
