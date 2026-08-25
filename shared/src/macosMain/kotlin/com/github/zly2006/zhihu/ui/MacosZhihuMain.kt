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

import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.github.zly2006.zhihu.account.NativeAccountStore
import com.github.zly2006.zhihu.data.fetchHighestQualityZhihuVideoUrl
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.CollectionContent
import com.github.zly2006.zhihu.navigation.Daily
import com.github.zly2006.zhihu.navigation.Follow
import com.github.zly2006.zhihu.navigation.History
import com.github.zly2006.zhihu.navigation.Home
import com.github.zly2006.zhihu.navigation.HotList
import com.github.zly2006.zhihu.navigation.MainTabs
import com.github.zly2006.zhihu.navigation.MyCollections
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.OnlineHistory
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.Search
import com.github.zly2006.zhihu.navigation.TopLevelDestination
import com.github.zly2006.zhihu.navigation.Video
import com.github.zly2006.zhihu.platform.platformBottomBarItemLimit
import com.github.zly2006.zhihu.platform.rememberExternalUrlOpener
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.ui.subscreens.BOTTOM_BAR_ITEMS_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.BOTTOM_BAR_ITEM_ORDER_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.COLLECTION_DIRECT_BROWSE_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.START_DESTINATION_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.bottomBarItemOrderFromPreference
import com.github.zly2006.zhihu.ui.subscreens.defaultBottomBarSelectionKeys
import com.github.zly2006.zhihu.ui.subscreens.navDestinationFromName
import com.github.zly2006.zhihu.ui.subscreens.normalizeBottomBarSelection
import com.github.zly2006.zhihu.ui.subscreens.resolveValidStartDestinationKey
import com.github.zly2006.zhihu.util.signZhihuFetchRequest
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import com.github.zly2006.zhihu.viewmodel.nativeArticleAnswerSwitchState
import com.github.zly2006.zhihu.viewmodel.prepareNativePendingContentOpen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * macOS Kotlin/Native 主界面入口。
 *
 * 窗口宿主只调用这个入口；所有页面、布局和导航图仍由共享 [ZhihuMain] 提供。
 */
@Composable
fun MacosZhihuMain(windowChrome: MacosWindowChromeHost? = null) {
    val navController = rememberNavController()
    val accountStore = remember { NativeAccountStore() }
    val httpClient = accountStore.httpClient()
    val coroutineScope = rememberCoroutineScope()
    val openExternalUrl = rememberExternalUrlOpener()
    val userMessages = rememberUserMessageSink()
    val preferenceState = rememberMacosZhihuMainPreferenceState()
    var mainTabNavigationTarget by remember { mutableStateOf<TopLevelDestination?>(null) }
    var currentMainTabOpenFrom by remember { mutableStateOf<String?>(null) }
    var currentMainTabDestination by remember { mutableStateOf(preferenceState.startDestination) }

    fun navigateToMainTabs() {
        navController.navigate(MainTabs) {
            launchSingleTop = true
            restoreState = true
            popUpTo(MainTabs) {
                saveState = true
            }
        }
    }

    fun currentContentOpenSource(): NavDestination? {
        val currentEntry = navController.currentBackStackEntry
        return runCatching {
            currentEntry?.toRoute<Article>()
        }.getOrNull() ?: runCatching {
            currentEntry?.toRoute<Question>()
        }.getOrNull() ?: runCatching {
            currentEntry?.toRoute<Pin>()
        }.getOrNull() ?: runCatching {
            currentEntry?.toRoute<CollectionContent>()
        }.getOrNull() ?: runCatching {
            currentEntry?.toRoute<History>()
        }.getOrNull() ?: runCatching {
            currentEntry?.toRoute<Notification>()
        }.getOrNull()
    }

    fun navigate(route: NavDestination) {
        when (route) {
            History -> navController.navigate(route)
            is TopLevelDestination -> {
                mainTabNavigationTarget = route
                navigateToMainTabs()
            }
            is Video -> {
                val current = runCatching {
                    navController.currentBackStackEntry?.toRoute<Article>()
                }.getOrNull() ?: runCatching {
                    navController.currentBackStackEntry?.toRoute<Question>()
                }.getOrNull()
                if (current == null) {
                    userMessages.showMessage("无法打开视频：未知的内容类型")
                    return
                }
                val (contentId, contentType) = when (current) {
                    is Article -> current.id.toString() to when (current.type) {
                        ArticleType.Answer -> "answer"
                        ArticleType.Article -> "article"
                    }
                    is Question -> current.questionId.toString() to "question"
                    else -> return
                }
                coroutineScope.launch {
                    val cookies = accountStore.load().cookies
                    val videoUrl = withContext(Dispatchers.Default) {
                        runCatching {
                            fetchHighestQualityZhihuVideoUrl(
                                httpClient = httpClient,
                                videoId = route.id.toString(),
                                contentId = contentId,
                                contentType = contentType,
                                xsrfToken = cookies["_xsrf"],
                            ) {
                                signZhihuFetchRequest(cookies)
                            }
                        }.getOrNull()
                    }
                    if (videoUrl == null) {
                        userMessages.showMessage("获取视频链接失败")
                    } else {
                        openExternalUrl(videoUrl)
                    }
                }
            }
            MainTabs -> {
                mainTabNavigationTarget = Home
                navigateToMainTabs()
            }
            else -> {
                prepareNativePendingContentOpen(
                    target = route,
                    currentMainTabOpenFrom = if (
                        runCatching { navController.currentBackStackEntry?.toRoute<MainTabs>() }.getOrNull() != null
                    ) {
                        currentMainTabOpenFrom
                    } else {
                        null
                    },
                    source = currentContentOpenSource(),
                )
                navController.navigate(route)
            }
        }
    }

    val content: @Composable (Modifier) -> Unit = { modifier ->
        ZhihuMain(
            modifier = modifier,
            navController = navController,
            mainTabNavigationTarget = mainTabNavigationTarget,
            navigate = ::navigate,
            setCurrentMainTabOpenFrom = { currentMainTabOpenFrom = it },
            consumeMainTabNavigationTarget = { destination ->
                if (mainTabNavigationTarget == destination) {
                    mainTabNavigationTarget = null
                }
            },
            preferenceState = preferenceState,
            isDarkTheme = ThemeManager.isDarkTheme(),
            showMainNavigationBar = windowChrome == null,
            showHomeTopActions = windowChrome == null,
            onCurrentMainTabDestinationChange = { currentMainTabDestination = it },
            articleEnterTransition = {
                when (nativeArticleAnswerSwitchState.answerTransitionDirection) {
                    ArticleAnswerTransitionDirection.VERTICAL_NEXT ->
                        slideInVertically(tween(300)) { it } + fadeIn(tween(300))
                    ArticleAnswerTransitionDirection.VERTICAL_PREVIOUS ->
                        slideInVertically(tween(300)) { -it } + fadeIn(tween(300))
                    ArticleAnswerTransitionDirection.HORIZONTAL_NEXT ->
                        slideInHorizontally(tween(300)) { it } + fadeIn(tween(300))
                    ArticleAnswerTransitionDirection.HORIZONTAL_PREVIOUS ->
                        slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300))
                    else -> slideInHorizontally(tween(300)) { it }
                }
            },
            articleExitTransition = {
                when (nativeArticleAnswerSwitchState.answerTransitionDirection) {
                    ArticleAnswerTransitionDirection.VERTICAL_NEXT ->
                        slideOutVertically(tween(300)) { -it } + fadeOut(tween(300))
                    ArticleAnswerTransitionDirection.VERTICAL_PREVIOUS ->
                        slideOutVertically(tween(300)) { it } + fadeOut(tween(300))
                    ArticleAnswerTransitionDirection.HORIZONTAL_NEXT ->
                        slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300))
                    ArticleAnswerTransitionDirection.HORIZONTAL_PREVIOUS ->
                        slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300))
                    else -> ExitTransition.None
                }
            },
            articleContent = { article: Article, navEntry ->
                val articleViewModel: ArticleViewModel = viewModel(navEntry) {
                    ArticleViewModel(article, httpClient, userMessages)
                }
                ArticleScreen(article, articleViewModel)
            },
        )
    }

    if (windowChrome == null) {
        content(Modifier)
    } else {
        val navigationItems = preferenceState.selectedBottomBarItemKeys.mapNotNull { destinationName ->
            val destination = navDestinationFromName(destinationName)
            val action = {
                mainTabNavigationTarget = destination
                navigateToMainTabs()
            }
            when (destination) {
                Home -> MacosWindowNavigationItem(Home.name, "首页", "house", "内容", Home == currentMainTabDestination, action)
                Follow -> MacosWindowNavigationItem(Follow.name, "关注", "person.2", "内容", Follow == currentMainTabDestination, action)
                HotList -> MacosWindowNavigationItem(HotList.name, "热榜", "flame", "内容", HotList == currentMainTabDestination, action)
                Daily -> MacosWindowNavigationItem(Daily.name, "日报", "newspaper", "内容", Daily == currentMainTabDestination, action)
                OnlineHistory -> MacosWindowNavigationItem(
                    OnlineHistory.name,
                    "历史",
                    "clock.arrow.circlepath",
                    "资料",
                    OnlineHistory == currentMainTabDestination,
                    action,
                )
                MyCollections -> MacosWindowNavigationItem(
                    MyCollections.name,
                    "收藏",
                    "bookmark",
                    "资料",
                    MyCollections == currentMainTabDestination,
                    action,
                )
                Account -> MacosWindowNavigationItem(
                    Account.name,
                    "账号",
                    "person.crop.circle",
                    "账号",
                    Account == currentMainTabDestination,
                    action,
                )
                else -> null
            }
        }
        windowChrome(
            MacosWindowChrome(
                navigationItems = navigationItems,
                trailingToolbarItems = listOf(
                    MacosWindowToolbarItem(
                        identifier = "search",
                        label = "搜索",
                        systemSymbolName = "magnifyingglass",
                        action = { navigate(Search()) },
                    ),
                    MacosWindowToolbarItem(
                        identifier = "notifications",
                        label = "通知",
                        systemSymbolName = "bell",
                        action = { navigate(Notification) },
                    ),
                ),
            ),
            content,
        )
    }
}

@Composable
private fun rememberMacosZhihuMainPreferenceState(): ZhihuMainPreferenceState {
    val settings = rememberSettingsStore()
    val allBottomBarItemKeys = remember {
        listOf(Home.name, Follow.name, HotList.name, Daily.name, OnlineHistory.name, MyCollections.name, Account.name)
    }
    return rememberZhihuMainPreferenceState {
        val duo3HomeAccount = settings.getBoolean("duo3_home_account", false)
        val selectedKeys = normalizeBottomBarSelection(
            settings.getStringSet(
                BOTTOM_BAR_ITEMS_PREFERENCE_KEY,
                defaultBottomBarSelectionKeys(duo3HomeAccount, platformBottomBarItemLimit),
            ),
            duo3HomeAccount,
            enforceMinimumSelection = true,
            maximumSelection = platformBottomBarItemLimit,
        )
        val orderedSelectedKeys = bottomBarItemOrderFromPreference(
            settings.getStringOrNull(BOTTOM_BAR_ITEM_ORDER_PREFERENCE_KEY),
            selectedKeys,
        )
        ZhihuMainPreferenceSnapshot(
            duo3HomeAccount = duo3HomeAccount,
            tapToScrollToTopEnabled = settings.getBoolean("bottomBarTapScrollToTop", true),
            autoHideBottomBar = settings.getBoolean("autoHideBottomBar", false),
            collectionDirectBrowseEnabled = settings.getBoolean(COLLECTION_DIRECT_BROWSE_PREFERENCE_KEY, false),
            selectedBottomBarItemKeys = orderedSelectedKeys,
            startDestination = navDestinationFromName(
                resolveValidStartDestinationKey(
                    settings.getString(START_DESTINATION_PREFERENCE_KEY, Home.name),
                    orderedSelectedKeys.ifEmpty { allBottomBarItemKeys.filter { it in selectedKeys } },
                ),
            ),
        )
    }
}
