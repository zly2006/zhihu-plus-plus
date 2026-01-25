package com.github.zly2006.zhihu.ui.subscreens

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.components.SwitchSettingItem
import com.github.zly2006.zhihu.updater.UpdateManager
import com.github.zly2006.zhihu.updater.UpdateManager.UpdateState
import kotlinx.coroutines.GlobalScope
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

            val checkNightlyUpdates = remember { mutableStateOf(preferences.getBoolean("checkNightlyUpdates", false)) }
            SwitchSettingItem(
                title = "检查 Nightly 版本更新",
                description = "检查每日构建版本 (可能不稳定)",
                checked = checkNightlyUpdates.value,
                onCheckedChange = {
                    checkNightlyUpdates.value = it
                    preferences.edit { putBoolean("checkNightlyUpdates", it) }
                },
            )

            val allowTelemetry = remember { mutableStateOf(preferences.getBoolean("allowTelemetry", true)) }
            SwitchSettingItem(
                title = "允许发送遥测统计数据",
                description = "仅用于改进应用，不包含个人隐私",
                checked = allowTelemetry.value,
                onCheckedChange = {
                    allowTelemetry.value = it
                    preferences.edit { putBoolean("allowTelemetry", it) }
                },
            )

            val updateState by UpdateManager.updateState.collectAsState()

            Button(
                onClick = {
                    GlobalScope.launch {
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
        }
    }
}
