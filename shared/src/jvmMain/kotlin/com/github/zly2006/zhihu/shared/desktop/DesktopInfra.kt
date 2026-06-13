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

package com.github.zly2006.zhihu.shared.desktop
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.shared.account.ZhihuAccountRepository
import com.github.zly2006.zhihu.shared.account.ZhihuAccountSession
import com.github.zly2006.zhihu.shared.account.ZhihuAccountSessionStore
import com.github.zly2006.zhihu.shared.data.ZHIHU_READ_HISTORY_ADD_URL
import com.github.zly2006.zhihu.shared.data.buildZhihuReadHistoryBody
import com.github.zly2006.zhihu.shared.data.executeZhihuAuthenticatedRequest
import com.github.zly2006.zhihu.shared.data.fetchVerifiedZhihuSession
import com.github.zly2006.zhihu.shared.data.fetchZhihuAuthenticatedJson
import com.github.zly2006.zhihu.shared.data.installZhihuCommonClientConfig
import com.github.zly2006.zhihu.shared.login.SharedQrLoginPane
import com.github.zly2006.zhihu.shared.util.signZhihuFetchRequest
import com.github.zly2006.zhihu.theme.ZhihuTheme
import com.github.zly2006.zhihu.ui.DesktopZhihuMain
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

typealias DesktopAccountData = ZhihuAccountSession

class DesktopAccountStore(
    private val accountFile: Path = desktopZhihuLegacyAccountFile(),
) {
    private val repository = ZhihuAccountRepository(PathAccountSessionStore(accountFile))
    private var lastRefreshCookie = 0L

    fun load(): DesktopAccountData = repository.load()

    fun save(data: DesktopAccountData) = repository.save(data)

    fun clear() = repository.clear()

    fun createHttpClient(cookies: MutableMap<String, String>): HttpClient = HttpClient(CIO) {
        val savedData = load()
        installZhihuCommonClientConfig(
            cookies = cookies,
            userAgent = savedData.userAgent,
            onCookieChanged = {
                save(
                    savedData.copy(
                        cookies = cookies.toMutableMap(),
                    ),
                )
            },
        )
    }

    suspend fun fetchAuthenticatedJson(
        url: String,
        block: suspend HttpRequestBuilder.() -> Unit = {},
    ): JsonObject? {
        val account = load()
        return createHttpClient(account.cookies).use { client ->
            fetchZhihuAuthenticatedJson(
                client = client,
                url = url,
                lastRefreshMillis = lastRefreshCookie,
                updateLastRefreshMillis = { lastRefreshCookie = it },
                block = block,
            )
        }
    }

    suspend fun <T> withAuthenticatedResponse(
        url: String,
        block: suspend HttpRequestBuilder.() -> Unit = {},
        transform: suspend (HttpResponse) -> T,
    ): T {
        val account = load()
        return createHttpClient(account.cookies).use { client ->
            val response = executeZhihuAuthenticatedRequest(
                client = client,
                url = url,
                lastRefreshMillis = lastRefreshCookie,
                updateLastRefreshMillis = { lastRefreshCookie = it },
                block = block,
            )
            transform(response)
        }
    }

    suspend fun addReadHistory(
        contentToken: String,
        contentTypeName: String,
    ) {
        runCatching {
            val account = load()
            val dc0 = account.cookies["d_c0"] ?: return
            val body = buildZhihuReadHistoryBody(contentToken, contentTypeName)
            fetchAuthenticatedJson(ZHIHU_READ_HISTORY_ADD_URL) {
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                setBody(body)
                signDesktopRequest(account.cookies, body)
            }
        }
    }

    suspend fun verifyAndSave(cookies: MutableMap<String, String>): Boolean {
        createHttpClient(cookies).use { client ->
            val session = fetchVerifiedZhihuSession(client, cookies, load().userAgent) ?: return false
            save(session)
            return true
        }
    }
}

private class PathAccountSessionStore(
    private val accountFile: Path,
) : ZhihuAccountSessionStore {
    override fun readText(): String? = if (accountFile.exists()) {
        accountFile.readText()
    } else {
        null
    }

    override fun writeText(text: String) {
        accountFile.parent.createDirectories()
        accountFile.writeText(text)
    }

    override fun delete() {
        accountFile.deleteIfExists()
    }
}

/**
 * JVM 签名便捷函数：从 cookies 提取 d_c0 并签名。
 * 对应 Android 端 Utils.kt 的 signFetchRequest()。
 */
fun HttpRequestBuilder.signDesktopRequest(
    cookies: Map<String, String>,
    body: String? = null,
) {
    cookies["d_c0"]?.let { dc0 ->
        signZhihuFetchRequest(dc0 = dc0, body = body)
    }
}

/**
 * 签名后发起认证请求的便捷方法。
 * 内部自动加载账号 cookies 并签名，对应 Android 端 fetchGet/fetchPost 模式。
 */
suspend fun DesktopAccountStore.signedFetchJson(
    url: String,
    block: suspend HttpRequestBuilder.() -> Unit = {},
): JsonObject? = fetchAuthenticatedJson(url) {
    signDesktopRequest(load().cookies)
    block()
}

/**
 * 签名后发起认证请求并返回响应的便捷方法。
 */
suspend fun <T> DesktopAccountStore.signedWithResponse(
    url: String,
    block: suspend HttpRequestBuilder.() -> Unit = {},
    transform: suspend (HttpResponse) -> T,
): T = withAuthenticatedResponse(url, {
    signDesktopRequest(load().cookies)
    block()
}, transform)

suspend fun DesktopAccountStore.saveImageToDownloads(
    url: String,
    filePrefix: String,
): File = withContext(Dispatchers.IO) {
    val account = load()
    val imageBytes = createHttpClient(account.cookies).use { client ->
        client.get(url).body<ByteArray>()
    }
    val downloadsDir = desktopZhihuDownloadsDir()
    val file = File(downloadsDir, desktopImageFileName(filePrefix, url))
    file.writeBytes(imageBytes)
    file
}

private fun desktopImageFileName(
    filePrefix: String,
    url: String,
): String {
    val pathName = runCatching {
        URI(url).path.substringAfterLast('/').substringBefore('?')
    }.getOrNull().orEmpty()
    val extension = pathName.substringAfterLast('.', "").takeIf { it.length in 2..5 } ?: "jpg"
    return "${filePrefix}_${System.currentTimeMillis()}.$extension"
}

internal object DesktopLoginRequests {
    val version = MutableStateFlow(0)

    fun requestLogin() {
        version.update { it + 1 }
    }
}

@Composable
fun DesktopQrLoginScreen(
    store: DesktopAccountStore = remember { DesktopAccountStore() },
) {
    var statusText by remember { mutableStateOf("正在获取二维码") }
    var isLoggedIn by remember { mutableStateOf(false) }
    var didCheckSavedAccount by remember { mutableStateOf(false) }
    val loginRequestVersion by DesktopLoginRequests.version.collectAsState()

    LaunchedEffect(Unit) {
        val savedData = store.load()
        val cookies = savedData.cookies
        if (savedData.login && cookies.isNotEmpty()) {
            statusText = "正在验证已备份 cookie"
            if (store.verifyAndSave(cookies)) {
                statusText = "已使用备份 cookie 登录：${store.load().username}"
                isLoggedIn = true
                return@LaunchedEffect
            }
        }
        didCheckSavedAccount = true
    }

    LaunchedEffect(loginRequestVersion) {
        if (loginRequestVersion > 0) {
            statusText = "正在获取二维码"
            isLoggedIn = false
            didCheckSavedAccount = true
        }
    }

    ZhihuTheme {
        if (isLoggedIn) {
            DesktopZhihuMain()
        } else if (didCheckSavedAccount) {
            SharedQrLoginPane(
                createClient = { cookies -> store.createHttpClient(cookies) },
                onLoginSuccess = { cookies ->
                    store.verifyAndSave(cookies.toMutableMap()).also { success ->
                        if (success) {
                            isLoggedIn = true
                        }
                    }
                },
                initialCookies = store.load().cookies,
                qrReadyMessage = "请打开知乎 App 扫一扫",
                onQrReady = {
                    notifyUser("需要扫码登录 JVM 端")
                },
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.size(16.dp))
                Text(statusText)
            }
        }
    }
}

private fun notifyUser(message: String) {
    runCatching {
        ProcessBuilder("terminal-notifier", "-message", message, "-sound", "default")
            .start()
    }
}

class DesktopHistoryStorage(
    private val historyFile: File = desktopZhihuDataFile("history.json"),
) {
    private val historyMap = linkedMapOf<NavDestination, NavDestination>()
    val history: List<NavDestination>
        get() = historyMap.values.reversed()

    init {
        load()
    }

    fun add(data: NavDestination) {
        historyMap.remove(data)
        historyMap[data] = data
        while (historyMap.size > 1000) {
            historyMap.remove(historyMap.keys.first())
        }
        save()
    }

    fun clearAndSave() {
        historyMap.clear()
        save()
    }

    private fun save() {
        historyFile.parentFile?.mkdirs()
        historyFile.writeText(Json.encodeToString(historyMap.values.toList()))
    }

    private fun load() {
        if (!historyFile.exists()) return
        runCatching {
            val data = Json.decodeFromString<List<NavDestination>>(historyFile.readText())
            data.forEach { historyMap[it] = it }
        }
    }
}

fun desktopZhihuDataDir(): File =
    File(System.getProperty("user.home"), ".zhihu-plus")

fun desktopZhihuDataFile(relativePath: String): File =
    File(desktopZhihuDataDir(), relativePath)

fun desktopZhihuDownloadsDir(errorMessage: String = "无法创建下载目录"): File =
    File(System.getProperty("user.home"), "Downloads/Zhihu++").also { directory ->
        if (!directory.exists() && !directory.mkdirs()) {
            throw IllegalStateException(errorMessage)
        }
    }

fun desktopZhihuLegacyAccountFile(): Path =
    Path.of(System.getProperty("user.home"), ".zhihu-plus-plus", "account.json")

internal fun openDesktopExternalUrl(url: String): Boolean = runCatching {
    if (!Desktop.isDesktopSupported()) {
        return@runCatching false
    }
    Desktop.getDesktop().browse(URI(url))
    true
}.getOrDefault(false)

internal fun copyDesktopPlainText(text: String) =
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
