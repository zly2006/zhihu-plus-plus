/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.github.zly2006.zhihu.shared.account.DEFAULT_ZHIHU_USER_AGENT
import com.github.zly2006.zhihu.shared.account.ZhihuAccountProfileSnapshot
import com.github.zly2006.zhihu.shared.account.ZhihuAccountRepository
import com.github.zly2006.zhihu.shared.account.ZhihuAccountSession
import com.github.zly2006.zhihu.shared.account.ZhihuAccountSessionStore
import com.github.zly2006.zhihu.shared.data.Person
import com.github.zly2006.zhihu.shared.data.ZHIHU_READ_HISTORY_ADD_URL
import com.github.zly2006.zhihu.shared.data.ZhihuCookieStorage
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.buildZhihuReadHistoryBody
import com.github.zly2006.zhihu.shared.data.fetchVerifiedZhihuSession
import com.github.zly2006.zhihu.shared.data.fetchZhihuAuthenticatedJson
import com.github.zly2006.zhihu.shared.data.installZhihuCommonClientConfig
import com.github.zly2006.zhihu.util.signFetchRequest
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import java.io.File

object AccountData {
    val json = ZhihuJson.json

    internal val ANDROID_HEADERS = mapOf(
        "x-api-version" to "3.1.8",
        "x-app-version" to "10.61.0",
        "x-app-za" to
            "OS=Android&Release=12&Model=sdk_gphone64_arm64&VersionName=10.61.0&VersionCode=26107&Product=com.zhihu.android&Width=1440&Height=2952&Installer=%E7%81%B0%E5%BA%A6&DeviceType=AndroidPhone&Brand=google",
    )

    const val ANDROID_USER_AGENT = "com.zhihu.android/Futureve/10.61.0 Mozilla/5.0 (Linux; Android 12; sdk_gphone64_arm64 " +
        "Build/SE1A.220630.001.A1; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/57.0.1000.10 Mobile Safari/537.36"

    @Serializable
    data class Data(
        val login: Boolean = false,
        val username: String = "",
        val cookies: MutableMap<String, String> = mutableMapOf(),
        val userAgent: String = DEFAULT_ZHIHU_USER_AGENT,
        val self: Person? = null,
    )

    fun loadData(context: Context): Data {
        dataState.value = accountRepository(context).load().toAndroidData()
        return data
    }

    val data: Data
        get() = dataState.value
    private val dataState = MutableStateFlow<Data>(Data())

    @Composable
    fun asState() = dataState.collectAsState()

    fun saveData(context: Context, data: Data) {
        dataState.value = data
        // https://static.zhihu.com/zse-ck/v4/24df2abbfcb1b98cd5ce1b519f02eeabea28c83ac9d9ec2778dc5b03a3b8b710.js
        accountRepository(context).save(data.toSession())
    }

    fun cookieStorage(context: Context, cookies: MutableMap<String, String>? = null) =
        ZhihuCookieStorage(cookies ?: data.cookies) {
            if (cookies == null) {
                saveData(context, data)
            }
        }

    private var httpClient: HttpClient? = null
    private var httpClientFactoryOverride: ((Context, MutableMap<String, String>?) -> HttpClient)? = null

    private fun <T : HttpClientEngineConfig> HttpClientConfig<T>.applyCommonConfiguration(
        context: Context,
        cookies: MutableMap<String, String>?,
    ) {
        installZhihuCommonClientConfig(
            cookies = cookies ?: data.cookies,
            userAgent = data.userAgent,
            onCookieChanged = {
                if (cookies == null) {
                    saveData(context, data)
                }
            },
            enableHttpCache = true,
        )
    }

    fun createConfiguredHttpClient(
        context: Context,
        cookies: MutableMap<String, String>? = null,
        engine: HttpClientEngine? = null,
    ): HttpClient = if (engine == null) {
        HttpClient {
            applyCommonConfiguration(context, cookies)
        }
    } else {
        HttpClient(engine) {
            applyCommonConfiguration(context, cookies)
        }
    }

    @Synchronized
    fun overrideHttpClientFactoryForTesting(factory: ((Context, MutableMap<String, String>?) -> HttpClient)?) {
        httpClient?.close()
        httpClient = null
        httpClientFactoryOverride = factory
    }

    fun httpClient(context: Context, cookies: MutableMap<String, String>? = null): HttpClient {
        if (httpClient != null && cookies == null) {
            return httpClient!!
        }
        val httpClient = httpClientFactoryOverride?.invoke(context, cookies) ?: createConfiguredHttpClient(context, cookies)
        if (context is LifecycleOwner && cookies == null) { // 没有指定cookie
            // 大概率是，包括 MainActivity 等。
            ContextCompat.getMainExecutor(context).execute {
                context.lifecycle.addObserver(object : DefaultLifecycleObserver {
                    override fun onDestroy(owner: LifecycleOwner) {
                        httpClient.close()
                        AccountData.httpClient = null
                    }
                })
                this.httpClient = httpClient
            }
        }
        return httpClient
    }

    suspend fun verifyLogin(context: Context, cookies: Map<String, String>): Boolean {
        val map = cookies.toMutableMap()
        val httpClient = httpClient(context, map)
        val session = fetchVerifiedZhihuSession(httpClient, map, data.userAgent) ?: return false
        saveData(
            context,
            session.toAndroidData(),
        )
        return true
    }

    fun delete(context: Context) {
        dataState.value = Data()
        accountRepository(context).clear()
    }

    private fun accountRepository(context: Context) = ZhihuAccountRepository(
        AndroidAccountSessionStore(File(context.filesDir, "account.json")),
    )

    private fun Data.toSession(): ZhihuAccountSession = ZhihuAccountSession(
        login = login,
        username = username,
        cookies = cookies.toMutableMap(),
        userAgent = userAgent,
        profile = self?.let {
            ZhihuAccountProfileSnapshot(
                id = it.id,
                name = it.name,
                urlToken = it.urlToken,
                userType = it.userType,
                avatarUrl = it.avatarUrl,
            )
        },
        self = self?.let { json.encodeToJsonElement(it) },
    )

    private fun ZhihuAccountSession.toAndroidData(): Data = Data(
        login = login,
        username = username,
        cookies = cookies.toMutableMap(),
        userAgent = userAgent,
        self = self?.let {
            runCatching {
                json.decodeFromJsonElement<Person>(it)
            }.getOrNull()
        },
    )

    private class AndroidAccountSessionStore(
        private val file: File,
    ) : ZhihuAccountSessionStore {
        override fun readText(): String? = if (file.exists()) {
            file.readText()
        } else {
            null
        }

        override fun writeText(text: String) {
            file.parentFile?.mkdirs()
            file.writeText(text)
        }

        override fun delete() {
            file.delete()
        }
    }

    /**
     * 将snake_case的JSON转换为camelCase并解析为对象
     */
    internal inline fun <reified T> decodeJson(json: JsonElement): T {
        val convertedJson = ZhihuJson.snakeCaseToCamelCase(json)
        try {
            return this.json.decodeFromJsonElement<T>(convertedJson)
        } catch (e: SerializationException) {
            Log.e("AccountData", "Failed to parse JSON: $convertedJson", e)
            throw SerializationException("Failed to parse JSON: ${e.message}\n\n$convertedJson", e)
        }
    }

    class ZhPlusJsonSerializationException(
        val originalJson: JsonElement,
        message: String,
        cause: Throwable?,
    ) : SerializationException(message, cause)

    internal fun <T> decodeJson(serializer: KSerializer<T>, json: JsonElement): T {
        val convertedJson = ZhihuJson.snakeCaseToCamelCase(json)
        try {
            return this.json.decodeFromJsonElement(serializer, convertedJson)
        } catch (e: SerializationException) {
            throw ZhPlusJsonSerializationException(convertedJson, "Failed to parse JSON: ${e.message}", e)
        }
    }

    @Suppress("FunctionName")
    fun snake_case2camelCase(snakeCase: String): String = ZhihuJson.snakeCaseToCamelCase(snakeCase)

    @Suppress("FunctionName")
    fun snake_case2camelCase(json: JsonElement): JsonElement = ZhihuJson.snakeCaseToCamelCase(json)

    private var lastRefreshCookie = 0L

    suspend fun fetch(context: Context, url: String, block: suspend HttpRequestBuilder.() -> Unit = {}): JsonObject? {
        val client = httpClient(context)
        return fetchZhihuAuthenticatedJson(
            client = client,
            url = url,
            lastRefreshMillis = lastRefreshCookie,
            updateLastRefreshMillis = { lastRefreshCookie = it },
            block = block,
        )
    }

    suspend fun fetchGet(context: Context, url: String, block: suspend HttpRequestBuilder.() -> Unit = {}) = fetch(context, url) {
        block()
        method = HttpMethod.Get
    }

    suspend fun fetchPost(context: Context, url: String, block: suspend HttpRequestBuilder.() -> Unit = {}) = fetch(context, url) {
        block()
        method = HttpMethod.Post
    }

    /**
     * Signed convenience: auto-calls [signFetchRequest] then delegates to [fetch].
     * Mirrors JVM [DesktopAccountStore.signedFetchJson].
     */
    suspend fun signedFetch(
        context: Context,
        url: String,
        block: suspend HttpRequestBuilder.() -> Unit = {},
    ): JsonObject? = fetch(context, url) {
        signFetchRequest()
        block()
    }

    /** Signed GET convenience. */
    suspend fun signedFetchGet(
        context: Context,
        url: String,
        block: suspend HttpRequestBuilder.() -> Unit = {},
    ): JsonObject? = fetchGet(context, url) {
        signFetchRequest()
        block()
    }

    /** Signed POST convenience. */
    suspend fun signedFetchPost(
        context: Context,
        url: String,
        block: suspend HttpRequestBuilder.() -> Unit = {},
    ): JsonObject? = fetchPost(context, url) {
        signFetchRequest()
        block()
    }

    /**
     * 添加在线阅读历史记录
     * @param contentType 内容类型 (如 "article", "answer", "profile" 等)
     */
    suspend fun addReadHistory(
        context: Context,
        contentToken: String,
        contentType: String,
    ) {
        runCatching {
            fetchPost(context, ZHIHU_READ_HISTORY_ADD_URL) {
                signFetchRequest()
                contentType(ContentType.Application.Json)
                setBody(buildZhihuReadHistoryBody(contentToken, contentType))
            }
        }
    }
}
