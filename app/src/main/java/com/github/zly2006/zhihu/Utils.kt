package com.github.zly2006.zhihu

import android.content.Context
import io.ktor.client.request.*
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext

suspend fun HttpRequestBuilder.signFetchRequest(context: Context) {
    val url = url.buildString()
    withContext(context.mainExecutor.asCoroutineDispatcher()) {
        header("x-zse-93", MainActivity.ZSE93)
        header("x-zse-96",
            (context as? MainActivity)?.signRequest96(url)
        )
        header("x-requested-with", "fetch")
    }
}
