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

package com.github.zly2006.zhihu.ui.subscreens
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.shared.platform.androidUserMessageSink
import com.github.zly2006.zhihu.shared.platform.rememberIsLiteVariant
import com.github.zly2006.zhihu.updater.UpdateManager
import com.github.zly2006.zhihu.updater.UpdateManager.UpdateState
import com.github.zly2006.zhihu.util.PowerSaveModeCompat
import com.github.zly2006.zhihu.util.ZhihuCredentialRefresher
import com.github.zly2006.zhihu.util.signFetchRequest
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.withContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File

@Composable
actual fun rememberDeveloperSettingsRuntime(): DeveloperSettingsRuntime {
    val context = LocalContext.current
    val dataState by AccountData.asState()
    return remember(context, dataState) {
        DeveloperSettingsRuntime(
            cookies = { dataState.cookies },
            networkStatus = { context.networkStatusText() },
            powerSaveModeText = {
                when (PowerSaveModeCompat.getPowerSaveMode(context)) {
                    PowerSaveModeCompat.POWER_SAVE -> "省电模式：已开启"
                    PowerSaveModeCompat.HUAWEI_POWER_SAVE -> "省电模式：华为傻逼模式已开启"
                    else -> null
                }
            },
            runtimeInfo = { (context as? DeveloperRuntimeInfoProvider)?.developerRuntimeInfo ?: DeveloperRuntimeInfo() },
            verifyLogin = { cookies ->
                AccountData.verifyLogin(context, cookies)
            },
            refreshToken = {
                val httpClient = AccountData.httpClient(context)
                ZhihuCredentialRefresher.refreshZhihuToken(
                    ZhihuCredentialRefresher.fetchRefreshToken(httpClient),
                    httpClient,
                )
            },
            saveCookies = { cookies ->
                AccountData.saveData(
                    context,
                    AccountData.data.copy(
                        cookies = cookies.toMutableMap(),
                        login = true,
                    ),
                )
            },
            signedGet = { url ->
                AccountData
                    .fetchGet(context, url) {
                        signFetchRequest()
                    }.toString()
            },
        )
    }
}

private fun Context.networkStatusText(): String {
    val connectivityManager = getSystemService(ConnectivityManager::class.java)
    val activeNetwork = connectivityManager.activeNetwork
    return buildString {
        append("网络状态：")
        if (activeNetwork != null) {
            append("已连接")
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> append(" (移动数据)")
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> append(" (Wi-Fi)")
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true -> append(" (VPN)")
            }
        } else {
            append("未连接")
        }
    }
}

@Composable
actual fun WebViewCustomFontSettings(
    customFontName: String?,
    onCustomFontNameChange: (String?) -> Unit,
) {
    val context = LocalContext.current
    val userMessages = androidUserMessageSink(context)
    val fontFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: uri.toString()
        val destFile = File(context.filesDir, "custom_font")
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        }
        onCustomFontNameChange(name)
        userMessages.showShortMessage("字体已设置，重新打开文章后生效")
    }
    Column(
        modifier = Modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    fontFilePicker.launch(arrayOf("font/ttf", "font/otf", "application/octet-stream"))
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Text("选择", modifier = Modifier.padding(start = 4.dp))
            }
            if (customFontName != null) {
                OutlinedButton(
                    onClick = {
                        File(context.filesDir, "custom_font").delete()
                        onCustomFontNameChange(null)
                        userMessages.showShortMessage("已清除自定义字体")
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null)
                    Text("清除", modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

@Composable
actual fun rememberSystemUpdateRuntime(): SystemUpdateRuntime {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context, scope) {
        SystemUpdateRuntime(
            state = UpdateManager.updateState.map { it.toSystemUpdateState() }.stateIn(
                scope,
                SharingStarted.Eagerly,
                UpdateManager.updateState.value.toSystemUpdateState(),
            ),
            autoCheckEnabled = { UpdateManager.isAutoCheckEnabled(context) },
            setAutoCheckEnabled = { enabled ->
                UpdateManager.setAutoCheckEnabled(context, enabled)
                if (!enabled) {
                    UpdateManager.updateState.value = UpdateState.NoUpdate
                }
            },
            checkForUpdate = { UpdateManager.checkForUpdate(context) },
            skipVersion = { version ->
                UpdateManager.skipVersion(context, version)
                UpdateManager.updateState.value = UpdateState.Latest
            },
            resetToNoUpdate = {
                UpdateManager.updateState.value = UpdateState.NoUpdate
            },
            downloadUpdate = { url -> UpdateManager.downloadUpdate(context, url) },
            installDownloadedUpdate = {
                val state = UpdateManager.updateState.value
                if (state is UpdateState.Downloaded) {
                    UpdateManager.installUpdate(context, state.file)
                }
            },
            setError = { message ->
                UpdateManager.updateState.value = UpdateState.Error(message)
            },
            supportsApkInstall = true,
        )
    }
}

private fun UpdateState.toSystemUpdateState(): SystemUpdateState = when (this) {
    UpdateState.NoUpdate -> SystemUpdateState.NoUpdate
    UpdateState.Checking -> SystemUpdateState.Checking
    UpdateState.Latest -> SystemUpdateState.Latest
    is UpdateState.UpdateAvailable -> SystemUpdateState.UpdateAvailable(
        version = version.toString(),
        isNightly = isNightly,
        releaseNotes = releaseNotes,
        downloadUrl = downloadUrl,
        cnDownloadUrl = cnDownloadUrl,
    )
    UpdateState.Downloading -> SystemUpdateState.Downloading
    is UpdateState.Downloaded -> SystemUpdateState.Downloaded
    is UpdateState.Error -> SystemUpdateState.Error(message)
}

@Composable
actual fun rememberOpenSourceLicensesLibraries(): Libs {
    val context = LocalContext.current
    return remember(context) {
        Libs
            .Builder()
            .withContext(context)
            .build()
    }
}

@Composable
actual fun rememberShowFullVariantLicenses(): Boolean = !rememberIsLiteVariant()
