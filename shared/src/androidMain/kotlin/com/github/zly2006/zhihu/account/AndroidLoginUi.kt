/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.account

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.ui.components.WebviewComp
import com.github.zly2006.zhihu.ui.components.setupUpWebviewClient
import com.github.zly2006.zhihu.util.telemetry
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import java.util.TimeZone

actual val supportedLoginMethods: List<LoginMethod> = listOf(
    LoginMethod.Phone,
    LoginMethod.Qr,
    LoginMethod.Web,
)

actual val isLoginRiskControlSupported: Boolean = true

@Composable
actual fun rememberLoginHttpClient(cookies: MutableMap<String, String>): HttpClient {
    val context = LocalContext.current
    val httpClient = remember(context) { androidZhihuAccountStore(context).client.temporaryHttpClient(cookies) }
    DisposableEffect(httpClient) {
        onDispose(httpClient::close)
    }
    return httpClient
}

actual val phoneLoginDeviceInfo: ZhihuPhoneLoginDeviceInfo
    get() = currentAndroidApplicationContext().phoneLoginDeviceInfo()

actual fun decodePhoneLoginCaptchaImage(content: String) = runCatching {
    val encoded = content.substringAfter("base64,", content)
    val bytes = Base64.decode(encoded, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
}.getOrNull()

@Composable
actual fun QrLoginPane(onLoginSuccess: (String) -> Unit) {
    val context = LocalContext.current
    val accountStore = rememberZhihuAccountStore()
    SharedQrLoginPane(
        onLoginSuccess = { cookies ->
            if (accountStore.login(cookies.toMutableMap())) {
                telemetry(context, "login")
                onLoginSuccess(accountStore.session.username)
                true
            } else {
                false
            }
        },
        initialCookies = AccountData.data.cookies,
    )
}

@Composable
actual fun WebLoginPane(onLoginSuccess: (String) -> Unit) {
    val context = LocalContext.current
    val accountStore = rememberZhihuAccountStore()
    val scope = rememberCoroutineScope()
    var isVerifying by remember { mutableStateOf(false) }

    WebviewComp(
        modifier = Modifier.fillMaxSize(),
        onLoad = { webView ->
            configureWebLogin(webView) { cookies ->
                if (!isVerifying) {
                    isVerifying = true
                    scope.launch {
                        try {
                            if (accountStore.login(cookies.toMutableMap())) {
                                telemetry(context, "login")
                                onLoginSuccess(accountStore.session.username)
                            }
                        } finally {
                            isVerifying = false
                        }
                    }
                }
            }
        },
    )
}

@Composable
actual fun LoginRiskControlPane(
    url: String,
    cookies: Map<String, String>,
    onCookiesChanged: (Map<String, String>) -> Unit,
) {
    WebviewComp(
        modifier = Modifier.fillMaxSize(),
        onLoad = { webView ->
            configureRiskControlWebView(
                webView = webView,
                url = url,
                cookies = cookies,
                onCookiesChanged = onCookiesChanged,
            )
        },
    )
}

@SuppressLint("SetJavaScriptEnabled")
private fun configureWebLogin(
    webView: WebView,
    onCookiesReady: (Map<String, String>) -> Unit,
) {
    webView.setupUpWebviewClient()
    webView.settings.javaScriptEnabled = true
    webView.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest,
        ): Boolean {
            if (request.url.toString() == ZHIHU_HOME_URL) {
                webView.settings.userAgentString = AccountData.ANDROID_USER_AGENT
            }
            return request.url?.scheme == "zhihu"
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            if (url == ZHIHU_HOME_URL) {
                onCookiesReady(readWebViewCookies(url))
            }
        }
    }
    CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
    if (webView.url.isNullOrEmpty()) {
        CookieManager.getInstance().removeAllCookies { webView.loadUrl(ZHIHU_SIGNIN_URL) }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun configureRiskControlWebView(
    webView: WebView,
    url: String,
    cookies: Map<String, String>,
    onCookiesChanged: (Map<String, String>) -> Unit,
) {
    webView.setupUpWebviewClient()
    webView.settings.javaScriptEnabled = true
    webView.settings.userAgentString = ZHIHU_DESKTOP_USER_AGENT
    val cookieManager = CookieManager.getInstance()
    cookieManager.setAcceptThirdPartyCookies(webView, true)
    cookies.forEach { (name, value) ->
        cookieManager.setCookie(ZHIHU_HOME_URL, "$name=$value; Domain=.zhihu.com; Path=/")
    }
    cookieManager.flush()
    webView.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest,
        ): Boolean = request.url?.scheme == "zhihu"

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            onCookiesChanged(readWebViewCookies(url))
        }
    }
    if (webView.url != url) {
        webView.loadUrl(url)
    }
}

private fun readWebViewCookies(url: String?): Map<String, String> =
    parseCookieAssignments(
        CookieManager.getInstance().getCookie(url ?: ZHIHU_HOME_URL).orEmpty(),
    )

private fun Context.phoneLoginDeviceInfo(): ZhihuPhoneLoginDeviceInfo {
    val runtime = Runtime.getRuntime()
    val storage = StatFs(Environment.getDataDirectory().path)
    val installTime = runCatching {
        packageManager.getPackageInfo(packageName, 0).firstInstallTime
    }.getOrDefault(0L)
    return ZhihuPhoneLoginDeviceInfo(
        timezoneOffsetSeconds = TimeZone.getDefault().rawOffset / 1_000L,
        appInstallTimeMillis = installTime,
        notificationEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled(),
        bluetoothAvailable = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH),
        phoneBrand = Build.BRAND,
        phoneModel = Build.MODEL,
        androidRelease = Build.VERSION.RELEASE,
        cpuType = Build.SUPPORTED_ABIS.firstOrNull().orEmpty(),
        cpuCount = runtime.availableProcessors(),
        cpuUsage = "0.0",
        totalMemoryMegabytes = (runtime.totalMemory() / 1_048_576L).toInt(),
        freeMemoryMegabytes = (runtime.freeMemory() / 1_048_576L).toInt(),
        totalStorageMegabytes = (storage.totalBytes / 1_048_576L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        freeStorageMegabytes = (storage.availableBytes / 1_048_576L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
    )
}
