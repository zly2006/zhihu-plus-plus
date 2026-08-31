package com.github.zly2006.zhihu.account

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO

internal actual val accountHttpClientEngineFactory: HttpClientEngineFactory<*> = CIO
