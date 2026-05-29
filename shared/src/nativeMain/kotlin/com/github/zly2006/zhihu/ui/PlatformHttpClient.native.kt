package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.account.IosAccountStore
import io.ktor.client.HttpClient

@Composable
actual fun rememberZhihuHttpClient(): HttpClient {
    val store = remember { IosAccountStore() }
    val session = remember { store.load() }
    return remember(store, session) { store.createHttpClient(session.cookies.toMutableMap()) }
}
