package com.github.zly2006.zhihu.shared.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.shared.login.SharedQrLoginPane
import com.github.zly2006.zhihu.theme.ZhihuTheme
import com.github.zly2006.zhihu.ui.DesktopZhihuMain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal object DesktopLoginRequests {
    val version = MutableStateFlow(0)

    fun requestLogin() {
        version.update { it + 1 }
    }
}

@Composable
fun DesktopQrLoginScreen(
    store: DesktopAccountStore = remember { DesktopAccountStore() },
) {
    var statusText by remember { mutableStateOf("正在获取二维码") }
    var isLoggedIn by remember { mutableStateOf(false) }
    var didCheckSavedAccount by remember { mutableStateOf(false) }
    val loginRequestVersion by DesktopLoginRequests.version.collectAsState()

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

    LaunchedEffect(loginRequestVersion) {
        if (loginRequestVersion > 0) {
            statusText = "正在获取二维码"
            isLoggedIn = false
            didCheckSavedAccount = true
        }
    }

    ZhihuTheme {
        if (isLoggedIn) {
            DesktopZhihuMain()
        } else if (didCheckSavedAccount) {
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

private fun notifyUser(message: String) {
    runCatching {
        ProcessBuilder("terminal-notifier", "-message", message, "-sound", "default")
            .start()
    }
}
