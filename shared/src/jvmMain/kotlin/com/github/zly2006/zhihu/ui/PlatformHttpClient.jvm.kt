package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import io.ktor.client.HttpClient

@Composable
actual fun rememberZhihuHttpClient(): HttpClient {
    val store = remember { DesktopAccountStore() }
    val session = remember { store.load() }
    val client = remember(store, session) { store.createHttpClient(session.cookies) }
    DisposableEffect(client) {
        onDispose { client.close() }
    }
    return client
}
