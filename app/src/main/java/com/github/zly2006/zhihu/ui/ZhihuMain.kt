package com.github.zly2006.zhihu.ui

import android.annotation.SuppressLint
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.github.zly2006.zhihu.Account
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.CollectionContent
import com.github.zly2006.zhihu.Collections
import com.github.zly2006.zhihu.Daily
import com.github.zly2006.zhihu.Follow
import com.github.zly2006.zhihu.History
import com.github.zly2006.zhihu.Home
import com.github.zly2006.zhihu.HotList
import com.github.zly2006.zhihu.LocalNavigator
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.Notification
import com.github.zly2006.zhihu.OnlineHistory
import com.github.zly2006.zhihu.Person
import com.github.zly2006.zhihu.Pin
import com.github.zly2006.zhihu.Question
import com.github.zly2006.zhihu.Search
import com.github.zly2006.zhihu.SentenceSimilarityTest
import com.github.zly2006.zhihu.theme.ZhihuTheme
import com.github.zly2006.zhihu.ui.subscreens.AppearanceSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.ContentFilterSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.DeveloperSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.SystemAndUpdateSettingsScreen
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import com.github.zly2006.zhihu.viewmodel.NotificationViewModel
import kotlin.reflect.KClass
import com.github.zly2006.zhihu.ui.NavHost as MyNavHost

@SuppressLint("RestrictedApi")
@Composable
fun ZhihuMain(modifier: Modifier = Modifier, navController: NavHostController) {
    val bottomPadding = ScaffoldDefaults.contentWindowInsets.asPaddingValues().calculateBottomPadding()
    val activity = LocalActivity.current as MainActivity
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(PREFERENCE_NAME, android.content.Context.MODE_PRIVATE) }

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
                    return slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300))
                }

                !isPop && isEnter -> {
                    return slideInHorizontally(tween(300)) { it }
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
            slideInHorizontally(tween(300)) { it * offset } + fadeIn(tween(300))
        } else {
            slideOutHorizontally(tween(300)) { it * offset } + fadeOut(tween(300))
        }
    }

    Scaffold(
        bottomBar = {
            val navEntry by navController.currentBackStackEntryAsState()
            if (navEntry != null) {
                if (isTopLevelDest(navEntry)) {
                    NavigationBar(
                        modifier = modifier.height(56.dp + bottomPadding),
                    ) {
                        val allItems = listOf(
                            Triple(Home, "主页", Icons.Filled.Home),
                            Triple(Follow, "关注", Icons.Filled.PersonAddAlt1),
                            Triple(HotList, "热榜", Icons.Filled.Whatshot),
                            Triple(Daily, "日报", Icons.Filled.Newspaper),
                            Triple(OnlineHistory, "历史", Icons.Filled.History),
                            Triple(Account, "账号", Icons.Filled.ManageAccounts),
                        )
                        val defaultKeys = setOf(Home.name, Follow.name, Daily.name, OnlineHistory.name, Account.name)
                        val selectedKeys = preferences.getStringSet("bottom_bar_items", defaultKeys) ?: defaultKeys
                        val bottomBarItems = allItems.filter { it.first.name in selectedKeys }

                        @Composable
                        fun Item(
                            destination: NavDestination,
                            label: String,
                            icon: ImageVector,
                        ) {
                            NavigationBarItem(
                                navEntry.hasRoute(destination::class),
                                onClick = {
                                    if (!navEntry.hasRoute(destination::class)) {
                                        navController.navigate(destination) {
                                            popUpTo(Home)
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                label = {
                                    Text(
                                        label,
                                        style = TextStyle(
                                            fontSize = 9.sp,
                                            color = LocalContentColor.current.copy(alpha = 0.6f),
                                        ),
                                    )
                                },
                                alwaysShowLabel = false,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xff66ccff),
                                    indicatorColor = Color.Transparent,
                                ),
                                icon = {
                                    Icon(icon, contentDescription = label)
                                },
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
        CompositionLocalProvider(LocalNavigator provides activity::navigate) {
            MyNavHost(
                navController,
                modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
                startDestination = Home,
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
                    HomeScreen()
                }
                composable<Question> { navEntry ->
                    val question: Question = navEntry.toRoute()
                    QuestionScreen(question)
                }
                composable<Article> { navEntry ->
                    val article: Article = navEntry.toRoute()
                    val viewModel: ArticleViewModel = viewModel(navEntry) {
                        ArticleViewModel(article, activity.httpClient, navEntry)
                    }
                    ArticleScreen(article, viewModel)
                }
                composable<HotList> {
                    HotListScreen()
                }
                composable<Follow> {
                    FollowScreen()
                }
                composable<Daily> {
                    DailyScreen()
                }
                composable<History> {
                    HistoryScreen()
                }
                composable<OnlineHistory> {
                    OnlineHistoryScreen()
                }
                composable<Account> {
                    AccountSettingScreen(innerPadding)
                }
                composable<Search> { navEntry ->
                    val search: Search = navEntry.toRoute()
                    SearchScreen(search) {
                        navController.popBackStack()
                    }
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
                    PinScreen(
                        pin,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                    )
                }
                composable<Account.RecommendSettings.Blocklist> {
                    BlocklistSettingsScreen(
                        innerPadding = innerPadding,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                    )
                }
                composable<Notification> {
                    val viewModel = viewModel<NotificationViewModel>()
                    NotificationScreen(
                        viewModel = viewModel,
                        onBack = {
                            navController.popBackStack()
                        },
                    )
                }
                composable<Notification.NotificationSettings> {
                    NotificationSettingsScreen(
                        onBack = {
                            navController.popBackStack()
                        },
                    )
                }
                composable<SentenceSimilarityTest> {
                    SentenceSimilarityTestScreen {
                        navController.popBackStack()
                    }
                }
                composable<Account.AppearanceSettings> {
                    val args = it.toRoute<Account.AppearanceSettings>()
                    AppearanceSettingsScreen(
                        setting = args.setting,
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
                composable<Account.RecommendSettings> {
                    ContentFilterSettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
                composable<Account.SystemAndUpdateSettings> {
                    SystemAndUpdateSettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
                composable<Account.DeveloperSettings> {
                    DeveloperSettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                    )
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
