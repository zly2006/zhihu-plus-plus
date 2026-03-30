package com.github.zly2006.zhihu.ui

import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.zly2006.zhihu.Account
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.ui.subscreens.AppearanceSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.BlockedFeedHistoryScreen
import com.github.zly2006.zhihu.ui.subscreens.ColorSchemeScreen
import com.github.zly2006.zhihu.ui.subscreens.ContentFilterSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.DeveloperSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.SystemAndUpdateSettingsScreen
import kotlinx.parcelize.Parcelize

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val SettingsListDetailBackBehavior = BackNavigationBehavior.PopUntilContentChange

@Parcelize
internal data class SettingsPaneDestination(
    val type: Type,
    val setting: String = "",
) : Parcelable {
    enum class Type {
        Appearance,
        Recommend,
        SystemAndUpdate,
        Developer,
        DeveloperColorScheme,
        RecommendBlocklist,
        RecommendBlockedHistory,
    }
}

private fun NavDestination.toSettingsPaneDestination(): SettingsPaneDestination? = when (this) {
    is Account.AppearanceSettings -> SettingsPaneDestination(
        type = SettingsPaneDestination.Type.Appearance,
        setting = setting,
    )

    is Account.RecommendSettings -> SettingsPaneDestination(
        type = SettingsPaneDestination.Type.Recommend,
        setting = setting,
    )

    Account.SystemAndUpdateSettings -> SettingsPaneDestination(SettingsPaneDestination.Type.SystemAndUpdate)
    Account.DeveloperSettings -> SettingsPaneDestination(SettingsPaneDestination.Type.Developer)
    Account.DeveloperSettings.ColorScheme -> SettingsPaneDestination(SettingsPaneDestination.Type.DeveloperColorScheme)
    Account.RecommendSettings.Blocklist -> SettingsPaneDestination(SettingsPaneDestination.Type.RecommendBlocklist)
    Account.RecommendSettings.BlockedFeedHistory -> SettingsPaneDestination(SettingsPaneDestination.Type.RecommendBlockedHistory)
    else -> null
}

@Composable
private fun SettingsPaneContainer(
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SettingsListDetailScreen(
    innerPadding: PaddingValues,
    unreadCount: Int = 0,
    onSinglePaneDetailChanged: (Boolean) -> Unit = {},
    onExit: () -> Unit = {},
) {
    BaseListDetailScreen(
        backBehavior = SettingsListDetailBackBehavior,
        toPaneDestination = { it.toSettingsPaneDestination() },
        emptyPane = {
            ListDetailEmptyPane(
                text = "选择一个设置项",
                icon = Icons.Outlined.Settings,
                iconTint = MaterialTheme.colorScheme.primary,
            )
        },
        onSinglePaneDetailChanged = onSinglePaneDetailChanged,
        listPane = { _, selectionState ->
            AccountSettingScreen(
                innerPadding = innerPadding,
                unreadCount = unreadCount,
                selectedSettingType = (selectionState as? ListDetailSelectionState.ShowSelection)?.content?.type?.name,
            )
        },
        detailPane = { paneDestination, _ ->
            when (paneDestination.type) {
                SettingsPaneDestination.Type.Appearance -> {
                    AppearanceSettingsScreen(
                        innerPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
                        setting = paneDestination.setting,
                        onExit = onExit,
                    )
                }

                SettingsPaneDestination.Type.Recommend -> {
                    ContentFilterSettingsScreen(
                        innerPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
                        setting = paneDestination.setting,
                    )
                }

                SettingsPaneDestination.Type.SystemAndUpdate -> {
                    SystemAndUpdateSettingsScreen(
                        innerPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
                    )
                }

                SettingsPaneDestination.Type.Developer -> {
                    DeveloperSettingsScreen(
                        innerPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
                    )
                }

                SettingsPaneDestination.Type.DeveloperColorScheme -> {
                    ColorSchemeScreen(
                        innerPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
                    )
                }

                SettingsPaneDestination.Type.RecommendBlocklist -> {
                    BlocklistSettingsScreen(
                        innerPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
                    )
                }

                SettingsPaneDestination.Type.RecommendBlockedHistory -> {
                    BlockedFeedHistoryScreen()
                }
            }
        },
        paneContainer = { content ->
            SettingsPaneContainer(content)
        },
    )
}
