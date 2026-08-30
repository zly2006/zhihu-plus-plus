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

package com.github.zly2006.zhihu.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.github.zly2006.zhihu.data.ZhihuJson.json
import com.github.zly2006.zhihu.platform.androidSettingsStore
import com.github.zly2006.zhihu.viewmodel.AIGC_VOTE_SERVER_URL_KEY
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File
import java.security.MessageDigest
import java.util.UUID

private const val CRASH_REPORT_INSTALL_ID_KEY = "crashReportInstallId"
private const val CRASH_REPORT_DIRECTORY = "crash-reports"
private const val DEFAULT_CRASH_REPORT_SERVER_URL = "https://aigc-vote.ai.fintechedu.cn"
private const val MAX_STACK_TRACE_LENGTH = 32_000
private const val TAG = "CrashReporter"

@Serializable
data class CrashReportPayload(
    val client_hash: String,
    val app_version: String,
    val platform: String,
    val device_model: String? = null,
    val os_version: String? = null,
    val build_code: Long? = null,
    val occurred_at: Long,
    val exception_type: String,
    val exception_message: String? = null,
    val thread_name: String? = null,
    val stack_trace: String,
)

/**
 * 崩溃日志本地留存与 opt-in 匿名上报。
 *
 * 崩溃日志总是先写入应用私有目录，用户开启 [CRASH_REPORT_ENABLED_PREFERENCE_KEY] 后才在下次启动时
 * 上报到 aigc-vote 服务端；上报内容不含账号、Cookie 或浏览内容。用户也可以手动导出本地日志。
 */
object CrashReporter {
    private var handlerInstalled = false
    private var fallbackHandler: ((Thread, Throwable) -> Unit)? = null
    private var previousHandler: Thread.UncaughtExceptionHandler? = null

    @Synchronized
    fun install(context: Context, fallback: ((Thread, Throwable) -> Unit)? = null) {
        if (fallback != null || fallbackHandler == null) {
            fallbackHandler = fallback
        }
        if (handlerInstalled) return
        handlerInstalled = true
        previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            recordCrash(context.applicationContext, thread, throwable)
            fallbackHandler?.invoke(thread, throwable)
                ?: previousHandler?.uncaughtException(thread, throwable)
        }
    }

    fun isEnabled(context: Context): Boolean =
        androidSettingsStore(context).getBoolean(CRASH_REPORT_ENABLED_PREFERENCE_KEY, false)

    fun pendingCrashReports(context: Context): List<File> =
        crashReportsDir(context)
            ?.listFiles { file -> file.isFile && file.extension == "json" }
            ?.sortedBy { it.name }
            ?: emptyList()

    fun crashReportsDir(context: Context): File? = runCatching {
        File(context.applicationContext.filesDir, CRASH_REPORT_DIRECTORY).apply { mkdirs() }
    }.getOrNull()

    fun recordCrash(context: Context, thread: Thread, throwable: Throwable) {
        val dir = crashReportsDir(context) ?: return
        val payload = buildPayload(context.applicationContext, thread, throwable)
        val file = File(dir, "crash-${System.currentTimeMillis()}.json")
        runCatching { file.writeText(json.encodeToString(payload)) }
    }

    suspend fun uploadPendingCrashReports(context: Context): Int = withContext(Dispatchers.IO) {
        if (!isEnabled(context)) return@withContext 0
        val files = pendingCrashReports(context)
        if (files.isEmpty()) return@withContext 0
        val settings = androidSettingsStore(context)
        val baseUrl = settings
            .getString(AIGC_VOTE_SERVER_URL_KEY, DEFAULT_CRASH_REPORT_SERVER_URL)
            .trimEnd('/')
            .ifBlank { DEFAULT_CRASH_REPORT_SERVER_URL }
        val versionName = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "unknown"
        val client = HttpClient()
        var uploaded = 0
        try {
            for (file in files) {
                val payload = runCatching {
                    json.decodeFromString<CrashReportPayload>(file.readText())
                }.getOrNull() ?: run {
                    file.delete()
                    continue
                }
                val response: HttpResponse = client.post("$baseUrl/v1/crash-reports") {
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                    header(HttpHeaders.UserAgent, "Zhihu++/$versionName")
                }
                if (response.status.isSuccess()) {
                    file.delete()
                    uploaded += 1
                } else {
                    break
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "上传崩溃日志失败", e)
        } finally {
            client.close()
        }
        uploaded
    }

    fun exportCrashReports(context: Context): Int {
        val files = pendingCrashReports(context)
        if (files.isEmpty()) return 0
        val appContext = context.applicationContext
        val uris = files.map {
            FileProvider.getUriForFile(appContext, "${appContext.packageName}.provider", it)
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "application/json"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val started = runCatching {
            appContext.startActivity(
                Intent.createChooser(intent, "导出崩溃日志").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.isSuccess
        if (!started) {
            Log.w(TAG, "导出崩溃日志失败")
        }
        return if (started) files.size else 0
    }

    private fun buildPayload(
        context: Context,
        thread: Thread,
        throwable: Throwable,
    ): CrashReportPayload {
        val versionName = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "unknown"
        val versionCode = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
        }.getOrNull() ?: 0L
        return CrashReportPayload(
            client_hash = installHash(context),
            app_version = versionName,
            platform = "android",
            device_model = Build.MODEL,
            os_version = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            build_code = versionCode.takeIf { it > 0 },
            occurred_at = System.currentTimeMillis() / 1000,
            exception_type = throwable.javaClass.name,
            exception_message = throwable.message,
            thread_name = thread.name,
            stack_trace = throwable.stackTraceToString().take(MAX_STACK_TRACE_LENGTH),
        )
    }

    private fun installHash(context: Context): String {
        val settings = androidSettingsStore(context)
        val installId = settings.getStringOrNull(CRASH_REPORT_INSTALL_ID_KEY) ?: run {
            val id = UUID.randomUUID().toString()
            settings.putString(CRASH_REPORT_INSTALL_ID_KEY, id)
            id
        }
        return MessageDigest
            .getInstance("SHA-256")
            .digest(installId.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }
    }
}
