package com.github.zly2006.zhihu.updater

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.content.edit
import com.github.zly2006.zhihu.BuildConfig
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.updater.UpdateManager.UpdateState.Downloading
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI

object UpdateManager {
    private const val GITHUB_API_LATEST = "https://api.github.com/repos/zly2006/zhihu-plus-plus/releases/latest"
    private const val GITHUB_API_NIGHTLY = "https://api.github.com/repos/zly2006/zhihu-plus-plus/releases/tags/nightly"
    private const val PREF_SKIPPED_VERSION = "skippedVersion"
    private const val PREF_AUTO_CHECK_UPDATES = "autoCheckUpdates"
    private const val PREF_LAST_UPDATE_CHECK = "lastUpdateCheck"

    sealed class UpdateState {
        object NoUpdate : UpdateState()

        object Checking : UpdateState()

        object Latest : UpdateState()

        data class UpdateAvailable(
            val version: SchematicVersion,
            val isNightly: Boolean = false,
            val releaseNotes: String? = null,
        ) : UpdateState()

        object Downloading : UpdateState()

        data class Downloaded(
            val file: File,
        ) : UpdateState()

        data class Error(
            val message: String,
        ) : UpdateState()
    }

    val updateState = MutableStateFlow<UpdateState>(UpdateState.NoUpdate)

    private fun getGitHubToken(context: Context): String? {
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return preferences.getString("githubToken", null)?.takeIf { it.isNotBlank() }
    }

    private fun shouldCheckNightly(context: Context): Boolean {
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return preferences.getBoolean("checkNightlyUpdates", false)
    }

    /**
     * 获取跳过的版本
     */
    private fun getSkippedVersion(context: Context): String? {
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return preferences.getString(PREF_SKIPPED_VERSION, null)
    }

    /**
     * 设置跳过的版本
     */
    fun skipVersion(context: Context, version: String) {
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        preferences.edit { putString(PREF_SKIPPED_VERSION, version) }
    }

    /**
     * 检查是否需要进行自动更新检查（避免频繁检查）
     */
    private fun shouldPerformAutoCheck(context: Context): Boolean {
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        if (!preferences.getBoolean(PREF_AUTO_CHECK_UPDATES, true)) return false

        val lastCheck = preferences.getLong(PREF_LAST_UPDATE_CHECK, 0)
        val now = System.currentTimeMillis()
        val dayInMillis = 3 * 60 * 60 * 1000L

        return (now - lastCheck) > dayInMillis // 每3h最多检查一次
    }

    /**
     * 更新最后检查时间
     */
    private fun updateLastCheckTime(context: Context) {
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        preferences.edit { putLong(PREF_LAST_UPDATE_CHECK, System.currentTimeMillis()) }
    }

    private fun String.extractReleaseNotes() = this
        .replace("\r\n", "\n")
        .substringAfter("## What's Changed\n")
        .substringBefore("\n\n\n**Full Changelog**:")

    /**
     * 自动检查更新（在应用启动时调用）
     */
    suspend fun autoCheckForUpdate(context: Context): Boolean {
        if (!shouldPerformAutoCheck(context) && getGitHubToken(context) == null) return false
        Log.i("UpdateManager", "Performing auto update check")

        try {
            updateState.value = UpdateState.Checking
            updateLastCheckTime(context)

            val client = AccountData.httpClient(context)
            val currentVersion = SchematicVersion.fromString(BuildConfig.VERSION_NAME)
            val skippedVersion = getSkippedVersion(context)

            var latestVersion: SchematicVersion?

            // 检查正式版本
            val latestResponse = client
                .get(GITHUB_API_LATEST) {
                    getGitHubToken(context)?.let { token ->
                        headers {
                            append(HttpHeaders.Authorization, "Bearer $token")
                        }
                    }
                }.body<GithubRelease>()
            Log.i("UpdateManager", "Latest version response: $latestResponse")
            latestVersion = latestResponse.tagName.takeIf { it.isNotBlank() }?.let { SchematicVersion.fromString(it) }

            if (latestVersion != null && latestVersion > currentVersion) {
                val versionString = latestVersion.toString()
                // 检查是否是被跳过的版本
                if (skippedVersion != versionString) {
                    updateState.value = UpdateState.UpdateAvailable(
                        latestVersion,
                        false,
                        latestResponse.body?.extractReleaseNotes(),
                    )
                    return true // 有可用更新且未被跳过
                } else {
                    updateState.value = UpdateState.Latest
                }
            } else {
                updateState.value = UpdateState.Latest
            }
        } catch (e: Exception) {
            Log.e("UpdateManager", "Error checking for updates", e)
            updateState.value = UpdateState.Error(e.message ?: "Unknown error")
        }

        return false
    }

    suspend fun checkForUpdate(context: Context) {
        try {
            updateState.value = UpdateState.Checking
            updateLastCheckTime(context)

            val client = AccountData.httpClient(context)
            val currentVersion = SchematicVersion.fromString(BuildConfig.VERSION_NAME)
            val checkNightly = shouldCheckNightly(context)
            val skippedVersion = getSkippedVersion(context)

            var latestVersion: SchematicVersion?
            var isNightly = false
            var releaseNotes: String? = null

            // 检查正式版本
            val latestResponse = client
                .get(GITHUB_API_LATEST) {
                    getGitHubToken(context)?.let { token ->
                        headers {
                            append(HttpHeaders.Authorization, "Bearer $token")
                        }
                    }
                }.body<GithubRelease>()
            latestVersion = latestResponse.tagName.takeIf { it.isNotBlank() }?.let { SchematicVersion.fromString(it) }
            releaseNotes = latestResponse.body?.extractReleaseNotes()

            // 如果启用了nightly检查，也检查nightly版本
            if (checkNightly) {
                try {
                    val nightlyResponse = client
                        .get(GITHUB_API_NIGHTLY) {
                            getGitHubToken(context)?.let { token ->
                                headers {
                                    append(HttpHeaders.Authorization, "Bearer $token")
                                }
                            }
                        }.body<GithubRelease>()

                    // 如果nightly版本比正式版本新，则使用nightly版本
                    if (nightlyResponse.tagName == "nightly") {
                        latestVersion = SchematicVersion(
                            allComponents = listOf(999, 0, 0),
                            preRelease = "nightly",
                            build = "",
                        )
                        isNightly = true
                        releaseNotes = nightlyResponse.body?.extractReleaseNotes()
                    }
                } catch (e: Exception) {
                    // nightly版本检查失败时，继续使用正式版本
                    e.printStackTrace()
                }
            }

            if (latestVersion != null && latestVersion > currentVersion) {
                val versionString = latestVersion.toString()
                // 检查是否是被跳过的版本
                if (skippedVersion != versionString) {
                    updateState.value = UpdateState.UpdateAvailable(latestVersion, isNightly, releaseNotes)
                } else {
                    updateState.value = UpdateState.Latest
                }
            } else {
                updateState.value = UpdateState.Latest
            }
        } catch (e: Exception) {
            updateState.value = UpdateState.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun downloadUpdate(context: Context) {
        val state = updateState.value
        if (state !is UpdateState.UpdateAvailable) return

        try {
            updateState.value = Downloading
            val client = AccountData.httpClient(context)

            // 根据版本类型选择API端点
            val apiUrl = if (state.isNightly) GITHUB_API_NIGHTLY else GITHUB_API_LATEST

            val release = client
                .get(apiUrl) {
                    getGitHubToken(context)?.let { token ->
                        headers {
                            append(HttpHeaders.Authorization, "Bearer $token")
                        }
                    }
                }.body<GithubRelease>()
            val apkAssets = release.assets.filter {
                it.contentType == "application/vnd.android.package-archive"
            }

            @Suppress("KotlinConstantConditions")
            val selectedAsset = if (BuildConfig.IS_LITE) {
                // Lite version: strictly look for "lite" in filename
                apkAssets.firstOrNull { it.name.contains("lite", ignoreCase = true) }
            } else {
                // Full version: prefer "full" in filename, fallback to non-lite (legacy)
                apkAssets.firstOrNull { it.name.contains("full", ignoreCase = true) }
                    ?: apkAssets.firstOrNull { !it.name.contains("lite", ignoreCase = true) }
            }

            val downloadUrl = selectedAsset?.browserDownloadUrl

            if (downloadUrl != null) {
                val file = withContext(Dispatchers.IO) {
                    val apkFile = File(context.cacheDir, "update.apk")
                    URI(downloadUrl)
                        .toURL()
                        .openConnection()
                        .getInputStream()
                        .use { input -> apkFile.outputStream().use { output -> input.copyTo(output) } }
                    apkFile
                }
                updateState.value = UpdateState.Downloaded(file)
            } else {
                updateState.value = UpdateState.Error("Download URL not found")
            }
        } catch (e: Exception) {
            updateState.value = UpdateState.Error(e.message ?: "Unknown error")
        }
    }

    fun installUpdate(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
