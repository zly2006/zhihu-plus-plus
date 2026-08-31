package com.github.zly2006.zhihu.account

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.android.Android

internal actual val accountHttpClientEngineFactory: HttpClientEngineFactory<*> = Android
