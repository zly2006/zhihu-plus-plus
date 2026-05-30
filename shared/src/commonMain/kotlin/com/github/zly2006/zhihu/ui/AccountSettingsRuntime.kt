package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.github.zly2006.zhihu.navigation.TopLevelDestination
import com.github.zly2006.zhihu.shared.platform.SettingsStore

data class AccountSettingsAccountState(
    val login: Boolean = false,
    val username: String = "",
    val avatarUrl: String? = null,
    val id: String = "",
    val urlToken: String? = null,
)

data class AccountSettingsRuntime(
    val accountState: State<AccountSettingsAccountState>,
    val refreshProfile: suspend () -> Unit,
    val requestLogin: () -> Unit,
    val requestQrLoginScan: () -> Unit,
    val logout: () -> Unit,
    val appVersionInfo: () -> String,
    val openExternalUrl: (String) -> Unit,
    val selectMainTab: (TopLevelDestination) -> Unit,
)

@Composable
expect fun rememberAccountSettingsPlatformRuntime(): AccountSettingsRuntime

fun noopSettingsStore(): SettingsStore = SettingsStore(
    getBoolean = { _, defaultValue -> defaultValue },
    putBoolean = { _, _ -> },
    getString = { _, defaultValue -> defaultValue },
    putString = { _, _ -> },
    getStringOrNull = { _ -> null },
    putStringSet = { _, _ -> },
    getStringSet = { _, defaultValue -> defaultValue },
    getInt = { _, defaultValue -> defaultValue },
    putInt = { _, _ -> },
    getLong = { _, defaultValue -> defaultValue },
    putLong = { _, _ -> },
    getFloat = { _, defaultValue -> defaultValue },
    putFloat = { _, _ -> },
    remove = { _ -> },
)
