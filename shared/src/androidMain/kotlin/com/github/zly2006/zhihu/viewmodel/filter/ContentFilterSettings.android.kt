package com.github.zly2006.zhihu.viewmodel.filter

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.github.zly2006.zhihu.shared.platform.SettingsStore
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME

fun Context.contentFilterSettings(): FeedFilterSettings =
    getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        .toContentFilterSettingsStore()
        .toFeedFilterSettings()

private fun SharedPreferences.toContentFilterSettingsStore(): SettingsStore = SettingsStore(
    getBoolean = ::getBoolean,
    putBoolean = { key, value -> edit { putBoolean(key, value) } },
    getString = ::getStringValue,
    putString = { key, value -> edit { putString(key, value) } },
    getStringOrNull = { key -> getString(key, null) },
    putStringSet = { key, value -> edit { putStringSet(key, value) } },
    getStringSet = { key, defaultValue -> getStringSet(key, defaultValue)?.toSet() ?: defaultValue },
    getInt = ::getInt,
    putInt = { key, value -> edit { putInt(key, value) } },
    getFloat = ::getFloat,
    putFloat = { key, value -> edit { putFloat(key, value) } },
    remove = { key -> edit { remove(key) } },
)

private fun SharedPreferences.getStringValue(key: String, defaultValue: String): String =
    getString(key, defaultValue) ?: defaultValue
