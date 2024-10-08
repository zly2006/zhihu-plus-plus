package com.github.zly2006.zhihu.data

import androidx.fragment.app.FragmentActivity
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.NavDestination
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class HistoryStorage(
    val activity: MainActivity
) {
    private val _history = linkedSetOf<NavDestination>()
    val history: List<NavDestination> get() = _history.toList()

    init {
        load()
    }

    fun add(data: NavDestination) {
        if (!this._history.add(data)) {
            this._history.remove(data)
            this._history.add(data)
        }
        save()
    }

    fun save() {
        val json = Json.encodeToString(this._history)
        val file = File(activity.filesDir, "history.json")
        file.writeText(json)
    }

    fun load() {
        val file = File(activity.filesDir, "history.json")
        if (file.exists()) runCatching {
            val json = file.readText()
            this._history.addAll(Json.decodeFromString(json))
        }
    }

    companion object {
        inline fun <reified T: NavDestination> FragmentActivity.recordHistory(data: T) {
            if (this is MainActivity) {
                Json.encodeToString(data)
//                this.history.add(data)
            }
        }
    }
}
