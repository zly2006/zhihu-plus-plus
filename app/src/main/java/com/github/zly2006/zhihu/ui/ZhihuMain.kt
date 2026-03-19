package com.github.zly2006.zhihu.ui

import android.annotation.SuppressLint
import android.content.SharedPreferences
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
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
import com.github.zly2006.zhihu.ui.subscreens.BlockedFeedHistoryScreen
import com.github.zly2006.zhihu.ui.subscreens.ColorSchemeScreen
import com.github.zly2006.zhihu.ui.subscreens.ContentFilterSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.DeveloperSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.SystemAndUpdateSettingsScreen
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import kotlin.reflect.KClass
import com.github.zly2006.zhihu.ui.NavHost as MyNavHost

const val SURVEY_URL = "https://v.wjx.cn/vm/Ppfw2R4.aspx#"

@SuppressLint("RestrictedApi")
@Composable
fun ZhihuMain(modifier: Modifier = Modifier, navController: NavHostController) {
    val bottomPadding = ScaffoldDefaults.contentWindowInsets.asPaddingValues().calculateBottomPadding()
    val activity = LocalActivity.current as MainActivity
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(PREFERENCE_NAME, android.content.Context.MODE_PRIVATE) }

    val keySurveyDone = "survey_feedback_done"
    var installed3Hours by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val installTime = try {
            context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
        if (System.currentTimeMillis() - installTime >= 3 * 60 * 60 * 1000L) {
            installed3Hours = true
        }
    }

    // 首次启动提示
    var showFilterExplainDialog by remember {
        mutableStateOf(!preferences.getBoolean("filterExplainDialogShown", false))
    }
    var uiChanges by remember {
        mutableStateOf(!preferences.getBoolean("duo3uiChangesDialogShown", false))
    }
    // 首次启动过滤说明对话框
    if (showFilterExplainDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("为什么有的内容突然消失了？") },
            text = {
                Text(
                    "知乎++会默认屏蔽知乎盐选、知乎广告平台、知乎学堂、微信公众号文章。" +
                        "除此之外，您也可以手动屏蔽的用户、话题、问题等内容。" +
                        "由于我们需要更详细的数据来精准屏蔽，而获取数据需要时间，所以他们会闪一下然后消失。",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    preferences.edit().putBoolean("filterExplainDialogShown", true).apply()
                    showFilterExplainDialog = false
                }) {
                    Text("好")
                }
            },
        )
    }
    if (uiChanges && installed3Hours) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("UI新变化！") },
            text = {
                Text(
                    "知乎++正在测试一套新的UI。欢迎尝试。如有任何意见，请在GitHub issues提出。",
                )
            },
            dismissButton = {
                TextButton(onClick = {
                    preferences.edit().putBoolean("duo3uiChangesDialogShown", true).apply()
                    uiChanges = false
                }) {
                    Text("算了")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    preferences.edit().putBoolean("duo3uiChangesDialogShown", true).apply()
                    uiChanges = false
                    activity.navigate(Account.AppearanceSettings("123Duo3"))
                }) {
                    Text("去看看")
                }
            },
        )
    }
    if (installed3Hours) {
        // 暂停显示调查问卷对话框
//        AlertDialog(
//            onDismissRequest = { showSurveyDialog = false },
//            title = { Text("希望听到您的声音") },
//            text = {
//                Text("我们诚挚地邀请您填写一份简短的调查问卷，帮助我们了解您的使用体验和需求，以便为您带来更好的产品。只需 1~2 分钟，非常感谢您的支持！")
//            },
//            confirmButton = {
//                TextButton(onClick = {
//                    showSurveyDialog = false
//                    preferences.edit { putBoolean(keySurveyDone, true) }
//                    val intent = Intent(
//                        Intent.ACTION_VIEW,
//                        SURVEY_URL.toUri(),
//                    )
//                    context.startActivity(intent)
//                }) { Text("去填写") }
//            },
//            dismissButton = {
//                androidx.compose.foundation.layout.Row {
//                    TextButton(onClick = { showSurveyDialog = false }) { Text("取消") }
//                    TextButton(onClick = {
//                        showSurveyDialog = false
//                        preferences.edit { putBoolean(keySurveyDone, true) }
//                    }) { Text("我已填写") }
//                }
//            },
//        )
    }

    // 底部导航栏功能
    var duo3HomeAccount by remember { mutableStateOf(preferences.getBoolean("duo3_home_account", false)) }
    var duo3NavStyle by remember { mutableStateOf(preferences.getBoolean("duo3_nav_style", false)) }
    var tapToScrollToTopEnabled by remember { mutableStateOf(preferences.getBoolean("bottomBarTapScrollToTop", true)) }
    var autoHideBottomBar by remember { mutableStateOf(preferences.getBoolean("autoHideBottomBar", false)) }
    val preferenceListener = remember(preferences) {
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "duo3_home_account" -> duo3HomeAccount = preferences.getBoolean("duo3_home_account", false)
                "duo3_nav_style" -> duo3NavStyle = preferences.getBoolean("duo3_nav_style", false)
                "bottomBarTapScrollToTop" -> tapToScrollToTopEnabled = preferences.getBoolean("bottomBarTapScrollToTop", true)
                "autoHideBottomBar" -> autoHideBottomBar = preferences.getBoolean("autoHideBottomBar", false)
            }
        }
    }

    DisposableEffect(preferences, preferenceListener) {
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        onDispose {
            preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        }
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
        modifier = modifier
            .nestedScroll(bottomBarScrollConnection)
            .semantics { testTagsAsResourceId = true },
        bottomBar = {
            val navEntry by navController.currentBackStackEntryAsState()
            if (navEntry != null) {
                // 页面切换时重置底部导航栏可见状态
                LaunchedEffect(navEntry) { isBottomBarVisible = true }
                AnimatedVisibility(
                    visible = (!autoHideBottomBar || isBottomBarVisible) && isTopLevelDest(navEntry),
                    enter = slideInVertically(tween(200)) { it },
                    exit = slideOutVertically(tween(200)) { it },
                ) {
                    NavigationBar(
                        modifier = Modifier.height(
                            (if (duo3NavStyle) 64.dp else 56.dp) + bottomPadding,
                        ),
                    ) {
                        val allItems = if (duo3HomeAccount) {
                            listOf(
                                Triple(Home, "主页", Icons.Filled.Home),
                                Triple(Follow, "关注", Icons.Filled.Group),
                                Triple(HotList, "热榜", Icons.Filled.Whatshot),
                                Triple(Daily, "日报", Icons.Filled.Newspaper),
                            )
                        } else {
                            listOf(
                                Triple(Home, "主页", Icons.Filled.Home),
                                Triple(Follow, "关注", Icons.Filled.PersonAddAlt1),
                                Triple(HotList, "热榜", Icons.Filled.Whatshot),
                                Triple(Daily, "日报", Icons.Filled.Newspaper),
                                Triple(OnlineHistory, "历史", Icons.Filled.History),
                                Triple(Account, "账号", Icons.Filled.ManageAccounts),
                            )
                        }
                        val defaultKeys = if (duo3HomeAccount) {
                            setOf(Home.name, Follow.name, Daily.name)
                        } else {
                            setOf(Home.name, Follow.name, Daily.name, OnlineHistory.name, Account.name)
                        }
                        val selectedKeys = preferences.getStringSet("bottom_bar_items", defaultKeys) ?: defaultKeys
                        val bottomBarItems = allItems.filter { it.first.name in selectedKeys }

                        @Composable
                        fun Item(
                            destination: NavDestination,
                            label: String,
                            icon: ImageVector,
                        ) {
                            val tag = "nav_tab_${(destination as? TopLevelDestination)?.name?.lowercase() ?: label.lowercase()}"
                            NavigationBarItem(
                                navEntry.hasRoute(destination::class),
                                onClick = {
                                    if (!navEntry.hasRoute(destination::class)) {
                                        navController.navigate(destination) {
                                            popUpTo(Home)
                                            launchSingleTop = true
                                            restoreState = true
                                        }
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
                                    NavigationBarItemDefaults.colors()
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
                onNavigate = activity::navigate,
                onNavigateBack = navController::popBackStack,
            ),
        ) {
            MyNavHost(
                navController,
                modifier = Modifier,
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
                    HomeScreen(
                        scrollToTopTrigger = scrollToTopTrigger,
                        innerPadding = innerPadding,
                    )
                }
                composable<Question> { navEntry ->
                    val question: Question = navEntry.toRoute()
                    QuestionScreen(question, innerPadding)
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
                    ArticleScreen(article, viewModel, innerPadding)
                }
                composable<HotList> {
                    HotListScreen(innerPadding)
                }
                composable<Follow> {
                    FollowScreen(innerPadding)
                }
                composable<Daily> {
                    DailyScreen(innerPadding)
                }
                composable<History> {
                    HistoryScreen(innerPadding)
                }
                composable<OnlineHistory> {
                    OnlineHistoryScreen(innerPadding)
                }
                composable<Account> {
                    AccountSettingScreen(innerPadding)
                }
                composable<Search> { navEntry ->
                    val search: Search = navEntry.toRoute()
                    SearchScreen(innerPadding, search)
                }
                composable<Collections> {
                    val data: Collections = it.toRoute()
                    CollectionScreen(data.userToken, innerPadding)
                }
                composable<CollectionContent> {
                    val content: CollectionContent = it.toRoute()
                    CollectionContentScreen(content.collectionId, innerPadding)
                }
                composable<Person> {
                    val person: Person = it.toRoute()
                    PeopleScreen(innerPadding, person)
                }
                composable<Pin> {
                    val pin = it.toRoute<Pin>()
                    PinScreen(innerPadding, pin)
                }
                composable<Account.RecommendSettings.Blocklist> {
                    BlocklistSettingsScreen(innerPadding)
                }
                composable<Account.RecommendSettings.BlockedFeedHistory> {
                    BlockedFeedHistoryScreen()
                }
                composable<Notification> {
                    NotificationScreen(innerPadding)
                }
                composable<Notification.NotificationSettings> {
                    NotificationSettingsScreen(innerPadding)
                }
                composable<SentenceSimilarityTest> {
                    SentenceSimilarityTestScreen(innerPadding)
                }
                composable<Account.AppearanceSettings> {
                    val args = it.toRoute<Account.AppearanceSettings>()
                    AppearanceSettingsScreen(
                        innerPadding,
                        setting = args.setting,
                    )
                }
                composable<Account.RecommendSettings> {
                    val args = it.toRoute<Account.RecommendSettings>()
                    ContentFilterSettingsScreen(
                        innerPadding,
                        setting = args.setting,
                    )
                }
                composable<Account.SystemAndUpdateSettings> {
                    SystemAndUpdateSettingsScreen(innerPadding)
                }
                composable<Account.DeveloperSettings> {
                    DeveloperSettingsScreen(innerPadding)
                }
                composable<Account.DeveloperSettings.ColorScheme> {
                    ColorSchemeScreen(innerPadding)
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
