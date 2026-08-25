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

package com.github.zly2006.zhihu.desktop

import com.github.zly2006.zhihu.account.ZhihuAccountClient
import com.github.zly2006.zhihu.account.ZhihuAccountRepository
import com.github.zly2006.zhihu.account.ZhihuAccountSession
import com.github.zly2006.zhihu.account.ZhihuAccountSessionStore
import com.github.zly2006.zhihu.account.ZhihuMobileLoginToken
import com.github.zly2006.zhihu.data.executeZhihuAuthenticatedRequest
import com.github.zly2006.zhihu.data.installZhihuCommonClientConfig
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.util.signZhihuFetchRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

typealias DesktopAccountData = ZhihuAccountSession

private val defaultDesktopAccountState = MutableStateFlow(ZhihuAccountSession())

private val defaultDesktopAccountClient by lazy {
    createDesktopAccountClient(desktopZhihuLegacyAccountFile()) {
        defaultDesktopAccountState.value = it
    }
}

private fun createDesktopAccountClient(
    accountFile: Path,
    onSessionChanged: (ZhihuAccountSession) -> Unit,
): ZhihuAccountClient =
    ZhihuAccountClient(
        repository = ZhihuAccountRepository(PathAccountSessionStore(accountFile)),
        createClient = { cookies, session, onCookieChanged, _ ->
            createDesktopHttpClient(cookies, session.userAgent, onCookieChanged)
        },
        onSessionChanged = onSessionChanged,
    )

private fun createDesktopHttpClient(
    cookies: MutableMap<String, String>,
    userAgent: String,
    onCookieChanged: () -> Unit = {},
): HttpClient = HttpClient(CIO) {
    installZhihuCommonClientConfig(
        cookies = cookies,
        userAgent = userAgent,
        onCookieChanged = onCookieChanged,
    )
}

class DesktopAccountStore(
    accountFile: Path = desktopZhihuLegacyAccountFile(),
) {
    private val usesDefaultAccountFile = accountFile == desktopZhihuLegacyAccountFile()
    private val mutableAccountState =
        if (usesDefaultAccountFile) defaultDesktopAccountState else MutableStateFlow(ZhihuAccountSession())
    private val accountClient =
        if (usesDefaultAccountFile) {
            defaultDesktopAccountClient
        } else {
            createDesktopAccountClient(accountFile) {
                mutableAccountState.value = it
            }
        }
    val accountState: StateFlow<DesktopAccountData> = mutableAccountState.asStateFlow()

    init {
        accountClient.load()
    }

    fun load(): DesktopAccountData = accountClient.load()

    fun save(data: DesktopAccountData) = accountClient.save(data)

    fun clear() = accountClient.clear()

    fun httpClient(): HttpClient = accountClient.httpClient()

    fun createHttpClient(cookies: MutableMap<String, String>): HttpClient =
        accountClient.temporaryHttpClient(cookies)

    suspend fun <T> withAuthenticatedClient(
        block: suspend (client: HttpClient, cookies: Map<String, String>) -> T,
    ): T = accountClient.withAuthenticatedClient(block)

    suspend fun verifyAndSave(cookies: MutableMap<String, String>): Boolean =
        accountClient.verifyAndSave(cookies)

    suspend fun verifyMobileAndSave(token: ZhihuMobileLoginToken): Boolean =
        accountClient.verifyMobileAndSave(token)

    suspend fun refreshAndSaveProfile(): ZhihuAccountSession? =
        accountClient.refreshAndSaveProfile()
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
 * 签名后发起认证请求并返回响应的便捷方法。
 */
suspend fun <T> DesktopAccountStore.signedWithResponse(
    url: String,
    block: suspend HttpRequestBuilder.() -> Unit = {},
    transform: suspend (HttpResponse) -> T,
): T {
    val response = executeZhihuAuthenticatedRequest(
        client = httpClient(),
        url = url,
    ) {
        signZhihuFetchRequest(load().cookies)
        block()
    }
    return transform(response)
}

suspend fun DesktopAccountStore.saveImageToDownloads(
    url: String,
    filePrefix: String,
): File = withContext(Dispatchers.IO) {
    val imageBytes = httpClient().get(url).body<ByteArray>()
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

internal class DesktopPropertiesFile(
    relativePath: String,
    private val comments: String,
) {
    private val file = desktopZhihuDataFile(relativePath)
    val properties: Properties = Properties()

    init {
        if (file.isFile) {
            file.inputStream().use(properties::load)
        }
    }

    fun save() {
        file.parentFile?.mkdirs()
        file.outputStream().use { output ->
            properties.store(output, comments)
        }
    }
}

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
