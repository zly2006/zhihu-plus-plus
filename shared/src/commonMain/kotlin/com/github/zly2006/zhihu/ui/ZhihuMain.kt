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

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
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
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.ArticleTypeNavType
import com.github.zly2006.zhihu.navigation.CollectionContent
import com.github.zly2006.zhihu.navigation.Collections
import com.github.zly2006.zhihu.navigation.Daily
import com.github.zly2006.zhihu.navigation.Follow
import com.github.zly2006.zhihu.navigation.History
import com.github.zly2006.zhihu.navigation.Home
import com.github.zly2006.zhihu.navigation.HotList
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.MainTabs
import com.github.zly2006.zhihu.navigation.MyCollections
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
import com.github.zly2006.zhihu.shared.filter.ContentOpenFrom
import com.github.zly2006.zhihu.ui.subscreens.AppearanceSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.BlockedFeedHistoryScreen
import com.github.zly2006.zhihu.ui.subscreens.ColorSchemeScreen
import com.github.zly2006.zhihu.ui.subscreens.ContentFilterSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.DeveloperSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.OpenSourceLicensesScreen
import com.github.zly2006.zhihu.ui.subscreens.SystemAndUpdateSettingsScreen
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

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

    data object MyCollectionsPage : MainTabPage(MyCollections, "my_collections")

    data object AccountPage : MainTabPage(Account, "account")
}

/**
 * 共享主壳使用的平台适配层。
 *
 * [ZhihuMain] 负责导航图、底部栏、主 pager 和通用页面路由；Android 和 Desktop 只在这里注入依赖平台服务的内容，
 * 例如文章页 ViewModel、回答切换转场、NLP 管理页和不可用功能的兜底展示。把适配面收窄后，共享 UI 可以专注描述产品结构，
 * 平台代码则继续处理生命周期、浏览器、模型加载等细节。
 */
data class ZhihuMainPlatformAdapter(
    val article: @Composable (Article, NavBackStackEntry) -> Unit,
    val sentenceSimilarityTest: @Composable () -> Unit = {
        Text("Sentence similarity test is not available on this platform.")
    },
    val blocklistSettingsNlpContent: BlocklistSettingsNlpContent? = null,
    val articleEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? = null,
    val articleExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? = null,
)

/**
 * Zhihu++ 的共享应用主壳。
 *
 * 这个 composable 是顶层体验的唯一所有者：渲染可配置底部导航栏，承载横向主 tab pager，向子页面提供 [LocalNavigator]，
 * 并注册跨平台共享的 typed [NavDestination] route。设计上把顶层 tab 收在 [MainTabs] 内部，而不是把每个 tab
 * 都作为独立 NavHost 页面 push，这样 tab 重选、回到顶部、顶/底栏自动隐藏和持久化 tab 选择都能使用同一套状态模型。
 *
 * 用户可见的主壳设置通过 [preferenceState] 流入。设置页退出时只 reload 这份状态，不重建 NavHost，从而在应用底栏和主题相关变更时
 * 保留已加载页面、返回栈和滚动位置。
 */
@OptIn(ExperimentalFoundationApi::class)
@Suppress("RestrictedApi")
@Composable
fun ZhihuMain(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    navigationState: ZhihuMainNavigationState,
    preferenceState: ZhihuMainPreferenceState,
    isDarkTheme: Boolean,
    platformAdapter: ZhihuMainPlatformAdapter,
) {
    val bottomPadding = ScaffoldDefaults.contentWindowInsets.asPaddingValues().calculateBottomPadding()
    val duo3HomeAccount = preferenceState.duo3HomeAccount
    val duo3NavStyle = preferenceState.duo3NavStyle
    val tapToScrollToTopEnabled = preferenceState.tapToScrollToTopEnabled
    val autoHideBottomBar = preferenceState.autoHideBottomBar
    val selectedBottomBarItemKeys = preferenceState.selectedBottomBarItemKeys
    val startDestination = preferenceState.startDestination
    val reloadBottomBarPreferences = preferenceState::reload

    val navEntry by navController.currentBackStackEntryAsState()

    // 离开文章页时恢复系统状态栏（只在实际切换时触发）
    val isOnArticle = navEntry?.destination?.hasRoute<Article>() == true
    var wasOnArticle by remember { mutableStateOf(false) }
    if (!isOnArticle && wasOnArticle) {
        LeaveImmersiveModeCleanup()
    }
    SideEffect {
        wasOnArticle = isOnArticle
    }

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
        Triple(MyCollections, "收藏夹", Icons.Filled.Bookmarks),
        Triple(Account, "账号", Icons.Filled.ManageAccounts),
    )
    val bottomBarItems = selectedBottomBarItemKeys.mapNotNull { key ->
        allBottomBarItems.firstOrNull { it.first.name == key }
    }

    val mainTabPages = remember(bottomBarItems) {
        bottomBarItems.flatMap { item ->
            when (item.first) {
                Home -> listOf(MainTabPage.HomePage)
                Follow -> listOf(MainTabPage.FollowRecommendPage, MainTabPage.FollowDynamicPage)
                HotList -> listOf(MainTabPage.HotListPage)
                Daily -> listOf(MainTabPage.DailyPage)
                OnlineHistory -> listOf(MainTabPage.OnlineHistoryPage)
                MyCollections -> listOf(MainTabPage.MyCollectionsPage)
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
            navigationState.setCurrentMainTabOpenFrom(destination.openFrom)
        }
    }

    val mainTabNavigationTarget = navigationState.mainTabNavigationTarget
    LaunchedEffect(mainTabNavigationTarget, mainTabPages) {
        mainTabNavigationTarget?.let { destination ->
            // 平台适配层会把旧的顶层 route 请求映射到 MainTabs。这里消费该请求，
            // 让 deeplink 等调用方仍能选中 Home/Follow 等 tab，而不是把旧 route 压入返回栈。
            mainPagerState.scrollToPage(pageIndexForBottomDestination(destination))
            navigationState.consumeMainTabNavigationTarget(destination)
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
            .nestedScroll(bottomBarScrollConnection),
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
                                    if (!isDarkTheme) {
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
                    navigationState.navigate(destination)
                },
                onNavigateBack = navController::popBackStack,
            ),
        ) {
            NavHost(
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
                    typeMap = mapOf(typeOf<ArticleType>() to ArticleTypeNavType),
                    enterTransition = platformAdapter.articleEnterTransition,
                    exitTransition = platformAdapter.articleExitTransition,
                ) { navEntry ->
                    val article: Article = navEntry.toRoute()
                    platformAdapter.article(article, navEntry)
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
                    DailyScreen()
                }
                composable<History> {
                    LegacyLocalHistoryScreen(innerPadding)
                }
                composable<OnlineHistory> {
                    OnlineHistoryScreen()
                }
                composable<Account> {
                    AccountSettingScreen(innerPadding)
                }
                composable<Search> { navEntry ->
                    val search: Search = navEntry.toRoute()
                    SearchScreen(search)
                }
                composable<Collections> { navEntry ->
                    val data: Collections = navEntry.toRoute()
                    CollectionScreen(data.userToken)
                }
                composable<CollectionContent> { navEntry ->
                    val content: CollectionContent = navEntry.toRoute()
                    CollectionContentScreen(content.collectionId)
                }
                composable<Person> { navEntry ->
                    val person: Person = navEntry.toRoute()
                    PeopleScreen(person)
                }
                composable<Pin> { navEntry ->
                    val pin: Pin = navEntry.toRoute()
                    PinScreen(pin)
                }
                composable<Account.RecommendSettings.Blocklist> {
                    BlocklistSettingsScreen(platformAdapter.blocklistSettingsNlpContent)
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
                    platformAdapter.sentenceSimilarityTest()
                }
                composable<Account.AppearanceSettings> { navEntry ->
                    val args = navEntry.toRoute<Account.AppearanceSettings>()
                    AppearanceSettingsScreen(
                        setting = args.setting,
                        onExit = reloadBottomBarPreferences,
                    )
                }
                composable<Account.RecommendSettings> { navEntry ->
                    val args = navEntry.toRoute<Account.RecommendSettings>()
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

/**
 * 渲染可配置底部导航主壳内的页面。
 *
 * pager 的页数可以多于底部栏项，因为 [Follow] 会拆成“推荐”和“动态”两个页面。这样横向滑动仍然自然，而底部栏仍只展示一个“关注”入口。
 * 每个页面都接收主壳给出的 [innerPadding]，保证系统栏、底部栏和子页面之间的留白一致。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainTabsPager(
    pagerState: PagerState,
    pages: List<MainTabPage>,
    scrollToTopTrigger: Int,
    innerPadding: PaddingValues,
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
            MainTabPage.FollowRecommendPage -> FollowTopLevelPage(
                selectedTabIndex = 0,
                onTabSelected = onFollowTabSelected,
                scrollToTopTrigger = scrollToTopTrigger,
                innerPadding = innerPadding,
                isActive = pagerState.currentPage == pageIndex,
            )
            MainTabPage.FollowDynamicPage -> FollowTopLevelPage(
                selectedTabIndex = 1,
                onTabSelected = onFollowTabSelected,
                scrollToTopTrigger = scrollToTopTrigger,
                innerPadding = innerPadding,
                isActive = pagerState.currentPage == pageIndex,
            )
            MainTabPage.HotListPage -> HotListScreen(innerPadding)
            MainTabPage.DailyPage -> DailyScreen()
            MainTabPage.OnlineHistoryPage -> OnlineHistoryScreen()
            MainTabPage.MyCollectionsPage -> MyCollectionsTopLevelPage()
            MainTabPage.AccountPage -> AccountSettingScreen(innerPadding)
        }
    }
}

@Composable
private fun MyCollectionsTopLevelPage() {
    val runtime = rememberAccountSettingsPlatformRuntime()
    val account = runtime.accountState.value
    CollectionScreen(
        urlToken = account.urlToken,
        showBackButton = false,
    )
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
