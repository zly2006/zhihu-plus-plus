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

package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.github.zly2006.zhihu.account.ZHIHU_DESKTOP_USER_AGENT
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * 桌面端内嵌 WebView 组件，使用 JavaFX WebView + SwingPanel 嵌入 Compose Desktop。
 *
 * 对应 Android 端的 WebviewComp，用于承载知乎风控验证页面，实现嵌入式验证体验。
 *
 * @param url 需要加载的页面 URL
 * @param modifier Compose Modifier
 * @param userAgent 浏览器 User-Agent，默认使用知乎桌面端 UA
 * @param initialCookies 预设 cookies，在首个网络请求前写入 WebKit CookieHandler
 * @param onPageFinished 页面加载完成时的回调，携带当前 URL
 * @param onCookiesChanged cookie 变化时的回调，携带当前页面可见的全部 cookies（含 HttpOnly）
 */
@Composable
fun DesktopWebviewComp(
    url: String?,
    modifier: Modifier = Modifier,
    userAgent: String = ZHIHU_DESKTOP_USER_AGENT,
    initialCookies: Map<String, String> = emptyMap(),
    onPageFinished: (url: String?) -> Unit = {},
    onCookiesChanged: (Map<String, String>) -> Unit = {},
) {
    val pageFinished = rememberUpdatedState(onPageFinished)
    val cookiesChanged = rememberUpdatedState(onCookiesChanged)
    val panel = remember { JFXPanel() }
    val sessionRef = remember(panel) { AtomicReference<DesktopWebviewSession?>() }
    val disposed = remember(panel) { AtomicBoolean(false) }
    DisposableEffect(panel) {
        // 关闭隐式退出，避免验证页或登录页销毁后 JavaFX 工具包退出，导致再次打开时只得到空白页。
        Platform.setImplicitExit(false)
        onDispose {
            disposed.set(true)
            Platform.runLater {
                sessionRef.getAndSet(null)?.close()
                panel.scene = null
            }
        }
    }
    SwingPanel(
        modifier = modifier,
        factory = { panel.also { it.enableInputMethods(false) } },
        update = { jfxPanel ->
            Platform.runLater {
                if (disposed.get()) return@runLater
                val session = sessionRef.get()
                    ?: DesktopWebviewSession(
                        onPageFinished = { pageFinished.value(it) },
                        onCookiesChanged = { cookiesChanged.value(it) },
                    ).also {
                        sessionRef.set(it)
                        jfxPanel.scene = Scene(it.view)
                    }
                session.load(url, userAgent, initialCookies)
            }
        },
    )
}

/**
 * 桌面端风控验证 WebView 容器。
 *
 * 对应共享登录页中的二维码风控内容区域，
 * 负责加载风控验证 URL 并在用户完成验证后回传 cookies。
 */
@Composable
fun DesktopRiskControlWebView(
    url: String,
    cookies: Map<String, String>,
    onCookiesChanged: (Map<String, String>) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        DesktopWebviewComp(
            url = url,
            modifier = Modifier.fillMaxSize(),
            initialCookies = cookies,
            onPageFinished = {},
            onCookiesChanged = { updatedCookies ->
                onCookiesChanged(updatedCookies)
            },
        )
    }
}
