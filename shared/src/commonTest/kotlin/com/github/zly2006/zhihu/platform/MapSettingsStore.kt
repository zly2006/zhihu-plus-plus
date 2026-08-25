/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.github.zly2006.zhihu.platform

class MapSettingsStore(
    private val values: MutableMap<String, Any> = mutableMapOf(),
) : SettingsStore {
    override fun getBoolean(key: String, defaultValue: Boolean) = values[key] as? Boolean ?: defaultValue

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }

    override fun getString(key: String, defaultValue: String) = values[key] as? String ?: defaultValue

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun getStringOrNull(key: String) = values[key] as? String

    override fun putStringSet(key: String, value: Set<String>) {
        values[key] = value
    }

    override fun getStringSet(key: String, defaultValue: Set<String>): Set<String> =
        (values[key] as? Iterable<*>)?.filterIsInstance<String>()?.toSet() ?: defaultValue

    override fun getInt(key: String, defaultValue: Int) = values[key] as? Int ?: defaultValue

    override fun putInt(key: String, value: Int) {
        values[key] = value
    }

    override fun getLong(key: String, defaultValue: Long) = values[key] as? Long ?: defaultValue

    override fun putLong(key: String, value: Long) {
        values[key] = value
    }

    override fun getFloat(key: String, defaultValue: Float) = values[key] as? Float ?: defaultValue

    override fun putFloat(key: String, value: Float) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}
