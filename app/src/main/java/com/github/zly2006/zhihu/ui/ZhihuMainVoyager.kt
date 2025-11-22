package com.github.zly2006.zhihu.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.ui.screens.AccountScreenVoyager
import com.github.zly2006.zhihu.ui.screens.FollowScreenVoyager
import com.github.zly2006.zhihu.ui.screens.HistoryScreenVoyager
import com.github.zly2006.zhihu.ui.screens.HomeScreenVoyager

/**
 * Main navigation screen using Voyager for KMP compatibility
 */
@Composable
fun ZhihuMainVoyager(modifier: Modifier = Modifier) {
    val bottomPadding = ScaffoldDefaults.contentWindowInsets.asPaddingValues().calculateBottomPadding()
    val activity = LocalActivity.current as? MainActivity

    Navigator(HomeScreenVoyager) { navigator ->
        activity?.navigator = navigator

        val currentScreen = navigator.lastItem
        val isTopLevel = isTopLevelScreen(currentScreen)

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (isTopLevel) {
                    NavigationBar(
                        modifier = modifier.height(56.dp + bottomPadding),
                    ) {
                        TabItem(HomeScreenVoyager, "主页", Icons.Filled.Home)
                        TabItem(FollowScreenVoyager, "关注", Icons.Filled.PersonAddAlt1)
                        TabItem(HistoryScreenVoyager, "历史", Icons.Filled.History)
                        TabItem(AccountScreenVoyager, "账号", Icons.Filled.ManageAccounts)
                    }
                }
            },
        ) { innerPadding ->
            AnimatedContent(
                targetState = navigator.lastItem,
                transitionSpec = {
                    val previousScreen = navigator.items.getOrNull(navigator.size - 2)
                    val fromIndex = getScreenIndex(previousScreen)
                    val toIndex = getScreenIndex(targetState)

                    // If both are top-level screens, use slide animation
                    if (fromIndex != -1 && toIndex != -1) {
                        val offset = if (toIndex > fromIndex) 1 else -1
                        (
                            slideInHorizontally(
                                animationSpec = tween(300),
                                initialOffsetX = { it * offset },
                            ) + fadeIn(
                                animationSpec = tween(300),
                            )
                        ) togetherWith (
                            slideOutHorizontally(
                                animationSpec = tween(300),
                                targetOffsetX = { it * -offset },
                            ) + fadeOut(
                                animationSpec = tween(300),
                            )
                        )
                    } else {
                        // For non-top-level screens, use default slide from right
                        slideInHorizontally(
                            animationSpec = tween(300),
                            initialOffsetX = { it },
                        ) togetherWith slideOutHorizontally(
                            animationSpec = tween(300),
                            targetOffsetX = { -it / 4 },
                        )
                    }
                },
                modifier = Modifier.padding(innerPadding),
                label = "screen_transition",
            ) { screen ->
                screen.Content()
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TabItem(
    screen: Screen,
    label: String,
    icon: ImageVector,
) {
    val navigator = LocalNavigator.currentOrThrow
    val isSelected = navigator.lastItem::class == screen::class

    NavigationBarItem(
        selected = isSelected,
        onClick = {
            if (!isSelected) {
                // Replace all screens with the selected tab
                navigator.replaceAll(screen)
            }
        },
        label = {
            Text(
                label,
                style = TextStyle(
                    fontSize = 9.sp,
                    color = LocalContentColor.current.copy(alpha = 0.6f),
                ),
            )
        },
        alwaysShowLabel = false,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color(0xff66ccff),
            indicatorColor = Color.Transparent,
        ),
        icon = {
            Icon(icon, contentDescription = label)
        },
    )
}

private fun isTopLevelScreen(screen: Screen): Boolean = screen is HomeScreenVoyager ||
    screen is FollowScreenVoyager ||
    screen is HistoryScreenVoyager ||
    screen is AccountScreenVoyager

private fun getScreenIndex(screen: Screen?): Int = when (screen) {
    is HomeScreenVoyager -> 0
    is FollowScreenVoyager -> 1
    is HistoryScreenVoyager -> 2
    is AccountScreenVoyager -> 3
    else -> -1
}
