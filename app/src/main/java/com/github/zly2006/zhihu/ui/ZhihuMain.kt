package com.github.zly2006.zhihu.ui

import android.annotation.SuppressLint
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldValue
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
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
import com.github.zly2006.zhihu.Navigator
import com.github.zly2006.zhihu.Notification
import com.github.zly2006.zhihu.OnlineHistory
import com.github.zly2006.zhihu.Person
import com.github.zly2006.zhihu.Pin
import com.github.zly2006.zhihu.Question
import com.github.zly2006.zhihu.Search
import com.github.zly2006.zhihu.SentenceSimilarityTest
import com.github.zly2006.zhihu.TopLevelDestination
import com.github.zly2006.zhihu.theme.ZhihuTheme
import com.github.zly2006.zhihu.ui.subscreens.AppearanceSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.BOTTOM_BAR_ITEMS_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.BlockedFeedHistoryScreen
import com.github.zly2006.zhihu.ui.subscreens.ColorSchemeScreen
import com.github.zly2006.zhihu.ui.subscreens.ContentFilterSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.DeveloperSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.START_DESTINATION_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.SystemAndUpdateSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.defaultBottomBarSelectionKeys
import com.github.zly2006.zhihu.ui.subscreens.navDestinationFromName
import com.github.zly2006.zhihu.ui.subscreens.normalizeBottomBarSelection
import com.github.zly2006.zhihu.ui.subscreens.resolveValidStartDestinationKey
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import kotlin.reflect.KClass

const val SURVEY_URL = "https://v.wjx.cn/vm/Ppfw2R4.aspx#"

@SuppressLint("RestrictedApi")
@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3AdaptiveNavigationSuiteApi::class)
@Composable
fun ZhihuMain(modifier: Modifier = Modifier, navController: NavHostController) {
    val activity = LocalActivity.current as MainActivity
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(PREFERENCE_NAME, android.content.Context.MODE_PRIVATE) }

    // 底部导航栏功能
    var duo3HomeAccount by remember { mutableStateOf(preferences.getBoolean("duo3_home_account", false)) }
    var duo3NavStyle by remember { mutableStateOf(preferences.getBoolean("duo3_nav_style", false)) }
    var tapToScrollToTopEnabled by remember { mutableStateOf(preferences.getBoolean("bottomBarTapScrollToTop", true)) }
    val allBottomBarItemKeys = remember {
        listOf(Home.name, Follow.name, HotList.name, Daily.name, OnlineHistory.name, Account.name)
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
        selectedBottomBarItemKeys = updatedSelectedBottomBarItemKeys
        startDestination = computeStartDestination(updatedSelectedBottomBarItemKeys)
    }

    val navEntry by navController.currentBackStackEntryAsState()
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val paneDirective = calculatePaneScaffoldDirective(adaptiveInfo)
    val navigationSuiteState = rememberNavigationSuiteScaffoldState()
    val isSinglePaneWindow = paneDirective.maxHorizontalPartitions == 1
    var isSinglePaneListDetailShowingDetail by rememberSaveable { mutableStateOf(false) }
    val isStandaloneDetailRoute = when {
        navEntry?.destination?.hasRoute(Article::class) == true -> true
        navEntry?.destination?.hasRoute(Question::class) == true -> true
        navEntry?.destination?.hasRoute(Person::class) == true -> true
        navEntry?.destination?.hasRoute(Pin::class) == true -> true
        navEntry?.destination?.hasRoute(CollectionContent::class) == true -> true
        navEntry?.destination?.hasRoute(Notification.NotificationSettings::class) == true -> true
        navEntry?.destination?.hasRoute(Account.AppearanceSettings::class) == true -> true
        navEntry?.destination?.hasRoute(Account.RecommendSettings::class) == true -> true
        navEntry?.destination?.hasRoute(Account.SystemAndUpdateSettings::class) == true -> true
        navEntry?.destination?.hasRoute(Account.DeveloperSettings::class) == true -> true
        navEntry?.destination?.hasRoute(Account.DeveloperSettings.ColorScheme::class) == true -> true
        navEntry?.destination?.hasRoute(Account.RecommendSettings.Blocklist::class) == true -> true
        navEntry?.destination?.hasRoute(Account.RecommendSettings.BlockedFeedHistory::class) == true -> true
        else -> false
    }

    LaunchedEffect(isSinglePaneWindow, isSinglePaneListDetailShowingDetail, isStandaloneDetailRoute) {
        val shouldHideNavigationSuite = isSinglePaneWindow && (isSinglePaneListDetailShowingDetail || isStandaloneDetailRoute)
        navigationSuiteState.snapTo(
            if (shouldHideNavigationSuite) {
                NavigationSuiteScaffoldValue.Hidden
            } else {
                NavigationSuiteScaffoldValue.Visible
            },
        )
    }
    var scrollToTopTrigger by remember { mutableIntStateOf(0) }
    val allBottomBarItems = listOf(
        Triple(Home, "主页", Icons.Filled.Home),
        Triple(Follow, "关注", if (duo3NavStyle) Icons.Filled.Group else Icons.Filled.PersonAddAlt1),
        Triple(HotList, "热榜", Icons.Filled.Whatshot),
        Triple(Daily, "日报", Icons.Filled.Newspaper),
        Triple(OnlineHistory, "历史", Icons.Filled.History),
        Triple(Account, "账号", Icons.Filled.ManageAccounts),
    )
    val bottomBarItems = allBottomBarItems.filter { it.first.name in selectedBottomBarItemKeys }

    fun topLevelKey(destination: NavDestination): String = (destination as? TopLevelDestination)?.name ?: Home.name
    var currentTopLevelDestinationKey by rememberSaveable { mutableStateOf(topLevelKey(startDestination)) }

    when {
        navEntry?.hasRoute(Home::class) == true -> currentTopLevelDestinationKey = Home.name
        navEntry?.hasRoute(Follow::class) == true -> currentTopLevelDestinationKey = Follow.name
        navEntry?.hasRoute(HotList::class) == true -> currentTopLevelDestinationKey = HotList.name
        navEntry?.hasRoute(Daily::class) == true -> currentTopLevelDestinationKey = Daily.name
        navEntry?.hasRoute(OnlineHistory::class) == true -> currentTopLevelDestinationKey = OnlineHistory.name
        navEntry?.hasRoute(Account::class) == true -> currentTopLevelDestinationKey = Account.name
    }

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

    val useVerticalTopLevelAnimation = paneDirective.maxHorizontalPartitions > 1

    // 通用动画创建函数
    @Suppress("KotlinConstantConditions")
    fun createSlideAnimation(
        isEnter: Boolean,
        isPop: Boolean,
        fromIndex: Int,
        toIndex: Int,
        useVerticalAnimation: Boolean = false,
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
        return if (useVerticalAnimation) {
            if (isEnter) {
                slideInVertically(tween(300)) { it * offset } + fadeIn(tween(300))
            } else {
                slideOutVertically(tween(300)) { it * offset } + fadeOut(tween(300))
            }
        } else {
            if (isEnter) {
                slideInHorizontally(tween(300)) { it * offset } + fadeIn(tween(300))
            } else {
                slideOutHorizontally(tween(300)) { it * offset } + fadeOut(tween(300))
            }
        }
    }

    NavigationSuiteScaffold(
        modifier = modifier,
        state = navigationSuiteState,
        navigationSuiteItems = {
            bottomBarItems.forEach { item ->
                val destination = item.first
                item(
                    selected = currentTopLevelDestinationKey == destination.name,
                    onClick = {
                        if (!navEntry.hasRoute(destination::class)) {
                            currentTopLevelDestinationKey = destination.name
                            navController.navigate(destination) {
                                popUpTo(startDestination)
                                launchSingleTop = true
                                restoreState = true
                            }
                        } else if (tapToScrollToTopEnabled) {
                            scrollToTopTrigger++
                        }
                    },
                    icon = {
                        Icon(item.third, contentDescription = item.second)
                    },
                    label = {
                        Text(item.second)
                    },
                    alwaysShowLabel = duo3NavStyle,
                )
            }
        },
    ) {
        CompositionLocalProvider(
            LocalNavigator provides Navigator(
                onNavigate = activity::navigate,
                onNavigateBack = navController::popBackStack,
            ),
        ) {
            NavHost(
                navController,
                startDestination = startDestination,
                modifier = modifier,
                enterTransition = {
                    val fromIndex = getPageIndex(initialState.destination)
                    val toIndex = getPageIndex(targetState.destination)
                    createSlideAnimation(
                        isEnter = true,
                        isPop = false,
                        fromIndex = fromIndex,
                        toIndex = toIndex,
                        useVerticalAnimation = useVerticalTopLevelAnimation,
                    ) as EnterTransition
                },
                exitTransition = {
                    val fromIndex = getPageIndex(initialState.destination)
                    val toIndex = getPageIndex(targetState.destination)
                    createSlideAnimation(
                        isEnter = false,
                        isPop = false,
                        fromIndex = fromIndex,
                        toIndex = toIndex,
                        useVerticalAnimation = useVerticalTopLevelAnimation,
                    ) as ExitTransition
                },
                popEnterTransition = {
                    val fromIndex = getPageIndex(initialState.destination)
                    val toIndex = getPageIndex(targetState.destination)
                    createSlideAnimation(
                        isEnter = true,
                        isPop = true,
                        fromIndex = fromIndex,
                        toIndex = toIndex,
                        useVerticalAnimation = useVerticalTopLevelAnimation,
                    ) as EnterTransition
                },
                popExitTransition = {
                    val fromIndex = getPageIndex(initialState.destination)
                    val toIndex = getPageIndex(targetState.destination)
                    createSlideAnimation(
                        isEnter = false,
                        isPop = true,
                        fromIndex = fromIndex,
                        toIndex = toIndex,
                        useVerticalAnimation = useVerticalTopLevelAnimation,
                    ) as ExitTransition
                },
            ) {
                composable<Home> {
                    HomeListDetailScreen(
                        scrollToTopTrigger = scrollToTopTrigger,
                        innerPadding = PaddingValues(),
                        onSinglePaneDetailChanged = { isSinglePaneListDetailShowingDetail = it },
                    )
                }
                composable<Question> { navEntry ->
                    val question: Question = navEntry.toRoute()
                    QuestionScreen(question, PaddingValues())
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
                    ArticleScreen(article, viewModel, PaddingValues())
                }
                composable<HotList> {
                    ContentListDetailScreen(
                        innerPadding = PaddingValues(),
                        onSinglePaneDetailChanged = { isSinglePaneListDetailShowingDetail = it },
                    ) { navigator ->
                        HotListScreen(
                            innerPadding = PaddingValues(),
                            onContentNavigate = navigator.onNavigate,
                        )
                    }
                }
                composable<Follow> {
                    ContentListDetailScreen(
                        innerPadding = PaddingValues(),
                        onSinglePaneDetailChanged = { isSinglePaneListDetailShowingDetail = it },
                    ) { navigator ->
                        FollowScreen(
                            innerPadding = PaddingValues(),
                            onContentNavigate = navigator.onNavigate,
                        )
                    }
                }
                composable<Daily> {
                    ContentListDetailScreen(
                        innerPadding = PaddingValues(),
                        onSinglePaneDetailChanged = { isSinglePaneListDetailShowingDetail = it },
                    ) { navigator ->
                        DailyScreen(
                            innerPadding = PaddingValues(),
                            onContentNavigate = navigator.onNavigate,
                        )
                    }
                }
                composable<History> {
                    HistoryScreen(PaddingValues())
                }
                composable<OnlineHistory> {
                    ContentListDetailScreen(
                        innerPadding = PaddingValues(),
                        onSinglePaneDetailChanged = { isSinglePaneListDetailShowingDetail = it },
                    ) { navigator ->
                        OnlineHistoryScreen(
                            innerPadding = PaddingValues(),
                            onContentNavigate = navigator.onNavigate,
                        )
                    }
                }
                composable<Account> {
                    SettingsListDetailScreen(
                        innerPadding = PaddingValues(),
                        onSinglePaneDetailChanged = { isSinglePaneListDetailShowingDetail = it },
                        onExit = reloadBottomBarPreferences,
                    )
                }
                composable<Search> { navEntry ->
                    val search: Search = navEntry.toRoute()
                    ContentListDetailScreen(
                        innerPadding = PaddingValues(),
                        onSinglePaneDetailChanged = { isSinglePaneListDetailShowingDetail = it },
                    ) { navigator ->
                        SearchScreen(
                            innerPadding = PaddingValues(),
                            search = search,
                            onContentNavigate = navigator.onNavigate,
                        )
                    }
                }
                composable<Collections> {
                    val data: Collections = it.toRoute()
                    CollectionScreen(data.userToken, PaddingValues())
                }
                composable<CollectionContent> {
                    val content: CollectionContent = it.toRoute()
                    CollectionContentScreen(content.collectionId, PaddingValues())
                }
                composable<Person> {
                    val person: Person = it.toRoute()
                    PeopleScreen(PaddingValues(), person)
                }
                composable<Pin> {
                    val pin = it.toRoute<Pin>()
                    PinScreen(PaddingValues(), pin)
                }
                composable<Account.RecommendSettings.Blocklist> {
                    BlocklistSettingsScreen(PaddingValues())
                }
                composable<Account.RecommendSettings.BlockedFeedHistory> {
                    BlockedFeedHistoryScreen()
                }
                composable<Notification> {
                    ContentListDetailScreen(
                        innerPadding = PaddingValues(),
                        onSinglePaneDetailChanged = { isSinglePaneListDetailShowingDetail = it },
                    ) { navigator ->
                        NotificationScreen(
                            innerPadding = PaddingValues(),
                            onContentNavigate = navigator.onNavigate,
                        )
                    }
                }
                composable<Notification.NotificationSettings> {
                    NotificationSettingsScreen(PaddingValues())
                }
                composable<SentenceSimilarityTest> {
                    SentenceSimilarityTestScreen(PaddingValues())
                }
                composable<Account.AppearanceSettings> {
                    val args = it.toRoute<Account.AppearanceSettings>()
                    AppearanceSettingsScreen(
                        PaddingValues(),
                        setting = args.setting,
                        onExit = reloadBottomBarPreferences,
                    )
                }
                composable<Account.RecommendSettings> {
                    val args = it.toRoute<Account.RecommendSettings>()
                    ContentFilterSettingsScreen(
                        PaddingValues(),
                        setting = args.setting,
                    )
                }
                composable<Account.SystemAndUpdateSettings> {
                    SystemAndUpdateSettingsScreen(PaddingValues())
                }
                composable<Account.DeveloperSettings> {
                    DeveloperSettingsScreen(PaddingValues())
                }
                composable<Account.DeveloperSettings.ColorScheme> {
                    ColorSchemeScreen(PaddingValues())
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
