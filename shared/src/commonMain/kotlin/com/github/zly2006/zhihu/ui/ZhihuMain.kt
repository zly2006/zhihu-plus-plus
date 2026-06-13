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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
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
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.theme.ThemeStyle
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.components.LocalAutoHideTopBarVisible
import com.github.zly2006.zhihu.ui.miuix.MiuixAccountSettingScreen
import com.github.zly2006.zhihu.ui.miuix.MiuixBlocklistSettingsScreen
import com.github.zly2006.zhihu.ui.miuix.MiuixCollectionContentScreen
import com.github.zly2006.zhihu.ui.miuix.MiuixCollectionScreen
import com.github.zly2006.zhihu.ui.miuix.MiuixDailyScreen
import com.github.zly2006.zhihu.ui.miuix.MiuixFollowTopLevelPage
import com.github.zly2006.zhihu.ui.miuix.MiuixHomeScreen
import com.github.zly2006.zhihu.ui.miuix.MiuixHotListScreen
import com.github.zly2006.zhihu.ui.miuix.MiuixLocalHistoryScreen
import com.github.zly2006.zhihu.ui.miuix.MiuixNotificationScreen
import com.github.zly2006.zhihu.ui.miuix.MiuixOnlineHistoryScreen
import com.github.zly2006.zhihu.ui.miuix.MiuixPeopleScreen
import com.github.zly2006.zhihu.ui.miuix.MiuixPinScreen
import com.github.zly2006.zhihu.ui.miuix.MiuixQuestionScreen
import com.github.zly2006.zhihu.ui.miuix.MiuixSearchScreen
import com.github.zly2006.zhihu.ui.miuix.subscreens.MiuixAboutScreen
import com.github.zly2006.zhihu.ui.miuix.subscreens.MiuixAppearanceSettingsScreen
import com.github.zly2006.zhihu.ui.miuix.subscreens.MiuixBlockedFeedHistoryScreen
import com.github.zly2006.zhihu.ui.miuix.subscreens.MiuixContentFilterSettingsScreen
import com.github.zly2006.zhihu.ui.miuix.subscreens.MiuixDeveloperSettingsScreen
import com.github.zly2006.zhihu.ui.miuix.subscreens.MiuixNotificationSettingsScreen
import com.github.zly2006.zhihu.ui.miuix.subscreens.MiuixOpenSourceLicensesScreen
import com.github.zly2006.zhihu.ui.miuix.subscreens.MiuixSystemAndUpdateSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.AboutScreen
import com.github.zly2006.zhihu.ui.subscreens.AppearanceSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.BlockedFeedHistoryScreen
import com.github.zly2006.zhihu.ui.subscreens.ColorSchemeScreen
import com.github.zly2006.zhihu.ui.subscreens.ContentFilterSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.DeveloperSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.OpenSourceLicensesScreen
import com.github.zly2006.zhihu.ui.subscreens.SystemAndUpdateSettingsScreen
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.nav.core.NavController
import top.yukonga.miuix.kmp.nav.core.NavCornerClipMode
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.basic.NavigationBar as MiuixNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem as MiuixNavigationBarItem

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
    val article: @Composable (Article) -> Unit,
    val sentenceSimilarityTest: @Composable () -> Unit = {
        Text("Sentence similarity test is not available on this platform.")
    },
    val blocklistSettingsNlpContent: BlocklistSettingsNlpContent? = null,
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
    navController: NavController<NavDestination>,
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
    val autoHideTopBar = preferenceState.autoHideTopBar
    val selectedBottomBarItemKeys = preferenceState.selectedBottomBarItemKeys
    val startDestination = preferenceState.startDestination
    val reloadBottomBarPreferences = preferenceState::reload

    // miuix-nav 的返回栈是 SnapshotStateList，直接读取栈顶即响应式（替代 currentBackStackEntryAsState）。
    val currentTopDestination = navController.backStack.lastOrNull()

    // 离开文章页时恢复系统状态栏（只在实际切换时触发）
    val isOnArticle = currentTopDestination is Article
    var wasOnArticle by remember { mutableStateOf(false) }
    if (!isOnArticle && wasOnArticle) {
        LeaveImmersiveModeCleanup()
    }
    SideEffect {
        wasOnArticle = isOnArticle
    }

    var scrollToTopTrigger by remember { mutableIntStateOf(0) }
    // 滚动时自动隐藏底部导航栏（顶栏自动隐藏复用同一信号，见 LocalAutoHideTopBarVisible）
    var isBottomBarVisible by remember { mutableStateOf(true) }
    val bottomBarScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // 只响应用户拖动：fling 惯性与 miuix overScroll 回弹会产生正负交替的 available.y，
                // 松手后会把顶栏/底栏又弹回来跳变，必须忽略这些 SideEffect 来源。
                // 且只在状态真正变化时赋值，避免每帧 set 触发顶层重组。
                // 阈值提高到 10f + 滞回：prevents overscroll 回弹小幅振荡触发反复 toggle
                if (source == NestedScrollSource.UserInput) {
                    when {
                        available.y < -10f -> if (isBottomBarVisible) isBottomBarVisible = false
                        available.y > 10f -> if (!isBottomBarVisible) isBottomBarVisible = true
                    }
                }
                return Offset.Zero
            }
        }
    }

    val useMiuix = ThemeManager.getThemeStyle() == ThemeStyle.Miuix
    val settings = rememberSettingsStore()
    val blurEnabled = settings.getBoolean("blurEnabled", true)
    val bottomBarBackdrop = rememberMiuixBlurBackdrop(useMiuix && blurEnabled)
    // 设置项“启用预测性返回”：关闭后系统返回仍可用，但不跟手播放预测返回动画（返回设置页后下次重组生效）。
    val enablePredictiveBack = settings.getBoolean("enable_predictive_back", true)

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
            run {
                // 页面切换时重置底部导航栏可见状态（顶栏复用此信号，一并恢复）
                LaunchedEffect(currentTopDestination) { isBottomBarVisible = true }
                val currentBottomDestination = mainTabPages
                    .getOrNull(mainPagerState.targetPage)
                    ?.bottomDestination
                AnimatedVisibility(
                    visible = (!autoHideBottomBar || isBottomBarVisible) && currentTopDestination is MainTabs,
                    enter = slideInVertically(tween(200)) { it },
                    exit = slideOutVertically(tween(200)) { it },
                ) {
                    if (useMiuix) {
                        MiuixNavigationBar(
                            modifier = Modifier
                                .height(64.dp + bottomPadding)
                                .installerMiuixBlurEffect(bottomBarBackdrop),
                            color = bottomBarBackdrop.getMiuixAppBarColor(),
                        ) {
                            bottomBarItems.forEach { item ->
                                val selected = currentBottomDestination?.let { it::class == item.first::class } == true
                                MiuixNavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        if (!selected) {
                                            navigateTopLevel(item.first)
                                        } else if (tapToScrollToTopEnabled) {
                                            scrollToTopTrigger++
                                        }
                                    },
                                    icon = item.third,
                                    label = item.second,
                                )
                            }
                        }
                    } else {
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
            }
        },
    ) { innerPadding ->
        val systemCornerRadius = rememberNavSystemCornerRadius()
        // miuix：滑动时裁前缘屏幕圆角（与自有横滑转场配套）。
        // M3：关闭 effects 圆角，圆角由 AOSP 预测返回转场自行在手势中处理，避免双重裁切。
        val navEffects = if (useMiuix) {
            NavDisplayEffects(
                cornerClipRadius = systemCornerRadius,
                cornerClipMode = NavCornerClipMode.Leading,
                dimAmount = 0f,
                // 不拦截 transition 期间输入：避免 settle 慢尾挡住点击造成“返回后卡一段时间”。
                blockInputDuringTransition = false,
            )
        } else {
            // M3：关 effects 圆角（交给 AOSP 转场）；关闭 transition 期间的输入拦截——否则预测返回 settle 的
            // ~500ms 弹簧慢尾会一直挡住已揭示页面的点击，造成“返回后卡一段时间点不动”。
            NavDisplayEffects(enableCornerClip = false, dimAmount = 0f, blockInputDuringTransition = false)
        }
        CompositionLocalProvider(
            LocalNavigator provides Navigator(
                onNavigate = { destination ->
                    navigationState.navigate(destination)
                },
                onNavigateBack = { navController.pop() },
            ),
        ) {
            NavDisplay(
                navController,
                modifier = Modifier,
                onBack = { navController.pop() },
                // M3 用 AOSP 风格预测性返回（手势中缩放+圆角+跟随边），miuix 保持自有横滑转场。
                transition = if (useMiuix) NavTransitions.MiuixDefault else AospPredictiveBackTransition,
                effects = navEffects,
                enablePredictiveBack = enablePredictiveBack,
            ) {
                entry<MainTabs> {
                    CompositionLocalProvider(
                        LocalAutoHideTopBarVisible provides (!autoHideTopBar || isBottomBarVisible),
                    ) {
                        MainTabsPager(
                            pagerState = mainPagerState,
                            pages = mainTabPages,
                            scrollToTopTrigger = scrollToTopTrigger,
                            innerPadding = innerPadding,
                            bottomBarBackdrop = bottomBarBackdrop,
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
                }
                entry<Question> { question ->
                    if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                        MiuixQuestionScreen(question)
                    } else {
                        QuestionScreen(question)
                    }
                }
                entry<Article> { article ->
                    // 同一回答链在单个 entry 内用 AnimatedContent 切换：返回键直接回到来源页，
                    // 切换动画两层互补满屏滑动，不透出上一个回答。
                    ArticleAnswerSlot(article) { answer ->
                        platformAdapter.article(answer)
                    }
                }
                // 注：HotList/Follow/Daily/Account 是纯 TopLevelDestination（非 NavDestination），
                // 只能作为底栏 tab 在 MainTabs pager 内渲染，无法被 push，故不注册为独立 entry。
                entry<History> {
                    if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                        MiuixLocalHistoryScreen(innerPadding)
                    } else {
                        LegacyLocalHistoryScreen(innerPadding)
                    }
                }
                entry<OnlineHistory> {
                    // 作为独立路由 push 进来（如账号页/面板"浏览历史"入口），显示返回箭头。
                    if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                        MiuixOnlineHistoryScreen(showBackButton = true)
                    } else {
                        OnlineHistoryScreen()
                    }
                }
                entry<Search> { search ->
                    if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                        MiuixSearchScreen(search)
                    } else {
                        SearchScreen(search)
                    }
                }
                entry<Collections> { data ->
                    if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                        MiuixCollectionScreen(data.userToken)
                    } else {
                        CollectionScreen(data.userToken)
                    }
                }
                entry<CollectionContent> { content ->
                    if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                        MiuixCollectionContentScreen(content.collectionId)
                    } else {
                        CollectionContentScreen(content.collectionId)
                    }
                }
                entry<Person> { person ->
                    if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                        MiuixPeopleScreen(person)
                    } else {
                        PeopleScreen(person)
                    }
                }
                entry<Pin> { pin ->
                    if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                        MiuixPinScreen(pin)
                    } else {
                        PinScreen(pin)
                    }
                }
                entry<Account.RecommendSettings.Blocklist> {
                    if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                        MiuixBlocklistSettingsScreen(platformAdapter.blocklistSettingsNlpContent)
                    } else {
                        BlocklistSettingsScreen(platformAdapter.blocklistSettingsNlpContent)
                    }
                }
                entry<Account.RecommendSettings.BlockedFeedHistory> {
                    if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                        MiuixBlockedFeedHistoryScreen()
                    } else {
                        BlockedFeedHistoryScreen()
                    }
                }
                entry<Notification> {
                    if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                        MiuixNotificationScreen()
                    } else {
                        NotificationScreen()
                    }
                }
                entry<Notification.NotificationSettings> {
                    if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                        MiuixNotificationSettingsScreen()
                    } else {
                        NotificationSettingsScreen()
                    }
                }
                entry<SentenceSimilarityTest> {
                    platformAdapter.sentenceSimilarityTest()
                }
                entry<Account.AppearanceSettings> { args ->
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
                entry<Account.RecommendSettings> { args ->
                    if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                        MiuixContentFilterSettingsScreen(args.setting)
                    } else {
                        ContentFilterSettingsScreen(args.setting)
                    }
                }
                entry<Account.SystemAndUpdateSettings> {
                    if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                        MiuixSystemAndUpdateSettingsScreen()
                    } else {
                        SystemAndUpdateSettingsScreen()
                    }
                }
                entry<Account.About> {
                    if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                        MiuixAboutScreen(innerPadding)
                    } else {
                        AboutScreen(innerPadding)
                    }
                }
                entry<Account.OpenSourceLicenses> {
                    if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                        MiuixOpenSourceLicensesScreen()
                    } else {
                        OpenSourceLicensesScreen()
                    }
                }
                entry<Account.DeveloperSettings> {
                    if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                        MiuixDeveloperSettingsScreen()
                    } else {
                        DeveloperSettingsScreen()
                    }
                }
                entry<Account.DeveloperSettings.ColorScheme> {
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
    bottomBarBackdrop: LayerBackdrop? = null,
    onFollowTabSelected: (Int) -> Unit,
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .then(if (bottomBarBackdrop != null) Modifier.layerBackdrop(bottomBarBackdrop) else Modifier),
    ) { pageIndex ->
        val page = pages.getOrNull(pageIndex) ?: return@HorizontalPager
        when (page) {
            MainTabPage.HomePage -> if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                MiuixHomeScreen(
                    scrollToTopTrigger = scrollToTopTrigger,
                    innerPadding = innerPadding,
                )
            } else {
                HomeScreen(
                    scrollToTopTrigger = scrollToTopTrigger,
                    innerPadding = innerPadding,
                )
            }
            MainTabPage.FollowRecommendPage -> if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                MiuixFollowTopLevelPage(
                    selectedTabIndex = 0,
                    onTabSelected = onFollowTabSelected,
                    scrollToTopTrigger = scrollToTopTrigger,
                    innerPadding = innerPadding,
                    isActive = pagerState.currentPage == pageIndex,
                )
            } else {
                FollowTopLevelPage(
                    selectedTabIndex = 0,
                    onTabSelected = onFollowTabSelected,
                    scrollToTopTrigger = scrollToTopTrigger,
                    innerPadding = innerPadding,
                    isActive = pagerState.currentPage == pageIndex,
                )
            }
            MainTabPage.FollowDynamicPage -> if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                MiuixFollowTopLevelPage(
                    selectedTabIndex = 1,
                    onTabSelected = onFollowTabSelected,
                    scrollToTopTrigger = scrollToTopTrigger,
                    innerPadding = innerPadding,
                    isActive = pagerState.currentPage == pageIndex,
                )
            } else {
                FollowTopLevelPage(
                    selectedTabIndex = 1,
                    onTabSelected = onFollowTabSelected,
                    scrollToTopTrigger = scrollToTopTrigger,
                    innerPadding = innerPadding,
                    isActive = pagerState.currentPage == pageIndex,
                )
            }
            MainTabPage.HotListPage -> if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                MiuixHotListScreen(innerPadding)
            } else {
                HotListScreen(innerPadding)
            }
            MainTabPage.DailyPage -> if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                MiuixDailyScreen()
            } else {
                DailyScreen()
            }
            MainTabPage.OnlineHistoryPage -> if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                MiuixOnlineHistoryScreen()
            } else {
                OnlineHistoryScreen()
            }
            MainTabPage.MyCollectionsPage -> MyCollectionsTopLevelPage()
            MainTabPage.AccountPage -> if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                MiuixAccountSettingScreen(innerPadding)
            } else {
                AccountSettingScreen(innerPadding)
            }
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

private val TopLevelDestination.openFrom: String?
    get() = when (this) {
        Home -> ContentOpenFrom.HOME_FEED
        OnlineHistory -> ContentOpenFrom.HISTORY
        else -> null
    }
