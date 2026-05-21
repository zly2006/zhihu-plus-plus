package com.github.zly2006.zhihu.shared.platform

import androidx.compose.runtime.Composable

data class SettingsStore(
    val getBoolean: (String, Boolean) -> Boolean,
    val putBoolean: (String, Boolean) -> Unit,
    val getString: (String, String) -> String,
    val putString: (String, String) -> Unit,
    val getInt: (String, Int) -> Int,
    val putInt: (String, Int) -> Unit,
)

@Composable
expect fun rememberSettingsStore(): SettingsStore
