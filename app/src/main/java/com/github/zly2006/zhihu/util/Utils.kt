package com.github.zly2006.zhihu.util

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.github.zly2006.zhihu.BuildConfig
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.AccountData.json
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.contentType
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.serializer
import java.security.MessageDigest

suspend fun HttpRequestBuilder.signFetchRequest(context: Context) {
    val url = url.buildString()
    val body = if (contentType() == ContentType.Application.Json) {
        body as? String
            ?: bodyType?.kotlinType?.let { type ->
                json.encodeToString(serializer(type), body)
            }
    } else {
        null
    }
    withContext(context.mainExecutor.asCoroutineDispatcher()) {
        header("x-zse-93", MainActivity.ZSE93)
        header(
            "x-zse-96",
            (context as? MainActivity)?.signRequest96(url, body),
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
                val hash = MessageDigest
                    .getInstance("MD5")
                    .apply {
                        data.self!!
                            .userType
                            .toByteArray()
                            .let(this::update)
                        data.self.urlToken
                            ?.toByteArray()
                            ?.let(this::update)
                    }.digest(data.self!!.id.toByteArray())
                    .toHexString()
                AccountData
                    .httpClient(context)
                    .post("https://redenmc.com/api/zhihu/usage?client_hash=$hash${data.self.id}&usage=$usage") {
                        contentType(ContentType.Application.Json)
                        header(
                            HttpHeaders.UserAgent,
                            "Zhihu++/${BuildConfig.VERSION_NAME}",
                        )
                    }
            }
        }
    }
}

/**
 * 洛天依主题浏览器打开
 */
fun luoTianYiUrlLauncher(context: Context, uri: Uri) {
    if (uri.host == "link.zhihu.com") {
        Url(uri.toString()).parameters["target"]?.let {
            luoTianYiUrlLauncher(context, it.toUri())
            return
        }
    }
    val intent = CustomTabsIntent
        .Builder()
        .setDefaultColorSchemeParams(
            CustomTabColorSchemeParams
                .Builder()
                .setToolbarColor(0xff_66CCFF.toInt())
                .build(),
        ).build()
    intent.launchUrl(context, uri)
}

val Context.clipboardManager
    get() = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
