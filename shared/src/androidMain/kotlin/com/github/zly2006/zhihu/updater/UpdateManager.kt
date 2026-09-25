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

package com.github.zly2006.zhihu.updater

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.github.zly2006.zhihu.account.accountHttpClientEngineFactory
import com.github.zly2006.zhihu.account.androidZhihuAccountStore
import com.github.zly2006.zhihu.platform.androidSettingsStore
import com.github.zly2006.zhihu.platform.isAndroidLiteVariantPackageName
import com.github.zly2006.zhihu.updater.DownloadResult
import com.github.zly2006.zhihu.updater.DownloadVerifier
import com.github.zly2006.zhihu.updater.GithubAsset
import com.github.zly2006.zhihu.updater.GithubRelease
import com.github.zly2006.zhihu.updater.MIRROR_ACCELERATION_ENABLED_PREFERENCE_KEY
import com.github.zly2006.zhihu.updater.MirrorDownloadRequest
import com.github.zly2006.zhihu.updater.MirrorSelector
import com.github.zly2006.zhihu.updater.SchematicVersion
import com.github.zly2006.zhihu.updater.UpdateManager.UpdateState.Downloading
import com.github.zly2006.zhihu.updater.androidUpdateVerifier
import com.github.zly2006.zhihu.updater.extractGithubReleaseNotes
import com.github.zly2006.zhihu.updater.fetchLatestZhihuRelease
import com.github.zly2006.zhihu.updater.fetchNightlyZhihuRelease
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import java.io.File

/** 更新流程失败所处阶段：获取版本元数据，或下载与校验安装包。 */
enum class UpdateErrorPhase {
    /** 检查更新（获取版本元数据）失败。 */
    Check,

    /** 下载安装包失败，或下载后完整性校验未通过被拒绝。 */
    Download,
}

object UpdateManager {
    private const val AUTO_CHECK_INTERVAL_MILLIS = 3 * 60 * 60 * 1000L

    /**
     * 自动检查更新要跳过的版本
     */
    private const val PREF_SKIPPED_VERSION = "skippedVersion"
    private const val PREF_LAST_UPDATE_CHECK = "lastUpdateCheck"

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
            val opensExternally: Boolean = false,
            /** 官方资产的字节数，用于镜像探测筛选与下载后大小校验；元数据缺失时为 null。 */
            val downloadSize: Long? = null,
            /** 官方资产的 SHA256 摘要（Releases 的 digest 字段），缺失时降级为大小加签名校验。 */
            val downloadDigest: String? = null,
        ) : UpdateState()

        data class Downloading(
            /** 当前使用的下载来源；探测尚未选出时为 null。 */
            val sourceName: String? = null,
            val downloadedBytes: Long = 0,
            val totalBytes: Long = 0,
        ) : UpdateState()

        data class Downloaded(
            val file: File,
            /** 实际生效的校验层级，用于向用户说明下载物为什么可信。 */
            val verification: String? = null,
        ) : UpdateState()

        data class Error(
            val message: String,
            /** 失败所处阶段，界面据此区分「检查更新失败」与「下载/校验失败」。 */
            val phase: UpdateErrorPhase,
        ) : UpdateState()
    }

    val updateState = MutableStateFlow<UpdateState>(UpdateState.NoUpdate)

    /**
     * 镜像选优器。作为长生命周期实例持有，使各来源的连续失败次数能跨多次下载累积。
     */
    private val mirrorSelector = MirrorSelector()

    /**
     * 更新下载专用的匿名客户端。
     *
     * 不复用知乎账户客户端：镜像站与 GitHub 都是第三方，把知乎 Cookie、UA 与签名头带过去属于
     * 无必要的隐私外泄。这里只复用平台 HTTP 引擎。
     */
    private val updateHttpClient by lazy { HttpClient(accountHttpClientEngineFactory) {} }

    private fun getGitHubToken(context: Context): String? = androidSettingsStore(context).getStringOrNull("githubToken")?.takeIf { it.isNotBlank() }

    private fun Context.versionName(): String = runCatching {
        packageManager.getPackageInfo(packageName, 0).versionName
    }.getOrNull() ?: "0.0.0"

    /**
     * 设置跳过的版本
     */
    fun skipVersion(context: Context, version: String) {
        androidSettingsStore(context).putString(PREF_SKIPPED_VERSION, version)
    }

    /**
     * 检查是否需要进行自动更新检查（避免频繁检查）
     */
    private fun shouldPerformAutoCheck(context: Context): Boolean {
        val settings = androidSettingsStore(context)
        return (System.currentTimeMillis() - settings.getLong(PREF_LAST_UPDATE_CHECK, 0)) >= AUTO_CHECK_INTERVAL_MILLIS
    }

    suspend fun getLatestVersion(context: Context): GithubRelease {
        val client = androidZhihuAccountStore(context).client.httpClient()
        return fetchLatestZhihuRelease(client, getGitHubToken(context))
    }

    /**
     * 自动检查更新（在应用启动时调用）
     */
    suspend fun autoCheckForUpdate(context: Context): Boolean {
        if (!shouldPerformAutoCheck(context) && getGitHubToken(context) == null) return false
        Log.i("UpdateManager", "Performing auto update check")

        try {
            updateState.value = UpdateState.Checking
            androidSettingsStore(context).putLong(PREF_LAST_UPDATE_CHECK, System.currentTimeMillis())

            val currentVersion = SchematicVersion.fromString(context.versionName())
            val skippedVersion = androidSettingsStore(context).getStringOrNull(PREF_SKIPPED_VERSION)

            var latestVersion: SchematicVersion?

            // 检查正式版本
            val latestResponse = getLatestVersion(context)
            Log.i("UpdateManager", "Latest version response: $latestResponse")
            latestVersion = latestResponse.tagName.takeIf { it.isNotBlank() }?.let { SchematicVersion.fromString(it) }
            val latestDownloadInfo = latestResponse.extractAndroidDownloadInfo(isAndroidLiteVariantPackageName(context.packageName))

            if (latestVersion != null && latestVersion > currentVersion) {
                val versionString = latestVersion.toString()
                // 检查是否是被跳过的版本
                if (skippedVersion != versionString) {
                    updateState.value = UpdateState.UpdateAvailable(
                        version = latestVersion,
                        isNightly = false,
                        releaseNotes = latestResponse.body?.let(::extractGithubReleaseNotes),
                        downloadUrl = latestDownloadInfo.browserDownloadUrl,
                        cnDownloadUrl = latestDownloadInfo.cnDownloadUrl,
                        opensExternally = latestDownloadInfo.opensExternally,
                        downloadSize = latestDownloadInfo.size,
                        downloadDigest = latestDownloadInfo.digest,
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
            updateState.value = UpdateState.Error(e.message ?: "Unknown error", UpdateErrorPhase.Check)
        }

        return false
    }

    suspend fun checkForUpdate(context: Context) {
        try {
            updateState.value = UpdateState.Checking
            androidSettingsStore(context).putLong(PREF_LAST_UPDATE_CHECK, System.currentTimeMillis())

            val client = androidZhihuAccountStore(context).client.httpClient()
            val currentVersion = SchematicVersion.fromString(context.versionName())
            val checkNightly = androidSettingsStore(context).getBoolean("checkNightlyUpdates", false)

            var latestVersion: SchematicVersion?
            var isNightly = false
            var releaseNotes: String?

            // 检查正式版本
            val latestResponse = getLatestVersion(context)
            latestVersion = latestResponse.tagName.takeIf { it.isNotBlank() }?.let { SchematicVersion.fromString(it) }
            releaseNotes = latestResponse.body?.let(::extractGithubReleaseNotes)
            var downloadInfo = latestResponse.extractAndroidDownloadInfo(isAndroidLiteVariantPackageName(context.packageName))

            // 如果启用了nightly检查，也检查nightly版本
            if (checkNightly) {
                try {
                    val nightlyResponse = fetchNightlyZhihuRelease(client, getGitHubToken(context))

                    // 如果nightly版本比正式版本新，则使用nightly版本
                    if (nightlyResponse.tagName == "nightly") {
                        latestVersion = SchematicVersion(
                            allComponents = listOf(999, 0, 0),
                            preRelease = "nightly",
                            build = "",
                        )
                        isNightly = true
                        releaseNotes = nightlyResponse.body?.let(::extractGithubReleaseNotes)
                        downloadInfo = nightlyResponse.extractAndroidDownloadInfo(isAndroidLiteVariantPackageName(context.packageName))
                    }
                } catch (e: Exception) {
                    // nightly版本检查失败时，继续使用正式版本
                    Log.e("UpdateManager", "Failed to check nightly release", e)
                }
            }

            if (latestVersion != null && latestVersion > currentVersion) {
                updateState.value = UpdateState.UpdateAvailable(
                    version = latestVersion,
                    isNightly = isNightly,
                    releaseNotes = releaseNotes,
                    downloadUrl = downloadInfo.browserDownloadUrl,
                    cnDownloadUrl = downloadInfo.cnDownloadUrl,
                    opensExternally = downloadInfo.opensExternally,
                    downloadSize = downloadInfo.size,
                    downloadDigest = downloadInfo.digest,
                )
            } else {
                updateState.value = UpdateState.Latest
            }
        } catch (e: Exception) {
            updateState.value = UpdateState.Error(e.message ?: "Unknown error", UpdateErrorPhase.Check)
        }
    }

    /**
     * 下载更新安装包。
     *
     * 走多源镜像加速：并行探测候选源后按实测带宽选优，低速、停滞或中断时换源并从断点续传。
     * 下载完成后必须通过文件大小、SHA256 摘要与 APK 签名三层校验，才会进入
     * [UpdateState.Downloaded]，校验是安装门禁，不通过不放行。
     */
    suspend fun downloadUpdate(context: Context, downloadUrl: String) {
        val state = updateState.value
        if (state !is UpdateState.UpdateAvailable) return
        try {
            if (state.opensExternally) {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, downloadUrl.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                return
            }
            updateState.value = Downloading()

            val apkFile = File(context.cacheDir, "update.apk")
            val result = withContext(Dispatchers.IO) {
                mirrorSelector.download(
                    client = updateHttpClient,
                    request = MirrorDownloadRequest(
                        officialUrl = downloadUrl,
                        destination = Path(apkFile.absolutePath),
                        expectedSize = state.downloadSize,
                        expectedDigest = DownloadVerifier.parseDigest(state.downloadDigest),
                        mirrorEnabled = androidSettingsStore(context)
                            .getBoolean(MIRROR_ACCELERATION_ENABLED_PREFERENCE_KEY, true),
                    ),
                    verifier = androidUpdateVerifier(context),
                ) { progress ->
                    updateState.value = Downloading(
                        progress.source.name,
                        progress.downloadedBytes,
                        progress.totalBytes,
                    )
                }
            }
            updateState.value = when (result) {
                is DownloadResult.Verified -> UpdateState.Downloaded(apkFile, result.layers.joinToString(" + "))
                is DownloadResult.Failed -> UpdateState.Error(result.reason, UpdateErrorPhase.Download)
            }
        } catch (e: Exception) {
            updateState.value = UpdateState.Error(e.message ?: "Unknown error", UpdateErrorPhase.Download)
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
