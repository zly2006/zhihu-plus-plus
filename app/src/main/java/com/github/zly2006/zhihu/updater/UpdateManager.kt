package com.github.zly2006.zhihu.updater

import android.content.Context
import android.content.Intent
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
        object Latest : UpdateState()
        data class UpdateAvailable(val version: SchematicVersion) : UpdateState()
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
            val latestVersion = response["tag_name"]?.jsonPrimitive?.content?.let { SchematicVersion.fromString(it) }
            val currentVersion = SchematicVersion.fromString(BuildConfig.VERSION_NAME)

            if (latestVersion != null && latestVersion > currentVersion) {
                updateState.value = UpdateState.UpdateAvailable(latestVersion)
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
