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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.github.zly2006.zhihu.shared.login.ZHIHU_DESKTOP_USER_AGENT
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView

/**
 * 桌面端内嵌 WebView 组件，使用 JavaFX WebView + SwingPanel 嵌入 Compose Desktop。
 *
 * 对应 Android 端的 WebviewComp，用于承载知乎风控验证页面，实现嵌入式验证体验。
 *
 * @param url 需要加载的页面 URL
 * @param modifier Compose Modifier
 * @param userAgent 浏览器 User-Agent，默认使用知乎桌面端 UA
 * @param initialCookies 预设 cookies，在页面加载前通过 JavaScript 注入
 * @param onPageFinished 页面加载完成时的回调，携带当前 URL
 * @param onCookiesChanged 页面加载完成后回传当前页面的 cookies
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
    var engineState by remember { mutableStateOf<WebEngine?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            Platform.runLater {
                engineState?.load(null)
            }
        }
    }

    SwingPanel(
        modifier = modifier,
        factory = {
            JFXPanel().also { jfxPanel ->
                jfxPanel.enableInputMethods(false)
                Platform.runLater {
                    val webView = WebView().apply {
                        engine.userAgent = userAgent
                        isContextMenuEnabled = true
                    }
                    val engine = webView.engine
                    engineState = engine

                    // 监听页面加载完成，与 Android 端 WebViewClient.onPageFinished 对应
                    engine.loadWorker.stateProperty().addListener { _, _, newState ->
                        if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                            val currentUrl = engine.location
                            onPageFinished(currentUrl)
                            readCookies(engine)?.let { onCookiesChanged(it) }
                        }
                    }

                    // 注入初始 cookies（用于风控验证的会话上下文）
                    if (initialCookies.isNotEmpty()) {
                        val cookieScript = initialCookies.entries.joinToString(";") { (name, value) ->
                            "document.cookie='$name=$value;path=/;domain=.zhihu.com'"
                        }
                        engine.documentProperty().addListener { _, _, doc ->
                            if (doc != null) {
                                engine.executeScript(cookieScript)
                            }
                        }
                    }

                    jfxPanel.scene = Scene(webView)

                    // 加载目标 URL
                    if (url != null) {
                        engine.load(url)
                    }
                }
            }
        },
    )
}

/**
 * 桌面端风控验证 WebView 容器。
 *
 * 对应 Android 端 LoginActivity 中 QrLoginPane 的风险控制内容区域，
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

/**
 * 从 JavaFX WebView 的 WebEngine 中读取页面 cookies。
 *
 * 通过 JavaScript 在页面上下文中读取 document.cookie，
 * 对应 Android 端的 CookieManager.getCookie()。
 *
 * 注意：document.cookie 不包含 HttpOnly cookies，
 * 但对于风控验证页面（用于人机验证的会话 cookie），此限制不影响功能。
 */
private fun readCookies(engine: WebEngine): Map<String, String>? {
    return try {
        val cookieString = engine.executeScript("document.cookie") as? String
            ?: return null
        if (cookieString.isBlank()) return null
        cookieString
            .split(";")
            .mapNotNull { entry ->
                val trimmed = entry.trim()
                if (trimmed.isBlank() || !trimmed.contains("=")) return@mapNotNull null
                val name = trimmed.substringBefore("=").trim()
                val value = trimmed.substringAfter("=").trim()
                if (name.isNotBlank() && value.isNotBlank()) name to value else null
            }.toMap()
            .ifEmpty { null }
    } catch (_: Exception) {
        null
    }
}
