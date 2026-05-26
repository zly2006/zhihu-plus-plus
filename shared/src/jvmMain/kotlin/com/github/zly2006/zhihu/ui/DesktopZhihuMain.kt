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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.Daily
import com.github.zly2006.zhihu.navigation.Follow
import com.github.zly2006.zhihu.navigation.Home
import com.github.zly2006.zhihu.navigation.HotList
import com.github.zly2006.zhihu.navigation.MainTabs
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.OnlineHistory
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.TopLevelDestination
import com.github.zly2006.zhihu.navigation.Video
import com.github.zly2006.zhihu.shared.data.fetchHighestQualityZhihuVideoUrl
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.util.signZhihuFetchRequest
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import com.github.zly2006.zhihu.viewmodel.desktopArticleAnswerSwitchState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.net.URI

@Composable
fun DesktopZhihuMain() {
    val navController = rememberNavController()
    val httpClient = rememberZhihuHttpClient()
    val accountStore = remember { DesktopAccountStore() }
    val coroutineScope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()
    var mainTabNavigationTarget by remember { mutableStateOf<TopLevelDestination?>(null) }

    fun navigateToMainTabs() {
        navController.navigate(MainTabs) {
            launchSingleTop = true
            restoreState = true
            popUpTo(MainTabs) {
                saveState = true
            }
        }
    }

    fun navigate(route: NavDestination) {
        when (route) {
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
                    val videoUrl = withContext(Dispatchers.IO) {
                        runCatching {
                            fetchHighestQualityZhihuVideoUrl(
                                httpClient = httpClient,
                                videoId = route.id.toString(),
                                contentId = contentId,
                                contentType = contentType,
                                xsrfToken = cookies["_xsrf"],
                            ) {
                                signZhihuFetchRequest(dc0 = cookies["d_c0"] ?: "")
                            }
                        }.getOrNull()
                    }
                    if (videoUrl == null) {
                        userMessages.showMessage("获取视频链接失败")
                    } else if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(URI(videoUrl))
                    }
                }
            }
            MainTabs -> {
                mainTabNavigationTarget = Home
                navigateToMainTabs()
            }
            else -> navController.navigate(route)
        }
    }

    ZhihuMain(
        navController = navController,
        navigationState = ZhihuMainNavigationState(
            mainTabNavigationTarget = mainTabNavigationTarget,
            navigate = ::navigate,
            setCurrentMainTabOpenFrom = {},
            consumeMainTabNavigationTarget = { destination ->
                if (mainTabNavigationTarget == destination) {
                    mainTabNavigationTarget = null
                }
            },
        ),
        preferenceState = rememberDesktopZhihuMainPreferenceState(),
        isDarkTheme = ThemeManager.isDarkTheme(),
        platformAdapter = ZhihuMainPlatformAdapter(
            articleEnterTransition = {
                when (desktopArticleAnswerSwitchState.answerTransitionDirection) {
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
                when (desktopArticleAnswerSwitchState.answerTransitionDirection) {
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
            article = { article: Article, navEntry ->
                val articleViewModel: ArticleViewModel = viewModel(navEntry) {
                    ArticleViewModel(article, httpClient)
                }
                ArticleScreen(article, articleViewModel)
            },
        ),
    )
}

@Composable
private fun rememberDesktopZhihuMainPreferenceState(): ZhihuMainPreferenceState {
    val settings = rememberSettingsStore()
    val allBottomBarItemKeys = remember {
        listOf(Home.name, Follow.name, HotList.name, Daily.name, OnlineHistory.name, Account.name)
    }
    return rememberZhihuMainPreferenceState {
        val duo3HomeAccount = settings.getBoolean("duo3_home_account", false)
        val selectedKeys = normalizeBottomBarSelection(
            settings.getStringSet(
                BOTTOM_BAR_ITEMS_PREFERENCE_KEY,
                defaultBottomBarSelectionKeys(duo3HomeAccount),
            ),
            duo3HomeAccount,
            enforceMinimumSelection = true,
        )
        ZhihuMainPreferenceSnapshot(
            duo3HomeAccount = duo3HomeAccount,
            duo3NavStyle = settings.getBoolean("duo3_nav_style", false),
            tapToScrollToTopEnabled = settings.getBoolean("bottomBarTapScrollToTop", true),
            autoHideBottomBar = settings.getBoolean("autoHideBottomBar", false),
            selectedBottomBarItemKeys = selectedKeys,
            startDestination = navDestinationFromName(
                resolveValidStartDestinationKey(
                    settings.getString(START_DESTINATION_PREFERENCE_KEY, Home.name),
                    allBottomBarItemKeys.filter { it in selectedKeys },
                ),
            ),
        )
    }
}
