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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.filter.ContentOpenFrom
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
import com.github.zly2006.zhihu.navigation.WriteAnswer
import com.github.zly2006.zhihu.navigation.WritePin
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
import com.github.zly2006.zhihu.ui.subscreens.IdentityManagementScreen
import com.github.zly2006.zhihu.ui.subscreens.OpenSourceLicensesScreen
import com.github.zly2006.zhihu.ui.subscreens.ReadingSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.SettingsSearchScreen
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
    mainTabNavigationTarget: TopLevelDestination?,
    navigate: (NavDestination) -> Unit,
    setCurrentMainTabOpenFrom: (String?) -> Unit,
    consumeMainTabNavigationTarget: (TopLevelDestination) -> Unit,
    preferenceState: ZhihuMainPreferenceState,
    isDarkTheme: Boolean,
    articleContent: @Composable (Article) -> Unit,
    sentenceSimilarityContent: @Composable () -> Unit = {
        Text("Sentence similarity test is not available on this platform.")
    },
    blocklistSettingsNlpContent: BlocklistSettingsNlpContent? = null,
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

    // miuix-nav 的返回栈是 SnapshotStateList，直接读取栈顶即响应式（替代 currentBackStackEntryAsState）。
    val currentTopDestination = navController.backStack.lastOrNull()

    // ── 朗读播放器浮层 ──
    // 在正文类页面上自动展开成完整控制条，离开这些页面后缩成一个小圆钮，交由用户决定是否再展开。
    val readingPlayer = rememberReadingPlayerController()
    val readingPlayerState by readingPlayer.state
    val settings = rememberSettingsStore()
    var showReadingQueue by remember { mutableStateOf(false) }
    var isReadingPlayerExpandedByUser by remember { mutableStateOf(false) }
    var readingPlayerHeightPx by remember { mutableIntStateOf(0) }
    val readingPlayerOverlayOffsetState = remember { ReadingPlayerOverlayOffsetState() }
    val density = LocalDensity.current

    val isOnReadingDetail = currentTopDestination is Article ||
        currentTopDestination is Question ||
        currentTopDestination is Pin
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
    // 播放器自动播到下一篇时，把停留在旧正文页的用户一起带过去（替换栈顶而不是继续堆叠）。
    var previousReadingItemKey by remember { mutableStateOf(readingPlayerState.currentItem?.key) }
    LaunchedEffect(readingPlayerState.currentItem?.key) {
        val currentItem = readingPlayerState.currentItem
        val currentItemKey = currentItem?.key
        val itemChanged = previousReadingItemKey != null && previousReadingItemKey != currentItemKey
        previousReadingItemKey = currentItemKey
        if (itemChanged && currentItem != null) {
            val currentDestination = currentTopDestination.takeIf {
                it is Article || it is Pin || it is Question
            }
            val destination = currentItem.toDestination(readingPlayerState.sourceId)
            if (currentDestination != null && currentDestination != destination) {
                navController.pop()
                navigate(destination)
            }
        }
    }

    // 离开文章页时恢复系统状态栏（只在实际切换时触发）
    val isOnArticle = currentTopDestination is Article
    LaunchedEffect(currentTopDestination) {
        isReadingPlayerExpandedByUser = false
        if (!isOnArticle) readingPlayerOverlayOffsetState.revokeOwner()
    }
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
    var autoHideTopBarScrollFraction by remember { mutableStateOf<Float?>(null) }
    var autoHideTopBarHeightPx by remember { mutableFloatStateOf(0f) }

    fun settleAutoHideTopBar() {
        val fraction = autoHideTopBarScrollFraction ?: return
        val shouldShow = fraction >= 0.5f
        if (isBottomBarVisible != shouldShow) isBottomBarVisible = shouldShow
        autoHideTopBarScrollFraction = null
    }
    val bottomBarScrollConnection = remember(autoHideTopBar, currentTopDestination) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // 只响应用户拖动：fling 惯性与 miuix overScroll 回弹会产生正负交替的 available.y，
                // 松手后会把顶栏/底栏又弹回来跳变，必须忽略这些 SideEffect 来源。
                // 且只在状态真正变化时赋值，避免每帧 set 触发顶层重组。
                // 阈值提高到 10f + 滞回：prevents overscroll 回弹小幅振荡触发反复 toggle
                if (source == NestedScrollSource.UserInput) {
                    if (autoHideTopBar && currentTopDestination is MainTabs && autoHideTopBarHeightPx > 0f) {
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
    // 设置项“启用预测性返回”：关闭后系统返回仍可用，但不跟手播放预测返回动画（返回设置页后下次重组生效）。
    val enablePredictiveBack = rememberSettingBoolean("enable_predictive_back", true, settings)

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

    fun currentMainTabPage(): MainTabPage? = mainTabPages.getOrNull(mainPagerState.currentPage)
    var currentMainTabDestination by remember { mutableStateOf(startDestination) }

    fun navigateTopLevel(destination: TopLevelDestination) {
        val targetPage = pageIndexForDestination(destination)
        coroutineScope.launch {
            mainPagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(mainPagerState.currentPage, mainTabPages) {
        when (val page = currentMainTabPage()) {
            MainTabPage.FollowPage -> Unit
            else -> {}
        }
        currentMainTabPage()?.bottomDestination?.let { destination ->
            currentMainTabDestination = destination
            setCurrentMainTabOpenFrom(destination.openFrom)
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
                run {
                    // 页面切换时重置底部导航栏可见状态（顶栏复用此信号，一并恢复）
                    LaunchedEffect(currentTopDestination) {
                        isBottomBarVisible = true
                        autoHideTopBarScrollFraction = null
                    }
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
                        navigate(destination)
                    },
                    onNavigateBack = { navController.pop() },
                ),
                LocalReadingPlayerOverlayPadding provides readingPlayerOverlayPadding,
                LocalReadingPlayerOverlayOffsetState provides readingPlayerOverlayOffsetState,
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
                    entry<WriteAnswer> { writeAnswer ->
                        WriteAnswerScreen(writeAnswer)
                    }
                    entry<WritePin> {
                        WritePinScreen()
                    }
                    entry<Article> { article ->
                        // 同一回答链在单个 entry 内用 AnimatedContent 切换：返回键直接回到来源页，
                        // 切换动画两层互补满屏滑动，不透出上一个回答。
                        ArticleAnswerSlot(article) { answer ->
                            articleContent(answer)
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
                            MiuixBlocklistSettingsScreen(blocklistSettingsNlpContent)
                        } else {
                            BlocklistSettingsScreen(blocklistSettingsNlpContent)
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
                    entry<Notification.Entry> { args ->
                        NotificationTimelineScreen(args.entryName, args.title)
                    }
                    entry<Notification.Invitations> {
                        NotificationTimelineScreen("invite", "邀请回答")
                    }
                    entry<Notification.Message> { args ->
                        PrivateMessageScreen(args)
                    }
                    entry<Notification.NotificationSettings> {
                        if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                            MiuixNotificationSettingsScreen()
                        } else {
                            NotificationSettingsScreen()
                        }
                    }
                    entry<SentenceSimilarityTest> {
                        sentenceSimilarityContent()
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
                    entry<Account.SystemAndUpdateSettings> { args ->
                        if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                            // miuix 版还没有设置项高亮定位，暂时忽略 setting 深链参数。
                            MiuixSystemAndUpdateSettingsScreen()
                        } else {
                            SystemAndUpdateSettingsScreen(args.setting)
                        }
                    }
                    entry<Account.About> {
                        if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                            MiuixAboutScreen(innerPadding)
                        } else {
                            AboutScreen(innerPadding)
                        }
                    }
                    // 上游新增的三个设置页暂无 miuix 版本，两种风格共用 M3 实现。
                    entry<Account.IdentityManagement> {
                        IdentityManagementScreen()
                    }
                    entry<Account.ReadingSettings> {
                        ReadingSettingsScreen()
                    }
                    entry<Account.SettingsSearch> {
                        SettingsSearchScreen()
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
                val currentDestination = currentTopDestination.takeIf {
                    it is Article || it is Pin || it is Question
                }
                if (currentDestination != destination) {
                    if (currentDestination != null) {
                        navController.pop()
                    }
                    navigate(destination)
                }
            },
            onOpenSettings = {
                showReadingQueue = false
                isReadingPlayerExpandedByUser = false
                if (currentTopDestination !is Account.ReadingSettings) {
                    navigate(Account.ReadingSettings)
                }
            },
        )
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
    collectionDirectBrowseEnabled: Boolean,
) {
    HorizontalPager(
        state = pagerState,
        pageNestedScrollConnection = NoOpPagerNestedScrollConnection,
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
            // 上游把关注的「推荐/动态」两个 pager 页合并成一页，页内自带 tab 并接管父 pager 手势。
            MainTabPage.FollowPage -> if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
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
                )
            }
            MainTabPage.HotListPage -> if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                MiuixHotListScreen(innerPadding)
            } else {
                HotListScreen(
                    innerPadding = innerPadding,
                    scrollToTopTrigger = scrollToTopTrigger,
                    isActive = pagerState.currentPage == pageIndex,
                )
            }
            MainTabPage.DailyPage -> if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                MiuixDailyScreen()
            } else {
                DailyScreen(
                    scrollToTopTrigger = scrollToTopTrigger,
                    isActive = pagerState.currentPage == pageIndex,
                )
            }
            MainTabPage.OnlineHistoryPage -> if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                MiuixOnlineHistoryScreen()
            } else {
                OnlineHistoryScreen(
                    scrollToTopTrigger = scrollToTopTrigger,
                    isActive = pagerState.currentPage == pageIndex,
                )
            }
            MainTabPage.MyCollectionsPage -> MyCollectionsTopLevelPage(
                innerPadding = innerPadding,
                scrollToTopTrigger = scrollToTopTrigger,
                collectionDirectBrowseEnabled = collectionDirectBrowseEnabled,
                isActive = pagerState.currentPage == pageIndex,
            )
            MainTabPage.AccountPage -> if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
                MiuixAccountSettingScreen(innerPadding)
            } else {
                AccountSettingScreen(innerPadding)
            }
        }
    }
}

@Composable
private fun MyCollectionsTopLevelPage(
    innerPadding: PaddingValues,
    scrollToTopTrigger: Int,
    collectionDirectBrowseEnabled: Boolean,
    isActive: Boolean,
) {
    val account = rememberAccountSettingsAccountState().value
    when {
        // 直达浏览是上游新增的收藏夹模式，miuix 尚未复刻，先共用 M3 实现。
        collectionDirectBrowseEnabled -> CollectionBrowseScreen(
            urlToken = account.urlToken,
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
