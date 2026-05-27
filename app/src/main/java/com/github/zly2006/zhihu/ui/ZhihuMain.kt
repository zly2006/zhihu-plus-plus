/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.ui

import android.annotation.SuppressLint
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.CollectionContent
import com.github.zly2006.zhihu.navigation.Collections
import com.github.zly2006.zhihu.navigation.Daily
import com.github.zly2006.zhihu.navigation.Follow
import com.github.zly2006.zhihu.navigation.History
import com.github.zly2006.zhihu.navigation.Home
import com.github.zly2006.zhihu.navigation.HotList
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.MainTabs
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Navigator
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.OnlineHistory
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.Search
import com.github.zly2006.zhihu.navigation.SentenceSimilarityTest
import com.github.zly2006.zhihu.navigation.TopLevelDestination
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.theme.ThemeStyle
import com.github.zly2006.zhihu.theme.ZhihuTheme
import com.github.zly2006.zhihu.ui.miuix.MiuixAppearanceSettingsScreen
import com.github.zly2006.zhihu.ui.miuix.MiuixAccountSettingScreen
import com.github.zly2006.zhihu.ui.miuix.MiuixBlocklistSettingsScreen
import com.github.zly2006.zhihu.ui.miuix.MiuixFollowScreen
import com.github.zly2006.zhihu.ui.miuix.MiuixFollowTopLevelPage
import com.github.zly2006.zhihu.ui.subscreens.AppearanceSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.BOTTOM_BAR_ITEMS_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.BlockedFeedHistoryScreen
import com.github.zly2006.zhihu.ui.subscreens.ColorSchemeScreen
import com.github.zly2006.zhihu.ui.subscreens.ContentFilterSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.DeveloperSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.OpenSourceLicensesScreen
import com.github.zly2006.zhihu.ui.subscreens.START_DESTINATION_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.SystemAndUpdateSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.defaultBottomBarSelectionKeys
import com.github.zly2006.zhihu.ui.subscreens.navDestinationFromName
import com.github.zly2006.zhihu.ui.subscreens.normalizeBottomBarSelection
import com.github.zly2006.zhihu.ui.subscreens.resolveValidStartDestinationKey
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import com.github.zly2006.zhihu.viewmodel.filter.ContentOpenFrom
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
import com.github.zly2006.zhihu.ui.NavHost as MyNavHost

const val SURVEY_URL = "https://v.wjx.cn/vm/Ppfw2R4.aspx#"

private sealed class MainTabPage(
    val bottomDestination: TopLevelDestination,
    val key: String,
) {
    data object HomePage : MainTabPage(Home, "home")

    data object FollowRecommendPage : MainTabPage(Follow, "follow_recommend")

    data object FollowDynamicPage : MainTabPage(Follow, "follow_dynamic")

    data object HotListPage : MainTabPage(HotList, "hotlist")

    data object DailyPage : MainTabPage(Daily, "daily")

    data object OnlineHistoryPage : MainTabPage(OnlineHistory, "online_history")

    data object AccountPage : MainTabPage(Account, "account")
}

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("RestrictedApi")
@Composable
fun ZhihuMain(modifier: Modifier = Modifier, navController: NavHostController) {
    val bottomPadding = ScaffoldDefaults.contentWindowInsets.asPaddingValues().calculateBottomPadding()
    val activity = LocalActivity.current as MainActivity
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(PREFERENCE_NAME, android.content.Context.MODE_PRIVATE) }

    // 底部导航栏功能
    var duo3HomeAccount by remember { mutableStateOf(preferences.getBoolean("duo3_home_account", false)) }
    var duo3NavStyle by remember { mutableStateOf(preferences.getBoolean("duo3_nav_style", false)) }
    var tapToScrollToTopEnabled by remember { mutableStateOf(preferences.getBoolean("bottomBarTapScrollToTop", true)) }
    var autoHideBottomBar by remember { mutableStateOf(preferences.getBoolean("autoHideBottomBar", false)) }
    val allBottomBarItemKeys = remember {
        listOf(Home.name, Follow.name, HotList.name, Daily.name, OnlineHistory.name, Account.name)
    }

    fun computeSelectedKeys(isDuo3HomeAccount: Boolean) = normalizeBottomBarSelection(
        preferences
            .getStringSet(BOTTOM_BAR_ITEMS_PREFERENCE_KEY, defaultBottomBarSelectionKeys(isDuo3HomeAccount))
            ?.toSet() ?: defaultBottomBarSelectionKeys(isDuo3HomeAccount),
        isDuo3HomeAccount,
        enforceMinimumSelection = true,
    )

    fun computeStartDestination(selectedKeys: Set<String>) = navDestinationFromName(
        resolveValidStartDestinationKey(
            preferences.getString(START_DESTINATION_PREFERENCE_KEY, Home.name),
            allBottomBarItemKeys.filter { it in selectedKeys },
        ),
    )

    var selectedBottomBarItemKeys by remember { mutableStateOf(computeSelectedKeys(duo3HomeAccount)) }
    var startDestination by remember { mutableStateOf(computeStartDestination(selectedBottomBarItemKeys)) }

    val reloadBottomBarPreferences = {
        val updatedDuo3HomeAccount = preferences.getBoolean("duo3_home_account", false)
        val updatedSelectedBottomBarItemKeys = computeSelectedKeys(updatedDuo3HomeAccount)
        duo3HomeAccount = updatedDuo3HomeAccount
        duo3NavStyle = preferences.getBoolean("duo3_nav_style", false)
        tapToScrollToTopEnabled = preferences.getBoolean("bottomBarTapScrollToTop", true)
        autoHideBottomBar = preferences.getBoolean("autoHideBottomBar", false)
        selectedBottomBarItemKeys = updatedSelectedBottomBarItemKeys
        startDestination = computeStartDestination(updatedSelectedBottomBarItemKeys)
    }

    val navEntry by navController.currentBackStackEntryAsState()

    var scrollToTopTrigger by remember { mutableIntStateOf(0) }
    // 滚动时自动隐藏底部导航栏
    var isBottomBarVisible by remember { mutableStateOf(true) }
    val bottomBarScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                when {
                    available.y < -3f -> isBottomBarVisible = false
                    available.y > 3f -> isBottomBarVisible = true
                }
                return Offset.Zero
            }
        }
    }

    val allBottomBarItems = listOf(
        Triple(Home, "主页", Icons.Filled.Home),
        Triple(Follow, "关注", if (duo3NavStyle) Icons.Filled.Group else Icons.Filled.PersonAddAlt1),
        Triple(HotList, "热榜", Icons.Filled.Whatshot),
        Triple(Daily, "日报", Icons.Filled.Newspaper),
        Triple(OnlineHistory, "历史", Icons.Filled.History),
        Triple(Account, "账号", Icons.Filled.ManageAccounts),
    )
    val bottomBarItems = allBottomBarItems.filter { it.first.name in selectedBottomBarItemKeys }

    val mainTabPages = remember(bottomBarItems) {
        bottomBarItems.flatMap { item ->
            when (item.first) {
                Home -> listOf(MainTabPage.HomePage)
                Follow -> listOf(MainTabPage.FollowRecommendPage, MainTabPage.FollowDynamicPage)
                HotList -> listOf(MainTabPage.HotListPage)
                Daily -> listOf(MainTabPage.DailyPage)
                OnlineHistory -> listOf(MainTabPage.OnlineHistoryPage)
                Account -> listOf(MainTabPage.AccountPage)
                else -> emptyList()
            }
        }
    }

    fun pageIndexForDestination(destination: TopLevelDestination): Int = mainTabPages
        .indexOfFirst {
            it.bottomDestination::class == destination::class
        }.takeIf { it >= 0 } ?: mainTabPages
        .indexOfFirst {
            it.bottomDestination::class == startDestination::class
        }.takeIf { it >= 0 } ?: 0

    var lastFollowPageKey by rememberSaveable { mutableStateOf(MainTabPage.FollowRecommendPage.key) }
    val mainPagerState = rememberPagerState(
        initialPage = pageIndexForDestination(startDestination),
        pageCount = { mainTabPages.size },
    )
    val coroutineScope = rememberCoroutineScope()

    fun currentMainTabPage(): MainTabPage? = mainTabPages.getOrNull(mainPagerState.currentPage)
    var currentMainTabDestination by remember { mutableStateOf(startDestination) }

    fun pageIndexForBottomDestination(destination: TopLevelDestination): Int {
        if (destination == Follow) {
            val rememberedFollowPage = mainTabPages.indexOfFirst { it.key == lastFollowPageKey }
            if (rememberedFollowPage >= 0) return rememberedFollowPage
        }
        return pageIndexForDestination(destination)
    }

    fun navigateTopLevel(destination: TopLevelDestination) {
        val targetPage = pageIndexForBottomDestination(destination)
        coroutineScope.launch {
            mainPagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(mainPagerState.currentPage, mainTabPages) {
        when (val page = currentMainTabPage()) {
            MainTabPage.FollowRecommendPage, MainTabPage.FollowDynamicPage -> lastFollowPageKey = page.key
            else -> {}
        }
        currentMainTabPage()?.bottomDestination?.let { destination ->
            currentMainTabDestination = destination
            activity.setCurrentMainTabOpenFrom(destination.openFrom)
        }
    }

    val mainTabNavigationTarget = activity.mainTabNavigationTarget
    LaunchedEffect(mainTabNavigationTarget, mainTabPages) {
        mainTabNavigationTarget?.let { destination ->
            // MainActivity maps legacy top-level route requests onto MainTabs. Consume that request
            // here so callers such as deeplinks can still select Home/Follow/etc. without pushing
            // those old routes onto the back stack.
            mainPagerState.scrollToPage(pageIndexForBottomDestination(destination))
            activity.consumeMainTabNavigationTarget(destination)
        }
    }

    LaunchedEffect(mainTabPages) {
        if (mainTabPages.isNotEmpty()) {
            val currentDestinationStillVisible = mainTabPages.any {
                it.bottomDestination::class == currentMainTabDestination::class
            }
            val targetDestination = if (currentDestinationStillVisible) {
                currentMainTabDestination
            } else {
                startDestination
            }
            val targetPage = pageIndexForDestination(targetDestination)
            if (mainPagerState.currentPage != targetPage || mainPagerState.currentPage !in mainTabPages.indices) {
                mainPagerState.scrollToPage(targetPage)
            }
        }
    }

    Scaffold(
        modifier = modifier
            .nestedScroll(bottomBarScrollConnection)
            .semantics { testTagsAsResourceId = true },
        bottomBar = {
            if (navEntry != null) {
                // 页面切换时重置底部导航栏可见状态
                LaunchedEffect(navEntry) { isBottomBarVisible = true }
                val currentBottomDestination = mainTabPages
                    .getOrNull(mainPagerState.targetPage)
                    ?.bottomDestination
                AnimatedVisibility(
                    visible = (!autoHideBottomBar || isBottomBarVisible) && isTopLevelDest(navEntry),
                    enter = slideInVertically(tween(200)) { it },
                    exit = slideOutVertically(tween(200)) { it },
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.height(
                            (if (duo3NavStyle) 64.dp else 56.dp) + bottomPadding,
                        ),
                    ) {
                        @Composable
                        fun Item(
                            destination: TopLevelDestination,
                            label: String,
                            icon: ImageVector,
                        ) {
                            val tag = "nav_tab_${destination.name.lowercase()}"
                            NavigationBarItem(
                                currentBottomDestination?.let { it::class == destination::class } == true,
                                onClick = {
                                    if (currentBottomDestination?.let { it::class == destination::class } != true) {
                                        navigateTopLevel(destination)
                                    } else if (tapToScrollToTopEnabled) {
                                        scrollToTopTrigger++
                                    }
                                },
                                label = {
                                    if (duo3NavStyle) {
                                        Text(label)
                                    } else {
                                        Text(
                                            label,
                                            style = TextStyle(
                                                fontSize = 9.sp,
                                                color = LocalContentColor.current.copy(alpha = 0.6f),
                                            ),
                                        )
                                    }
                                },
                                alwaysShowLabel = duo3NavStyle,
                                colors = if (duo3NavStyle) {
                                    if (!ThemeManager.isDarkTheme()) {
                                        NavigationBarItemDefaults.colors().copy(
                                            selectedIndicatorColor =
                                                MaterialTheme.colorScheme.secondaryContainer
                                                    .copy(alpha = 0.92f)
                                                    .compositeOver(MaterialTheme.colorScheme.secondary),
                                        )
                                    } else {
                                        NavigationBarItemDefaults.colors()
                                    }
                                } else {
                                    NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xff66ccff),
                                        indicatorColor = Color.Transparent,
                                    )
                                },
                                icon = {
                                    Icon(icon, contentDescription = label)
                                },
                                modifier = (if (duo3NavStyle) Modifier.padding(top = 4.dp) else Modifier).testTag(tag),
                            )
                        }

                        bottomBarItems.forEach { item ->
                            Item(item.first, item.second, item.third)
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        CompositionLocalProvider(
            LocalNavigator provides Navigator(
                onNavigate = { destination ->
                    activity.navigate(destination)
                },
                onNavigateBack = navController::popBackStack,
            ),
        ) {
            MyNavHost(
                navController,
                modifier = Modifier,
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
                    MainTabsPager(
                        pagerState = mainPagerState,
                        pages = mainTabPages,
                        scrollToTopTrigger = scrollToTopTrigger,
                        innerPadding = innerPadding,
                        onFollowTabSelected = { followTabIndex ->
                            val page = if (followTabIndex == 0) {
                                MainTabPage.FollowRecommendPage
                            } else {
                                MainTabPage.FollowDynamicPage
                            }
                            val index = mainTabPages.indexOfFirst { it.key == page.key }
                            if (index >= 0) {
                                coroutineScope.launch {
                                    mainPagerState.animateScrollToPage(index)
                                }
                            }
                        },
                    )
                }
                composable<Question> { navEntry ->
                    val question: Question = navEntry.toRoute()
                    QuestionScreen(question)
                }
                composable<Article>(
                    enterTransition = {
                        val sharedData = try {
                            ViewModelProvider(activity)[ArticleViewModel.ArticlesSharedData::class.java]
                        } catch (_: Exception) {
                            null
                        }
                        when (sharedData?.answerTransitionDirection) {
                            ArticleViewModel.AnswerTransitionDirection.VERTICAL_NEXT ->
                                slideInVertically(tween(300)) { it } + fadeIn(tween(300))
                            ArticleViewModel.AnswerTransitionDirection.VERTICAL_PREVIOUS ->
                                slideInVertically(tween(300)) { -it } + fadeIn(tween(300))
                            ArticleViewModel.AnswerTransitionDirection.HORIZONTAL_NEXT ->
                                slideInHorizontally(tween(300)) { it } + fadeIn(tween(300))
                            ArticleViewModel.AnswerTransitionDirection.HORIZONTAL_PREVIOUS ->
                                slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300))
                            else -> slideInHorizontally(tween(300)) { it }
                        }
                    },
                    exitTransition = {
                        val sharedData = try {
                            (activity as? androidx.activity.ComponentActivity)
                                ?.let { ViewModelProvider(it)[ArticleViewModel.ArticlesSharedData::class.java] }
                        } catch (_: Exception) {
                            null
                        }
                        when (sharedData?.answerTransitionDirection) {
                            ArticleViewModel.AnswerTransitionDirection.VERTICAL_NEXT ->
                                slideOutVertically(tween(300)) { -it } + fadeOut(tween(300))
                            ArticleViewModel.AnswerTransitionDirection.VERTICAL_PREVIOUS ->
                                slideOutVertically(tween(300)) { it } + fadeOut(tween(300))
                            ArticleViewModel.AnswerTransitionDirection.HORIZONTAL_NEXT ->
                                slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300))
                            ArticleViewModel.AnswerTransitionDirection.HORIZONTAL_PREVIOUS ->
                                slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300))
                            else -> ExitTransition.None
                        }
                    },
                ) { navEntry ->
                    val article: Article = navEntry.toRoute()
                    val viewModel: ArticleViewModel = viewModel(navEntry) {
                        ArticleViewModel(article, activity.httpClient, navEntry)
                    }
                    ArticleScreen(article, viewModel)
                }
                composable<HotList> {
                    HotListScreen(innerPadding)
                }
                composable<Follow> {
                    if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                        MiuixFollowScreen(
                            scrollToTopTrigger = scrollToTopTrigger,
                            innerPadding = innerPadding,
                        )
                    } else {
                        FollowScreen(
                            scrollToTopTrigger = scrollToTopTrigger,
                            innerPadding = innerPadding,
                        )
                    }
                }
                composable<Daily> {
                    DailyScreen()
                }
                composable<History> {
                    LegacyLocalHistoryScreen(innerPadding)
                }
                composable<OnlineHistory> {
                    OnlineHistoryScreen()
                }
                composable<Account> {
                    val style = ThemeManager.getThemeStyle()
                    if (style == ThemeStyle.Miuix) {
                        MiuixAccountSettingScreen(innerPadding)
                    } else {
                        AccountSettingScreen(innerPadding)
                    }
                }
                composable<Search> { navEntry ->
                    val search: Search = navEntry.toRoute()
                    SearchScreen(search)
                }
                composable<Collections> {
                    val data: Collections = it.toRoute()
                    CollectionScreen(data.userToken)
                }
                composable<CollectionContent> {
                    val content: CollectionContent = it.toRoute()
                    CollectionContentScreen(content.collectionId)
                }
                composable<Person> {
                    val person: Person = it.toRoute()
                    PeopleScreen(person)
                }
                composable<Pin> {
                    val pin = it.toRoute<Pin>()
                    PinScreen(pin)
                }
                composable<Account.RecommendSettings.Blocklist> {
                    if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                        MiuixBlocklistSettingsScreen()
                    } else {
                        BlocklistSettingsScreen()
                    }
                }
                composable<Account.RecommendSettings.BlockedFeedHistory> {
                    BlockedFeedHistoryScreen()
                }
                composable<Notification> {
                    NotificationScreen()
                }
                composable<Notification.NotificationSettings> {
                    NotificationSettingsScreen()
                }
                composable<SentenceSimilarityTest> {
                    SentenceSimilarityTestScreen()
                }
                composable<Account.AppearanceSettings> {
                    val args = it.toRoute<Account.AppearanceSettings>()
                    val useMiuix = ThemeManager.getThemeStyle() == ThemeStyle.Miuix
                    if (useMiuix) {
                        MiuixAppearanceSettingsScreen(
                            setting = args.setting,
                            onExit = reloadBottomBarPreferences,
                        )
                    } else {
                        AppearanceSettingsScreen(
                            setting = args.setting,
                            onExit = reloadBottomBarPreferences,
                        )
                    }
                }
                composable<Account.RecommendSettings> {
                    val args = it.toRoute<Account.RecommendSettings>()
                    ContentFilterSettingsScreen(args.setting)
                }
                composable<Account.SystemAndUpdateSettings> {
                    SystemAndUpdateSettingsScreen()
                }
                composable<Account.OpenSourceLicenses> {
                    OpenSourceLicensesScreen()
                }
                composable<Account.DeveloperSettings> {
                    DeveloperSettingsScreen()
                }
                composable<Account.DeveloperSettings.ColorScheme> {
                    ColorSchemeScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainTabsPager(
    pagerState: PagerState,
    pages: List<MainTabPage>,
    scrollToTopTrigger: Int,
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    onFollowTabSelected: (Int) -> Unit,
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
    ) { pageIndex ->
        val page = pages.getOrNull(pageIndex) ?: return@HorizontalPager
        when (page) {
            MainTabPage.HomePage -> HomeScreen(
                scrollToTopTrigger = scrollToTopTrigger,
                innerPadding = innerPadding,
            )
            MainTabPage.FollowRecommendPage -> if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                MiuixFollowTopLevelPage(
                    selectedTabIndex = 0, onTabSelected = onFollowTabSelected,
                    scrollToTopTrigger = scrollToTopTrigger, innerPadding = innerPadding,
                    isActive = pagerState.currentPage == pageIndex,
                )
            } else {
                FollowTopLevelPage(
                    selectedTabIndex = 0, onTabSelected = onFollowTabSelected,
                    scrollToTopTrigger = scrollToTopTrigger, innerPadding = innerPadding,
                    isActive = pagerState.currentPage == pageIndex,
                )
            }
            MainTabPage.FollowDynamicPage -> if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                MiuixFollowTopLevelPage(
                    selectedTabIndex = 1, onTabSelected = onFollowTabSelected,
                    scrollToTopTrigger = scrollToTopTrigger, innerPadding = innerPadding,
                    isActive = pagerState.currentPage == pageIndex,
                )
            } else {
                FollowTopLevelPage(
                    selectedTabIndex = 1, onTabSelected = onFollowTabSelected,
                    scrollToTopTrigger = scrollToTopTrigger, innerPadding = innerPadding,
                    isActive = pagerState.currentPage == pageIndex,
                )
            }
            MainTabPage.HotListPage -> HotListScreen(innerPadding)
            MainTabPage.DailyPage -> DailyScreen()
            MainTabPage.OnlineHistoryPage -> OnlineHistoryScreen()
            MainTabPage.AccountPage -> AccountSettingScreen(innerPadding)
        }
    }
}

private fun isTopLevelDest(navEntry: NavBackStackEntry?): Boolean = navEntry.hasRoute(MainTabs::class)

private val TopLevelDestination.openFrom: String?
    get() = when (this) {
        Home -> ContentOpenFrom.HOME_FEED
        OnlineHistory -> ContentOpenFrom.HISTORY
        else -> null
    }

internal fun NavBackStackEntry?.hasRoute(cls: KClass<out NavDestination>): Boolean {
    val dest = this?.destination ?: return false
    return dest.hierarchy.any { it.hasRoute(cls) }
}

@Preview(showBackground = true)
@Composable
private fun GreetingPreview() {
    val navController = rememberNavController()
    ZhihuTheme {
        ZhihuMain(navController = navController)
    }
}
