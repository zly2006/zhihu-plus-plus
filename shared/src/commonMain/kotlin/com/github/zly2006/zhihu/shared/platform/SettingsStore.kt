package com.github.zly2006.zhihu.shared.platform

import androidx.compose.runtime.Composable

data class SettingsStore(
    val getBoolean: (String, Boolean) -> Boolean,
    val putBoolean: (String, Boolean) -> Unit,
    val getString: (String, String) -> String,
    val putString: (String, String) -> Unit,
    val getStringOrNull: (String) -> String?,
    val putStringSet: (String, Set<String>) -> Unit,
    val getStringSet: (String, Set<String>) -> Set<String>,
    val getInt: (String, Int) -> Int,
    val putInt: (String, Int) -> Unit,
    val getLong: (String, Long) -> Long,
    val putLong: (String, Long) -> Unit,
    val getFloat: (String, Float) -> Float,
    val putFloat: (String, Float) -> Unit,
    val remove: (String) -> Unit,
    val observeKeyChanges: (onChanged: (String) -> Unit) -> () -> Unit = { {} },
)

@Composable
expect fun rememberSettingsStore(): SettingsStore
