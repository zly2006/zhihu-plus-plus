/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only), as published by the Free Software Foundation.
 */

package com.github.zly2006.zhihu.ui.components

import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.concurrent.Worker
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.web.WebView
import javafx.util.Duration
import java.net.CookieHandler
import java.net.CookieManager
import java.net.URI

internal class DesktopWebviewSession(
    private val onPageFinished: (String?) -> Unit,
    private val onCookiesChanged: (Map<String, String>) -> Unit,
) {
    val view = WebView().apply { isContextMenuEnabled = true }
    private val engine = view.engine
    private val cookieHandler = CookieHandler.getDefault() ?: CookieManager().also { CookieHandler.setDefault(it) }
    private var loadedUrl: String? = null
    private var hasLoaded = false
    private var closed = false
    private var lastCookies: Map<String, String>? = null
    private val cookiePoll = Timeline(
        KeyFrame(Duration.millis(500.0), EventHandler<ActionEvent> { publishCookies() }),
    ).apply {
        cycleCount = Timeline.INDEFINITE
        play()
    }

    init {
        engine.loadWorker.stateProperty().addListener { _, _, state ->
            when (state) {
                Worker.State.SUCCEEDED -> {
                    onPageFinished(engine.location)
                    publishCookies()
                }
                Worker.State.FAILED -> engine.loadWorker.exception?.printStackTrace()
                else -> Unit
            }
        }
    }

    fun load(url: String?, userAgent: String, initialCookies: Map<String, String>) {
        if (closed) return
        engine.userAgent = userAgent
        if (hasLoaded && loadedUrl == url) return
        loadedUrl = url
        hasLoaded = true
        lastCookies = null
        if (url != null && initialCookies.isNotEmpty()) {
            runCatching { URI(url) }.getOrNull()?.let { uri ->
                val domain = if (uri.host == "zhihu.com" || uri.host?.endsWith(".zhihu.com") == true) {
                    "; Domain=.zhihu.com"
                } else {
                    ""
                }
                cookieHandler.put(
                    uri,
                    mapOf("Set-Cookie" to initialCookies.map { (name, value) -> "$name=$value; Path=/$domain" }),
                )
            }
        }
        engine.load(url)
    }

    private fun publishCookies() {
        if (closed) return
        val location = engine.location.takeIf { it.isNotBlank() } ?: return
        val uri = runCatching { URI(location) }.getOrNull() ?: return
        if (uri.scheme != "http" && uri.scheme != "https") return
        // WebKit 的 CookieJar 使用同一 CookieHandler；HTTP URI 能读到 document.cookie 隐藏的 HttpOnly 凭据。
        val cookies = cookieHandler
            .get(uri, emptyMap())
            .filterKeys { it.equals("Cookie", ignoreCase = true) }
            .values
            .flatten()
            .flatMap { it.split(';') }
            .mapNotNull { cookie ->
                val entry = cookie.trim()
                if ('=' !in entry) return@mapNotNull null
                val name = entry.substringBefore('=')
                if (name.isBlank()) return@mapNotNull null
                name to entry.substringAfter('=')
            }.toMap()
        if (cookies.isEmpty() || cookies == lastCookies) return
        lastCookies = cookies
        onCookiesChanged(cookies)
    }

    fun close() {
        if (closed) return
        closed = true
        cookiePoll.stop()
        engine.load(null)
    }
}
