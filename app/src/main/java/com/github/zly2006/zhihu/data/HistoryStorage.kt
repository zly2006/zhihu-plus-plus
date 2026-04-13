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

package com.github.zly2006.zhihu.data

import android.content.Context
import androidx.core.content.ContextCompat
import com.github.zly2006.zhihu.NavDestination
import kotlinx.serialization.json.Json
import java.io.File

class HistoryStorage(
    val activity: Context,
) {
    private val _history = linkedMapOf<NavDestination, NavDestination>()
    val history: List<NavDestination> get() = _history.values.reversed()

    init {
        load()
    }

    fun add(data: NavDestination) {
        ContextCompat.getMainExecutor(activity).execute {
            this._history.remove(data)
            this._history[data] = data
            while (this._history.size > 1000) {
                this._history.remove(this._history.keys.first())
            }
            save()
        }
    }

    fun save() {
        val json = Json.encodeToString(this._history.values.toList())
        val file = File(activity.filesDir, "history.json")
        file.writeText(json)
    }

    fun load() {
        val file = File(activity.filesDir, "history.json")
        if (file.exists()) {
            runCatching {
                val json = file.readText()
                val data = Json.decodeFromString<List<NavDestination>>(json)
                data.forEach { _history[it] = it }
            }
        }
    }

    fun post(data: NavDestination) {
        if (data in _history) {
            _history[data] = data
        }
    }

    fun clearAndSave() {
        ContextCompat.getMainExecutor(activity).execute {
            _history.clear()
            save()
        }
    }
}
