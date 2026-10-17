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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
import com.github.zly2006.zhihu.navigation.EmptyDetail
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
import com.github.zly2006.zhihu.navigation.Video
import com.github.zly2006.zhihu.navigation.WriteAnswer
import com.github.zly2006.zhihu.navigation.WritePin
import com.github.zly2006.zhihu.navigation.loginNavigationRequestFlow
import com.github.zly2006.zhihu.platform.PlatformBackHandler
import com.github.zly2006.zhihu.platform.platformName
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.reading.rememberReadingPlayerController
import com.github.zly2006.zhihu.reading.saveReadingPlaybackSpeed
import com.github.zly2006.zhihu.ui.components.CompactReadingPlayerButton
import com.github.zly2006.zhihu.ui.components.NoOpPagerNestedScrollConnection
import com.github.zly2006.zhihu.ui.components.ReadingPlayerBar
import com.github.zly2006.zhihu.ui.components.ReadingQueueSheet
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

private sealed interface ReadingSlotTarget {
    data class ArticlePage(
        val article: Article,
        val entry: NavBackStackEntry,
    ) : ReadingSlotTarget

    data class PinPage(
        val pin: Pin,
    ) : ReadingSlotTarget
}

/**
 * Zhihu++ 的共享应用主壳。
 *
 * 这个 composable 是顶层体验的唯一所有者：渲染可配置底部导航栏，承载横向主 tab pager，向子页面提供 [LocalNavigator]，
 * 并注册跨平台共享的 typed [NavDestination] route。设计上把顶层 tab 收在 [MainTabs] 内部，而不是把每个 tab
 * 都作为独立 NavHost 页面 push，这样 tab 重选、回到顶部、顶/底栏自动隐藏和持久化 tab 选择都能使用同一套状态模型。
 *
 * 用户可见的主壳设置通过 [preferenceState] 流入。设置页退出时只 reload 这份状态，不重建 NavHost，从而在应用底栏和主题相关变更时
 * 保留已加载页面、返回栈和滚动位置。
 *
 * [navController] 是主内容 NavHost 的控制器；[navigate] 负责把目标交给平台主导航入口。
 * [navigateContent] 可将目标交给指定的内容控制器，用于横屏列表/详情布局的右侧 NavHost；未提供平台实现时会回退到 [navigate]。
 *
 * @param navController 主内容 NavHost；顶层 tab 和默认内容页使用它维护返回栈。
 * @param navigate 平台主导航入口，负责处理顶层 tab 与外部导航。
 * @param navigateContent 指定内容 NavHost 的导航入口；大屏列表/详情布局通过它打开右侧内容。
 */
@OptIn(ExperimentalFoundationApi::class)
@Suppress("RestrictedApi")
@Composable
fun ZhihuMain(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    mainTabNavigationTarget: TopLevelDestination?,
    navigate: (NavDestination) -> Unit,
    navigateContent: (NavDestination, NavHostController) -> Unit = { destination, _ -> navigate(destination) },
    enableLandscapeListDetail: Boolean = false,
    isListDetailCapable: Boolean = true,
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
    articlePopExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? = null,
    articlePopEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? = null,
) = BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val containerWidth = maxWidth
    val isLargeLandscape = canShowLandscapeListDetail(
        enabled = enableLandscapeListDetail,
        deviceSupportsListDetail = isListDetailCapable,
        preferenceEnabled = preferenceState.landscapeListDetailEnabled,
        width = containerWidth,
        height = maxHeight,
    )
    val bottomPadding = ScaffoldDefaults.contentWindowInsets.asPaddingValues().calculateBottomPadding()
    val duo3HomeAccount = preferenceState.duo3HomeAccount
    val tapToScrollToTopEnabled = preferenceState.tapToScrollToTopEnabled
    val autoHideBottomBar = preferenceState.autoHideBottomBar
    val collectionDirectBrowseEnabled = preferenceState.collectionDirectBrowseEnabled
    val selectedBottomBarItemKeys = preferenceState.selectedBottomBarItemKeys
    val startDestination = preferenceState.startDestination
    val reloadBottomBarPreferences = preferenceState::reload
    val readingPlayer = rememberReadingPlayerController()
    val readingPlayerState by readingPlayer.state
    val settings = rememberSettingsStore()

    /** 右侧详情独立持有返回栈；栏内导航保留上级，从左侧重新选项时清空。 */
    val paneStateHolder = rememberSaveableStateHolder()
    val detailNavController = rememberNavController()

    val contentSlotStores: ContentViewModelStores = viewModel()
    val contentSlots = remember(contentSlotStores) {
        ContentSlotRegistry(contentSlotStores) { entryId ->
            navController.currentBackStack.value.any { it.id == entryId } ||
                detailNavController.currentBackStack.value.any { it.id == entryId }
        }
    }
    val mainBackStack by navController.currentBackStack.collectAsState()
    val detailBackStack by detailNavController.currentBackStack.collectAsState()
    val liveContentKeys = remember(mainBackStack, detailBackStack) {
        (mainBackStack + detailBackStack).mapNotNull { it.contentIdentityOrNull() }.toSet()
    }
    LaunchedEffect(contentSlots, liveContentKeys) {
        delay(CONTENT_SLOT_RELEASE_DELAY_MILLIS)
        contentSlots.dropMissing(liveContentKeys)
    }
    val detailEntry by detailNavController.currentBackStackEntryAsState()
    val selectedContentDestination = detailEntry.readingDestinationOrNull()
    val hasOpenSecondaryDetail = detailEntry?.destination?.hasRoute<EmptyDetail>() == false
    var showReadingQueue by remember { mutableStateOf(false) }
    var isReadingPlayerExpandedByUser by remember { mutableStateOf(false) }
    var readingPlayerHeightPx by remember { mutableIntStateOf(0) }
    val readingPlayerOverlayOffsetState = remember { ReadingPlayerOverlayOffsetState() }
    val density = LocalDensity.current
    val currentOnMainTabDestinationChange by rememberUpdatedState(onCurrentMainTabDestinationChange)

    val navEntry by navController.currentBackStackEntryAsState()
    val showMainNavigation = navEntry?.destination?.hasRoute<MainTabs>() == true
    val isListPaneContext = navEntry.isListPaneDestination()
    val showListDetail = isLargeLandscape && isListPaneContext
    val showDetailPane = hasOpenSecondaryDetail &&
        (showListDetail || (isListPaneContext && navEntry.readingDestinationOrNull() == null))
    val hasOpenDetail = showDetailPane && selectedContentDestination != null
    val useSecondaryContentNavigation = showListDetail
    // isLargeLandscape 来自 BoxWithConstraints，只在测量阶段更新；同一帧里既改窗口尺寸又切播放项时，
    // LaunchedEffect 会拿着上一帧的模式按窄屏语义导航（把主栈顶的列表页弹掉）。等一帧后再决定归属时，
    // 必须读这两个"最新值"，而不是启动 effect 那次组合捕获的值。
    val latestIsLargeLandscape by rememberUpdatedState(isLargeLandscape)
    val latestUseSecondaryContentNavigation by rememberUpdatedState(useSecondaryContentNavigation)
    LaunchedEffect(navEntry, detailEntry) {
        contentSlots.claim(navEntry)
        contentSlots.claim(detailEntry)
    }
    val detailLifecycleOwner = rememberLifecycleOwner(
        maxLifecycle = if (showListDetail || showDetailPane) Lifecycle.State.RESUMED else Lifecycle.State.CREATED,
    )
    val listLifecycleOwner = rememberLifecycleOwner(
        maxLifecycle = if (showDetailPane && !showListDetail) Lifecycle.State.CREATED else Lifecycle.State.RESUMED,
    )

    fun detailPaneContentDepth(): Int = detailNavController.currentBackStack.value.count {
        it.destination !is NavGraph && !it.destination.hasRoute<EmptyDetail>()
    }

    fun clearDetailPaneContent() {
        var contentLayers = detailPaneContentDepth()
        while (contentLayers > 0 && detailNavController.popBackStack()) {
            contentLayers--
        }
    }

    fun claimContentHost(controller: NavHostController) {
        contentSlots.claim(controller.currentBackStackEntry)
    }

    val readingSlotContent: @Composable (Any?) -> Unit = { slotTarget ->
        when (val target = slotTarget as ReadingSlotTarget) {
            is ReadingSlotTarget.ArticlePage -> articleContent(target.article, target.entry)
            is ReadingSlotTarget.PinPage -> PinScreen(target.pin)
        }
    }

    fun openListDetail(destination: NavDestination) {
        clearDetailPaneContent()
        detailNavController.popBackStack<EmptyDetail>(inclusive = true)
        navigateContent(destination, detailNavController)
        claimContentHost(detailNavController)
    }

    fun resetDetailPane() {
        clearDetailPaneContent()
        detailNavController.navigate(EmptyDetail) {
            launchSingleTop = true
        }
    }

    fun openReadingDestination(destination: NavDestination, onlyIfReading: Boolean = false) {
        val current = if (hasOpenDetail) selectedContentDestination else navEntry.readingDestinationOrNull()
        if (current == destination || (onlyIfReading && current == null)) return
        val opensInDetail = latestIsLargeLandscape &&
            latestUseSecondaryContentNavigation &&
            destination.isDetailPaneDestination()
        if (hasOpenDetail) {
            if (opensInDetail) {
                detailNavController.popBackStack()
            } else {
                resetDetailPane()
            }
        } else if (current != null && !opensInDetail) {
            navController.popBackStack()
        }
        if (opensInDetail) {
            navigateContent(destination, detailNavController)
            claimContentHost(detailNavController)
        } else {
            navigate(destination)
            claimContentHost(navController)
        }
    }

    fun popDetailOrFallThrough() {
        if (detailPaneContentDepth() > 1) {
            detailNavController.popBackStack()
        } else if (navController.currentBackStack.value.count { it.destination !is NavGraph } > 1) {
            navController.popBackStack()
        } else {
            resetDetailPane()
        }
    }
    val isOnReadingDetail = hasOpenDetail ||
        navEntry?.destination?.hasRoute<Article>() == true ||
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
            // 等本帧布局落定：播放项切换可能与窗口尺寸变化同帧，归属判断要用测量后的模式。
            withFrameNanos { }
            openReadingDestination(currentItem.toDestination(readingPlayerState.sourceId), onlyIfReading = true)
        }
    }

    // 离开文章页时恢复系统状态栏（只在实际切换时触发）
    val isOnArticle = (hasOpenDetail && detailEntry?.destination?.hasRoute<Article>() == true) ||
        navEntry?.destination?.hasRoute<Article>() == true
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
    val isAccountSurface = currentMainTabDestination == Account

    PlatformBackHandler(enabled = navEntry != null && !showMainNavigation && !showDetailPane) {
        navController.popBackStack()
    }

    fun navigateTopLevel(destination: TopLevelDestination) {
        if (destination == Account && hasOpenDetail) {
            resetDetailPane()
        }
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

    LaunchedEffect(currentMainTabDestination, navEntry) {
        if (isAccountSurface && hasOpenDetail) {
            // A reading detail belongs to the feed that opened it. Do not carry it into
            // the account split pane when the user switches tabs.
            resetDetailPane()
        }
    }

    PlatformBackHandler(
        showMainNavigation &&
            mainPagerState.currentPage != 0 &&
            !showDetailPane,
    ) {
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

    var listPaneRatio by remember {
        mutableFloatStateOf(
            settings.getFloat(LANDSCAPE_LIST_PANE_RATIO_KEY, DEFAULT_LIST_PANE_RATIO),
        )
    }

    @Composable
    fun DetailPane(modifier: Modifier) {
        CompositionLocalProvider(
            LocalLifecycleOwner provides detailLifecycleOwner,
            LocalArticleNavController provides detailNavController,
            LocalNavigator provides Navigator(
                onNavigate = { destination ->
                    if (
                        destination is Video || destination.isDetailPaneDestination() || destination.isAccountDetailDestination()
                    ) {
                        navigateContent(destination, detailNavController)
                        claimContentHost(detailNavController)
                    } else {
                        navigate(destination)
                        claimContentHost(navController)
                    }
                },
                onNavigateBack = ::popDetailOrFallThrough,
                onNavigateTopLevel = ::navigateTopLevel,
            ),
            LocalSelectedContentDestination provides selectedContentDestination,
            LocalReadingPlayerOverlayPadding provides readingPlayerOverlayPadding,
            LocalReadingPlayerOverlayOffsetState provides readingPlayerOverlayOffsetState,
        ) {
            NavHost(
                navController = detailNavController,
                startDestination = EmptyDetail,
                modifier = modifier.testTag("detail_pane"),
                enterTransition = { slideInHorizontally(tween(300)) { it } },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) },
            ) {
                composable<EmptyDetail> {
                    EmptyDetailPane()
                }
                composable<Article>(
                    typeMap = mapOf(typeOf<ArticleType>() to ArticleTypeNavType),
                    enterTransition = articleEnterTransition,
                    exitTransition = articleExitTransition,
                    popEnterTransition = articlePopEnterTransition,
                    popExitTransition = articlePopExitTransition,
                ) { entry ->
                    ContentSlotHost(contentSlots, entry, ReadingSlotTarget.ArticlePage(entry.toRoute(), entry), readingSlotContent)
                }
                composable<Pin> { entry ->
                    ContentSlotHost(contentSlots, entry, ReadingSlotTarget.PinPage(entry.toRoute()), readingSlotContent)
                }
                accountSettings(reloadBottomBarPreferences, blocklistSettingsNlpContent, contentSlots)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        val listPaneWidth = if (showListDetail) {
            normalizedListPaneWidth(containerWidth, listPaneRatio)
        } else {
            containerWidth
        }
        val listPaneModifier = if (showListDetail) {
            Modifier.width(listPaneWidth).fillMaxHeight().align(Alignment.CenterStart)
        } else {
            Modifier.fillMaxSize()
        }
        LaunchedEffect(isLargeLandscape, navEntry, detailEntry) {
            if (!isLargeLandscape) {
                if (!hasOpenDetail) return@LaunchedEffect
                val reading = selectedContentDestination
                val entries = navController.currentBackStack.value.filter { it.destination !is NavGraph }
                val listRootIndex = entries.indexOfFirst { it.destination.hasRoute<MainTabs>() }
                val laterPages = if (listRootIndex < 0) {
                    emptyList()
                } else {
                    entries.drop(listRootIndex + 1).map { it.toListPaneDestinationOrNull() }
                }
                if (listRootIndex >= 0 && laterPages.none { it == null }) {
                    navController.popBackStack<MainTabs>(inclusive = false)
                    navController.navigate(reading)
                    claimContentHost(navController)
                    laterPages.filterNotNull().forEach {
                        navController.navigate(it)
                        claimContentHost(navController)
                    }
                    resetDetailPane()
                }
                return@LaunchedEffect
            }
            val entries = navController.currentBackStack.value.filter { it.destination !is NavGraph }
            val isReadingEntry = { entry: NavBackStackEntry ->
                entry.destination.hasRoute<Article>() || entry.destination.hasRoute<Pin>()
            }
            val readingIndex = entries.indexOfLast { isReadingEntry(it) }
            val plainPage = entries.any { !it.isListPaneDestination() && !isReadingEntry(it) }
            val listIndex = entries.indexOfLast {
                it.isListPaneDestination() && !it.destination.hasRoute<MainTabs>()
            }
            if (readingIndex < 0 || plainPage) return@LaunchedEffect
            if (entries.none { it.destination.hasRoute<MainTabs>() }) return@LaunchedEffect
            val reading = entries[readingIndex].readingDestinationOrNull() ?: return@LaunchedEffect
            val keepList = entries.getOrNull(listIndex)?.toListPaneDestinationOrNull()
            if (detailPaneContentDepth() == 0) openListDetail(reading)
            navController.popBackStack<MainTabs>(inclusive = false)
            if (keepList != null) {
                navController.navigate(keepList)
                claimContentHost(navController)
            }
        }
        if (showListDetail || !showDetailPane) {
            paneStateHolder.SaveableStateProvider("list") {
                Scaffold(
                    modifier = listPaneModifier
                        .testTag("list_pane")
                        .nestedScroll(bottomBarScrollConnection),
                    bottomBar = {
                        if (showMainNavigationBar && navEntry != null) {
                            // 页面切换时重置底部导航栏可见状态
                            LaunchedEffect(navEntry) { isBottomBarVisible = true }
                            val currentBottomDestination = mainTabPages
                                .getOrNull(mainPagerState.targetPage)
                                ?.bottomDestination
                            AnimatedVisibility(
                                visible = showMainNavigation && (!autoHideBottomBar || isBottomBarVisible),
                                enter = slideInVertically(tween(200)) { it },
                                exit = slideOutVertically(tween(200)) { it },
                            ) {
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
                    },
                ) { innerPadding ->
                    CompositionLocalProvider(
                        LocalLifecycleOwner provides listLifecycleOwner,
                        LocalArticleNavController provides navController,
                        LocalNavigator provides Navigator(
                            onNavigate = { destination ->
                                if (
                                    useSecondaryContentNavigation &&
                                    (
                                        destination.isDetailPaneDestination() ||
                                            (showListDetail && currentMainTabDestination == Account && destination.isAccountDetailDestination())
                                    )
                                ) {
                                    openListDetail(destination)
                                } else {
                                    navigate(destination)
                                    claimContentHost(navController)
                                }
                            },
                            onNavigateBack = navController::popBackStack,
                            onNavigateTopLevel = ::navigateTopLevel,
                        ),
                        LocalReadingPlayerOverlayPadding provides readingPlayerOverlayPadding,
                        LocalReadingPlayerOverlayOffsetState provides readingPlayerOverlayOffsetState,
                        LocalSelectedContentDestination provides selectedContentDestination,
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
                                MainTabsPager(
                                    pagerState = mainPagerState,
                                    pages = mainTabPages,
                                    scrollToTopTrigger = scrollToTopTrigger,
                                    innerPadding = innerPadding,
                                    collectionDirectBrowseEnabled = collectionDirectBrowseEnabled,
                                    showHomeTopActions = showHomeTopActions,
                                )
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
                                ContentSlotHost(contentSlots, navEntry, Unit) { QuestionScreen(question) }
                            }
                            composable<Topic> { navEntry ->
                                ContentSlotHost(contentSlots, navEntry, Unit) { TopicScreen(navEntry.toRoute()) }
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
                                popEnterTransition = articlePopEnterTransition,
                                popExitTransition = articlePopExitTransition,
                            ) { navEntry ->
                                val article: Article = navEntry.toRoute()
                                ContentSlotHost(contentSlots, navEntry, ReadingSlotTarget.ArticlePage(article, navEntry), readingSlotContent)
                            }
                            composable<HotList> {
                                HotListScreen(innerPadding)
                            }
                            composable<Follow> {
                                FollowScreen(
                                    scrollToTopTrigger = scrollToTopTrigger,
                                    innerPadding = innerPadding,
                                    parentPagerState = mainPagerState,
                                )
                            }
                            composable<Daily> {
                                DailyScreen()
                            }
                            composable<History> {
                                ContentSlotHost(contentSlots, it, Unit) { LegacyLocalHistoryScreen(innerPadding) }
                            }
                            composable<OnlineHistory> {
                                OnlineHistoryScreen()
                            }
                            composable<Account> {
                                AccountSettingScreen(innerPadding)
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
                                ContentSlotHost(contentSlots, navEntry, Unit) { SearchScreen(search) }
                            }
                            composable<Collections> { navEntry ->
                                val data: Collections = navEntry.toRoute()
                                ContentSlotHost(contentSlots, navEntry, Unit) {
                                    CollectionScreen(
                                        urlToken = data.userToken,
                                        contentPadding = innerPadding,
                                    )
                                }
                            }
                            composable<CollectionContent> { navEntry ->
                                val content: CollectionContent = navEntry.toRoute()
                                ContentSlotHost(contentSlots, navEntry, Unit) { CollectionContentScreen(content.collectionId) }
                            }
                            composable<Person> { navEntry ->
                                val person: Person = navEntry.toRoute()
                                ContentSlotHost(contentSlots, navEntry, Unit) { PeopleScreen(person) }
                            }
                            composable<Pin> { navEntry ->
                                val pin: Pin = navEntry.toRoute()
                                ContentSlotHost(contentSlots, navEntry, ReadingSlotTarget.PinPage(pin), readingSlotContent)
                            }
                            accountSettings(reloadBottomBarPreferences, blocklistSettingsNlpContent, contentSlots)
                            composable<Notification> {
                                ContentSlotHost(contentSlots, it, Unit) { NotificationScreen() }
                            }
                            composable<Notification.Entry> { navEntry ->
                                val entry: Notification.Entry = navEntry.toRoute()
                                ContentSlotHost(contentSlots, navEntry, Unit) {
                                    NotificationTimelineScreen(entry.entryName, entry.title)
                                }
                            }
                            composable<Notification.Invitations> {
                                ContentSlotHost(contentSlots, it, Unit) { NotificationTimelineScreen("invite", "邀请回答") }
                            }
                            composable<Notification.Message> { navEntry ->
                                PrivateMessageScreen(navEntry.toRoute())
                            }
                            composable<Notification.NotificationSettings> { navEntry ->
                                NotificationSettingsScreen(
                                    setting = navEntry.toRoute<Notification.NotificationSettings>().setting,
                                )
                            }
                            composable<SentenceSimilarityTest> {
                                sentenceSimilarityContent()
                            }
                        }
                    }
                }
            }
        }

        if (showListDetail || showDetailPane || detailEntry == null) {
            paneStateHolder.SaveableStateProvider("detail") {
                // 尺寸变化只调整同一宿主的布局；隐藏时保存整个导航子树，独立弹窗也随页面离开组合。
                Row(if (showListDetail || showDetailPane) Modifier.fillMaxSize() else Modifier.size(0.dp)) {
                    if (showListDetail) {
                        Spacer(Modifier.width(listPaneWidth))
                        ListDetailDivider(
                            onDrag = { deltaPx ->
                                val usableWidthPx = with(density) {
                                    (containerWidth - LIST_DETAIL_DIVIDER_WIDTH).toPx()
                                }
                                if (usableWidthPx > 0f) {
                                    listPaneRatio = (listPaneRatio + deltaPx / usableWidthPx).coerceIn(0f, 1f)
                                }
                            },
                            onAdjustBy = { ratioDelta ->
                                listPaneRatio = (listPaneRatio + ratioDelta).coerceIn(0f, 1f)
                            },
                            onDragStopped = {
                                val normalizedWidth = normalizedListPaneWidth(containerWidth, listPaneRatio)
                                listPaneRatio = normalizedWidth.value /
                                    (containerWidth - LIST_DETAIL_DIVIDER_WIDTH).value
                                settings.putFloat(LANDSCAPE_LIST_PANE_RATIO_KEY, listPaneRatio)
                            },
                        )
                    }
                    DetailPane(Modifier.weight(1f).fillMaxSize())
                }
            }
        }

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = if (showListDetail && showDetailPane) listPaneWidth + LIST_DETAIL_DIVIDER_WIDTH else 0.dp,
                    bottom = bottomPadding + 16.dp + if (
                        !showDetailPane && showMainNavigation && showMainNavigationBar && (!autoHideBottomBar || isBottomBarVisible)
                    ) {
                        64.dp
                    } else {
                        0.dp
                    },
                ).fillMaxWidth(),
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
                openReadingDestination(item.toDestination(readingPlayerState.sourceId))
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
    collectionDirectBrowseEnabled: Boolean,
    showHomeTopActions: Boolean,
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        pageNestedScrollConnection = NoOpPagerNestedScrollConnection,
    ) { pageIndex ->
        val page = pages.getOrNull(pageIndex) ?: return@HorizontalPager
        when (page) {
            MainTabPage.HomePage -> HomeScreen(
                scrollToTopTrigger = scrollToTopTrigger,
                innerPadding = innerPadding,
                showTopActions = showHomeTopActions,
                isActive = pagerState.currentPage == pageIndex,
            )
            MainTabPage.FollowPage -> FollowScreen(
                scrollToTopTrigger = scrollToTopTrigger,
                innerPadding = innerPadding,
                parentPagerState = pagerState,
                isActive = pagerState.currentPage == pageIndex,
            )
            MainTabPage.HotListPage -> HotListScreen(
                innerPadding = innerPadding,
                scrollToTopTrigger = scrollToTopTrigger,
                isActive = pagerState.currentPage == pageIndex,
            )
            MainTabPage.DailyPage -> DailyScreen(
                scrollToTopTrigger = scrollToTopTrigger,
                isActive = pagerState.currentPage == pageIndex,
            )
            MainTabPage.OnlineHistoryPage -> OnlineHistoryScreen(
                scrollToTopTrigger = scrollToTopTrigger,
                isActive = pagerState.currentPage == pageIndex,
            )
            MainTabPage.MyCollectionsPage -> MyCollectionsTopLevelPage(
                scrollToTopTrigger = scrollToTopTrigger,
                innerPadding = innerPadding,
                collectionDirectBrowseEnabled = collectionDirectBrowseEnabled,
                isActive = pagerState.currentPage == pageIndex,
            )
            MainTabPage.AccountPage -> AccountSettingScreen(
                innerPadding = innerPadding,
                isActive = pagerState.currentPage == pageIndex,
            )
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
    if (collectionDirectBrowseEnabled) {
        CollectionBrowseScreen(
            urlToken = account.urlToken,
            contentPadding = innerPadding,
            showBackButton = false,
            scrollToTopTrigger = scrollToTopTrigger,
            isActive = isActive,
        )
    } else {
        CollectionScreen(
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

private fun NavBackStackEntry?.readingDestinationOrNull(): NavDestination? = when {
    this?.destination?.hasRoute<Article>() == true -> runCatching { toRoute<Article>() }.getOrNull()
    this?.destination?.hasRoute<Pin>() == true -> runCatching { toRoute<Pin>() }.getOrNull()
    this?.destination?.hasRoute<Question>() == true -> runCatching { toRoute<Question>() }.getOrNull()
    else -> null
}

private fun NavDestination.isAccountDetailDestination(): Boolean = when (this) {
    is Account.AppearanceSettings,
    Account.ReadingSettings,
    is Account.RecommendSettings,
    Account.RecommendSettings.Blocklist,
    Account.RecommendSettings.BlockedFeedHistory,
    Account.IdentityManagement,
    is Account.SystemAndUpdateSettings,
    Account.SettingsSearch,
    Account.OpenSourceLicenses,
    Account.DeveloperSettings,
    Account.DeveloperSettings.ColorScheme,
    -> true
    else -> false
}

private fun NavBackStackEntry?.toListPaneDestinationOrNull(): NavDestination? = when {
    this == null -> null
    destination.hasRoute<MainTabs>() -> MainTabs
    destination.hasRoute<Search>() -> runCatching { toRoute<Search>() }.getOrNull()
    destination.hasRoute<History>() -> History
    destination.hasRoute<Collections>() -> runCatching { toRoute<Collections>() }.getOrNull()
    destination.hasRoute<CollectionContent>() -> runCatching { toRoute<CollectionContent>() }.getOrNull()
    destination.hasRoute<Question>() -> runCatching { toRoute<Question>() }.getOrNull()
    destination.hasRoute<Person>() -> runCatching { toRoute<Person>() }.getOrNull()
    destination.hasRoute<Topic>() -> runCatching { toRoute<Topic>() }.getOrNull()
    destination.hasRoute<Notification.Entry>() -> runCatching { toRoute<Notification.Entry>() }.getOrNull()
    destination.hasRoute<Notification.Invitations>() -> Notification.Invitations
    destination.hasRoute<Notification>() -> Notification
    destination.hasRoute<Account.RecommendSettings.BlockedFeedHistory>() -> Account.RecommendSettings.BlockedFeedHistory
    else -> null
}

private fun NavBackStackEntry?.isListPaneDestination(): Boolean = toListPaneDestinationOrNull() != null

private fun NavGraphBuilder.accountSettings(
    reloadPreferences: () -> Unit,
    nlpContent: @Composable (onNavigateBack: () -> Unit) -> Unit,
    contentSlots: ContentSlotRegistry,
) {
    composable<Account.RecommendSettings.Blocklist> {
        BlocklistSettingsScreen(nlpContent)
    }
    composable<Account.RecommendSettings.BlockedFeedHistory> {
        ContentSlotHost(contentSlots, it, Unit) { BlockedFeedHistoryScreen() }
    }
    composable<Account.AppearanceSettings> { entry ->
        val args = entry.toRoute<Account.AppearanceSettings>()
        AppearanceSettingsScreen(
            setting = args.setting,
            onExit = reloadPreferences,
        )
    }
    composable<Account.RecommendSettings> { entry ->
        val args = entry.toRoute<Account.RecommendSettings>()
        ContentFilterSettingsScreen(args.setting)
    }
    composable<Account.IdentityManagement> {
        IdentityManagementScreen()
    }
    composable<Account.SystemAndUpdateSettings> { entry ->
        SystemAndUpdateSettingsScreen(
            setting = entry.toRoute<Account.SystemAndUpdateSettings>().setting,
        )
    }
    composable<Account.ReadingSettings> {
        ReadingSettingsScreen()
    }
    composable<Account.SettingsSearch> {
        SettingsSearchScreen()
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
