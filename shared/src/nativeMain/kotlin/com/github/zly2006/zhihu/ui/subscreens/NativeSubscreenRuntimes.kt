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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mikepenz.aboutlibraries.Libs
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
actual fun rememberSystemUpdateRuntime(): SystemUpdateRuntime = remember {
    SystemUpdateRuntime(
        state = MutableStateFlow(SystemUpdateState.NoUpdate),
        autoCheckEnabled = { false }, // TODO: iOS 自动检查更新
        setAutoCheckEnabled = { }, // TODO: iOS 设置自动检查更新
        checkForUpdate = { }, // TODO: iOS 检查更新
        skipVersion = { }, // TODO: iOS 跳过版本
        resetToNoUpdate = { }, // TODO: iOS 重置更新状态
        downloadUpdate = { }, // TODO: iOS 下载更新
        installDownloadedUpdate = { }, // TODO: iOS 安装更新
        setError = { }, // TODO: iOS 设置错误状态
        supportsApkInstall = false,
    )
} // TODO: iOS 更新检查实现

@Composable
actual fun rememberDeveloperSettingsRuntime(): DeveloperSettingsRuntime = remember {
    DeveloperSettingsRuntime(
        cookies = { emptyMap() }, // TODO: iOS cookies 获取
        networkStatus = { "网络状态：iOS 端使用系统网络" },
        powerSaveModeText = { null },
        runtimeInfo = { DeveloperRuntimeInfo() },
        verifyLogin = { false }, // TODO: iOS 登录验证
        refreshToken = { }, // TODO: iOS token 刷新
        saveCookies = { }, // TODO: iOS cookies 保存
        signedGet = { "iOS 暂不支持" }, // TODO: iOS signed GET
    )
}

@Composable
actual fun rememberOpenSourceLicensesLibraries(): Libs = remember {
    Libs(emptyList(), emptySet())
} // TODO: iOS 开源许可证加载

@Composable
actual fun rememberShowFullVariantLicenses(): Boolean = false

@Composable
actual fun WebViewCustomFontSettings(
    customFontName: String?,
    onCustomFontNameChange: (String?) -> Unit,
) = Unit // TODO: iOS WebView 自定义字体设置
