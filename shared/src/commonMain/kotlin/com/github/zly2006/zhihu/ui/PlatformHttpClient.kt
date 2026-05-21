package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import io.ktor.client.HttpClient

@Composable
expect fun rememberZhihuHttpClient(): HttpClient
