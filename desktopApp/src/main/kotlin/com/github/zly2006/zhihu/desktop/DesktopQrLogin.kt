package com.github.zly2006.zhihu.desktop

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.fetchHotListPage
import com.github.zly2006.zhihu.shared.data.flattenFeeds
import com.github.zly2006.zhihu.shared.data.toDisplayItem
import com.github.zly2006.zhihu.shared.login.ZHIHU_RISK_CONTROL_URL
import com.github.zly2006.zhihu.shared.login.normalizeDeadline
import com.github.zly2006.zhihu.shared.login.pollQrCodeLogin
import com.github.zly2006.zhihu.shared.login.prefetchQrLoginContext
import com.github.zly2006.zhihu.shared.login.requestQrCode
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.CancellationException
import java.awt.Color
import java.awt.image.BufferedImage

@Composable
fun DesktopQrLoginScreen(
    store: DesktopAccountStore = remember { DesktopAccountStore() },
) {
    var refreshKey by remember { mutableIntStateOf(0) }
    var qrBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var statusText by remember { mutableStateOf("正在获取二维码") }
    var isWorking by remember { mutableStateOf(true) }
    var isLoggedIn by remember { mutableStateOf(false) }

    LaunchedEffect(refreshKey) {
        val savedData = store.load()
        val cookies = savedData.cookies
        if (savedData.login && cookies.isNotEmpty()) {
            statusText = "正在验证已备份 cookie"
            isWorking = true
            if (store.verifyAndSave(cookies)) {
                statusText = "已使用备份 cookie 登录：${store.load().username}"
                isWorking = false
                isLoggedIn = true
                return@LaunchedEffect
            }
        }

        store.createHttpClient(cookies).use { client ->
            qrBitmap = null
            statusText = "正在获取二维码"
            isWorking = true

            try {
                prefetchQrLoginContext(client, cookies)
                val qrCode = requestQrCode(client, cookies)
                val qrLink = qrCode.link ?: throw IllegalStateException("知乎没有返回二维码链接")
                val qrToken = qrCode.token ?: qrCode.qrcodeToken ?: throw IllegalStateException("知乎没有返回二维码 token")
                qrBitmap = generateQrBitmap(qrLink)
                statusText = "请打开知乎 App 扫一扫"
                notifyUser("需要扫码登录 JVM 端")

                val success = pollQrCodeLogin(
                    client = client,
                    cookies = cookies,
                    token = qrToken,
                    deadline = normalizeDeadline(qrCode.expiresAt),
                    onScanned = {
                        statusText = "请在知乎 App 上确认登录"
                    },
                    onRiskControl = { message, redirectUrl ->
                        statusText = message ?: "知乎需要验证当前网络环境：${redirectUrl ?: ZHIHU_RISK_CONTROL_URL}"
                        isWorking = false
                    },
                )

                if (success) {
                    statusText = if (store.verifyAndSave(cookies)) {
                        isLoggedIn = true
                        "登录成功，cookie 已备份"
                    } else {
                        "登录结果验证失败，请重试"
                    }
                    isWorking = false
                } else if (isWorking) {
                    statusText = "二维码已过期，请重试"
                    isWorking = false
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                statusText = e.message ?: "二维码获取失败，请重试"
                isWorking = false
            }
        }
    }

    MaterialTheme {
        if (isLoggedIn) {
            DesktopHotListScreen(store = store)
            return@MaterialTheme
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            qrBitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = "知乎登录二维码",
                    modifier = Modifier.size(280.dp),
                )
                Spacer(modifier = Modifier.size(16.dp))
            } ?: run {
                if (isWorking) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.size(16.dp))
                }
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.size(20.dp))

            Button(onClick = { refreshKey += 1 }) {
                Text("刷新二维码")
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

private fun generateQrBitmap(content: String): ImageBitmap {
    val size = 960
    val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
    for (x in 0 until size) {
        for (y in 0 until size) {
            image.setRGB(x, y, if (bitMatrix.get(x, y)) Color.BLACK.rgb else Color.WHITE.rgb)
        }
    }
    return image.toComposeImageBitmap()
}

private fun notifyUser(message: String) {
    runCatching {
        ProcessBuilder("terminal-notifier", "-message", message, "-sound", "default")
            .start()
    }
}
