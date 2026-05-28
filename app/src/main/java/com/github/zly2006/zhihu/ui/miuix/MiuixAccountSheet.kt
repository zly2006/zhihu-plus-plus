/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.LoginActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Collections
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.theme.AppTokens
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
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
    onDismiss: () -> Unit,
    testAccountData: AccountData.Data? = null,
) {
    // Only mount WindowBottomSheet when show is true, clean up on dispose
    if (!show) return

    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE) }
    val data = testAccountData ?: AccountData.asState().let { val v by it; v }

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
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            onDismiss()
                            navigator.onNavigate(Person(id = data.self?.id ?: "", urlToken = data.self?.urlToken ?: "", name = data.username))
                        }.padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(data.self?.avatarUrl, "头像", modifier = Modifier.size(56.dp).clip(CircleShape))
                        Spacer(Modifier.width(12.dp))
                        Text(data.username, style = AppTokens.text.titleMedium, modifier = Modifier.weight(1f))
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
                            onClick = { onDismiss(); context.startActivity(Intent(context, LoginActivity::class.java)) },
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
                        ArrowPreference(title = "收藏夹", onClick = { onDismiss(); data.self?.urlToken?.let { navigator.onNavigate(Collections(it)) } }, startAction = { Icon(Icons.Default.BookmarkBorder, null) })
                        ArrowPreference(title = "关注订阅", onClick = { onDismiss(); navigator.onNavigate(Person(id = data.self?.id ?: "", urlToken = data.self?.urlToken ?: "", name = data.username, jumpTo = "关注订阅")) }, startAction = { Icon(Icons.Default.Groups, null) })
                        ArrowPreference(title = "通知", summary = if (unreadCount > 0) "$unreadCount 条未读" else null, onClick = { onDismiss(); navigator.onNavigate(Notification) }, startAction = { Icon(Icons.Default.Notifications, null) })
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
                    ArrowPreference(title = "外观与阅读体验", summary = "主题颜色、字体大小等", onClick = { onDismiss(); navigator.onNavigate(Account.AppearanceSettings()) }, startAction = { Icon(Icons.Default.Palette, null) })
                    ArrowPreference(title = "推荐系统与内容过滤", summary = "推荐、智能过滤、关键词屏蔽等", onClick = { onDismiss(); navigator.onNavigate(Account.RecommendSettings()) }, startAction = { Icon(Icons.Default.FilterAlt, null) })
                    if (preferences.getBoolean("developer", false)) {
                        ArrowPreference(title = "开发者选项", onClick = { onDismiss(); navigator.onNavigate(Account.DeveloperSettings) }, startAction = { Icon(Icons.Default.Code, null) })
                    }
                }
            }

            // ── 关于 ──
            item {
                Card(
                    modifier = Modifier.padding(bottom = 12.dp),
                    colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
                ) {
                    ArrowPreference(title = "关于", onClick = { onDismiss(); navigator.onNavigate(Account.About) }, startAction = { Icon(Icons.Default.Info, null) })
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
                            onClick = { onDismiss(); AccountData.delete(context); Toast.makeText(context, "已退出登录", Toast.LENGTH_SHORT).show() },
                            startAction = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MiuixTheme.colorScheme.error) },
                        )
                    }
                }
            }
        }
    }
}
