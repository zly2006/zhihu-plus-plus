package com.github.zly2006.zhihu.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.github.zly2006.zhihu.*
import com.github.zly2006.zhihu.theme.ZhihuTheme
import kotlin.reflect.KClass

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("RestrictedApi")
@Composable
fun ZhihuMain(modifier: Modifier = Modifier.Companion, navController: NavHostController) {
    val bottomPadding = ScaffoldDefaults.contentWindowInsets.asPaddingValues().calculateBottomPadding()
    val activity = LocalContext.current as MainActivity
    val navEntry by navController.currentBackStackEntryAsState()
    fun changeDestination(destination: NavDestination) {
        if (!navEntry.hasRoute(destination::class)) {
            navController.navigate(destination) {
                popUpTo(Home())
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (navEntry != null) {
                if (isTopLevelDest(navEntry)) {
                    NavigationBar(
                        modifier = modifier.height(56.dp + bottomPadding),
                    ) {
                        @Composable
                        fun Item(
                            destination: NavDestination,
                            label: String,
                            icon: ImageVector,
                        ) {
                            NavigationBarItem(
                                navEntry.hasRoute(destination::class),
                                onClick = { changeDestination(destination) },
                                label = {
                                    Text(
                                        label, style = TextStyle(
                                            fontSize = 9.sp,
                                            color = LocalContentColor.current.copy(alpha = 0.6f)
                                        )
                                    )
                                },
                                alwaysShowLabel = false,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xff66ccff),
                                    indicatorColor = Color.Transparent,
                                ),
                                icon = {
                                    Icon(icon, contentDescription = label)
                                }
                            )
                        }
                        Item(Home(), "主页", Icons.Filled.Home)
                        Item(History, "历史", Icons.Filled.History)
                        Item(Account, "账号", Icons.Filled.ManageAccounts)
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            modifier = Modifier.padding(innerPadding),
            startDestination = Home()
        ) {
            composable<Home> {
                var homeType by remember { mutableStateOf(it.toRoute<Home>().type) }
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        TextButton(
                            onClick = {
                                homeType = Home.Type.Home
                            }, colors = ButtonDefaults.textButtonColors(
                                containerColor = if (homeType == Home.Type.Home)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.background,
                            )
                        ) {
                            Text("推荐")
                        }
                        TextButton(
                            onClick = {
                                homeType = Home.Type.Follow
                            },
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = if (homeType == Home.Type.Follow)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.background,
                            )
                        ) {
                            Text("关注")
                        }
                        TextButton(
                            onClick = {
                                homeType = Home.Type.Hot
                            },
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = if (homeType == Home.Type.Hot)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.background,
                            )
                        ) {
                            Text("热榜")
                        }
                    }
                    when (homeType) {
                        Home.Type.Home -> HomeScreen(activity::navigate)
                        Home.Type.Follow -> FollowScreen(activity::navigate)
                        Home.Type.Hot -> {
                        }
                    }
                }
            }
            composable<Question> { navEntry ->
                val question: Question = navEntry.toRoute()
                QuestionScreen(question, activity::navigate)
            }
            composable<Article> { navEntry ->
                val article: Article = navEntry.toRoute()
                ArticleScreen(article, activity::navigate)
            }
            composable<History> {
                HistoryScreen(activity::navigate)
            }
            composable<Account> {
                AccountSettingScreen(innerPadding, activity::navigate)
            }
            composable<Collections> {
                val data: Collections = it.toRoute()
                CollectionScreen(data.userToken, activity::navigate)
            }
            composable<CollectionContent> {
                val content: CollectionContent = it.toRoute()
                CollectionContentScreen(content.collectionId, activity::navigate)
            }
        }
    }
}

private fun isTopLevelDest(navEntry: NavBackStackEntry?): Boolean = navEntry.hasRoute(Home::class) ||
        navEntry.hasRoute(History::class) ||
        navEntry.hasRoute(Account::class)

internal fun NavBackStackEntry?.hasRoute(cls: KClass<out NavDestination>): Boolean {
    val dest = this?.destination ?: return false
    return dest.hierarchy.any { it.hasRoute(cls) } == true
}

@Preview(showBackground = true)
@Composable
private fun GreetingPreview() {
    val navController = rememberNavController()
    ZhihuTheme {
        ZhihuMain(navController = navController)
    }
}
