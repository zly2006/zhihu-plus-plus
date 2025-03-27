package com.github.zly2006.zhihu.updater

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.github.zly2006.zhihu.BuildConfig
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.updater.UpdateManager.UpdateState.Downloading
import io.ktor.client.call.*
import io.ktor.client.request.*
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
    private const val GITHUB_API = "https://api.github.com/repos/zly2006/zhihu-plus-plus/releases/latest"

    sealed class UpdateState {
        object Checking : UpdateState()
        object NoUpdate : UpdateState()
        data class UpdateAvailable(val version: String) : UpdateState()
        data class Downloaded(val file: File) : UpdateState()
        object Downloading : UpdateState()
        data class Error(val message: String) : UpdateState()
    }

    val updateState = MutableStateFlow<UpdateState>(UpdateState.NoUpdate)

    suspend fun checkForUpdate(context: Context) {
        try {
            updateState.value = UpdateState.Checking

            val client = AccountData.httpClient(context)
            val response = client.get(GITHUB_API).body<JsonObject>()
            val latestVersion = response["tag_name"]?.jsonPrimitive?.content

            if (latestVersion != null && latestVersion != BuildConfig.VERSION_NAME) {
                updateState.value = UpdateState.UpdateAvailable(latestVersion)
            } else {
                updateState.value = UpdateState.NoUpdate
                context.mainExecutor.execute {
                    Toast.makeText(
                        context,
                        "已经是最新版本",
                        Toast.LENGTH_SHORT
                    ).show()
                }
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
            val response = client.get(GITHUB_API).body<JsonObject>()
            val assets = response["assets"]?.jsonArray
            val downloadUrl = assets?.firstOrNull()
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
        try {
            if (!file.exists()) {
                updateState.value = UpdateState.Error("Update file not found")
                return
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // 确保有可以处理此Intent的应用
            val activities = context.packageManager.queryIntentActivities(intent, 0)
            if (activities.isEmpty()) {
                updateState.value = UpdateState.Error("No app can handle APK installation")
                return
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            updateState.value = UpdateState.Error("Install failed: ${e.message}")
        }
    }
}
