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
import androidx.compose.material.icons.filled.Login
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.Account
import com.github.zly2006.zhihu.BuildConfig
import com.github.zly2006.zhihu.Collections
import com.github.zly2006.zhihu.LocalNavigator
import com.github.zly2006.zhihu.LoginActivity
import com.github.zly2006.zhihu.Notification
import com.github.zly2006.zhihu.OnlineHistory
import com.github.zly2006.zhihu.Person
import com.github.zly2006.zhihu.QRCodeScanActivity
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.WebviewActivity
import com.github.zly2006.zhihu.data.AccountData
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

@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun AccountSettingScreen(
    innerPadding: PaddingValues,
    unreadCount: Int = 0,
    onDismissRequest: () -> Unit = {},
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
    val data by AccountData.asState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(padding),
        ) {
            LaunchedEffect(Unit) {
                val data = AccountData.data
                if (data.login) {
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
                    Modifier.padding(16.dp, 0.dp, 16.dp, 16.dp).clickable {
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
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable {
                                    navigator.onNavigate(Collections(AccountData.data.self!!.urlToken!!))
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
                            onClick = { navigator.onNavigate(Collections(AccountData.data.self!!.urlToken!!)) },
                        )
                    }
                }
            }

            SettingItemGroup {
                SettingItem(
                    title = { Text("外观与阅读体验") },
                    description = { Text("主题颜色、字体大小等") },
                    icon = { Icon(Icons.Default.Palette, null) },
                    onClick = { navigator.onNavigate(Account.AppearanceSettings()) },
                )

                SettingItem(
                    title = { Text("推荐系统与内容过滤") },
                    description = { Text("推荐、智能过滤、关键词屏蔽等") },
                    icon = { Icon(Icons.Default.FilterAlt, null) },
                    onClick = { navigator.onNavigate(Account.RecommendSettings()) },
                )

                SettingItem(
                    title = { Text("系统与更新") },
                    description = { Text("GitHub、更新设置等") },
                    icon = { Icon(Icons.Default.Settings, null) },
                    onClick = { navigator.onNavigate(Account.SystemAndUpdateSettings) },
                )

                AnimatedVisibility(isDeveloper) {
                    SettingItem(
                        title = { Text("开发者选项") },
                        icon = { Icon(Icons.Default.Code, null) },
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
                    title = { Text("开源协议") },
                    description = { Text("AGPL") },
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
