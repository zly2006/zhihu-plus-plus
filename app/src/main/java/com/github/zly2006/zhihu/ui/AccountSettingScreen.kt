package com.github.zly2006.zhihu.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.github.zly2006.zhihu.Person
import com.github.zly2006.zhihu.QRCodeScanActivity
import com.github.zly2006.zhihu.WebviewActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.updater.UpdateManager
import com.github.zly2006.zhihu.updater.UpdateManager.UpdateState
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.util.signFetchRequest
import io.ktor.http.Url
import kotlinx.coroutines.DelicateCoroutinesApi

@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun AccountSettingScreen(
    @Suppress("UNUSED_PARAMETER") innerPadding: PaddingValues,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences(
            PREFERENCE_NAME,
            Context.MODE_PRIVATE,
        )
    }

    var isDeveloper by remember { mutableStateOf(preferences.getBoolean("developer", false)) }
    var clickTimes by remember { mutableIntStateOf(0) }
    LaunchedEffect(isDeveloper) {
        preferences.edit {
            putBoolean("developer", isDeveloper)
        }
    }
    val data by AccountData.asState()

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
//        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LaunchedEffect(Unit) {
            val data = AccountData.data
            if (data.login) {
                try {
                    val response = AccountData.fetchGet(context, "https://www.zhihu.com/api/v4/me") {
                        signFetchRequest(context)
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
            Box {
                Column {
                    Text(
                        "版本号：${BuildConfig.VERSION_NAME} ${BuildConfig.BUILD_TYPE}, ${BuildConfig.GIT_HASH}",
                        modifier = Modifier.combinedClickable(
                            onLongClick = {
                                val versionInfo = "${BuildConfig.VERSION_NAME} ${BuildConfig.BUILD_TYPE}, ${BuildConfig.GIT_HASH}"
                                val clip = android.content.ClipData.newPlainText("version", versionInfo)
                                context.clipboardManager.setPrimaryClip(clip)
                                Toast.makeText(context, "已复制版本号", Toast.LENGTH_SHORT).show()
                            },
                            onClick = {
                                clickTimes++
                                if (clickTimes == 5) {
                                    clickTimes = 0
                                    isDeveloper = true
                                    Toast.makeText(context, "You are now a developer", Toast.LENGTH_SHORT).show()
                                }
                            },
                        ),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navigator.onNavigate(
                                    Person(
                                        id = data.self?.id ?: "",
                                        urlToken = data.self?.urlToken ?: "",
                                        name = data.username,
                                    ),
                                )
                            },
                    ) {
                        AsyncImage(
                            model = data.self?.avatarUrl,
                            contentDescription = "头像",
                            modifier = Modifier
                                .size(120.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .clip(CircleShape),
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(data.username, style = MaterialTheme.typography.headlineLarge)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
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
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "扫码登录",
                        )
                    }

                    FilledTonalIconButton(
                        onClick = {
                            AccountData.delete(context)
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "退出登录")
                    }
                }
            }

            Button(
                onClick = { navigator.onNavigate(Collections(AccountData.data.self!!.urlToken!!)) },
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            ) {
                Text("查看收藏夹")
            }
        } else {
            Text(
                "版本号：${BuildConfig.VERSION_NAME} ${BuildConfig.BUILD_TYPE}, ${BuildConfig.GIT_HASH}",
                modifier = Modifier.combinedClickable(
                    onLongClick = {
                        val versionInfo = "${BuildConfig.VERSION_NAME} ${BuildConfig.BUILD_TYPE}, ${BuildConfig.GIT_HASH}"
                        val clip = android.content.ClipData.newPlainText("version", versionInfo)
                        context.clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, "已复制版本号", Toast.LENGTH_SHORT).show()
                    },
                    onClick = {
                        clickTimes++
                        if (clickTimes == 5) {
                            clickTimes = 0
                            isDeveloper = true
                            Toast.makeText(context, "You are now a developer", Toast.LENGTH_SHORT).show()
                        }
                    },
                ),
            )
            Button(
                onClick = {
                    context.startActivity(Intent(context, LoginActivity::class.java))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("登录")
            }
        }
        Spacer(Modifier.height(8.dp))

        ListItem(
            headlineContent = { Text("外观与阅读体验") },
            supportingContent = { Text("主题颜色、字体大小等") },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
            modifier = Modifier.clickable { navigator.onNavigate(Account.AppearanceSettings()) },
        )
        ListItem(
            headlineContent = { Text("推荐系统与内容过滤") },
            supportingContent = { Text("推荐、智能过滤、关键词屏蔽等") },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
            modifier = Modifier.clickable { navigator.onNavigate(Account.RecommendSettings) },
        )

        ListItem(
            headlineContent = { Text("系统与更新") },
            supportingContent = { Text("GitHub、更新设置等") },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
            modifier = Modifier.clickable { navigator.onNavigate(Account.SystemAndUpdateSettings) },
        )

        if (isDeveloper) {
            ListItem(
                headlineContent = { Text("开发者选项") },
                supportingContent = { Text("开发者选项") },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                modifier = Modifier.clickable { navigator.onNavigate(Account.DeveloperSettings) },
            )
        }

        val updateState by UpdateManager.updateState.collectAsState()
        LaunchedEffect(updateState) {
            val updateState = updateState
            if (updateState is UpdateState.UpdateAvailable) {
                val versionType = if (updateState.isNightly) "Nightly版本" else "正式版本"
                Toast
                    .makeText(
                        context,
                        "发现新$versionType ${updateState.version}",
                        Toast.LENGTH_SHORT,
                    ).show()
            }
            if (updateState is UpdateState.Error) {
                Toast
                    .makeText(
                        context,
                        "检查更新失败: ${updateState.message}",
                        Toast.LENGTH_LONG,
                    ).show()
            }
        }
        Row {
            Text(
                "关于",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        Row {
            Button(
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        "https://github.com/zly2006/zhihu-plus-plus".toUri(),
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("GitHub 项目地址")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        "https://github.com/zly2006/zhihu-plus-plus/blob/master/LICENSE.md".toUri(),
                    )
                    context.startActivity(intent)
                },
            ) {
                Text("开源协议")
            }
        }
        Text(
            "本软件仅供学习交流使用，应用内内容由知乎网站提供，著作权归其对应作者所有。",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AccountSettingScreenPreview() {
    AccountSettingScreen(
        innerPadding = PaddingValues(16.dp),
    )
}
