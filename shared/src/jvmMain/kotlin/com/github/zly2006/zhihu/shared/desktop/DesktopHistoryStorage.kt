package com.github.zly2006.zhihu.shared.desktop

import com.github.zly2006.zhihu.navigation.NavDestination
import kotlinx.serialization.json.Json
import java.io.File

fun desktopZhihuDownloadsDir(errorMessage: String = "无法创建下载目录"): File =
    File(System.getProperty("user.home"), "Downloads/Zhihu++").also { directory ->
        if (!directory.exists() && !directory.mkdirs()) {
            throw IllegalStateException(errorMessage)
        }
    }

class DesktopHistoryStorage(
    private val historyFile: File = File(System.getProperty("user.home"), ".zhihu-plus/history.json"),
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
