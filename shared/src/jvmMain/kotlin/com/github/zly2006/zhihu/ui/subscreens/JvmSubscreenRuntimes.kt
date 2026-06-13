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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.openDesktopExternalUrl
import com.github.zly2006.zhihu.shared.desktop.signedWithResponse
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.updater.SchematicVersion
import com.github.zly2006.zhihu.shared.updater.extractGithubReleaseNotes
import com.github.zly2006.zhihu.shared.updater.fetchLatestZhihuRelease
import com.github.zly2006.zhihu.shared.updater.fetchNightlyZhihuRelease
import com.github.zly2006.zhihu.util.ZhihuCredentialRefresher
import com.mikepenz.aboutlibraries.Libs
import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.util.Properties

internal val desktopSystemUpdateState = MutableStateFlow<SystemUpdateState>(SystemUpdateState.NoUpdate)

@Composable
actual fun rememberSystemUpdateRuntime(): SystemUpdateRuntime {
    val settings = rememberSettingsStore()
    val accountStore = remember { DesktopAccountStore() }
    val account = remember(accountStore) { accountStore.load() }
    val client = remember(accountStore, account) { accountStore.createHttpClient(account.cookies) }
    DisposableEffect(client) {
        onDispose { client.close() }
    }
    return remember(settings, client) {
        SystemUpdateRuntime(
            state = desktopSystemUpdateState,
            autoCheckEnabled = { settings.getBoolean(PREF_AUTO_CHECK_UPDATES, true) },
            setAutoCheckEnabled = { enabled ->
                settings.putBoolean(PREF_AUTO_CHECK_UPDATES, enabled)
                if (!enabled) {
                    desktopSystemUpdateState.value = SystemUpdateState.NoUpdate
                }
            },
            checkForUpdate = {
                checkDesktopUpdate(
                    client = client,
                    githubToken = settings.getStringOrNull("githubToken")?.takeIf { it.isNotBlank() },
                    checkNightly = settings.getBoolean("checkNightlyUpdates", false),
                    skippedVersion = settings.getStringOrNull(PREF_SKIPPED_VERSION),
                    state = desktopSystemUpdateState,
                )
            },
            skipVersion = { version ->
                settings.putString(PREF_SKIPPED_VERSION, version)
                desktopSystemUpdateState.value = SystemUpdateState.Latest
            },
            resetToNoUpdate = { desktopSystemUpdateState.value = SystemUpdateState.NoUpdate },
            downloadUpdate = { url ->
                runCatching {
                    if (url.isBlank()) {
                        error("下载链接为空")
                    }
                    openDesktopExternalUrl(url)
                }.onFailure {
                    desktopSystemUpdateState.value = SystemUpdateState.Error(it.message ?: "无法打开浏览器")
                }
            },
            installDownloadedUpdate = {
                desktopSystemUpdateState.value = SystemUpdateState.Error("桌面端不支持 APK 更新安装")
            },
            setError = { message -> desktopSystemUpdateState.value = SystemUpdateState.Error(message) },
            supportsApkInstall = false,
        )
    }
}

private const val PREF_AUTO_CHECK_UPDATES = "autoCheckUpdates"
private const val PREF_SKIPPED_VERSION = "skippedVersion"

private suspend fun checkDesktopUpdate(
    client: HttpClient,
    githubToken: String?,
    checkNightly: Boolean,
    skippedVersion: String?,
    state: MutableStateFlow<SystemUpdateState>,
) {
    try {
        state.value = SystemUpdateState.Checking
        val currentVersion = SchematicVersion.fromString(desktopVersionName())
        var latestResponse = fetchLatestZhihuRelease(client, githubToken)
        var latestVersion = latestResponse.tagName.takeIf { it.isNotBlank() }?.let { SchematicVersion.fromString(it) }
        var isNightly = false
        var releaseNotes = latestResponse.body?.let(::extractGithubReleaseNotes)

        if (checkNightly) {
            runCatching {
                fetchNightlyZhihuRelease(client, githubToken)
            }.onSuccess { nightlyResponse ->
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
            }
        }

        val version = latestVersion
        if (version != null && version > currentVersion) {
            val versionString = version.toString()
            if (skippedVersion != versionString) {
                state.value = SystemUpdateState.UpdateAvailable(
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
                state.value = SystemUpdateState.Latest
            }
        } else {
            state.value = SystemUpdateState.Latest
        }
    } catch (e: Exception) {
        state.value = SystemUpdateState.Error(e.message ?: "Unknown error")
    }
}

private fun desktopVersionName(): String =
    System.getProperty("zhihu.version")
        ?: SystemUpdateRuntime::class.java.`package`?.implementationVersion
        ?: readDesktopVersionFromGradleProperties()
        ?: "0.0.0"

private fun readDesktopVersionFromGradleProperties(): String? {
    var dir = File(System.getProperty("user.dir"))
    repeat(6) {
        val gradleProperties = File(dir, "gradle.properties")
        if (gradleProperties.isFile) {
            val properties = Properties()
            gradleProperties.inputStream().use(properties::load)
            return properties.getProperty("app.versionName")?.takeIf { it.isNotBlank() }
        }
        dir = dir.parentFile ?: return null
    }
    return null
}

@Composable
actual fun rememberDeveloperSettingsRuntime(): DeveloperSettingsRuntime {
    val store = remember { DesktopAccountStore() }
    return remember(store) {
        DeveloperSettingsRuntime(
            cookies = { store.load().cookies },
            networkStatus = { "网络状态：桌面端使用系统网络" },
            powerSaveModeText = { null },
            runtimeInfo = { DeveloperRuntimeInfo() },
            verifyLogin = { cookies ->
                store.verifyAndSave(cookies.toMutableMap())
            },
            refreshToken = {
                val account = store.load()
                store.createHttpClient(account.cookies).use { client ->
                    ZhihuCredentialRefresher.refreshZhihuToken(
                        ZhihuCredentialRefresher.fetchRefreshToken(client),
                        client,
                    )
                }
            },
            saveCookies = { cookies ->
                val current = store.load()
                store.save(
                    current.copy(
                        login = true,
                        cookies = cookies.toMutableMap(),
                    ),
                )
            },
            signedGet = { url ->
                store.signedWithResponse(
                    url = url,
                    block = { method = HttpMethod.Get },
                ) { response ->
                    response.bodyAsText()
                }
            },
        )
    }
}

@Composable
actual fun rememberOpenSourceLicensesLibraries(): Libs = remember { loadDesktopOpenSourceLicenses() }

@Composable
actual fun rememberShowFullVariantLicenses(): Boolean = false

private fun loadDesktopOpenSourceLicenses(): Libs =
    loadDesktopAboutLibrariesJson()
        ?.takeIf { it.isNotBlank() }
        ?.let { json ->
            runCatching {
                Libs.Builder().withJson(json).build()
            }.getOrElse { Libs(emptyList(), emptySet()) }
        } ?: Libs(emptyList(), emptySet())

private fun loadDesktopAboutLibrariesJson(): String? {
    val resourceJson = Thread
        .currentThread()
        .contextClassLoader
        ?.getResourceAsStream("aboutlibraries.json")
        ?.bufferedReader()
        ?.use { it.readText() }
    if (!resourceJson.isNullOrBlank()) {
        return resourceJson
    }

    return listOf(
        "app/build/generated/aboutLibraries/liteDebug/res/raw/aboutlibraries.json",
        "app/build/generated/aboutLibraries/fullDebug/res/raw/aboutlibraries.json",
        "app/build/intermediates/packaged_res/liteDebug/packageLiteDebugResources/raw/aboutlibraries.json",
        "app/build/intermediates/packaged_res/fullDebug/packageFullDebugResources/raw/aboutlibraries.json",
    ).firstNotNullOfOrNull { path ->
        File(path)
            .takeIf { it.isFile }
            ?.readText()
            ?.takeIf { it.isNotBlank() }
    }
}

@Composable
actual fun WebViewCustomFontSettings(
    customFontName: String?,
    onCustomFontNameChange: (String?) -> Unit,
) = Unit // TODO: 桌面端 WebView 自定义字体设置
