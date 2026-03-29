package com.github.zly2006.zhihu.ui

import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.LocalNavigator
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.Navigator
import com.github.zly2006.zhihu.ui.subscreens.LIST_PANE_DEFAULT_WIDTH_DP_PREFERENCE_KEY
import kotlinx.coroutines.launch

sealed interface ListDetailSelectionState<out T> {
    data object NoSelection : ListDetailSelectionState<Nothing>

    data class ShowSelection<T>(
        val content: T,
    ) : ListDetailSelectionState<T>
}

@Composable
fun ListDetailEmptyPane(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = Color.Gray,
    containerColor: Color = MaterialTheme.colorScheme.background,
) {
    Column(
        modifier = modifier.fillMaxSize().background(containerColor),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(40.dp).padding(bottom = 8.dp),
            tint = iconTint,
        )
        Text(text = text, color = textColor)
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun <T : Parcelable> BaseListDetailScreen(
    backBehavior: BackNavigationBehavior = BackNavigationBehavior.PopUntilContentChange,
    toPaneDestination: (NavDestination) -> T?,
    emptyPane: @Composable () -> Unit,
    listPane: @Composable (Navigator, ListDetailSelectionState<T>) -> Unit,
    detailPane: @Composable (T, ThreePaneScaffoldNavigator<T>) -> Unit,
    onSinglePaneDetailChanged: (Boolean) -> Unit = {},
    paneContainer: @Composable (@Composable () -> Unit) -> Unit = { content -> content() },
) {
    val context = LocalContext.current
    val prefs = androidx.compose.runtime.remember {
        context.getSharedPreferences(PREFERENCE_NAME, android.content.Context.MODE_PRIVATE)
    }
    val listPaneDefaultWidthDp = androidx.compose.runtime.remember {
        prefs.getInt(LIST_PANE_DEFAULT_WIDTH_DP_PREFERENCE_KEY, 320)
    }
    val scaffoldDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()).copy(
        horizontalPartitionSpacerSize = 0.dp,
        defaultPanePreferredWidth = listPaneDefaultWidthDp.dp,
        shouldAutoFocusCurrentDestination = false,
    )
    val paneNavigator = rememberListDetailPaneScaffoldNavigator<T>(
        scaffoldDirective = scaffoldDirective,
    )
    val coroutineScope = rememberCoroutineScope()
    val rootNavigator = LocalNavigator.current
    val detailDestination = paneNavigator.currentDestination?.contentKey
    val selectionState = detailDestination?.let {
        ListDetailSelectionState.ShowSelection(it)
    } ?: ListDetailSelectionState.NoSelection
    val isSinglePane = scaffoldDirective.maxHorizontalPartitions == 1

    LaunchedEffect(isSinglePane, detailDestination) {
        onSinglePaneDetailChanged(isSinglePane && detailDestination != null)
    }

    fun buildLocalNavigator(
        clearDetailHistoryBeforeNavigate: Boolean,
    ) = Navigator(
        onNavigate = { destination ->
            val paneDestination = toPaneDestination(destination)
            if (paneDestination != null) {
                coroutineScope.launch {
                    if (clearDetailHistoryBeforeNavigate) {
                        while (paneNavigator.currentDestination?.contentKey != null) {
                            paneNavigator.navigateBack(BackNavigationBehavior.PopLatest)
                        }
                    }
                    paneNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, paneDestination)
                }
            } else {
                rootNavigator.onNavigate(destination)
            }
        },
        onNavigateBack = {
            coroutineScope.launch {
                if (paneNavigator.canNavigateBack(backBehavior)) {
                    paneNavigator.navigateBack(backBehavior)
                } else {
                    rootNavigator.onNavigateBack()
                }
            }
        },
    )
    val listPaneNavigator = buildLocalNavigator(clearDetailHistoryBeforeNavigate = true)
    val detailPaneNavigator = buildLocalNavigator(clearDetailHistoryBeforeNavigate = false)

    NavigableListDetailPaneScaffold(
        navigator = paneNavigator,
        defaultBackBehavior = backBehavior,
        listPane = {
            AnimatedPane {
                paneContainer {
                    CompositionLocalProvider(
                        LocalNavigator provides listPaneNavigator,
                        LocalCardHorizontalPadding provides adaptiveListCardHorizontalPadding(),
                    ) {
                        listPane(listPaneNavigator, selectionState)
                    }
                }
            }
        },
        detailPane = {
            AnimatedPane {
                paneContainer {
                    if (detailDestination == null) {
                        emptyPane()
                        return@paneContainer
                    }
                    CompositionLocalProvider(LocalNavigator provides detailPaneNavigator) {
                        detailPane(detailDestination, paneNavigator)
                    }
                }
            }
        },
    )
}

// 如果新版本补充了 shouldAutoFocusCurrentDestination 这个参数，就可以删掉这个函数了
private fun PaneScaffoldDirective.copy(
    maxHorizontalPartitions: Int = this.maxHorizontalPartitions,
    horizontalPartitionSpacerSize: Dp = this.horizontalPartitionSpacerSize,
    maxVerticalPartitions: Int = this.maxVerticalPartitions,
    verticalPartitionSpacerSize: Dp = this.verticalPartitionSpacerSize,
    defaultPanePreferredWidth: Dp = this.defaultPanePreferredWidth,
    excludedBounds: List<Rect> = this.excludedBounds,
    defaultPanePreferredHeight: Dp = this.defaultPanePreferredHeight,
    shouldAutoFocusCurrentDestination: Boolean = this.shouldAutoFocusCurrentDestination,
): PaneScaffoldDirective =
    PaneScaffoldDirective(
        maxHorizontalPartitions = maxHorizontalPartitions,
        horizontalPartitionSpacerSize = horizontalPartitionSpacerSize,
        maxVerticalPartitions = maxVerticalPartitions,
        verticalPartitionSpacerSize = verticalPartitionSpacerSize,
        defaultPanePreferredWidth = defaultPanePreferredWidth,
        defaultPanePreferredHeight = defaultPanePreferredHeight,
        excludedBounds = excludedBounds,
        shouldAutoFocusCurrentDestination = shouldAutoFocusCurrentDestination,
    )
