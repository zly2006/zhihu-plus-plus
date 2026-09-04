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

import com.github.zly2006.zhihu.account.ZhihuAccountRepository
import com.github.zly2006.zhihu.account.ZhihuAccountSessionStore
import com.github.zly2006.zhihu.account.ZhihuAccountStore
import com.github.zly2006.zhihu.navigation.NavDestination
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
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

val defaultDesktopAccountStore by lazy {
    ZhihuAccountStore(
        repository = ZhihuAccountRepository(PathAccountSessionStore(desktopZhihuLegacyAccountFile())),
    )
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

suspend fun ZhihuAccountStore.saveImageToDownloads(
    url: String,
    filePrefix: String,
): File = withContext(Dispatchers.IO) {
    val imageBytes = client.httpClient().get(url).body<ByteArray>()
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

private fun readDesktopRiskControlCookies(
    @Suppress("UNUSED_PARAMETER") url: String?,
): Map<String, String> = emptyMap()
