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
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.ExperimentalMaterial3ComponentOverrideApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalNavigationBarOverride
import androidx.compose.material3.LocalNavigationRailOverride
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarOverride
import androidx.compose.material3.NavigationBarOverrideScope
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.NavigationRailOverride
import androidx.compose.material3.NavigationRailOverrideScope
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldState
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldValue
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.isTraversalGroup
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
import com.github.zly2006.zhihu.theme.ZhihuTheme
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
import kotlin.reflect.KClass
import com.github.zly2006.zhihu.ui.NavHost as MyNavHost

const val SURVEY_URL = "https://v.wjx.cn/vm/Ppfw2R4.aspx#"

@OptIn(ExperimentalMaterial3ComponentOverrideApi::class)
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
    val navSuiteType = NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfo())
    val navigationSuiteState = rememberSaveable(saver = MyNavigationSuiteScaffoldStateImpl.saver()) {
        MyNavigationSuiteScaffoldStateImpl(initialValue = NavigationSuiteScaffoldValue.Visible)
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

    // 获取页面索引的函数
    fun getPageIndex(route: androidx.navigation.NavDestination): Int = when {
        route.hasRoute<Home>() -> 0
        route.hasRoute<Follow>() -> 1
        route.hasRoute<HotList>() -> 2
        route.hasRoute<Daily>() -> 3
        route.hasRoute<OnlineHistory>() -> 4
        route.hasRoute<Account>() -> 5
        else -> -1
    }

    // 通用动画创建函数
    @Suppress("KotlinConstantConditions")
    fun createSlideAnimation(
        isEnter: Boolean,
        isPop: Boolean,
        fromIndex: Int,
        toIndex: Int,
    ): Any {
        // 如果不是一级页面之间的切换，使用默认动画
        if (fromIndex == -1 || toIndex == -1) {
            when {
                isPop && isEnter -> {
                    return EnterTransition.None
                }

                isPop && !isEnter -> {
                    return if (navSuiteType.isHorizontalNavigation()) {
                        slideOutHorizontally(tween(300)) { it }
                    } else {
                        slideOutVertically(tween(300)) { it }
                    } + fadeOut(tween(300))
                }

                !isPop && isEnter -> {
                    return if (navSuiteType.isHorizontalNavigation()) {
                        slideInHorizontally(tween(300)) { it }
                    } else {
                        slideInVertically(tween(300)) { it }
                    }
                }

                !isPop && !isEnter -> {
                    return ExitTransition.None
                }
            }
        }
        // 一级页面之间的切换
        val offset = when {
            // 向右滑动
            toIndex > fromIndex -> if (isEnter) 1 else -1
            // 向左滑动
            toIndex < fromIndex -> if (isEnter) -1 else 1
            // 同一页面
            else -> return if (isEnter) EnterTransition.None else ExitTransition.None
        }
        return if (isEnter) {
            if (navSuiteType.isHorizontalNavigation()) {
                slideInHorizontally(tween(300)) { it * offset }
            } else {
                slideInVertically(tween(300)) { it * offset }
            } + fadeIn(tween(300))
        } else {
            if (navSuiteType.isHorizontalNavigation()) {
                slideOutHorizontally(tween(300)) { it * offset }
            } else {
                slideOutVertically(tween(300)) { it * offset }
            } + fadeOut(tween(300))
        }
    }

    val suiteItemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = if (duo3NavStyle) {
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
        navigationRailItemColors = if (duo3NavStyle) {
            if (!ThemeManager.isDarkTheme()) {
                NavigationRailItemDefaults.colors().copy(
                    selectedIndicatorColor =
                        MaterialTheme.colorScheme.secondaryContainer
                            .copy(alpha = 0.92f)
                            .compositeOver(MaterialTheme.colorScheme.secondary),
                )
            } else {
                NavigationRailItemDefaults.colors()
            }
        } else {
            NavigationRailItemDefaults.colors(
                selectedIconColor = Color(0xff66ccff),
                indicatorColor = Color.Transparent,
            )
        },
    )

    val shouldShowNavigation = isTopLevelDest(navEntry) &&
        (!navSuiteType.isHorizontalNavigation() || !autoHideBottomBar || isBottomBarVisible)

    LaunchedEffect(shouldShowNavigation) {
        if (shouldShowNavigation) {
            navigationSuiteState.show()
        } else {
            navigationSuiteState.hide()
        }
    }
    // 页面切换时重置底部导航栏可见状态
    LaunchedEffect(navEntry) { isBottomBarVisible = true }

    @OptIn(ExperimentalMaterial3ComponentOverrideApi::class)
    val myCustomOverride = object : NavigationBarOverride, NavigationRailOverride {
        @Composable
        override fun NavigationBarOverrideScope.NavigationBar() {
            Surface(
                color = containerColor,
                contentColor = contentColor,
                tonalElevation = tonalElevation,
                modifier = this@NavigationBar.modifier.height(
                    (if (duo3NavStyle) 64.dp else 56.dp) + bottomPadding,
                ),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(windowInsets)
                            .padding(top = (if (duo3NavStyle) 4.dp else 0.dp))
                            .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content,
                )
            }
        }

        @Composable
        override fun NavigationRailOverrideScope.NavigationRail() {
            Surface(
                color = containerColor,
                contentColor = contentColor,
                modifier = this@NavigationRail.modifier.width(
                    (if (duo3NavStyle) 80.dp else 56.dp),
                ),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(windowInsets)
                            .widthIn(min = 80.dp)
                            .padding(vertical = 4.dp)
                            .selectableGroup()
                            .semantics { isTraversalGroup = true },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = content,
                )
            }
        }
    }

    CompositionLocalProvider(
        LocalNavigationBarOverride provides myCustomOverride,
        LocalNavigationRailOverride provides myCustomOverride,
    ) {
        NavigationSuiteScaffold(
            modifier = modifier
                .nestedScroll(bottomBarScrollConnection)
                .semantics { testTagsAsResourceId = true },
            navigationSuiteColors = NavigationSuiteDefaults.colors(
                shortNavigationBarContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                navigationBarContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                navigationRailContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            state = navigationSuiteState,
            navigationSuiteItems = {
                if (navEntry != null) {
                    bottomBarItems.forEach { (destination, label, icon) ->
                        val tag = "nav_tab_${(destination as? TopLevelDestination)?.name?.lowercase() ?: label.lowercase()}"
                        item(
                            selected = navEntry.hasRoute(destination::class),
                            onClick = {
                                if (!navEntry.hasRoute(destination::class)) {
                                    navController.navigate(destination) {
                                        popUpTo(startDestination)
                                        launchSingleTop = true
                                        restoreState = true
                                    }
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
                            colors = suiteItemColors,
                            icon = {
                                Icon(icon, contentDescription = label)
                            },
                            modifier = (if (duo3NavStyle) Modifier.padding(top = 4.dp) else Modifier).testTag(tag),
                        )
                    }
                }
            },
        ) {
            Scaffold { innerPadding ->
                CompositionLocalProvider(
                    LocalNavigator provides Navigator(
                        onNavigate = activity::navigate,
                        onNavigateBack = navController::popBackStack,
                    ),
                ) {
                    MyNavHost(
                        navController,
                        modifier = Modifier,
                        startDestination = startDestination,
                        enterTransition = {
                            val fromIndex = getPageIndex(initialState.destination)
                            val toIndex = getPageIndex(targetState.destination)
                            createSlideAnimation(isEnter = true, isPop = false, fromIndex, toIndex) as EnterTransition
                        },
                        exitTransition = {
                            val fromIndex = getPageIndex(initialState.destination)
                            val toIndex = getPageIndex(targetState.destination)
                            createSlideAnimation(isEnter = false, isPop = false, fromIndex, toIndex) as ExitTransition
                        },
                        popEnterTransition = {
                            val fromIndex = getPageIndex(initialState.destination)
                            val toIndex = getPageIndex(targetState.destination)
                            createSlideAnimation(isEnter = true, isPop = true, fromIndex, toIndex) as EnterTransition
                        },
                        popExitTransition = {
                            val fromIndex = getPageIndex(initialState.destination)
                            val toIndex = getPageIndex(targetState.destination)
                            createSlideAnimation(isEnter = false, isPop = true, fromIndex, toIndex) as ExitTransition
                        },
                    ) {
                        composable<Home> {
                            HomeScreen(
                                scrollToTopTrigger = scrollToTopTrigger,
                                innerPadding = innerPadding,
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
                            ArticleScreen(article, viewModel, innerPadding)
                        }
                        composable<HotList> {
                            HotListScreen(innerPadding)
                        }
                        composable<Follow> {
                            FollowScreen(
                                scrollToTopTrigger = scrollToTopTrigger,
                                innerPadding = innerPadding,
                            )
                        }
                        composable<Daily> {
                            DailyScreen(innerPadding)
                        }
                        composable<History> {
                            HistoryScreen(innerPadding)
                        }
                        composable<OnlineHistory> {
                            OnlineHistoryScreen(innerPadding)
                        }
                        composable<Account> {
                            AccountSettingScreen(innerPadding)
                        }
                        composable<Search> { navEntry ->
                            val search: Search = navEntry.toRoute()
                            SearchScreen(innerPadding, search)
                        }
                        composable<Collections> {
                            val data: Collections = it.toRoute()
                            CollectionScreen(data.userToken, innerPadding)
                        }
                        composable<CollectionContent> {
                            val content: CollectionContent = it.toRoute()
                            CollectionContentScreen(content.collectionId, innerPadding)
                        }
                        composable<Person> {
                            val person: Person = it.toRoute()
                            PeopleScreen(innerPadding, person)
                        }
                        composable<Pin> {
                            val pin = it.toRoute<Pin>()
                            PinScreen(innerPadding, pin)
                        }
                        composable<Account.RecommendSettings.Blocklist> {
                            BlocklistSettingsScreen(innerPadding)
                        }
                        composable<Account.RecommendSettings.BlockedFeedHistory> {
                            BlockedFeedHistoryScreen()
                        }
                        composable<Notification> {
                            NotificationScreen(innerPadding)
                        }
                        composable<Notification.NotificationSettings> {
                            NotificationSettingsScreen(innerPadding)
                        }
                        composable<SentenceSimilarityTest> {
                            SentenceSimilarityTestScreen(innerPadding)
                        }
                        composable<Account.AppearanceSettings> {
                            val args = it.toRoute<Account.AppearanceSettings>()
                            AppearanceSettingsScreen(
                                innerPadding,
                                setting = args.setting,
                                onExit = reloadBottomBarPreferences,
                            )
                        }
                        composable<Account.RecommendSettings> {
                            val args = it.toRoute<Account.RecommendSettings>()
                            ContentFilterSettingsScreen(
                                innerPadding,
                                setting = args.setting,
                            )
                        }
                        composable<Account.SystemAndUpdateSettings> {
                            SystemAndUpdateSettingsScreen(innerPadding)
                        }
                        composable<Account.OpenSourceLicenses> {
                            OpenSourceLicensesScreen()
                        }
                        composable<Account.DeveloperSettings> {
                            DeveloperSettingsScreen(innerPadding)
                        }
                        composable<Account.DeveloperSettings.ColorScheme> {
                            ColorSchemeScreen(innerPadding)
                        }
                    }
                }
            }
        }
    }
}

private fun isTopLevelDest(navEntry: NavBackStackEntry?): Boolean = navEntry.hasRoute(Home::class) ||
    navEntry.hasRoute(Follow::class) ||
    navEntry.hasRoute(HotList::class) ||
    navEntry.hasRoute(Daily::class) ||
    navEntry.hasRoute(OnlineHistory::class) ||
    navEntry.hasRoute(Account::class)

private fun NavigationSuiteType.isHorizontalNavigation(): Boolean =
    this == NavigationSuiteType.ShortNavigationBarCompact ||
        this == NavigationSuiteType.ShortNavigationBarMedium ||
        this == NavigationSuiteType.NavigationBar

internal fun NavBackStackEntry?.hasRoute(cls: KClass<out NavDestination>): Boolean {
    val dest = this?.destination ?: return false
    return dest.hierarchy.any { it.hasRoute(cls) }
}

// 覆盖原有动画
internal class MyNavigationSuiteScaffoldStateImpl(
    var initialValue: NavigationSuiteScaffoldValue,
) : NavigationSuiteScaffoldState {
    private val internalValue: Float = if (initialValue == NavigationSuiteScaffoldValue.Visible) VISIBLE else HIDDEN
    private val internalState = Animatable(internalValue, Float.VectorConverter)
    private val currentValueState = derivedStateOf {
        if (internalState.value == VISIBLE) {
            NavigationSuiteScaffoldValue.Visible
        } else {
            NavigationSuiteScaffoldValue.Hidden
        }
    }

    override val isAnimating: Boolean
        get() = internalState.isRunning

    override val targetValue: NavigationSuiteScaffoldValue
        get() =
            if (internalState.targetValue == VISIBLE) {
                NavigationSuiteScaffoldValue.Visible
            } else {
                NavigationSuiteScaffoldValue.Hidden
            }

    override val currentValue: NavigationSuiteScaffoldValue
        get() = currentValueState.value

    override suspend fun hide() {
        internalState.animateTo(targetValue = HIDDEN, animationSpec = tween(200))
    }

    override suspend fun show() {
        internalState.animateTo(targetValue = VISIBLE, animationSpec = tween(200))
    }

    override suspend fun toggle() {
        internalState.animateTo(
            targetValue = if (targetValue == NavigationSuiteScaffoldValue.Visible) HIDDEN else VISIBLE,
            animationSpec = tween(200),
        )
    }

    override suspend fun snapTo(targetValue: NavigationSuiteScaffoldValue) {
        val target = if (targetValue == NavigationSuiteScaffoldValue.Visible) VISIBLE else HIDDEN
        internalState.snapTo(target)
    }

    companion object {
        private const val HIDDEN = 0f
        private const val VISIBLE = 1f

        fun saver() =
            androidx.compose.runtime.saveable.Saver<NavigationSuiteScaffoldState, NavigationSuiteScaffoldValue>(
                save = { it.targetValue },
                restore = { MyNavigationSuiteScaffoldStateImpl(it) },
            )
    }
}

@Preview(showBackground = true)
@Composable
private fun GreetingPreview() {
    val navController = rememberNavController()
    ZhihuTheme {
        ZhihuMain(navController = navController)
    }
}
