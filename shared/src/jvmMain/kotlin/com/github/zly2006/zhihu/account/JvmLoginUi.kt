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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.github.zly2006.zhihu.desktop.defaultDesktopAccountStore
import com.github.zly2006.zhihu.platform.platformName
import com.github.zly2006.zhihu.ui.components.DesktopRiskControlWebView
import com.github.zly2006.zhihu.ui.components.DesktopWebviewComp
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import org.jetbrains.skia.Image
import java.util.Base64
import java.util.TimeZone

actual val supportedLoginMethods: List<LoginMethod> = listOf(
    LoginMethod.Phone,
    LoginMethod.Qr,
    LoginMethod.Web,
)

actual val isLoginRiskControlSupported: Boolean = true

@Composable
actual fun rememberLoginHttpClient(cookies: MutableMap<String, String>): HttpClient {
    val store = defaultDesktopAccountStore
    val httpClient = remember(store) { store.client.temporaryHttpClient(cookies) }
    DisposableEffect(httpClient) {
        onDispose(httpClient::close)
    }
    return httpClient
}

@Composable
actual fun rememberPhoneLoginDeviceInfo(): ZhihuPhoneLoginDeviceInfo {
    val runtime = Runtime.getRuntime()
    return remember {
        ZhihuPhoneLoginDeviceInfo(
            timezoneOffsetSeconds = TimeZone.getDefault().rawOffset / 1_000L,
            appInstallTimeMillis = 0,
            notificationEnabled = false,
            bluetoothAvailable = false,
            phoneBrand = platformName,
            phoneModel = System.getProperty("os.arch").orEmpty(),
            androidRelease = "12",
            cpuType = System.getProperty("os.arch").orEmpty(),
            cpuCount = runtime.availableProcessors(),
            cpuUsage = "0.0",
            totalMemoryMegabytes = (runtime.maxMemory() / 1_048_576L).toInt(),
            freeMemoryMegabytes = (runtime.freeMemory() / 1_048_576L).toInt(),
            totalStorageMegabytes = 0,
            freeStorageMegabytes = 0,
        )
    }
}

actual fun decodePhoneLoginCaptchaImage(content: String) = runCatching {
    val encoded = content.substringAfter("base64,", content)
    Image.makeFromEncoded(Base64.getDecoder().decode(encoded)).toComposeImageBitmap()
}.getOrNull()

@Composable
actual fun QrLoginPane(onLoginSuccess: (String) -> Unit) {
    val store = defaultDesktopAccountStore
    SharedQrLoginPane(
        onLoginSuccess = { cookies ->
            if (store.login(cookies.toMutableMap())) {
                onLoginSuccess(store.session.username)
                true
            } else {
                false
            }
        },
        initialCookies = store.session.cookies,
    )
}

@Composable
actual fun WebLoginPane(onLoginSuccess: (String) -> Unit) {
    val store = defaultDesktopAccountStore
    val scope = rememberCoroutineScope()
    var currentUrl by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }

    DesktopWebviewComp(
        url = ZHIHU_SIGNIN_URL,
        modifier = Modifier.fillMaxSize(),
        initialCookies = store.session.cookies,
        onPageFinished = { currentUrl = it },
        onCookiesChanged = { cookies ->
            if (currentUrl == ZHIHU_HOME_URL && !isVerifying) {
                isVerifying = true
                scope.launch {
                    try {
                        if (store.login(cookies.toMutableMap())) {
                            onLoginSuccess(store.session.username)
                        }
                    } finally {
                        isVerifying = false
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
) = DesktopRiskControlWebView(url, cookies, onCookiesChanged)
