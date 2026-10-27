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

package com.github.zly2006.zhihu.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSUserDefaults

@OptIn(ExperimentalForeignApi::class)
internal actual fun nativeSettingsStore(relativePath: String): SettingsStore {
    val defaults = NSUserDefaults.standardUserDefaults
    val namespace = "zhpp.$relativePath."
    val changes = "${namespace}changed"
    return object : SettingsStore {
        private fun read(key: String): String? = defaults.stringForKey(namespace + key)

        private fun write(key: String, value: Any?) {
            if (value == null) defaults.removeObjectForKey(namespace + key) else defaults.setObject(value, forKey = namespace + key)
            NSNotificationCenter.defaultCenter.postNotificationName(changes, `object` = key)
        }

        override fun getBoolean(key: String, defaultValue: Boolean) = read(key)?.toBooleanStrictOrNull() ?: defaultValue

        override fun putBoolean(key: String, value: Boolean) = write(key, value.toString())

        override fun getString(key: String, defaultValue: String) = read(key) ?: defaultValue

        override fun putString(key: String, value: String) = write(key, value)

        override fun getStringOrNull(key: String) = read(key)

        override fun putStringSet(key: String, value: Set<String>) = write(key, value.toList())

        override fun getStringSet(key: String, defaultValue: Set<String>): Set<String> =
            defaults.stringArrayForKey(namespace + key)?.map { it.toString() }?.toSet() ?: defaultValue

        override fun getInt(key: String, defaultValue: Int) = read(key)?.toIntOrNull() ?: defaultValue

        override fun putInt(key: String, value: Int) = write(key, value.toString())

        override fun getLong(key: String, defaultValue: Long) = read(key)?.toLongOrNull() ?: defaultValue

        override fun putLong(key: String, value: Long) = write(key, value.toString())

        override fun getFloat(key: String, defaultValue: Float) = read(key)?.toFloatOrNull() ?: defaultValue

        override fun putFloat(key: String, value: Float) = write(key, value.toString())

        override fun remove(key: String) = write(key, null)

        override fun removeByPrefix(prefix: String) {
            defaults
                .dictionaryRepresentation()
                .keys
                .map { it.toString() }
                .filter { it.startsWith(namespace + prefix) }
                .forEach { remove(it.removePrefix(namespace)) }
        }

        override fun observeKeyChanges(onChanged: (String) -> Unit): AutoCloseable {
            val center = NSNotificationCenter.defaultCenter
            val observer = center.addObserverForName(changes, `object` = null, queue = NSOperationQueue.mainQueue) { note ->
                note?.`object`?.toString()?.let(onChanged)
            }
            return AutoCloseable { center.removeObserver(observer) }
        }
    }
}
