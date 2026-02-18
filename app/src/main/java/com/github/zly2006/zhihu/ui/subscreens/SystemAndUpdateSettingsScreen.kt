package com.github.zly2006.zhihu.ui.subscreens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.components.SwitchSettingItem
import com.github.zly2006.zhihu.updater.UpdateManager
import com.github.zly2006.zhihu.updater.UpdateManager.UpdateState
import com.github.zly2006.zhihu.util.luoTianYiUrlLauncher
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemAndUpdateSettingsScreen(
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences(
            PREFERENCE_NAME,
            Context.MODE_PRIVATE,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("系统与更新") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                windowInsets = WindowInsets(0),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // Github Token
            var githubToken by remember { mutableStateOf(preferences.getString("githubToken", "") ?: "") }
            var showGithubToken by remember { mutableStateOf(false) }

            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = githubToken,
                    onValueChange = {
                        githubToken = it
                        preferences.edit { putString("githubToken", it) }
                    },
                    label = { Text("GitHub Token") },
                    visualTransformation = if (showGithubToken) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showGithubToken = !showGithubToken }) {
                            Icon(
                                imageVector = if (showGithubToken) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Text(
                    "用于访问 GitHub API 时解除限速，提高更新检查的稳定性。留空则使用匿名访问，检查更新可能会失败。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            var checkNightlyUpdates by remember { mutableStateOf(preferences.getBoolean("checkNightlyUpdates", false)) }
            SwitchSettingItem(
                title = "检查 Nightly 版本更新",
                description = "检查每日构建版本 (可能不稳定)",
                checked = checkNightlyUpdates,
                onCheckedChange = {
                    checkNightlyUpdates = it
                    preferences.edit { putBoolean("checkNightlyUpdates", it) }
                },
            )

            var allowTelemetry by remember { mutableStateOf(preferences.getBoolean("allowTelemetry", true)) }
            SwitchSettingItem(
                title = "允许发送遥测统计数据",
                description = "仅用于统计使用人数，不包含个人隐私",
                checked = allowTelemetry,
                onCheckedChange = {
                    allowTelemetry = it
                    preferences.edit { putBoolean("allowTelemetry", it) }
                },
            )

            val updateState by UpdateManager.updateState.collectAsState()
            val coroutineScope = rememberCoroutineScope()

            Button(
                onClick = {
                    coroutineScope.launch {
                        when (val updateState = updateState) {
                            is UpdateState.NoUpdate, is UpdateState.Error -> {
                                UpdateManager.checkForUpdate(context)
                            }
                            is UpdateState.UpdateAvailable -> {
                                UpdateManager.downloadUpdate(context)
                            }
                            is UpdateState.Downloaded -> {
                                UpdateManager.installUpdate(
                                    context,
                                    updateState.file,
                                )
                            }
                            UpdateState.Checking, UpdateState.Downloading, UpdateState.Latest -> { /* NOOP */ }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when (val updateState = updateState) {
                        is UpdateState.NoUpdate -> "检查更新"
                        is UpdateState.Checking -> "检查中..."
                        is UpdateState.Latest -> "已经是最新版本"
                        is UpdateState.UpdateAvailable -> "下载更新 ${updateState.version}"
                        is UpdateState.Downloading -> "下载中..."
                        is UpdateState.Downloaded -> "安装更新"
                        is UpdateState.Error -> "检查更新失败，点击重试"
                    },
                )
            }

            var releaseNotes: String? by remember { mutableStateOf(null) }
            LaunchedEffect(updateState) {
                val updateState = updateState
                if (updateState is UpdateState.UpdateAvailable) {
                    releaseNotes = updateState
                        .releaseNotes
                        ?.substringAfter("## What's Changed\n")
                        ?.substringBefore("\n**Full Changelog**:")
                }
            }
            if (releaseNotes != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Text(
                    "更新内容",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
                SelectionContainer {
                    Text(
                        buildAnnotatedString {
                            val prRegex = Regex("https://github.com/zly2006/zhihu-plus-plus/pull/(\\d+)")
                            var lastIndex = 0
                            prRegex.findAll(releaseNotes!!).forEach { matchResult ->
                                append(releaseNotes!!.substring(lastIndex, matchResult.range.first))
                                val prNumber = matchResult.groupValues[1]
                                withLink(LinkAnnotation.Url("https://github.com/zly2006/zhihu-plus-plus/pull/$prNumber")) {
                                    withStyle(
                                        MaterialTheme.typography.bodyMedium
                                            .copy(color = MaterialTheme.colorScheme.primary)
                                            .toSpanStyle(),
                                    ) {
                                        append("#$prNumber")
                                    }
                                }
                                lastIndex = matchResult.range.last + 1
                            }
                            append(releaseNotes!!.substring(lastIndex))
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = {
                    luoTianYiUrlLauncher(context, "https://github.com/zly2006/zhihu-plus-plus/releases".toUri())
                }) {
                    Text("查看完整更新日志")
                }
            }

            Text(
                "交流 & 闲聊",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )
            Text(
                "代码和功能反馈请前往GitHub。下面的频道用于用户交流和闲聊，开发者不一定会在线回答问题。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            ListItem(
                headlineContent = { Text("Discord 频道") },
                supportingContent = { Text("请在 my-other-apps/zhihu-plus-plus 频道讨论") },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                modifier = Modifier.clickable { luoTianYiUrlLauncher(context, "https://discord.gg/YCPFZV5XSA".toUri()) },
            )

            ListItem(
                headlineContent = { Text("Telegram 群组 (Hydrogen)") },
                supportingContent = { Text("另一个知乎客户端 Hydrogen 的群组，也可以在里面讨论知乎++哦") },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                modifier = Modifier.clickable { luoTianYiUrlLauncher(context, "https://t.me/+_A1Yto6EpyIyODA1".toUri()) },
            )
        }
    }
}
