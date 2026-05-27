/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.BuildConfig
import com.github.zly2006.zhihu.LoginActivity
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Collections
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.theme.AppTokens
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

private const val SHEET_ANIM_MS = 300L

@Composable
fun MiuixAccountSettingScreen(
    innerPadding: PaddingValues = PaddingValues(0.dp),
    unreadCount: Int = 0,
    onDismissRequest: () -> Unit = {},
    testAccountData: AccountData.Data? = null,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE) }
    val data = testAccountData ?: AccountData.asState().let { state ->
        val liveData by state; liveData
    }

    val show = rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { show.value = true }

    fun closeThen(block: () -> Unit) {
        show.value = false
        scope.launch { delay(SHEET_ANIM_MS); block() }
    }

    Box(Modifier.fillMaxSize().background(MiuixTheme.colorScheme.background)) {
    WindowBottomSheet(
        show = show.value,
        onDismissRequest = { closeThen { onDismissRequest(); navigator.onNavigateBack() } },
        title = "账号设置",
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            // ── 用户信息 ──
            if (data.login) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().clickable {
                        closeThen {
                            navigator.onNavigate(Person(
                                id = data.self?.id ?: "", urlToken = data.self?.urlToken ?: "",
                                name = data.username,
                            ))
                        }
                    }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(data.self?.avatarUrl, "头像",
                        modifier = Modifier.size(56.dp).clip(CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(data.username, style = AppTokens.text.titleMedium)
                    }
                    IconButton(onClick = {
                        show.value = false
                        scope.launch { delay(SHEET_ANIM_MS); AccountData.delete(context) }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "退出登录", tint = MiuixTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.height(8.dp))
            } else {
                Spacer(Modifier.height(12.dp))
                ArrowPreference(
                    title = "登录知乎",
                    onClick = { closeThen { context.startActivity(Intent(context, LoginActivity::class.java)) } },
                    startAction = { Icon(Icons.AutoMirrored.Filled.Login, null) },
                )
                Spacer(Modifier.height(8.dp))
            }

            // ── 快捷入口 ──
            if (data.login) {
                SmallTitle(text = "快捷入口")
                Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    ArrowPreference(
                        title = "收藏夹", onClick = { closeThen { data.self?.urlToken?.let { navigator.onNavigate(Collections(it)) } } },
                        startAction = { Icon(Icons.Default.BookmarkBorder, null) },
                    )
                    ArrowPreference(
                        title = "关注订阅", onClick = {
                            closeThen {
                                navigator.onNavigate(Person(id = data.self?.id ?: "", urlToken = data.self?.urlToken ?: "", name = data.username, jumpTo = "关注订阅"))
                            }
                        },
                        startAction = { Icon(Icons.Default.Groups, null) },
                    )
                    ArrowPreference(
                        title = "通知", onClick = { closeThen { onDismissRequest(); navigator.onNavigate(Notification) } },
                        startAction = { Icon(Icons.Default.Notifications, null) },
                    )
                }
            }

            // ── 设置 ──
            SmallTitle(text = "设置")
            Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                ArrowPreference(
                    title = "外观与阅读体验", summary = "主题颜色、字体大小等",
                    onClick = { closeThen { navigator.onNavigate(Account.AppearanceSettings()) } },
                    startAction = { Icon(Icons.Default.Palette, null) },
                )
                ArrowPreference(
                    title = "推荐系统与内容过滤", summary = "推荐、智能过滤、关键词屏蔽等",
                    onClick = { closeThen { navigator.onNavigate(Account.RecommendSettings()) } },
                    startAction = { Icon(Icons.Default.FilterAlt, null) },
                )
                ArrowPreference(
                    title = "系统与更新", summary = "GitHub、更新设置等",
                    onClick = { closeThen { navigator.onNavigate(Account.SystemAndUpdateSettings) } },
                    startAction = { Icon(Icons.Default.Settings, null) },
                )
                if (preferences.getBoolean("developer", false)) {
                    ArrowPreference(
                        title = "开发者选项",
                        onClick = { closeThen { navigator.onNavigate(Account.DeveloperSettings) } },
                        startAction = { Icon(Icons.Default.Code, null) },
                    )
                }
            }

            // ── 关于 ──
            SmallTitle(text = "关于")
            Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                ArrowPreference(
                    title = "开源许可",
                    onClick = { closeThen { navigator.onNavigate(Account.OpenSourceLicenses) } },
                )
                ArrowPreference(
                    title = "知乎++",
                    summary = "版本号：${BuildConfig.VERSION_NAME} ${BuildConfig.BUILD_TYPE}",
                )
            }

            // 退出登录确认
            if (data.login) {
                Spacer(Modifier.height(12.dp))
                TextButton(
                    text = "退出登录",
                    onClick = {
                        show.value = false
                        scope.launch { delay(SHEET_ANIM_MS); AccountData.delete(context); Toast.makeText(context, "已退出登录", Toast.LENGTH_SHORT).show() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    }
}
