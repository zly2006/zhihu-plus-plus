package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.openDesktopExternalUrl
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.updater.GithubRelease
import com.github.zly2006.zhihu.shared.updater.SchematicVersion
import com.github.zly2006.zhihu.shared.updater.ZHIHU_PLUS_PLUS_GITHUB_LATEST_RELEASE_URL
import com.github.zly2006.zhihu.shared.updater.ZHIHU_PLUS_PLUS_GITHUB_NIGHTLY_RELEASE_URL
import com.github.zly2006.zhihu.shared.updater.ZHIHU_PLUS_PLUS_REDEN_LATEST_RELEASE_URL
import com.github.zly2006.zhihu.shared.updater.extractGithubReleaseNotes
import com.github.zly2006.zhihu.shared.util.raiseForStatus
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
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
                    openDesktopUrl(url)
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

@Composable
actual fun rememberExternalUrlOpener(): (String) -> Unit = remember {
    { url -> openDesktopUrl(url) }
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
        var latestResponse = getLatestDesktopVersion(client, githubToken)
        var latestVersion = latestResponse.tagName.takeIf { it.isNotBlank() }?.let { SchematicVersion.fromString(it) }
        var isNightly = false
        var releaseNotes = latestResponse.body?.let(::extractGithubReleaseNotes)

        if (checkNightly) {
            runCatching {
                client
                    .get(ZHIHU_PLUS_PLUS_GITHUB_NIGHTLY_RELEASE_URL) {
                        githubToken?.let { token ->
                            headers {
                                append(HttpHeaders.Authorization, "Bearer $token")
                            }
                        }
                    }.raiseForStatus()
                    .body<GithubRelease>()
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

private suspend fun getLatestDesktopVersion(
    client: HttpClient,
    githubToken: String?,
): GithubRelease = runCatching {
    client.get(ZHIHU_PLUS_PLUS_REDEN_LATEST_RELEASE_URL).raiseForStatus().body<GithubRelease>()
}.getOrNull() ?: client
    .get(ZHIHU_PLUS_PLUS_GITHUB_LATEST_RELEASE_URL) {
        githubToken?.let { token ->
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }.raiseForStatus()
    .body<GithubRelease>()

private fun desktopVersionName(): String =
    System.getProperty("zhihu.version")
        ?: SystemAndUpdateSettingsRuntime::class.java.`package`?.implementationVersion
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

private fun openDesktopUrl(url: String) {
    if (url.isBlank()) {
        error("下载链接为空")
    }
    openDesktopExternalUrl(url)
}
