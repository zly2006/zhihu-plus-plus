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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.github.zly2006.zhihu.platform.platformName
import io.ktor.client.HttpClient
import org.jetbrains.skia.Image
import kotlin.io.encoding.Base64

actual val supportedLoginMethods: List<LoginMethod> = listOf(
    LoginMethod.Phone,
    LoginMethod.Qr,
)

actual val isLoginRiskControlSupported: Boolean = false

@Composable
actual fun rememberLoginHttpClient(cookies: MutableMap<String, String>): HttpClient {
    val store = defaultNativeAccountStore
    val httpClient = remember(store) { store.client.temporaryHttpClient(cookies) }
    DisposableEffect(httpClient) {
        onDispose(httpClient::close)
    }
    return httpClient
}

@Composable
actual fun rememberPhoneLoginDeviceInfo(): ZhihuPhoneLoginDeviceInfo = remember {
    ZhihuPhoneLoginDeviceInfo(
        timezoneOffsetSeconds = 0,
        appInstallTimeMillis = 0,
        notificationEnabled = false,
        bluetoothAvailable = false,
        phoneBrand = platformName,
        phoneModel = platformName,
        androidRelease = "12",
        cpuType = platformName,
        cpuCount = 1,
        cpuUsage = "0.0",
        totalMemoryMegabytes = 0,
        freeMemoryMegabytes = 0,
        totalStorageMegabytes = 0,
        freeStorageMegabytes = 0,
    )
}

actual fun decodePhoneLoginCaptchaImage(content: String) = runCatching {
    val encoded = content.substringAfter("base64,", content)
    Image.makeFromEncoded(Base64.Default.decode(encoded)).toComposeImageBitmap()
}.getOrNull()

@Composable
actual fun QrLoginPane(onLoginSuccess: (String) -> Unit) {
    val store = defaultNativeAccountStore
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
actual fun WebLoginPane(onLoginSuccess: (String) -> Unit): Unit =
    error("$platformName 暂不支持网页登录")

@Composable
actual fun LoginRiskControlPane(
    url: String,
    cookies: Map<String, String>,
    onCookiesChanged: (Map<String, String>) -> Unit,
): Unit = error("$platformName 暂不支持登录风控验证")
