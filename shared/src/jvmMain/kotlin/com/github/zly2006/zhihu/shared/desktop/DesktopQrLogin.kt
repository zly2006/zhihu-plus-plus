package com.github.zly2006.zhihu.shared.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.fetchHotListPage
import com.github.zly2006.zhihu.shared.data.flattenFeeds
import com.github.zly2006.zhihu.shared.data.toDisplayItem
import com.github.zly2006.zhihu.shared.login.SharedQrLoginPane

@Composable
fun DesktopQrLoginScreen(
    store: DesktopAccountStore = remember { DesktopAccountStore() },
) {
    var statusText by remember { mutableStateOf("正在获取二维码") }
    var isLoggedIn by remember { mutableStateOf(false) }
    var didCheckSavedAccount by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val savedData = store.load()
        val cookies = savedData.cookies
        if (savedData.login && cookies.isNotEmpty()) {
            statusText = "正在验证已备份 cookie"
            if (store.verifyAndSave(cookies)) {
                statusText = "已使用备份 cookie 登录：${store.load().username}"
                isLoggedIn = true
                return@LaunchedEffect
            }
        }
        didCheckSavedAccount = true
    }

    MaterialTheme {
        if (isLoggedIn) {
            DesktopHotListScreen(store = store)
            return@MaterialTheme
        }

        if (didCheckSavedAccount) {
            SharedQrLoginPane(
                createClient = { cookies -> store.createHttpClient(cookies) },
                onLoginSuccess = { cookies ->
                    store.verifyAndSave(cookies.toMutableMap()).also { success ->
                        if (success) {
                            isLoggedIn = true
                        }
                    }
                },
                initialCookies = store.load().cookies,
                qrReadyMessage = "请打开知乎 App 扫一扫",
                onQrReady = {
                    notifyUser("需要扫码登录 JVM 端")
                },
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.size(16.dp))
                Text(statusText)
            }
        }
    }
}

@Composable
private fun DesktopHotListScreen(
    store: DesktopAccountStore,
) {
    var refreshKey by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var hotItems by remember { mutableStateOf<List<FeedDisplayItem>>(emptyList()) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        errorText = null
        val account = store.load()
        store.createHttpClient(account.cookies).use { client ->
            runCatching {
                fetchHotListPage(client, account.cookies)
                    .data
                    .flattenFeeds()
                    .map { it.toDisplayItem(enableQualityFilter = false) }
            }.onSuccess {
                hotItems = it
            }.onFailure {
                errorText = it.message ?: "加载失败"
            }
        }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "知乎热榜",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "已登录：${store.load().username}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.size(12.dp))
            Button(onClick = { refreshKey += 1 }) {
                Text("刷新")
            }
        }

        when {
            isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            errorText != null -> Text(
                text = errorText.orEmpty(),
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.error,
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                items(hotItems, key = { it.stableKey }) { item ->
                    HotListItem(item)
                }
            }
        }
    }
}

@Composable
private fun HotListItem(item: FeedDisplayItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
            )
            item.summary?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = item.details,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun notifyUser(message: String) {
    runCatching {
        ProcessBuilder("terminal-notifier", "-message", message, "-sound", "default")
            .start()
    }
}
