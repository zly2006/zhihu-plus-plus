/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.github.zly2006.zhihu.account.LoginScreen
import com.github.zly2006.zhihu.filter.ContentOpenFrom
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
import com.github.zly2006.zhihu.navigation.Login
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
import com.github.zly2006.zhihu.navigation.Topic
import com.github.zly2006.zhihu.navigation.WriteAnswer
import com.github.zly2006.zhihu.navigation.WritePin
import com.github.zly2006.zhihu.navigation.loginNavigationRequestFlow
import com.github.zly2006.zhihu.platform.PlatformBackHandler
import com.github.zly2006.zhihu.platform.platformName
import com.github.zly2006.zhihu.platform.rememberSettingBoolean
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.reading.rememberReadingPlayerController
import com.github.zly2006.zhihu.reading.saveReadingPlaybackSpeed
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.theme.ThemeStyle
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.components.CompactReadingPlayerButton
import com.github.zly2006.zhihu.ui.components.LocalAutoHideTopBarHeightChanged
import com.github.zly2006.zhihu.ui.components.LocalAutoHideTopBarScrollFraction
import com.github.zly2006.zhihu.ui.components.LocalAutoHideTopBarVisible
import com.github.zly2006.zhihu.ui.components.NoOpPagerNestedScrollConnection
import com.github.zly2006.zhihu.ui.components.ReadingPlayerBar
import com.github.zly2006.zhihu.ui.components.ReadingQueueSheet
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
import com.github.zly2006.zhihu.ui.miuix.MiuixNotificationTimelineScreen
import com.github.zly2006.zhihu.ui.miuix.MiuixOnlineHistoryScreen
import com.github.zly2006.zhihu.ui.miuix.MiuixPeopleScreen
import com.github.zly2006.zhihu.ui.miuix.MiuixPinScreen
import com.github.zly2006.zhihu.ui.miuix.MiuixPrivateMessageScreen
import com.github.zly2006.zhihu.ui.miuix.MiuixQuestionScreen
import com.github.zly2006.zhihu.ui.miuix.MiuixSearchScreen
import com.github.zly2006.zhihu.ui.miuix.subscreens.MiuixAboutScreen
import com.github.zly2006.zhihu.ui.miuix.subscreens.MiuixAppearanceSettingsScreen
import com.github.zly2006.zhihu.ui.miuix.subscreens.MiuixBlockedFeedHistoryScreen
import com.github.zly2006.zhihu.ui.miuix.subscreens.MiuixContentFilterSettingsScreen
import com.github.zly2006.zhihu.ui.miuix.subscreens.MiuixDeveloperSettingsScreen
import com.github.zly2006.zhihu.ui.miuix.subscreens.MiuixIdentityManagementScreen
import com.github.zly2006.zhihu.ui.miuix.subscreens.MiuixNotificationSettingsScreen
import com.github.zly2006.zhihu.ui.miuix.subscreens.MiuixOpenSourceLicensesScreen
import com.github.zly2006.zhihu.ui.miuix.subscreens.MiuixReadingSettingsScreen
import com.github.zly2006.zhihu.ui.miuix.subscreens.MiuixSettingsSearchScreen
import com.github.zly2006.zhihu.ui.miuix.subscreens.MiuixSystemAndUpdateSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.AboutScreen
import com.github.zly2006.zhihu.ui.subscreens.AppearanceSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.BlockedFeedHistoryScreen
import com.github.zly2006.zhihu.ui.subscreens.ColorSchemeScreen
import com.github.zly2006.zhihu.ui.subscreens.ContentFilterSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.DeveloperSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.IdentityManagementScreen
import com.github.zly2006.zhihu.ui.subscreens.OpenSourceLicensesScreen
import com.github.zly2006.zhihu.ui.subscreens.ReadingSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.SettingsSearchScreen
import com.github.zly2006.zhihu.ui.subscreens.SystemAndUpdateSettingsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
import kotlin.reflect.typeOf
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.basic.NavigationBar as MiuixNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem as MiuixNavigationBarItem

private sealed class MainTabPage(
    val bottomDestination: TopLevelDestination,
    val key: String,
) {
    data object HomePage : MainTabPage(Home, "home")

    data object FollowPage : MainTabPage(Follow, "follow")

    data object HotListPage : MainTabPage(HotList, "hotlist")

    data object DailyPage : MainTabPage(Daily, "daily")

    data object OnlineHistoryPage : MainTabPage(OnlineHistory, "online_history")

    data object MyCollectionsPage : MainTabPage(MyCollections, "my_collections")

    data object AccountPage : MainTabPage(Account, "account")
}

internal val LocalReadingPlayerOverlayPadding = staticCompositionLocalOf { 0.dp }
internal val LocalArticleNavController = staticCompositionLocalOf<NavHostController?> { null }

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
    mainTabNavigationTarget: TopLevelDestination?,
    navigate: (NavDestination) -> Unit,
    setCurrentMainTabOpenFrom: (String?) -> Unit,
    consumeMainTabNavigationTarget: (TopLevelDestination) -> Unit,
    preferenceState: ZhihuMainPreferenceState,
    isDarkTheme: Boolean,
    articleContent: @Composable (Article, NavBackStackEntry) -> Unit,
    showMainNavigationBar: Boolean = true,
    showHomeTopActions: Boolean = true,
    onCurrentMainTabDestinationChange: (TopLevelDestination) -> Unit = {},
    sentenceSimilarityContent: @Composable () -> Unit = {
        error("$platformName 暂不支持句子相似度测试")
    },
    blocklistSettingsNlpContent: @Composable (onNavigateBack: () -> Unit) -> Unit = {
        error("$platformName 暂不支持 NLP 智能屏蔽设置")
    },
    articleEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? = null,
    articleExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? = null,
) {
    val bottomPadding = ScaffoldDefaults.contentWindowInsets.asPaddingValues().calculateBottomPadding()
    val duo3HomeAccount = preferenceState.duo3HomeAccount
    val tapToScrollToTopEnabled = preferenceState.tapToScrollToTopEnabled
    val autoHideBottomBar = preferenceState.autoHideBottomBar
    val autoHideTopBar = preferenceState.autoHideTopBar
    val collectionDirectBrowseEnabled = preferenceState.collectionDirectBrowseEnabled
    val selectedBottomBarItemKeys = preferenceState.selectedBottomBarItemKeys
    val startDestination = preferenceState.startDestination
    val reloadBottomBarPreferences = preferenceState::reload
    val readingPlayer = rememberReadingPlayerController()
    val readingPlayerState by readingPlayer.state
    val settings = rememberSettingsStore()
    var showReadingQueue by remember { mutableStateOf(false) }
    var isReadingPlayerExpandedByUser by remember { mutableStateOf(false) }
    var readingPlayerHeightPx by remember { mutableIntStateOf(0) }
    val readingPlayerOverlayOffsetState = remember { ReadingPlayerOverlayOffsetState() }
    val density = LocalDensity.current
    val currentOnMainTabDestinationChange by rememberUpdatedState(onCurrentMainTabDestinationChange)

    val navEntry by navController.currentBackStackEntryAsState()
    val showMainNavigation = navEntry?.destination?.hasRoute<MainTabs>() == true
    PlatformBackHandler(enabled = navEntry != null && !showMainNavigation) {
        navController.popBackStack()
    }
    val isOnReadingDetail = navEntry?.destination?.hasRoute<Article>() == true ||
        navEntry?.destination?.hasRoute<Question>() == true ||
        navEntry?.destination?.hasRoute<Pin>() == true
    val isReadingPlayerExpanded = readingPlayerState.hasSession &&
        (isOnReadingDetail || isReadingPlayerExpandedByUser)
    val shouldCompactPlayerOnBackgroundInteraction by rememberUpdatedState(
        isReadingPlayerExpandedByUser && !isOnReadingDetail,
    )
    val readingPlayerOverlayPadding = when {
        !readingPlayerState.hasSession -> 0.dp
        !isReadingPlayerExpanded -> 0.dp
        readingPlayerHeightPx > 0 -> with(density) { readingPlayerHeightPx.toDp() } + 16.dp
        else -> 16.dp
    }

    LaunchedEffect(readingPlayerState.hasSession) {
        if (!readingPlayerState.hasSession) {
            showReadingQueue = false
            isReadingPlayerExpandedByUser = false
            readingPlayerOverlayOffsetState.resetOffset()
        }
    }
    var previousReadingItemKey by remember { mutableStateOf(readingPlayerState.currentItem?.key) }
    LaunchedEffect(readingPlayerState.currentItem?.key) {
        val currentItem = readingPlayerState.currentItem
        val currentItemKey = currentItem?.key
        val itemChanged = previousReadingItemKey != null && previousReadingItemKey != currentItemKey
        previousReadingItemKey = currentItemKey
        if (itemChanged && currentItem != null) {
            val currentDestination = when {
                navEntry?.destination?.hasRoute<Article>() == true -> runCatching {
                    navEntry?.toRoute<Article>()
                }.getOrNull()
                navEntry?.destination?.hasRoute<Pin>() == true -> runCatching {
                    navEntry?.toRoute<Pin>()
                }.getOrNull()
                navEntry?.destination?.hasRoute<Question>() == true -> runCatching {
                    navEntry?.toRoute<Question>()
                }.getOrNull()
                else -> null
            }
            val destination = currentItem.toDestination(readingPlayerState.sourceId)
            if (currentDestination != null && currentDestination != destination) {
                navController.popBackStack()
                navigate(destination)
            }
        }
    }

    // 离开文章页时恢复系统状态栏（只在实际切换时触发）
    val isOnArticle = navEntry?.destination?.hasRoute<Article>() == true
    LaunchedEffect(navEntry) {
        isReadingPlayerExpandedByUser = false
        if (!isOnArticle) readingPlayerOverlayOffsetState.clearRoute()
    }
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
    // 顶栏自动隐藏复用底栏的同一个可见性信号，两者一起收起/展开，避免各自动画造成跳变。
    var autoHideTopBarScrollFraction by remember { mutableStateOf<Float?>(null) }
    var autoHideTopBarHeightPx by remember { mutableFloatStateOf(0f) }

    fun settleAutoHideTopBar() {
        val fraction = autoHideTopBarScrollFraction ?: return
        isBottomBarVisible = fraction >= 0.5f
        autoHideTopBarScrollFraction = null
    }
    val bottomBarScrollConnection = remember(autoHideTopBar, showMainNavigation) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // 只响应用户拖动：fling 惯性和 miuix overScroll 回弹会产生正负交替的 available.y，
                // 松手后会把顶栏/底栏又弹回来造成跳变。阈值 10f 带滞回，避免回弹小幅振荡反复 toggle。
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                if (autoHideTopBar && showMainNavigation && autoHideTopBarHeightPx > 0f) {
                    val baseFraction = autoHideTopBarScrollFraction ?: if (isBottomBarVisible) 1f else 0f
                    val nextFraction = (baseFraction + available.y / autoHideTopBarHeightPx).coerceIn(0f, 1f)
                    if (nextFraction != baseFraction) {
                        autoHideTopBarScrollFraction = nextFraction
                    }
                    when (nextFraction) {
                        0f -> if (isBottomBarVisible) isBottomBarVisible = false
                        1f -> if (!isBottomBarVisible) isBottomBarVisible = true
                    }
                }
                when {
                    available.y < -10f -> if (isBottomBarVisible) isBottomBarVisible = false
                    available.y > 10f -> if (!isBottomBarVisible) isBottomBarVisible = true
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                settleAutoHideTopBar()
                return Velocity.Zero
            }
        }
    }

    val useMiuix = ThemeManager.getThemeStyle() == ThemeStyle.Miuix
    val blurEnabled = rememberSettingBoolean("blurEnabled", true, settings)
    val bottomBarBackdrop = rememberMiuixBlurBackdrop(useMiuix && blurEnabled)

    val allBottomBarItems = listOf(
        Triple(Home, "主页", Icons.Filled.Home),
        Triple(Follow, "关注", Icons.Filled.Group),
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
                Follow -> listOf(MainTabPage.FollowPage)
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

    val mainPagerState = rememberPagerState(
        initialPage = pageIndexForDestination(startDestination),
        pageCount = { mainTabPages.size },
    )
    val coroutineScope = rememberCoroutineScope()

    var currentMainTabDestination by remember { mutableStateOf(startDestination) }

    fun navigateTopLevel(destination: TopLevelDestination) {
        val targetPage = pageIndexForDestination(destination)
        coroutineScope.launch {
            mainPagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(navController) {
        loginNavigationRequestFlow.collect {
            navController.navigate(Login) {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(mainPagerState.currentPage, mainTabPages) {
        mainTabPages.getOrNull(mainPagerState.currentPage)?.bottomDestination?.let { destination ->
            currentMainTabDestination = destination
            setCurrentMainTabOpenFrom(destination.openFrom)
            currentOnMainTabDestinationChange(destination)
        }
    }

    PlatformBackHandler(showMainNavigation && mainPagerState.currentPage != 0) {
        coroutineScope.launch {
            mainPagerState.animateScrollToPage(0)
        }
    }

    LaunchedEffect(mainTabNavigationTarget, mainTabPages) {
        mainTabNavigationTarget?.let { destination ->
            // 平台适配层会把旧的顶层 route 请求映射到 MainTabs。这里消费该请求，
            // 让 deeplink 等调用方仍能选中 Home/Follow 等 tab，而不是把旧 route 压入返回栈。
            mainPagerState.scrollToPage(pageIndexForDestination(destination))
            consumeMainTabNavigationTarget(destination)
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

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(bottomBarScrollConnection),
            floatingActionButton = {
                AnimatedVisibility(
                    visible = isReadingPlayerExpanded,
                    enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.92f),
                    exit = fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.92f),
                ) {
                    ReadingPlayerBar(
                        state = readingPlayerState,
                        onPrevious = readingPlayer::playPrevious,
                        onTogglePlayPause = readingPlayer::togglePlayPause,
                        onNext = readingPlayer::playNext,
                        onStop = readingPlayer::stop,
                        onOpenQueue = { showReadingQueue = true },
                        onPlaybackSpeedChange = { speed ->
                            saveReadingPlaybackSpeed(settings, speed)
                            readingPlayer.setPlaybackSpeed(speed)
                        },
                        onBackgroundInteraction = {
                            if (!isOnReadingDetail) isReadingPlayerExpandedByUser = false
                        },
                        modifier = Modifier
                            .onSizeChanged { readingPlayerHeightPx = it.height }
                            .graphicsLayer {
                                translationY = readingPlayerOverlayOffsetState.verticalOffsetPx
                            },
                    )
                }
            },
            floatingActionButtonPosition = FabPosition.Center,
            bottomBar = {
                if (showMainNavigationBar && navEntry != null) {
                    // 页面切换时重置底部导航栏可见状态（顶栏复用此信号，一并恢复）
                    LaunchedEffect(navEntry) {
                        isBottomBarVisible = true
                        autoHideTopBarScrollFraction = null
                    }
                    val currentBottomDestination = mainTabPages
                        .getOrNull(mainPagerState.targetPage)
                        ?.bottomDestination
                    AnimatedVisibility(
                        visible = showMainNavigation && (!autoHideBottomBar || isBottomBarVisible),
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
                                            isReadingPlayerExpandedByUser = false
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
                                    64.dp + bottomPadding,
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
                                            isReadingPlayerExpandedByUser = false
                                            if (currentBottomDestination?.let { it::class == destination::class } != true) {
                                                navigateTopLevel(destination)
                                            } else if (tapToScrollToTopEnabled) {
                                                scrollToTopTrigger++
                                            }
                                        },
                                        label = { Text(label) },
                                        alwaysShowLabel = true,
                                        colors = if (!isDarkTheme) {
                                            NavigationBarItemDefaults.colors().copy(
                                                selectedIndicatorColor =
                                                    MaterialTheme.colorScheme.secondaryContainer
                                                        .copy(alpha = 0.92f)
                                                        .compositeOver(MaterialTheme.colorScheme.secondary),
                                            )
                                        } else {
                                            NavigationBarItemDefaults.colors()
                                        },
                                        icon = {
                                            Icon(icon, contentDescription = label)
                                        },
                                        modifier = Modifier.padding(top = 4.dp).testTag(tag),
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
            CompositionLocalProvider(
                LocalArticleNavController provides navController,
                LocalNavigator provides Navigator(
                    onNavigate = { destination ->
                        navigate(destination)
                    },
                    onNavigateBack = navController::popBackStack,
                    onNavigateTopLevel = ::navigateTopLevel,
                ),
                LocalReadingPlayerOverlayPadding provides readingPlayerOverlayPadding,
                LocalReadingPlayerOverlayOffsetState provides readingPlayerOverlayOffsetState,
            ) {
                NavHost(
                    navController,
                    modifier = Modifier.pointerInput(Unit) {
                        while (true) {
                            awaitPointerEventScope {
                                awaitFirstDown(
                                    requireUnconsumed = false,
                                    pass = PointerEventPass.Initial,
                                )
                                while (
                                    awaitPointerEvent(PointerEventPass.Final)
                                        .changes
                                        .any { it.pressed }
                                ) {
                                    // 等手势完成后再重组，避免取消同一次背景点击或滚动。
                                }
                            }
                            if (shouldCompactPlayerOnBackgroundInteraction) {
                                delay(100)
                                isReadingPlayerExpandedByUser = false
                            }
                        }
                    },
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
                        CompositionLocalProvider(
                            LocalAutoHideTopBarVisible provides (!autoHideTopBar || isBottomBarVisible),
                            LocalAutoHideTopBarScrollFraction provides autoHideTopBarScrollFraction,
                            LocalAutoHideTopBarHeightChanged provides { height -> autoHideTopBarHeightPx = height.toFloat() },
                        ) {
                            MainTabsPager(
                                pagerState = mainPagerState,
                                pages = mainTabPages,
                                scrollToTopTrigger = scrollToTopTrigger,
                                innerPadding = innerPadding,
                                bottomBarBackdrop = bottomBarBackdrop,
                                collectionDirectBrowseEnabled = collectionDirectBrowseEnabled,
                                showHomeTopActions = showHomeTopActions,
                            )
                        }
                    }
                    composable<Login> {
                        LoginScreen(
                            onLoginComplete = { navController.popBackStack() },
                            onOpenTelemetrySettings = {
                                navController.navigate(Account.SystemAndUpdateSettings("allowTelemetry"))
                            },
                        )
                    }
                    composable<Question> { navEntry ->
                        val question: Question = navEntry.toRoute()
                        if (useMiuix) MiuixQuestionScreen(question) else QuestionScreen(question)
                    }
                    composable<Topic> { navEntry ->
                        TopicScreen(navEntry.toRoute())
                    }
                    composable<WriteAnswer> { navEntry ->
                        val args: WriteAnswer = navEntry.toRoute()
                        WriteAnswerScreen(args)
                    }
                    composable<WritePin> { navEntry ->
                        WritePinScreen(navEntry.toRoute())
                    }
                    composable<Article>(
                        typeMap = mapOf(typeOf<ArticleType>() to ArticleTypeNavType),
                        enterTransition = articleEnterTransition,
                        exitTransition = articleExitTransition,
                    ) { navEntry ->
                        val article: Article = navEntry.toRoute()
                        articleContent(article, navEntry)
                    }
                    composable<HotList> {
                        if (useMiuix) MiuixHotListScreen(innerPadding) else HotListScreen(innerPadding)
                    }
                    composable<Follow> {
                        if (useMiuix) {
                            MiuixFollowTopLevelPage(
                                scrollToTopTrigger = scrollToTopTrigger,
                                innerPadding = innerPadding,
                                parentPagerState = mainPagerState,
                            )
                        } else {
                            FollowScreen(
                                scrollToTopTrigger = scrollToTopTrigger,
                                innerPadding = innerPadding,
                                parentPagerState = mainPagerState,
                            )
                        }
                    }
                    composable<Daily> {
                        if (useMiuix) MiuixDailyScreen() else DailyScreen()
                    }
                    composable<History> {
                        if (useMiuix) MiuixLocalHistoryScreen(innerPadding) else LegacyLocalHistoryScreen(innerPadding)
                    }
                    composable<OnlineHistory> {
                        // 作为独立路由 push 进来（账号页/面板的"浏览历史"入口），显示返回箭头。
                        if (useMiuix) MiuixOnlineHistoryScreen(showBackButton = true) else OnlineHistoryScreen()
                    }
                    composable<Account> {
                        if (useMiuix) MiuixAccountSettingScreen(innerPadding) else AccountSettingScreen(innerPadding)
                    }
                    composable<Search>(
                        enterTransition = {
                            if (initialState.destination.hasRoute<Search>()) {
                                EnterTransition.None
                            } else {
                                fadeIn(animationSpec = tween(durationMillis = 240)) +
                                    slideInVertically(animationSpec = tween(durationMillis = 280)) { it / 16 } +
                                    scaleIn(
                                        animationSpec = tween(durationMillis = 280),
                                        initialScale = 0.985f,
                                    )
                            }
                        },
                        popExitTransition = {
                            if (targetState.destination.hasRoute<Search>()) {
                                ExitTransition.None
                            } else {
                                fadeOut(animationSpec = tween(durationMillis = 180)) +
                                    slideOutVertically(animationSpec = tween(durationMillis = 220)) { it / 20 } +
                                    scaleOut(
                                        animationSpec = tween(durationMillis = 220),
                                        targetScale = 0.985f,
                                    )
                            }
                        },
                    ) { navEntry ->
                        val search: Search = navEntry.toRoute()
                        if (useMiuix) MiuixSearchScreen(search) else SearchScreen(search)
                    }
                    composable<Collections> { navEntry ->
                        val data: Collections = navEntry.toRoute()
                        if (useMiuix) {
                            MiuixCollectionScreen(data.userToken)
                        } else {
                            CollectionScreen(
                                urlToken = data.userToken,
                                contentPadding = innerPadding,
                            )
                        }
                    }
                    composable<CollectionContent> { navEntry ->
                        val content: CollectionContent = navEntry.toRoute()
                        if (useMiuix) {
                            MiuixCollectionContentScreen(content.collectionId)
                        } else {
                            CollectionContentScreen(content.collectionId)
                        }
                    }
                    composable<Person> { navEntry ->
                        val person: Person = navEntry.toRoute()
                        if (useMiuix) MiuixPeopleScreen(person) else PeopleScreen(person)
                    }
                    composable<Pin> { navEntry ->
                        val pin: Pin = navEntry.toRoute()
                        if (useMiuix) MiuixPinScreen(pin) else PinScreen(pin)
                    }
                    composable<Account.RecommendSettings.Blocklist> {
                        if (useMiuix) {
                            MiuixBlocklistSettingsScreen(blocklistSettingsNlpContent)
                        } else {
                            BlocklistSettingsScreen(blocklistSettingsNlpContent)
                        }
                    }
                    composable<Account.RecommendSettings.BlockedFeedHistory> {
                        if (useMiuix) MiuixBlockedFeedHistoryScreen() else BlockedFeedHistoryScreen()
                    }
                    composable<Notification> {
                        if (useMiuix) MiuixNotificationScreen() else NotificationScreen()
                    }
                    composable<Notification.Entry> { navEntry ->
                        val entry: Notification.Entry = navEntry.toRoute()
                        if (useMiuix) {
                            MiuixNotificationTimelineScreen(entry.entryName, entry.title)
                        } else {
                            NotificationTimelineScreen(entry.entryName, entry.title)
                        }
                    }
                    composable<Notification.Invitations> {
                        if (useMiuix) {
                            MiuixNotificationTimelineScreen("invite", "邀请回答")
                        } else {
                            NotificationTimelineScreen("invite", "邀请回答")
                        }
                    }
                    composable<Notification.Message> { navEntry ->
                        val message: Notification.Message = navEntry.toRoute()
                        if (useMiuix) MiuixPrivateMessageScreen(message) else PrivateMessageScreen(message)
                    }
                    composable<Notification.NotificationSettings> { navEntry ->
                        val setting = navEntry.toRoute<Notification.NotificationSettings>().setting
                        // miuix 版尚未做设置项高亮定位，深链参数只对 M3 生效。
                        if (useMiuix) MiuixNotificationSettingsScreen() else NotificationSettingsScreen(setting = setting)
                    }
                    composable<SentenceSimilarityTest> {
                        sentenceSimilarityContent()
                    }
                    composable<Account.AppearanceSettings> { navEntry ->
                        val args = navEntry.toRoute<Account.AppearanceSettings>()
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
                    composable<Account.RecommendSettings> { navEntry ->
                        val args = navEntry.toRoute<Account.RecommendSettings>()
                        if (useMiuix) {
                            MiuixContentFilterSettingsScreen(args.setting)
                        } else {
                            ContentFilterSettingsScreen(args.setting)
                        }
                    }
                    composable<Account.About> {
                        if (useMiuix) MiuixAboutScreen(innerPadding) else AboutScreen(innerPadding)
                    }
                    composable<Account.IdentityManagement> {
                        if (useMiuix) MiuixIdentityManagementScreen() else IdentityManagementScreen()
                    }
                    composable<Account.SystemAndUpdateSettings> { navEntry ->
                        val setting = navEntry.toRoute<Account.SystemAndUpdateSettings>().setting
                        // miuix 版尚未做设置项高亮定位，深链参数只对 M3 生效。
                        if (useMiuix) MiuixSystemAndUpdateSettingsScreen() else SystemAndUpdateSettingsScreen(setting = setting)
                    }
                    composable<Account.ReadingSettings> {
                        if (useMiuix) MiuixReadingSettingsScreen() else ReadingSettingsScreen()
                    }
                    composable<Account.SettingsSearch> {
                        if (useMiuix) MiuixSettingsSearchScreen() else SettingsSearchScreen()
                    }
                    composable<Account.OpenSourceLicenses> {
                        if (useMiuix) MiuixOpenSourceLicensesScreen() else OpenSourceLicensesScreen()
                    }
                    composable<Account.DeveloperSettings> {
                        if (useMiuix) MiuixDeveloperSettingsScreen() else DeveloperSettingsScreen()
                    }
                    composable<Account.DeveloperSettings.ColorScheme> {
                        ColorSchemeScreen()
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = readingPlayerState.hasSession && !isReadingPlayerExpanded,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(160)),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                CompactReadingPlayerButton(
                    state = readingPlayerState,
                    onExpand = { isReadingPlayerExpandedByUser = true },
                )
            }
        }
    }

    if (showReadingQueue && readingPlayerState.hasSession) {
        ReadingQueueSheet(
            state = readingPlayerState,
            onDismissRequest = {
                showReadingQueue = false
                if (!isOnReadingDetail) isReadingPlayerExpandedByUser = false
            },
            onItemClick = { index, item ->
                previousReadingItemKey = item.key
                if (index != readingPlayerState.currentIndex) {
                    readingPlayer.playAt(index)
                }
                showReadingQueue = false
                val destination = item.toDestination(readingPlayerState.sourceId)
                val currentDestination = when {
                    navEntry?.destination?.hasRoute<Article>() == true -> runCatching {
                        navEntry?.toRoute<Article>()
                    }.getOrNull()
                    navEntry?.destination?.hasRoute<Pin>() == true -> runCatching {
                        navEntry?.toRoute<Pin>()
                    }.getOrNull()
                    navEntry?.destination?.hasRoute<Question>() == true -> runCatching {
                        navEntry?.toRoute<Question>()
                    }.getOrNull()
                    else -> null
                }
                if (currentDestination != destination) {
                    if (currentDestination != null) {
                        navController.popBackStack()
                    }
                    navigate(destination)
                }
            },
            onOpenSettings = {
                showReadingQueue = false
                isReadingPlayerExpandedByUser = false
                if (navEntry?.destination?.hasRoute<Account.ReadingSettings>() != true) {
                    navigate(Account.ReadingSettings)
                }
            },
        )
    }
}

/**
 * 渲染可配置底部导航主壳内的页面。
 *
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
    collectionDirectBrowseEnabled: Boolean,
    showHomeTopActions: Boolean,
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .then(if (bottomBarBackdrop != null) Modifier.layerBackdrop(bottomBarBackdrop) else Modifier),
        pageNestedScrollConnection = NoOpPagerNestedScrollConnection,
    ) { pageIndex ->
        val useMiuix = ThemeManager.getThemeStyle() == ThemeStyle.Miuix
        val page = pages.getOrNull(pageIndex) ?: return@HorizontalPager
        when (page) {
            MainTabPage.HomePage -> if (useMiuix) {
                MiuixHomeScreen(
                    scrollToTopTrigger = scrollToTopTrigger,
                    innerPadding = innerPadding,
                )
            } else {
                HomeScreen(
                    scrollToTopTrigger = scrollToTopTrigger,
                    innerPadding = innerPadding,
                    showTopActions = showHomeTopActions,
                    isActive = pagerState.currentPage == pageIndex,
                )
            }
            MainTabPage.FollowPage -> if (useMiuix) {
                MiuixFollowTopLevelPage(
                    scrollToTopTrigger = scrollToTopTrigger,
                    innerPadding = innerPadding,
                    parentPagerState = pagerState,
                )
            } else {
                FollowScreen(
                    scrollToTopTrigger = scrollToTopTrigger,
                    innerPadding = innerPadding,
                    parentPagerState = pagerState,
                    isActive = pagerState.currentPage == pageIndex,
                )
            }
            MainTabPage.HotListPage -> if (useMiuix) {
                MiuixHotListScreen(
                    innerPadding = innerPadding,
                    scrollToTopTrigger = scrollToTopTrigger,
                    isActive = pagerState.currentPage == pageIndex,
                )
            } else {
                HotListScreen(
                    innerPadding = innerPadding,
                    scrollToTopTrigger = scrollToTopTrigger,
                    isActive = pagerState.currentPage == pageIndex,
                )
            }
            MainTabPage.DailyPage -> if (useMiuix) {
                MiuixDailyScreen(
                    scrollToTopTrigger = scrollToTopTrigger,
                    isActive = pagerState.currentPage == pageIndex,
                )
            } else {
                DailyScreen(
                    scrollToTopTrigger = scrollToTopTrigger,
                    isActive = pagerState.currentPage == pageIndex,
                )
            }
            MainTabPage.OnlineHistoryPage -> if (useMiuix) {
                MiuixOnlineHistoryScreen(
                    scrollToTopTrigger = scrollToTopTrigger,
                    isActive = pagerState.currentPage == pageIndex,
                )
            } else {
                OnlineHistoryScreen(
                    scrollToTopTrigger = scrollToTopTrigger,
                    isActive = pagerState.currentPage == pageIndex,
                )
            }
            MainTabPage.MyCollectionsPage -> MyCollectionsTopLevelPage(
                scrollToTopTrigger = scrollToTopTrigger,
                innerPadding = innerPadding,
                collectionDirectBrowseEnabled = collectionDirectBrowseEnabled,
                isActive = pagerState.currentPage == pageIndex,
            )
            MainTabPage.AccountPage -> if (useMiuix) {
                MiuixAccountSettingScreen(innerPadding)
            } else {
                AccountSettingScreen(
                    innerPadding = innerPadding,
                    isActive = pagerState.currentPage == pageIndex,
                )
            }
        }
    }
}

@Composable
private fun MyCollectionsTopLevelPage(
    scrollToTopTrigger: Int,
    innerPadding: PaddingValues,
    collectionDirectBrowseEnabled: Boolean,
    isActive: Boolean,
) {
    val account = rememberAccountSettingsAccountState().value
    when {
        // 直达浏览是上游新增的收藏夹模式，miuix 尚未复刻，先共用 M3 实现。
        collectionDirectBrowseEnabled -> CollectionBrowseScreen(
            urlToken = account.urlToken,
            contentPadding = innerPadding,
            showBackButton = false,
            scrollToTopTrigger = scrollToTopTrigger,
            isActive = isActive,
        )
        ThemeManager.getThemeStyle() == ThemeStyle.Miuix -> MiuixCollectionScreen(
            urlToken = account.urlToken,
            showBackButton = false,
            innerPadding = innerPadding,
        )
        else -> CollectionScreen(
            urlToken = account.urlToken,
            contentPadding = innerPadding,
            showBackButton = false,
            isActive = isActive,
        )
    }
}

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
