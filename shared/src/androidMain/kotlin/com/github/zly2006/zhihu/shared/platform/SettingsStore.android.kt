package com.github.zly2006.zhihu.shared.platform

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME

@Composable
actual fun rememberSettingsStore(): SettingsStore {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

        SettingsStore(
            getBoolean = { key, defaultValue -> preferences.getBoolean(key, defaultValue) },
            putBoolean = { key, value -> preferences.edit { putBoolean(key, value) } },
            getString = { key, defaultValue -> preferences.getString(key, defaultValue) ?: defaultValue },
            putString = { key, value -> preferences.edit { putString(key, value) } },
            getStringOrNull = { key -> preferences.getString(key, null) },
            putStringSet = { key, value -> preferences.edit { putStringSet(key, value) } },
            getStringSet = { key, defaultValue -> preferences.getStringSet(key, defaultValue)?.toSet() ?: defaultValue },
            getInt = { key, defaultValue -> preferences.getInt(key, defaultValue) },
            putInt = { key, value -> preferences.edit { putInt(key, value) } },
            getFloat = { key, defaultValue -> preferences.getFloat(key, defaultValue) },
            putFloat = { key, value -> preferences.edit { putFloat(key, value) } },
            remove = { key -> preferences.edit { remove(key) } },
        )
    }
}
