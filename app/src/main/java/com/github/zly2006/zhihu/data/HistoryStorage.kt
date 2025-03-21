package com.github.zly2006.zhihu.data

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import com.github.zly2006.zhihu.LegacyMainActivity
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.catching
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class HistoryStorage(
    val activity: Context
) {
    private val _history = linkedMapOf<NavDestination, NavDestination>()
    val history: List<NavDestination> get() = _history.values.reversed()

    init {
        load()
    }

    fun add(data: NavDestination) {
        this._history.remove(data)
        this._history[data] = data
        while (this._history.size > 1000) {
            this._history.remove(this._history.keys.first())
        }
        save()
    }

    fun save() {
        val json = Json.encodeToString(this._history.values.toList())
        val file = File(activity.filesDir, "history.json")
        file.writeText(json)
    }

    fun load() {
        val file = File(activity.filesDir, "history.json")
        if (file.exists()) runCatching {
            val json = file.readText()
            val data = Json.decodeFromString<List<NavDestination>>(json)
            data.forEach { _history[it] = it }
        }
    }

    companion object {
        inline fun <reified T: NavDestination> FragmentActivity.recordHistory(data: T) {
            if (this is LegacyMainActivity) {
                catching {
                    Json.encodeToString(data)
                    this.history.add(data)
                }
            }
        }

        inline fun <reified T: NavDestination> FragmentActivity.navigate(data: T) {
            if (this is LegacyMainActivity) {
                catching {
                    val navController = findNavController(R.id.nav_host_fragment_activity_main)
                    navController.navigate(data)
                    recordHistory(data)
                }
            }
        }

        inline fun <reified T: NavDestination> FragmentActivity.postHistory(data: T) {
            if (this is LegacyMainActivity) {
                catching {
                    this.history.post(data)
                }
            }
        }
    }

    fun post(data: NavDestination) {
        if (data in _history) {
            _history[data] = data
        }
    }
}
