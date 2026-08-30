/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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
import com.github.zly2006.zhihu.account.NativeAccountStore
import com.github.zly2006.zhihu.platform.nativeAppVersionName
import com.github.zly2006.zhihu.platform.nativeBundledResourcePath
import com.github.zly2006.zhihu.platform.nativeIsDesktop
import com.github.zly2006.zhihu.platform.rememberExternalUrlOpener
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.ui.NativeArticleSpeechController
import com.github.zly2006.zhihu.ui.TtsState
import com.github.zly2006.zhihu.updater.SchematicVersion
import com.github.zly2006.zhihu.updater.extractGithubReleaseNotes
import com.github.zly2006.zhihu.updater.fetchLatestZhihuRelease
import com.github.zly2006.zhihu.updater.fetchNightlyZhihuRelease
import com.mikepenz.aboutlibraries.Libs
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import platform.Foundation.NSFileManager

private const val PREF_SKIPPED_VERSION = "skippedVersion"

private val nativeSystemUpdateState = MutableStateFlow<SystemUpdateState>(SystemUpdateState.NoUpdate)

@Composable
actual fun rememberSystemUpdateRuntime(): SystemUpdateRuntime {
    val settings = rememberSettingsStore()
    val accountStore = remember { NativeAccountStore() }
    val openExternalUrl = rememberExternalUrlOpener()
    return remember(settings, accountStore, openExternalUrl) {
        SystemUpdateRuntime(
            state = nativeSystemUpdateState,
            checkForUpdate = {
                checkNativeUpdate(
                    accountStore = accountStore,
                    githubToken = settings.getStringOrNull("githubToken")?.takeIf { it.isNotBlank() },
                    checkNightly = settings.getBoolean("checkNightlyUpdates", false),
                    skippedVersion = settings.getStringOrNull(PREF_SKIPPED_VERSION),
                )
            },
            skipVersion = { version ->
                settings.putString(PREF_SKIPPED_VERSION, version)
                nativeSystemUpdateState.value = SystemUpdateState.Latest
            },
            resetToNoUpdate = { nativeSystemUpdateState.value = SystemUpdateState.NoUpdate },
            downloadUpdate = { url ->
                try {
                    require(url.isNotBlank()) { "下载链接为空" }
                    openExternalUrl(url)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    nativeSystemUpdateState.value = SystemUpdateState.Error(error.message ?: "无法打开浏览器")
                }
            },
            installDownloadedUpdate = {
                nativeSystemUpdateState.value = SystemUpdateState.Error("桌面端不支持 APK 更新安装")
            },
            setError = { message -> nativeSystemUpdateState.value = SystemUpdateState.Error(message) },
            supportsApkInstall = false,
        )
    }
}

private suspend fun checkNativeUpdate(
    accountStore: NativeAccountStore,
    githubToken: String?,
    checkNightly: Boolean,
    skippedVersion: String?,
) {
    try {
        nativeSystemUpdateState.value = SystemUpdateState.Checking
        val currentVersion = SchematicVersion.fromString(nativeAppVersionName)
        var latestResponse = fetchLatestZhihuRelease(accountStore.httpClient(), githubToken)
        var latestVersion = latestResponse.tagName.takeIf { it.isNotBlank() }?.let(SchematicVersion::fromString)
        var isNightly = false
        var releaseNotes = latestResponse.body?.let(::extractGithubReleaseNotes)

        if (checkNightly) {
            try {
                val nightlyResponse = fetchNightlyZhihuRelease(accountStore.httpClient(), githubToken)
                if (nightlyResponse.tagName == "nightly") {
                    latestResponse = nightlyResponse
                    latestVersion = SchematicVersion(
                        allComponents = listOf(999, 0, 0),
                        preRelease = "nightly",
                        build = "",
                    )
                    isNightly = true
                    releaseNotes = nightlyResponse.body?.let(::extractGithubReleaseNotes)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // Stable release metadata remains usable when the optional nightly lookup fails.
            }
        }

        val version = latestVersion
        if (version != null && version > currentVersion) {
            val versionString = version.toString()
            if (skippedVersion != versionString) {
                nativeSystemUpdateState.value = SystemUpdateState.UpdateAvailable(
                    version = versionString,
                    isNightly = isNightly,
                    releaseNotes = releaseNotes,
                    downloadUrl = latestResponse.htmlUrl ?: latestResponse.assets
                        .firstOrNull()
                        ?.browserDownloadUrl
                        .orEmpty(),
                    cnDownloadUrl = latestResponse.assets.firstOrNull()?.cnDownloadUrl,
                )
            } else {
                nativeSystemUpdateState.value = SystemUpdateState.Latest
            }
        } else {
            nativeSystemUpdateState.value = SystemUpdateState.Latest
        }
    } catch (error: CancellationException) {
        nativeSystemUpdateState.value = SystemUpdateState.NoUpdate
        throw error
    } catch (error: Exception) {
        nativeSystemUpdateState.value = SystemUpdateState.Error(error.message ?: "Unknown error")
    }
}

@Composable
actual fun rememberDeveloperRuntimeInfo(): DeveloperRuntimeInfo =
    DeveloperRuntimeInfo(
        networkStatus = if (nativeIsDesktop) {
            "网络状态：桌面端使用系统网络"
        } else {
            "网络状态：iOS 端使用系统网络"
        },
        ttsState = if (nativeIsDesktop) NativeArticleSpeechController.currentState else TtsState.Uninitialized,
        currentTtsEngineLabel = if (nativeIsDesktop) "macOS 系统语音" else "未初始化",
        availableTtsEngineLabels = if (nativeIsDesktop) listOf("NSSpeechSynthesizer") else emptyList(),
    )

@Composable
actual fun rememberOpenSourceLicensesLibraries(): Libs = remember {
    loadNativeAboutLibrariesJson()
        ?.let { json ->
            runCatching { Libs.Builder().withJson(json).build() }
                .getOrElse { Libs(emptyList(), emptySet()) }
        } ?: Libs(emptyList(), emptySet())
}

@OptIn(ExperimentalForeignApi::class)
private fun loadNativeAboutLibrariesJson(): String? {
    val resourcePath = nativeBundledResourcePath("aboutlibraries.json") ?: return null
    val data = NSFileManager.defaultManager.contentsAtPath(resourcePath) ?: return null
    val bytes = data.bytes?.reinterpret<ByteVar>()?.readBytes(data.length.toInt()) ?: return null
    return bytes.decodeToString().takeIf { it.isNotBlank() }
}

@Composable
actual fun rememberShowFullVariantLicenses(): Boolean = false

@Composable
actual fun WebViewCustomFontSettings(
    customFontName: String?,
    onCustomFontNameChange: (String?) -> Unit,
) = Unit
