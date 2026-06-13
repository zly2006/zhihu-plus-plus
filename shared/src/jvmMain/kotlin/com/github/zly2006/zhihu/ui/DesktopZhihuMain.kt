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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.github.zly2006.zhihu.navigation.TopLevelDestination
import com.github.zly2006.zhihu.navigation.Video
import com.github.zly2006.zhihu.shared.data.fetchHighestQualityZhihuVideoUrl
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.openDesktopExternalUrl
import com.github.zly2006.zhihu.shared.desktop.signDesktopRequest
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.ui.subscreens.BOTTOM_BAR_ITEMS_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.BOTTOM_BAR_ITEM_ORDER_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.START_DESTINATION_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.bottomBarItemOrderFromPreference
import com.github.zly2006.zhihu.ui.subscreens.defaultBottomBarSelectionKeys
import com.github.zly2006.zhihu.ui.subscreens.navDestinationFromName
import com.github.zly2006.zhihu.ui.subscreens.normalizeBottomBarSelection
import com.github.zly2006.zhihu.ui.subscreens.resolveValidStartDestinationKey
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import com.github.zly2006.zhihu.viewmodel.prepareDesktopPendingContentOpen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.nav.core.rememberNavController

private val desktopMainPreferenceKeys = setOf(
    "duo3_home_account",
    BOTTOM_BAR_ITEMS_PREFERENCE_KEY,
    BOTTOM_BAR_ITEM_ORDER_PREFERENCE_KEY,
    "duo3_nav_style",
    "bottomBarTapScrollToTop",
    "autoHideBottomBar",
    "autoHideTopBar",
    START_DESTINATION_PREFERENCE_KEY,
)

/**
 * Desktop 平台的 Zhihu++ 主界面入口。
 *
 * 这里创建桌面 NavController、账号存储、HTTP 客户端和视频/文章等平台行为，再注入共享 [ZhihuMain]。
 * 设计上尽量复用 common 页面结构，只把浏览器打开、签名请求、回答切换状态和桌面账号读取留在 JVM 侧。
 */
@Composable
fun DesktopZhihuMain() {
    val navController = rememberNavController<NavDestination>(MainTabs)
    val httpClient = rememberZhihuHttpClient()
    val accountStore = remember { DesktopAccountStore() }
    val coroutineScope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()
    var mainTabNavigationTarget by remember { mutableStateOf<TopLevelDestination?>(null) }
    var currentMainTabOpenFrom by remember { mutableStateOf<String?>(null) }

    fun navigateToMainTabs() {
        navController.popUntil { it is MainTabs }
    }

    fun currentMainTabOpenFrom(): String? = if (navController.backStack.lastOrNull() is MainTabs) {
        currentMainTabOpenFrom
    } else {
        null
    }

    fun currentContentOpenSource(): NavDestination? = when (val top = navController.backStack.lastOrNull()) {
        is Article, is Question, is Pin, is CollectionContent, is History, is Notification -> top
        else -> null
    }

    fun navigate(route: NavDestination) {
        when (route) {
            is Video -> {
                val top = navController.backStack.lastOrNull()
                val current = top as? Article ?: top as? Question
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
                    val videoUrl = withContext(Dispatchers.IO) {
                        runCatching {
                            fetchHighestQualityZhihuVideoUrl(
                                httpClient = httpClient,
                                videoId = route.id.toString(),
                                contentId = contentId,
                                contentType = contentType,
                                xsrfToken = cookies["_xsrf"],
                            ) {
                                signDesktopRequest(cookies)
                            }
                        }.getOrNull()
                    }
                    if (videoUrl == null) {
                        userMessages.showMessage("获取视频链接失败")
                    } else {
                        openDesktopExternalUrl(videoUrl)
                    }
                }
            }
            MainTabs -> {
                mainTabNavigationTarget = Home
                navigateToMainTabs()
            }
            else -> {
                prepareDesktopPendingContentOpen(
                    target = route,
                    currentMainTabOpenFrom = currentMainTabOpenFrom(),
                    source = currentContentOpenSource(),
                )
                navController.push(route)
            }
        }
    }

    ZhihuMain(
        navController = navController,
        navigationState = ZhihuMainNavigationState(
            mainTabNavigationTarget = mainTabNavigationTarget,
            navigate = ::navigate,
            setCurrentMainTabOpenFrom = { currentMainTabOpenFrom = it },
            consumeMainTabNavigationTarget = { destination ->
                if (mainTabNavigationTarget == destination) {
                    mainTabNavigationTarget = null
                }
            },
        ),
        preferenceState = rememberDesktopZhihuMainPreferenceState(),
        isDarkTheme = ThemeManager.isDarkTheme(),
        platformAdapter = ZhihuMainPlatformAdapter(
            article = { article ->
                // 同一回答链共用一个导航 entry 的 store，故按回答 id 区分 ViewModel（见 ArticleAnswerSlot）。
                val articleViewModel: ArticleViewModel = viewModel(key = "article-${article.id}") {
                    ArticleViewModel(article, httpClient, userMessages)
                }
                ArticleScreen(article, articleViewModel)
            },
        ),
    )
}

/**
 * 读取 Desktop 设置中会影响主壳的偏好快照。
 *
 * 语义必须和 Android 的 `rememberAndroidZhihuMainPreferenceState()` 保持一致，避免同一个底栏/启动页设置在不同平台表现不同。
 */
@Composable
private fun rememberDesktopZhihuMainPreferenceState(): ZhihuMainPreferenceState {
    val settings = rememberSettingsStore()
    val allBottomBarItemKeys = remember {
        listOf(Home.name, Follow.name, HotList.name, Daily.name, OnlineHistory.name, MyCollections.name, Account.name)
    }
    return rememberZhihuMainPreferenceState(
        settings = settings,
        observedKeys = desktopMainPreferenceKeys,
        readSnapshot = {
            val duo3HomeAccount = settings.getBoolean("duo3_home_account", false)
            val selectedKeys = normalizeBottomBarSelection(
                settings.getStringSet(
                    BOTTOM_BAR_ITEMS_PREFERENCE_KEY,
                    defaultBottomBarSelectionKeys(duo3HomeAccount),
                ),
                duo3HomeAccount,
                enforceMinimumSelection = true,
            )
            val orderedSelectedKeys = bottomBarItemOrderFromPreference(
                settings.getStringOrNull(BOTTOM_BAR_ITEM_ORDER_PREFERENCE_KEY),
                selectedKeys,
            )
            ZhihuMainPreferenceSnapshot(
                duo3HomeAccount = duo3HomeAccount,
                duo3NavStyle = settings.getBoolean("duo3_nav_style", false),
                tapToScrollToTopEnabled = settings.getBoolean("bottomBarTapScrollToTop", true),
                autoHideBottomBar = settings.getBoolean("autoHideBottomBar", false),
                autoHideTopBar = settings.getBoolean("autoHideTopBar", false),
                selectedBottomBarItemKeys = orderedSelectedKeys,
                startDestination = navDestinationFromName(
                    resolveValidStartDestinationKey(
                        settings.getString(START_DESTINATION_PREFERENCE_KEY, Home.name),
                        orderedSelectedKeys.ifEmpty { allBottomBarItemKeys.filter { it in selectedKeys } },
                    ),
                ),
            )
        },
    )
}
