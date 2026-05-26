package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.github.zly2006.zhihu.navigation.TopLevelDestination
import com.github.zly2006.zhihu.shared.platform.SettingsStore
import com.github.zly2006.zhihu.shared.platform.UserMessageSink
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.ui.subscreens.SystemUpdateState
import com.github.zly2006.zhihu.ui.subscreens.rememberSystemUpdateRuntime
import kotlinx.coroutines.flow.StateFlow

data class AccountSettingsAccountState(
    val login: Boolean = false,
    val username: String = "",
    val avatarUrl: String? = null,
    val id: String = "",
    val urlToken: String? = null,
)

data class AccountSettingsRuntime(
    val accountState: State<AccountSettingsAccountState>,
    val settings: SettingsStore,
    val userMessages: UserMessageSink,
    val refreshProfile: suspend () -> Unit,
    val requestLogin: () -> Unit,
    val requestQrLoginScan: () -> Unit,
    val logout: () -> Unit,
    val appVersionInfo: () -> String,
    val copyText: (label: String, text: String) -> Unit,
    val openExternalUrl: (String) -> Unit,
    val selectMainTab: (TopLevelDestination) -> Unit,
    val updateState: StateFlow<SystemUpdateState>,
)

@Composable
fun rememberCommonAccountSettingsRuntime(): AccountSettingsRuntime {
    val runtime = rememberAccountSettingsPlatformRuntime()
    val settings = rememberSettingsStore()
    val userMessages = rememberUserMessageSink()
    val updateRuntime = rememberSystemUpdateRuntime()
    return runtime.copy(
        settings = settings,
        userMessages = userMessages,
        updateState = updateRuntime.state,
    )
}

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
