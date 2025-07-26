package com.github.zly2006.zhihu

import android.content.Context
import com.github.zly2006.zhihu.data.AccountData
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

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

@OptIn(DelicateCoroutinesApi::class)
fun telemetry(context: Context, usage: String) {
    require(usage in listOf("start", "login")) {
        "Usage must be either 'start' or 'login', but was '$usage'."
    }
    val preferences = context.getSharedPreferences("com.github.zly2006.zhihu.preferences", Context.MODE_PRIVATE)
    val data = AccountData.loadData(context)
    if (preferences.getBoolean("allowTelemetry", true)) {
        GlobalScope.launch {
            @OptIn(ExperimentalStdlibApi::class)
            runCatching {
                val hash = MessageDigest.getInstance("MD5").apply {
                    data.self!!.userType.toByteArray().let(this::update)
                    data.self.urlToken?.toByteArray()?.let(this::update)
                }
                    .digest(data.self!!.id.toByteArray())
                    .toHexString()
                AccountData.httpClient(context)
                    .post("https://redenmc.com/api/zhihu/usage?client_hash=$hash${data.self.id}&usage=$usage") {
                        contentType(ContentType.Application.Json)
                        header(
                            HttpHeaders.UserAgent,
                            "Zhihu++/${BuildConfig.VERSION_NAME}"
                        )
                    }
            }
        }
    }
}
