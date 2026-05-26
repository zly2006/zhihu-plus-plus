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

package com.github.zly2006.zhihu.updater

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.shared.platform.androidSettingsStore
import com.github.zly2006.zhihu.shared.updater.GithubAsset
import com.github.zly2006.zhihu.shared.updater.GithubRelease
import com.github.zly2006.zhihu.shared.updater.SchematicVersion
import com.github.zly2006.zhihu.shared.updater.ZHIHU_PLUS_PLUS_GITHUB_LATEST_RELEASE_URL
import com.github.zly2006.zhihu.shared.updater.ZHIHU_PLUS_PLUS_GITHUB_NIGHTLY_RELEASE_URL
import com.github.zly2006.zhihu.shared.updater.ZHIHU_PLUS_PLUS_REDEN_LATEST_RELEASE_URL
import com.github.zly2006.zhihu.shared.updater.extractGithubReleaseNotes
import com.github.zly2006.zhihu.shared.util.raiseForStatus
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
    private const val AUTO_CHECK_INTERVAL_MILLIS = 3 * 60 * 60 * 1000L
    private const val ANDROID_APK_CONTENT_TYPE = "application/vnd.android.package-archive"

    /**
     * 自动检查更新要跳过的版本
     */
    private const val PREF_SKIPPED_VERSION = "skippedVersion"
    private const val PREF_AUTO_CHECK_UPDATES = "autoCheckUpdates"
    private const val PREF_LAST_UPDATE_CHECK = "lastUpdateCheck"

    data class DownloadInfo(
        val browserDownloadUrl: String,
        val cnDownloadUrl: String? = null,
    )

    sealed class UpdateState {
        object NoUpdate : UpdateState()

        object Checking : UpdateState()

        object Latest : UpdateState()

        data class UpdateAvailable(
            val version: SchematicVersion,
            val isNightly: Boolean = false,
            val releaseNotes: String?,
            val downloadUrl: String,
            val cnDownloadUrl: String?,
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

    private fun getGitHubToken(context: Context): String? = androidSettingsStore(context).getStringOrNull("githubToken")?.takeIf { it.isNotBlank() }

    private fun shouldCheckNightly(context: Context): Boolean = androidSettingsStore(context).getBoolean("checkNightlyUpdates", false)

    private fun Context.isLiteVariant(): Boolean = packageName.endsWith(".lite")

    private fun Context.versionName(): String = runCatching {
        packageManager.getPackageInfo(packageName, 0).versionName
    }.getOrNull() ?: "0.0.0"

    /**
     * 获取跳过的版本
     */
    private fun getSkippedVersion(context: Context): String? = androidSettingsStore(context).getStringOrNull(PREF_SKIPPED_VERSION)

    /**
     * 设置跳过的版本
     */
    fun skipVersion(context: Context, version: String) {
        androidSettingsStore(context).putString(PREF_SKIPPED_VERSION, version)
    }

    fun isAutoCheckEnabled(context: Context): Boolean = androidSettingsStore(context).getBoolean(PREF_AUTO_CHECK_UPDATES, true)

    fun setAutoCheckEnabled(context: Context, enabled: Boolean) {
        androidSettingsStore(context).putBoolean(PREF_AUTO_CHECK_UPDATES, enabled)
    }

    /**
     * 检查是否需要进行自动更新检查（避免频繁检查）
     */
    private fun shouldPerformAutoCheck(context: Context): Boolean {
        val settings = androidSettingsStore(context)
        if (!isAutoCheckEnabled(context)) return false
        return (System.currentTimeMillis() - settings.getLong(PREF_LAST_UPDATE_CHECK, 0)) >= AUTO_CHECK_INTERVAL_MILLIS
    }

    /**
     * 更新最后检查时间
     */
    private fun updateLastCheckTime(context: Context) {
        androidSettingsStore(context).putLong(PREF_LAST_UPDATE_CHECK, System.currentTimeMillis())
    }

    private fun GithubRelease.extractDownloadInfo(context: Context): DownloadInfo {
        val apkAssets = assets.filter {
            it.contentType == ANDROID_APK_CONTENT_TYPE
        }

        val selectedAsset = selectApkAsset(apkAssets, context.isLiteVariant()) ?: apkAssets.first()
        return DownloadInfo(
            browserDownloadUrl = selectedAsset.browserDownloadUrl,
            cnDownloadUrl = selectedAsset.cnDownloadUrl,
        )
    }

    suspend fun getLatestVersion(context: Context): GithubRelease {
        val client = AccountData.httpClient(context)
        return runCatching {
            client.get(ZHIHU_PLUS_PLUS_REDEN_LATEST_RELEASE_URL).raiseForStatus().body<GithubRelease>()
        }.getOrNull() ?: run {
            client
                .get(ZHIHU_PLUS_PLUS_GITHUB_LATEST_RELEASE_URL) {
                    getGitHubToken(context)?.let { token ->
                        headers {
                            append(HttpHeaders.Authorization, "Bearer $token")
                        }
                    }
                }.raiseForStatus()
                .body<GithubRelease>()
        }
    }

    /**
     * 自动检查更新（在应用启动时调用）
     */
    suspend fun autoCheckForUpdate(context: Context): Boolean {
        if (!shouldPerformAutoCheck(context) && getGitHubToken(context) == null) return false
        Log.i("UpdateManager", "Performing auto update check")

        try {
            updateState.value = UpdateState.Checking
            updateLastCheckTime(context)

            val currentVersion = SchematicVersion.fromString(context.versionName())
            val skippedVersion = getSkippedVersion(context)

            var latestVersion: SchematicVersion?

            // 检查正式版本
            val latestResponse = getLatestVersion(context)
            Log.i("UpdateManager", "Latest version response: $latestResponse")
            latestVersion = latestResponse.tagName.takeIf { it.isNotBlank() }?.let { SchematicVersion.fromString(it) }
            val latestDownloadInfo = latestResponse.extractDownloadInfo(context)

            if (latestVersion != null && latestVersion > currentVersion) {
                val versionString = latestVersion.toString()
                // 检查是否是被跳过的版本
                if (skippedVersion != versionString) {
                    updateState.value = UpdateState.UpdateAvailable(
                        latestVersion,
                        false,
                        latestResponse.body?.let(::extractGithubReleaseNotes),
                        latestDownloadInfo.browserDownloadUrl,
                        latestDownloadInfo.cnDownloadUrl,
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
            val currentVersion = SchematicVersion.fromString(context.versionName())
            val checkNightly = shouldCheckNightly(context)

            var latestVersion: SchematicVersion?
            var isNightly = false
            var releaseNotes: String?

            // 检查正式版本
            val latestResponse = getLatestVersion(context)
            latestVersion = latestResponse.tagName.takeIf { it.isNotBlank() }?.let { SchematicVersion.fromString(it) }
            releaseNotes = latestResponse.body?.let(::extractGithubReleaseNotes)
            var downloadInfo = latestResponse.extractDownloadInfo(context)

            // 如果启用了nightly检查，也检查nightly版本
            if (checkNightly) {
                try {
                    val nightlyResponse = client
                        .get(ZHIHU_PLUS_PLUS_GITHUB_NIGHTLY_RELEASE_URL) {
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
                        releaseNotes = nightlyResponse.body?.let(::extractGithubReleaseNotes)
                        downloadInfo = nightlyResponse.extractDownloadInfo(context)
                    }
                } catch (e: Exception) {
                    // nightly版本检查失败时，继续使用正式版本
                    Log.e("UpdateManager", "Failed to check nightly release", e)
                }
            }

            if (latestVersion != null && latestVersion > currentVersion) {
                updateState.value = UpdateState.UpdateAvailable(
                    latestVersion,
                    isNightly,
                    releaseNotes,
                    downloadInfo.browserDownloadUrl,
                    downloadInfo.cnDownloadUrl,
                )
            } else {
                updateState.value = UpdateState.Latest
            }
        } catch (e: Exception) {
            updateState.value = UpdateState.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun downloadUpdate(context: Context, downloadUrl: String) {
        val state = updateState.value
        if (state !is UpdateState.UpdateAvailable) return
        try {
            updateState.value = Downloading

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
        } catch (e: Exception) {
            updateState.value = UpdateState.Error(e.message ?: "Unknown error")
        }
    }

    internal fun selectApkAsset(apkAssets: List<GithubAsset>, isLiteVariant: Boolean): GithubAsset? = if (isLiteVariant) {
        // Lite version: strictly look for "lite" in filename
        apkAssets.firstOrNull { it.name.contains("lite", ignoreCase = true) }
    } else {
        // Full version: prefer "full" in filename
        apkAssets.firstOrNull { it.name.contains("full", ignoreCase = true) }
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
