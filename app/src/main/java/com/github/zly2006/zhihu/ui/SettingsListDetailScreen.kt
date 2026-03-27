package com.github.zly2006.zhihu.ui

import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.Account
import com.github.zly2006.zhihu.LocalNavigator
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.Navigator
import com.github.zly2006.zhihu.ui.subscreens.AppearanceSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.BlockedFeedHistoryScreen
import com.github.zly2006.zhihu.ui.subscreens.ColorSchemeScreen
import com.github.zly2006.zhihu.ui.subscreens.ContentFilterSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.DeveloperSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.SystemAndUpdateSettingsScreen
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val SettingsListDetailBackBehavior = BackNavigationBehavior.PopUntilContentChange

@Parcelize
private data class SettingsPaneDestination(
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
private fun EmptySettingsDetailPane(
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = Color.Gray,
        )
        Text(text = text, color = Color.Gray)
    }
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
    onExit: () -> Unit = {},
) {
    val listPaneDefaultWidthDp = rememberListPaneDefaultWidthDp()
    val defaultDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo())
    val scaffoldDirective = defaultDirective.copy(
        horizontalPartitionSpacerSize = 0.dp,
        defaultPanePreferredWidth = listPaneDefaultWidthDp.dp,
    )
    val paneNavigator = rememberListDetailPaneScaffoldNavigator<SettingsPaneDestination>(
        scaffoldDirective = scaffoldDirective,
    )
    val coroutineScope = rememberCoroutineScope()
    val rootNavigator = LocalNavigator.current
    val detailDestination = paneNavigator.currentDestination?.contentKey

    val localNavigator = Navigator(
        onNavigate = { destination ->
            val paneDestination = destination.toSettingsPaneDestination()
            if (paneDestination != null) {
                coroutineScope.launch {
                    paneNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, paneDestination)
                }
            } else {
                rootNavigator.onNavigate(destination)
            }
        },
        onNavigateBack = {
            coroutineScope.launch {
                if (paneNavigator.canNavigateBack(SettingsListDetailBackBehavior)) {
                    paneNavigator.navigateBack(SettingsListDetailBackBehavior)
                } else {
                    rootNavigator.onNavigateBack()
                }
            }
        },
    )

    NavigableListDetailPaneScaffold(
        navigator = paneNavigator,
        defaultBackBehavior = SettingsListDetailBackBehavior,
        listPane = {
            AnimatedPane {
                SettingsPaneContainer {
                    CompositionLocalProvider(LocalNavigator provides localNavigator) {
                        AccountSettingScreen(
                            innerPadding = innerPadding,
                            unreadCount = unreadCount,
                        )
                    }
                }
            }
        },
        detailPane = {
            AnimatedPane {
                SettingsPaneContainer {
                    val destination = detailDestination
                    if (destination == null) {
                        EmptySettingsDetailPane(text = "选择一个设置项")
                        return@SettingsPaneContainer
                    }
                    CompositionLocalProvider(LocalNavigator provides localNavigator) {
                        when (destination.type) {
                            SettingsPaneDestination.Type.Appearance -> {
                                AppearanceSettingsScreen(
                                    innerPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
                                    setting = destination.setting,
                                    onExit = onExit,
                                )
                            }

                            SettingsPaneDestination.Type.Recommend -> {
                                ContentFilterSettingsScreen(
                                    innerPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
                                    setting = destination.setting,
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
                    }
                }
            }
        },
    )
}
