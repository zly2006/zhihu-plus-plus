/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
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

package com.github.zly2006.zhihu.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.BuildConfig
import com.github.zly2006.zhihu.LoginActivity
import com.github.zly2006.zhihu.QRCodeScanActivity
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.WebviewActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Collections
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.OnlineHistory
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.ui.components.SettingItem
import com.github.zly2006.zhihu.ui.components.SettingItemGroup
import com.github.zly2006.zhihu.ui.subscreens.BOTTOM_BAR_ITEMS_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.defaultBottomBarSelectionKeys
import com.github.zly2006.zhihu.ui.subscreens.normalizeBottomBarSelection
import com.github.zly2006.zhihu.ui.subscreens.shouldShowAccountHistoryShortcut
import com.github.zly2006.zhihu.updater.UpdateManager
import com.github.zly2006.zhihu.updater.UpdateManager.UpdateState
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.util.signFetchRequest
import io.ktor.http.Url
import kotlinx.coroutines.DelicateCoroutinesApi

internal const val ACCOUNT_SETTINGS_SCROLL_TAG = "accountSettings.scroll"
internal const val ACCOUNT_SETTINGS_LOGIN_ITEM_TAG = "accountSettings.loginItem"
internal const val ACCOUNT_SETTINGS_PROFILE_HEADER_TAG = "accountSettings.profileHeader"
internal const val ACCOUNT_SETTINGS_PROFILE_NAME_TAG = "accountSettings.profileName"
internal const val ACCOUNT_SETTINGS_SHORTCUT_COLLECTIONS_TAG = "accountSettings.shortcutCollections"
internal const val ACCOUNT_SETTINGS_SHORTCUT_NOTIFICATION_TAG = "accountSettings.shortcutNotification"
internal const val ACCOUNT_SETTINGS_SHORTCUT_HISTORY_TAG = "accountSettings.shortcutHistory"
internal const val ACCOUNT_SETTINGS_APPEARANCE_TAG = "accountSettings.appearance"
internal const val ACCOUNT_SETTINGS_RECOMMEND_TAG = "accountSettings.recommend"
internal const val ACCOUNT_SETTINGS_SYSTEM_TAG = "accountSettings.system"
internal const val ACCOUNT_SETTINGS_DEVELOPER_TAG = "accountSettings.developer"
internal const val ACCOUNT_SETTINGS_LICENSES_TAG = "accountSettings.licenses"

@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun AccountSettingScreen(
    innerPadding: PaddingValues,
    unreadCount: Int = 0,
    onDismissRequest: () -> Unit = {},
    refreshAccountProfileOnEnter: Boolean = true,
    testAccountData: AccountData.Data? = null,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences(
            PREFERENCE_NAME,
            Context.MODE_PRIVATE,
        )
    }

    val useDuo3HomeAccount = remember { preferences.getBoolean("duo3_home_account", false) }
    val selectedBottomBarItemKeys = remember {
        normalizeBottomBarSelection(
            preferences
                .getStringSet(
                    BOTTOM_BAR_ITEMS_PREFERENCE_KEY,
                    defaultBottomBarSelectionKeys(useDuo3HomeAccount),
                )?.toSet() ?: defaultBottomBarSelectionKeys(useDuo3HomeAccount),
            useDuo3HomeAccount,
            enforceMinimumSelection = true,
        )
    }
    var isDeveloper by remember { mutableStateOf(preferences.getBoolean("developer", false)) }
    var clickTimes by remember { mutableIntStateOf(0) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    LaunchedEffect(isDeveloper) {
        preferences.edit {
            putBoolean("developer", isDeveloper)
        }
    }
    val liveData by AccountData.asState()
    val data = testAccountData ?: liveData

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .testTag(ACCOUNT_SETTINGS_SCROLL_TAG)
                .verticalScroll(rememberScrollState())
                .padding(padding),
        ) {
            LaunchedEffect(data.login, refreshAccountProfileOnEnter) {
                if (refreshAccountProfileOnEnter && data.login) {
                    try {
                        val response = AccountData.fetchGet(context, "https://www.zhihu.com/api/v4/me") {
                            signFetchRequest()
                        }!!
                        val self = AccountData.decodeJson<com.github.zly2006.zhihu.data.Person>(response)
                        AccountData.saveData(
                            context,
                            data.copy(self = self),
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "获取用户信息失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            if (data.login) {
                Row(
                    Modifier
                        .testTag(ACCOUNT_SETTINGS_PROFILE_HEADER_TAG)
                        .padding(16.dp, 0.dp, 16.dp, 16.dp)
                        .clickable {
                            navigator.onNavigate(
                                Person(
                                    id = data.self?.id ?: "",
                                    urlToken = data.self?.urlToken ?: "",
                                    name = data.username,
                                ),
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = data.self?.avatarUrl,
                        contentDescription = "头像",
                        modifier = Modifier
                            .size(64.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .clip(CircleShape),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = data.username,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.testTag(ACCOUNT_SETTINGS_PROFILE_NAME_TAG),
                    )
                    Spacer(Modifier.weight(1f))
                    val scanActivityLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult(),
                    ) scan@{ result ->
                        if (result.resultCode == android.app.Activity.RESULT_OK) {
                            val scanResult = result.data?.getStringExtra(QRCodeScanActivity.EXTRA_SCAN_RESULT) ?: return@scan
                            val url = Url(scanResult)
                            if (url.rawSegments.dropLast(1).lastOrNull() != "login") {
                                Toast.makeText(context, "二维码内容不正确", Toast.LENGTH_SHORT).show()
                                return@scan
                            }
                            Toast.makeText(context, "扫描成功，正在处理登录请求...", Toast.LENGTH_SHORT).show()
                            Intent(context, WebviewActivity::class.java).let {
                                it.data = scanResult.toUri()
                                context.startActivity(it)
                            }
                        }
                    }
                    FilledTonalIconButton(
                        onClick = {
                            val intent = Intent(context, QRCodeScanActivity::class.java)
                            scanActivityLauncher.launch(intent)
                        },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "扫码登录",
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    FilledTonalIconButton(
                        onClick = {
                            showLogoutDialog = true
                        },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors().copy(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "退出登录",
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            } else {
                SettingItemGroup {
                    SettingItem(
                        title = { Text("登录知乎") },
                        icon = { Icon(Icons.AutoMirrored.Filled.Login, null) },
                        modifier = Modifier.testTag(ACCOUNT_SETTINGS_LOGIN_ITEM_TAG),
                        onClick = {
                            context.startActivity(Intent(context, LoginActivity::class.java))
                        },
                    )
                }
            }

            if (useDuo3HomeAccount) {
                Row(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 32.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (data.login) {
                        Column(
                            Modifier
                                .testTag(ACCOUNT_SETTINGS_SHORTCUT_COLLECTIONS_TAG)
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable {
                                    data.self?.urlToken?.let { navigator.onNavigate(Collections(it)) }
                                }.padding(8.dp, 16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                Icons.Default.Bookmark,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "收藏夹",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Column(
                            Modifier
                                .testTag(ACCOUNT_SETTINGS_SHORTCUT_NOTIFICATION_TAG)
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable {
                                    onDismissRequest()
                                    navigator.onNavigate(Notification)
                                }.padding(8.dp, 16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge { Text(unreadCount.toString()) }
                                    }
                                },
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "通知",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        if (shouldShowAccountHistoryShortcut(useDuo3HomeAccount, selectedBottomBarItemKeys)) {
                            Column(
                                Modifier
                                    .testTag(ACCOUNT_SETTINGS_SHORTCUT_HISTORY_TAG)
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable {
                                        onDismissRequest()
                                        navigator.onNavigate(OnlineHistory)
                                    }.padding(8.dp, 16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "浏览历史",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(32.dp))
                SettingItemGroup {
                    if (data.login) {
                        SettingItem(
                            title = { Text("查看收藏夹") },
                            icon = { Icon(Icons.Default.BookmarkBorder, null) },
                            onClick = {
                                data.self?.urlToken?.let { navigator.onNavigate(Collections(it)) }
                            },
                        )
                    }
                }
            }

            SettingItemGroup {
                SettingItem(
                    title = { Text("外观与阅读体验") },
                    description = { Text("主题颜色、字体大小等") },
                    icon = { Icon(Icons.Default.Palette, null) },
                    modifier = Modifier.testTag(ACCOUNT_SETTINGS_APPEARANCE_TAG),
                    onClick = { navigator.onNavigate(Account.AppearanceSettings()) },
                )

                SettingItem(
                    title = { Text("推荐系统与内容过滤") },
                    description = { Text("推荐、智能过滤、关键词屏蔽等") },
                    icon = { Icon(Icons.Default.FilterAlt, null) },
                    modifier = Modifier.testTag(ACCOUNT_SETTINGS_RECOMMEND_TAG),
                    onClick = { navigator.onNavigate(Account.RecommendSettings()) },
                )

                SettingItem(
                    title = { Text("系统与更新") },
                    description = { Text("GitHub、更新设置等") },
                    icon = { Icon(Icons.Default.Settings, null) },
                    modifier = Modifier.testTag(ACCOUNT_SETTINGS_SYSTEM_TAG),
                    onClick = { navigator.onNavigate(Account.SystemAndUpdateSettings) },
                )

                AnimatedVisibility(isDeveloper) {
                    SettingItem(
                        title = { Text("开发者选项") },
                        icon = { Icon(Icons.Default.Code, null) },
                        modifier = Modifier.testTag(ACCOUNT_SETTINGS_DEVELOPER_TAG),
                        onClick = { navigator.onNavigate(Account.DeveloperSettings) },
                    )
                }
            }

            val updateState by UpdateManager.updateState.collectAsState()
            LaunchedEffect(updateState) {
                if (updateState is UpdateState.UpdateAvailable) {
                    val state = updateState as UpdateState.UpdateAvailable
                    val versionType = if (state.isNightly) "Nightly版本" else "正式版本"
                    Toast.makeText(context, "发现新$versionType ${state.version}", Toast.LENGTH_SHORT).show()
                }
                if (updateState is UpdateState.Error) {
                    Toast.makeText(context, "检查更新失败: ${(updateState as UpdateState.Error).message}", Toast.LENGTH_LONG).show()
                }
            }

            SettingItemGroup(
                title = "关于",
                footer = { Text("本软件仅供学习交流使用，应用内内容由知乎网站提供，著作权归其对应作者所有。") },
            ) {
                SettingItem(
                    title = { Text("知乎++") },
                    description = { Text("版本号：${BuildConfig.VERSION_NAME} ${BuildConfig.BUILD_TYPE}, ${BuildConfig.GIT_HASH}") },
                    icon = {
                        Image(
                            painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier
                                .clip(CircleShape)
                                .size(32.dp),
                        )
                    },
                    modifier = Modifier.combinedClickable(
                        enabled = true,
                        onClick = {
                            clickTimes++
                            if (clickTimes == 5) {
                                clickTimes = 0
                                isDeveloper = true
                                Toast.makeText(context, "You are now a developer", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onLongClick = {
                            val versionInfo = "${BuildConfig.VERSION_NAME} ${BuildConfig.BUILD_TYPE}, ${BuildConfig.GIT_HASH}"
                            val clip = android.content.ClipData.newPlainText("version", versionInfo)
                            context.clipboardManager.setPrimaryClip(clip)
                            Toast.makeText(context, "已复制版本号", Toast.LENGTH_SHORT).show()
                        },
                    ),
                )
                SettingItem(
                    title = { Text("GitHub 项目地址") },
                    description = { Text("https://github.com/zly2006/zhihu-plus-plus") },
                    icon = { Icon(painterResource(R.drawable.ic_github_24dp), null) },
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://github.com/zly2006/zhihu-plus-plus".toUri())
                        context.startActivity(intent)
                    },
                    endAction = {
                        Icon(
                            Icons.Default.ArrowOutward,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )

                SettingItem(
                    title = { Text("项目协议") },
                    description = { Text("AGPL-3.0-only") },
                    icon = { Icon(painterResource(R.drawable.ic_license_24dp), null) },
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://github.com/zly2006/zhihu-plus-plus/blob/master/LICENSE".toUri())
                        context.startActivity(intent)
                    },
                    endAction = {
                        Icon(
                            Icons.Default.ArrowOutward,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                SettingItem(
                    title = { Text("开源许可") },
                    description = { Text("查看第三方组件许可证") },
                    icon = { Icon(painterResource(R.drawable.ic_license_24dp), null) },
                    modifier = Modifier.testTag(ACCOUNT_SETTINGS_LICENSES_TAG),
                    onClick = { navigator.onNavigate(Account.OpenSourceLicenses) },
                )
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("确定要退出登录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        AccountData.delete(context)
                        showLogoutDialog = false
                    },
                ) {
                    Text("退出")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("取消")
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AccountSettingScreenPreview() {
    AccountSettingScreen(
        innerPadding = PaddingValues(0.dp),
        unreadCount = 5,
    )
}
