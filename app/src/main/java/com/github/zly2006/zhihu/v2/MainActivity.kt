package com.github.zly2006.zhihu.v2

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.HistoryStorage
import com.github.zly2006.zhihu.v2.theme.ZhihuTheme
import kotlinx.coroutines.CompletableDeferred
import java.security.MessageDigest
import kotlin.reflect.KClass

class MainActivity : ComponentActivity() {
    lateinit var webview: WebView
    lateinit var history: HistoryStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZhihuTheme {
                ZhihuMain()
            }
        }
    }

    suspend fun signRequest96(url: String): String {
        val dc0 = AccountData.getData().cookies["d_c0"] ?: ""
        val zse93 = "101_3_3.0"
        val pathname = "/" + url.substringAfter("//").substringAfter('/')
        val signSource = "$zse93+$pathname+$dc0"
        val md5 = MessageDigest.getInstance("MD5").digest(signSource.toByteArray()).joinToString("") {
            "%02x".format(it)
        }
        val future = CompletableDeferred<String>()
        webview.evaluateJavascript("exports.encrypt('$md5')") {
            Log.i("MainActivity", "Sign request: $url")
            Log.i("MainActivity", "Sign source: $signSource")
            Log.i("MainActivity", "Sign result: $it")
            future.complete(it.trim('"'))
        }
        return "2.0_" + future.await()
    }
}

@SuppressLint("RestrictedApi")
@Composable
fun ZhihuMain(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val bottomPadding = ScaffoldDefaults.contentWindowInsets.asPaddingValues().calculateBottomPadding()
    Scaffold(
        bottomBar = {
            val navEntry by navController.currentBackStackEntryAsState()
            if (navEntry != null) {
                if (navEntry.hasRoute(Home::class) ||
                    navEntry.hasRoute(Follow::class) ||
                    navEntry.hasRoute(History::class) ||
                    navEntry.hasRoute(Account::class)
                ) {
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
                                onClick = {
                                    navController.navigate(destination) {
                                        popUpTo(Home)
                                        launchSingleTop = true
                                        restoreState = true
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

@Composable
fun FeedItem(
    feed: Feed,
    onFeedClick: (Feed) -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
//        Text(feed.title)
//        Text(feed.summary)
//        Text(feed.details)
        Button(onClick = { onFeedClick(feed) }) {
            Text("Click")
        }
    }
}

@Composable
fun FeedList(
    data: MutableList<Feed>,
    modifier: Modifier = Modifier,
    onFeedClick: (Feed) -> Unit,
    loadMore: suspend () -> Unit
) {
    val listState = rememberLazyListState()
    val reachBottom by remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == data.size - 1
        }
    }
    LaunchedEffect(reachBottom) {
        while (reachBottom) {
            loadMore()
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
    ) {
        items(data) { feed ->
            FeedItem(feed, onFeedClick)
        }
    }
}
