package com.github.zly2006.zhihu.ui

import androidx.activity.compose.LocalActivity
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.SlideTransition
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

    TabNavigator(HomeTab) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                NavigationBar(
                    modifier = modifier.height(56.dp + bottomPadding),
                ) {
                    TabNavigationItem(HomeTab)
                    TabNavigationItem(FollowTab)
                    TabNavigationItem(HistoryTab)
                    TabNavigationItem(AccountTab)
                }
            },
        ) { innerPadding ->
            CurrentTab()
        }
    }
}

@Composable
private fun TabNavigationItem(tab: Tab) {
    val tabNavigator = LocalTabNavigator.current
    NavigationBarItem(
        selected = tabNavigator.current == tab,
        onClick = { tabNavigator.current = tab },
        label = {
            Text(
                tab.options.title,
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
            Icon(tab.options.icon!!, contentDescription = tab.options.title)
        },
    )
}

// Define tabs
object HomeTab : Tab {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 0u,
            title = "主页",
            icon = Icons.Filled.Home,
        )

    @Composable
    override fun Content() {
        Navigator(HomeScreenVoyager) { navigator ->
            SlideTransition(navigator)
        }
    }
}

object FollowTab : Tab {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 1u,
            title = "关注",
            icon = Icons.Filled.PersonAddAlt1,
        )

    @Composable
    override fun Content() {
        Navigator(FollowScreenVoyager) { navigator ->
            SlideTransition(navigator)
        }
    }
}

object HistoryTab : Tab {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 2u,
            title = "历史",
            icon = Icons.Filled.History,
        )

    @Composable
    override fun Content() {
        Navigator(HistoryScreenVoyager) { navigator ->
            SlideTransition(navigator)
        }
    }
}

object AccountTab : Tab {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 3u,
            title = "账号",
            icon = Icons.Filled.ManageAccounts,
        )

    @Composable
    override fun Content() {
        Navigator(AccountScreenVoyager) { navigator ->
            SlideTransition(navigator)
        }
    }
}
