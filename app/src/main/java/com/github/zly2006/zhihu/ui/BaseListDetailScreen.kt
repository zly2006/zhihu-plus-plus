package com.github.zly2006.zhihu.ui

import android.os.Parcelable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.LocalNavigator
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.Navigator
import kotlinx.coroutines.launch

@Composable
fun ListDetailEmptyPane(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = Color.Gray,
) {
    Column(
        modifier = modifier.fillMaxSize(),
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
    listPane: @Composable (Navigator) -> Unit,
    detailPane: @Composable (T, Navigator) -> Unit,
    paneContainer: @Composable (@Composable () -> Unit) -> Unit = { content -> content() },
) {
    val listPaneDefaultWidthDp = rememberListPaneDefaultWidthDp()
    val defaultDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo())
    val scaffoldDirective = defaultDirective.copy(
        horizontalPartitionSpacerSize = 0.dp,
        defaultPanePreferredWidth = listPaneDefaultWidthDp.dp,
    )
    val paneNavigator = rememberListDetailPaneScaffoldNavigator<T>(
        scaffoldDirective = scaffoldDirective,
    )
    val coroutineScope = rememberCoroutineScope()
    val rootNavigator = LocalNavigator.current
    val detailDestination = paneNavigator.currentDestination?.contentKey

    val localNavigator = Navigator(
        onNavigate = { destination ->
            val paneDestination = toPaneDestination(destination)
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
                if (paneNavigator.canNavigateBack(backBehavior)) {
                    paneNavigator.navigateBack(backBehavior)
                } else {
                    rootNavigator.onNavigateBack()
                }
            }
        },
    )

    NavigableListDetailPaneScaffold(
        navigator = paneNavigator,
        defaultBackBehavior = backBehavior,
        listPane = {
            AnimatedPane {
                paneContainer {
                    CompositionLocalProvider(LocalNavigator provides localNavigator) {
                        listPane(localNavigator)
                    }
                }
            }
        },
        detailPane = {
            AnimatedPane {
                paneContainer {
                    val destination = detailDestination
                    if (destination == null) {
                        emptyPane()
                        return@paneContainer
                    }
                    CompositionLocalProvider(LocalNavigator provides localNavigator) {
                        detailPane(destination, localNavigator)
                    }
                }
            }
        },
    )
}
