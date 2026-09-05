package com.github.zly2006.zhihu.harmonyprobe

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.ArticleTypeNavType
import com.github.zly2006.zhihu.navigation.Home
import com.github.zly2006.zhihu.navigation.Daily
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.MainTabs
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Navigator
import com.github.zly2006.zhihu.navigation.Search
import com.github.zly2006.zhihu.navigation.TopLevelDestination
import kotlinx.coroutines.launch
import kotlin.reflect.typeOf

/**
 * P1 主壳使用的 tab。结构对齐真实 ZhihuMain：TopLevelDestination 只是 tab 选择目标，
 * NavHost 的唯一顶层 route 是共享的 [MainTabs]。
 */
private data class P1Tab(
    val destination: TopLevelDestination,
    val label: String,
    val icon: ImageVector,
)

private val P1_TABS = listOf(
    P1Tab(Home, "主页", P1IconHome),
    P1Tab(Daily, "日报", P1IconNewspaper),
    P1Tab(Account, "账号", P1IconManageAccounts),
)

/**
 * P1 共享主壳（最小裁剪入口）。
 *
 * 复用共享工程的真实部件：NavDestination typed routes（含 ArticleTypeNavType 自定义 NavType）、
 * LocalNavigator/Navigator、ThemeManager。页面内容为内存数据，平台能力只保留返回键与深色模式推送。
 * 与真实 ZhihuMain 的差异：页面实现与 ZhihuMainPreferenceState 尚未纳入探针（依赖闭包太大），
 * 这些属于后续阶段。
 */
@Composable
fun P1Shell(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { P1_TABS.size })
    val navEntry by navController.currentBackStackEntryAsState()
    val showBottomBar = navEntry.hasRoute(MainTabs::class)

    DisposableEffect(navController) {
        P1ShellState.backHandler = { navController.popBackStack() }
        onDispose { P1ShellState.backHandler = null }
    }

    fun selectTab(destination: TopLevelDestination) {
        val index = P1_TABS.indexOfFirst { it.destination.name == destination.name }
        if (index >= 0) {
            coroutineScope.launch { pagerState.animateScrollToPage(index) }
        }
    }

    fun navigate(destination: NavDestination) {
        navController.navigate(destination)
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    P1_TABS.forEach { tab ->
                        val selected = P1_TABS.getOrNull(pagerState.currentPage)?.destination == tab.destination
                        NavigationBarItem(
                            selected = selected,
                            onClick = { selectTab(tab.destination) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            modifier = Modifier.testTag("nav_tab_${tab.destination.name.lowercase()}"),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        CompositionLocalProvider(
            LocalNavigator provides Navigator(
                onNavigate = ::navigate,
                onNavigateBack = navController::popBackStack,
            ),
        ) {
            NavHost(
                navController,
                modifier = Modifier.fillMaxSize(),
                startDestination = MainTabs,
                enterTransition = {
                    slideInHorizontally(tween(300)) { it }
                },
                exitTransition = {
                    ExitTransition.None
                },
                popEnterTransition = {
                    EnterTransition.None
                },
                popExitTransition = {
                    slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300))
                },
            ) {
                composable<MainTabs> {
                    P1MainTabsPager(pagerState, innerPadding)
                }
                composable<Account.AppearanceSettings> { entry ->
                    val args = entry.toRoute<Account.AppearanceSettings>()
                    P1SettingsPage(title = "外观设置", subtitle = args.setting, innerPadding = innerPadding)
                }
                composable<Account.RecommendSettings> { entry ->
                    val args = entry.toRoute<Account.RecommendSettings>()
                    P1SettingsPage(title = "推荐与屏蔽设置", subtitle = args.setting, innerPadding = innerPadding)
                }
                composable<Account.SystemAndUpdateSettings> {
                    P1SettingsPage(
                        title = "系统与更新",
                        subtitle = "鸿蒙首版未接入更新通道，此页面用于验证 typed route",
                        innerPadding = innerPadding,
                    )
                }
                composable<Account.OpenSourceLicenses> {
                    P1LicensePage(innerPadding)
                }
                composable<Account.DeveloperSettings> {
                    P1DeveloperSettingsPage(innerPadding)
                }
                composable<Account.DeveloperSettings.ColorScheme> {
                    P1ColorSchemePage(innerPadding)
                }
                composable<Article>(
                    typeMap = mapOf(typeOf<ArticleType>() to ArticleTypeNavType),
                ) { entry ->
                    val article = entry.toRoute<Article>()
                    P1ArticlePage(article, innerPadding)
                }
                composable<Search> { entry ->
                    val search = entry.toRoute<Search>()
                    P1SearchPage(search, innerPadding)
                }
            }
        }
    }
}

@Composable
private fun P1MainTabsPager(pagerState: PagerState, innerPadding: PaddingValues) {
    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
        when (P1_TABS.getOrNull(pageIndex)?.destination) {
            Home -> P1HomePage(innerPadding)
            Daily -> P1DailyPage(innerPadding)
            Account -> P1AccountPage(innerPadding)
            else -> {}
        }
    }
}

private fun NavBackStackEntry?.hasRoute(cls: kotlin.reflect.KClass<out NavDestination>): Boolean {
    val dest = this?.destination ?: return false
    return dest.hierarchy.any { it.hasRoute(cls) }
}
