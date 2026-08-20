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
import com.github.zly2006.zhihu.platform.MacosUserMessageHost
import com.github.zly2006.zhihu.platform.UserMessageDuration
import com.github.zly2006.zhihu.platform.macosQrLoginRequestVersion
import com.github.zly2006.zhihu.platform.showMacosUserMessage
import com.github.zly2006.zhihu.theme.ZhihuTheme
import com.github.zly2006.zhihu.ui.MacosWindowChromeState
import com.github.zly2006.zhihu.ui.MacosZhihuMain
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

@Composable
fun MacosQrLoginScreen(windowChromeState: MacosWindowChromeState? = null) {
    val store = remember { NativeAccountStore() }
    var statusText by remember { mutableStateOf("正在获取二维码") }
    var isLoggedIn by remember { mutableStateOf(false) }
    var didCheckSavedAccount by remember { mutableStateOf(false) }
    val loginRequestVersion by macosQrLoginRequestVersion.collectAsState()

    LaunchedEffect(Unit) {
        try {
            val savedData = store.load()
            val cookies = savedData.cookies
            if (savedData.login && cookies.isNotEmpty()) {
                statusText = "正在验证已备份 cookie"
                val verified = withTimeoutOrNull(10.seconds) {
                    store.verifyAndSave(cookies)
                } == true
                if (verified) {
                    statusText = "已使用备份 cookie 登录：${store.load().username}"
                    isLoggedIn = true
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            statusText = "备份 cookie 验证失败，正在获取二维码"
            showMacosUserMessage(
                "备份 cookie 验证失败：${error.message ?: "未知错误"}",
                UserMessageDuration.Long,
            )
        } finally {
            didCheckSavedAccount = true
        }
    }

    LaunchedEffect(loginRequestVersion) {
        if (loginRequestVersion > 0) {
            statusText = "正在获取二维码"
            isLoggedIn = false
            didCheckSavedAccount = true
        }
    }

    ZhihuTheme {
        MacosUserMessageHost {
            if (isLoggedIn) {
                MacosZhihuMain(windowChromeState)
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
                    qrReadyMessage = "请打开知乎++ App 扫一扫",
                    onQrReady = {
                        showMacosUserMessage("需要扫码登录 macOS 端")
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
}
