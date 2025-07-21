package com.github.zly2006.zhihu.updater

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.github.zly2006.zhihu.BuildConfig
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.updater.UpdateManager.UpdateState.Downloading
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.URI

object UpdateManager {
    private const val GITHUB_API_LATEST = "https://api.github.com/repos/zly2006/zhihu-plus-plus/releases/latest"
    private const val GITHUB_API_NIGHTLY = "https://api.github.com/repos/zly2006/zhihu-plus-plus/releases/tags/nightly"

    sealed class UpdateState {
        object NoUpdate : UpdateState()
        object Checking : UpdateState()
        object Latest : UpdateState()
        data class UpdateAvailable(val version: SchematicVersion, val isNightly: Boolean = false) : UpdateState()
        object Downloading : UpdateState()
        data class Downloaded(val file: File) : UpdateState()
        data class Error(val message: String) : UpdateState()
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

    suspend fun checkForUpdate(context: Context) {
        try {
            updateState.value = UpdateState.Checking

            val client = AccountData.httpClient(context)
            val currentVersion = SchematicVersion.fromString(BuildConfig.VERSION_NAME)
            val checkNightly = shouldCheckNightly(context)

            var latestVersion: SchematicVersion? = null
            var isNightly = false

            // 检查正式版本
            val latestResponse = client.get(GITHUB_API_LATEST) {
                getGitHubToken(context)?.let { token ->
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $token")
                    }
                }
            }.body<JsonObject>()
            latestVersion = latestResponse["tag_name"]?.jsonPrimitive?.content?.let { SchematicVersion.fromString(it) }

            // 如果启用了nightly检查，也检查nightly版本
            if (checkNightly) {
                try {
                    val nightlyResponse = client.get(GITHUB_API_NIGHTLY) {
                        getGitHubToken(context)?.let { token ->
                            headers {
                                append(HttpHeaders.Authorization, "Bearer $token")
                            }
                        }
                    }.body<JsonObject>()
                    val nightlyVersion = nightlyResponse["tag_name"]?.jsonPrimitive?.content

                    // 如果nightly版本比正式版本新，则使用nightly版本
                    if (nightlyVersion == "nightly") {
                        latestVersion = SchematicVersion(
                            allComponents = listOf(999, 0, 0),
                            preRelease = "nightly",
                            build = ""
                        )
                        isNightly = true
                    }
                } catch (e: Exception) {
                    // nightly版本检查失败时，继续使用正式版本
                    e.printStackTrace()
                }
            }

            if (latestVersion != null && latestVersion > currentVersion) {
                updateState.value = UpdateState.UpdateAvailable(latestVersion, isNightly)
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

            val response = client.get(apiUrl) {
                getGitHubToken(context)?.let { token ->
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $token")
                    }
                }
            }.body<JsonObject>()
            val assets = response["assets"]?.jsonArray
            val downloadUrl = assets?.firstOrNull {
                it.jsonObject["content_type"]?.jsonPrimitive?.content == "application/vnd.android.package-archive"
            }
                ?.jsonObject?.get("browser_download_url")
                ?.jsonPrimitive?.content

            if (downloadUrl != null) {
                val file = withContext(Dispatchers.IO) {
                    val apkFile = File(context.cacheDir, "update.apk")
                    URI(downloadUrl).toURL().openConnection().getInputStream().copyTo(apkFile.outputStream())
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
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
