package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.data.AccountData
import io.ktor.client.HttpClient

@Composable
actual fun rememberZhihuHttpClient(): HttpClient = AccountData.httpClient(LocalContext.current)
