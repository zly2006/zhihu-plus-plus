package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.Daily
import com.github.zly2006.zhihu.navigation.Follow
import com.github.zly2006.zhihu.navigation.Home
import com.github.zly2006.zhihu.navigation.HotList
import com.github.zly2006.zhihu.navigation.MainTabs
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.OnlineHistory
import com.github.zly2006.zhihu.navigation.TopLevelDestination
import com.github.zly2006.zhihu.navigation.Video
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel

@Composable
fun DesktopZhihuMain() {
    val navController = rememberNavController()
    val httpClient = rememberZhihuHttpClient()
    var mainTabNavigationTarget by remember { mutableStateOf<TopLevelDestination?>(null) }

    fun navigateToMainTabs() {
        navController.navigate(MainTabs) {
            launchSingleTop = true
            restoreState = true
            popUpTo(MainTabs) {
                saveState = true
            }
        }
    }

    fun navigate(route: NavDestination) {
        when (route) {
            is TopLevelDestination -> {
                mainTabNavigationTarget = route
                navigateToMainTabs()
            }
            is Video -> Unit
            MainTabs -> {
                mainTabNavigationTarget = Home
                navigateToMainTabs()
            }
            else -> navController.navigate(route)
        }
    }

    ZhihuMain(
        navController = navController,
        navigationState = ZhihuMainNavigationState(
            mainTabNavigationTarget = mainTabNavigationTarget,
            navigate = ::navigate,
            setCurrentMainTabOpenFrom = {},
            consumeMainTabNavigationTarget = { destination ->
                if (mainTabNavigationTarget == destination) {
                    mainTabNavigationTarget = null
                }
            },
        ),
        preferenceState = rememberDesktopZhihuMainPreferenceState(),
        isDarkTheme = ThemeManager.isDarkTheme(),
        platformAdapter = ZhihuMainPlatformAdapter(
            article = { article: Article, navEntry ->
                val articleViewModel: ArticleViewModel = viewModel(navEntry) {
                    ArticleViewModel(article, httpClient)
                }
                ArticleScreen(article, articleViewModel)
            },
        ),
    )
}

@Composable
private fun rememberDesktopZhihuMainPreferenceState(): ZhihuMainPreferenceState {
    val settings = rememberSettingsStore()
    val allBottomBarItemKeys = remember {
        listOf(Home.name, Follow.name, HotList.name, Daily.name, OnlineHistory.name, Account.name)
    }
    return rememberZhihuMainPreferenceState {
        val duo3HomeAccount = settings.getBoolean("duo3_home_account", false)
        val selectedKeys = normalizeBottomBarSelection(
            settings.getStringSet(
                BOTTOM_BAR_ITEMS_PREFERENCE_KEY,
                defaultBottomBarSelectionKeys(duo3HomeAccount),
            ),
            duo3HomeAccount,
            enforceMinimumSelection = true,
        )
        ZhihuMainPreferenceSnapshot(
            duo3HomeAccount = duo3HomeAccount,
            duo3NavStyle = settings.getBoolean("duo3_nav_style", false),
            tapToScrollToTopEnabled = settings.getBoolean("bottomBarTapScrollToTop", true),
            autoHideBottomBar = settings.getBoolean("autoHideBottomBar", false),
            selectedBottomBarItemKeys = selectedKeys,
            startDestination = navDestinationFromName(
                resolveValidStartDestinationKey(
                    settings.getString(START_DESTINATION_PREFERENCE_KEY, Home.name),
                    allBottomBarItemKeys.filter { it in selectedKeys },
                ),
            ),
        )
    }
}
