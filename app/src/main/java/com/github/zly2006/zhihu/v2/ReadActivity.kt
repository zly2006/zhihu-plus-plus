package com.github.zly2006.zhihu.v2

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.github.zly2006.zhihu.*
import com.github.zly2006.zhihu.v2.theme.ZhihuTheme
import kotlin.reflect.KClass

class ReadActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZhihuTheme {
                ZhihuMain()
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
fun ZhihuMain(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val bottomPadding = ScaffoldDefaults.contentWindowInsets.asPaddingValues().calculateBottomPadding()
    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = modifier.height(56.dp + bottomPadding),
            ) {
                val navEntry by navController.currentBackStackEntryAsState()

                @Composable
                fun Item(
                    destination: NavDestination,
                    label: String,
                    icon: ImageVector,
                ) {
                    NavigationBarItem(
                        navEntry.hasRoute(destination::class),
                        onClick = {
                            navController.navigate(destination) {
                                popUpTo(Home)
                            }
                        },
                        label = {
                            Text(
                                label, style = TextStyle(
                                    fontSize = 9.sp,
                                    color = LocalContentColor.current.copy(alpha = 0.6f)
                                )
                            )
                        },
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors().copy(
                            selectedIconColor = Color(0xff66ccff)
                        ),
                        icon = {
                            Icon(icon, contentDescription = label)
                        }
                    )
                }
                Item(Home, "主页", Icons.Filled.Home)
                Item(Follow, "关注", Icons.Filled.PersonAddAlt1)
                Item(History, "历史", Icons.Filled.History)
                Item(Account, "账号", Icons.Filled.ManageAccounts)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            modifier = Modifier.padding(innerPadding),
            startDestination = Home
        ) {
            composable<Home> {
                Text("Home")
            }
            composable<Question> { navEntry ->
                val question: Question = navEntry.toRoute()
                Text("Question ${question.questionId}")
            }
            composable<Follow> {
                Text("Follow")
            }
            composable<History> {
                Text("History")
            }
            composable<Account> {
                LaunchedEffect(Unit) {

                }
                Column {
                    @Composable
                    fun displayPadding(padding: PaddingValues) = buildString {
                        append("(")
                        append(padding.calculateLeftPadding(LocalLayoutDirection.current))
                        append(", ")
                        append(padding.calculateTopPadding())
                        append(", ")
                        append(padding.calculateRightPadding(LocalLayoutDirection.current))
                        append(", ")
                        append(padding.calculateBottomPadding())
                        append(", start=")
                        append(padding.calculateStartPadding(LocalLayoutDirection.current))
                        append(")")
                    }
                    Text("Account")
                    Text("当前padding: ${displayPadding(innerPadding)}")
                    Text("statusBars: ${displayPadding(WindowInsets.statusBars.asPaddingValues())}")
                    Text("contentWindowInsets: ${displayPadding(ScaffoldDefaults.contentWindowInsets.asPaddingValues())}")
                }
            }
        }
    }
}

private fun NavBackStackEntry?.hasRoute(cls: KClass<out NavDestination>): Boolean {
    val dest = this?.destination ?: return false
    return dest.hierarchy.any { it.hasRoute(cls) } == true
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ZhihuTheme {
        ZhihuMain()
    }
}
