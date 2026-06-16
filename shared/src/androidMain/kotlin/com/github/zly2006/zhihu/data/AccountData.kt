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
import com.github.zly2006.zhihu.shared.account.ZhihuAccountClient
import com.github.zly2006.zhihu.shared.account.ZhihuAccountProfileSnapshot
import com.github.zly2006.zhihu.shared.account.ZhihuAccountRepository
import com.github.zly2006.zhihu.shared.account.ZhihuAccountSession
import com.github.zly2006.zhihu.shared.account.ZhihuAccountSessionStore
import com.github.zly2006.zhihu.shared.data.Person
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.installZhihuCommonClientConfig
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
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
        dataState.value = accountClient(context).load().toAndroidData()
        return data
    }

    val data: Data
        get() = dataState.value
    private val dataState = MutableStateFlow<Data>(Data())

    @Composable
    fun asState() = dataState.collectAsState()

    fun saveData(context: Context, data: Data) {
        dataState.value = data
        accountClient(context).save(data.toSession())
    }

    private var accountClient: ZhihuAccountClient? = null
    private var observedLifecycleClient: HttpClient? = null
    private var httpClientFactoryOverride: ((Context, MutableMap<String, String>?) -> HttpClient)? = null

    private fun <T : HttpClientEngineConfig> HttpClientConfig<T>.applyCommonConfiguration(
        context: Context,
        cookies: MutableMap<String, String>?,
        onCookieChanged: (() -> Unit)? = null,
    ) {
        installZhihuCommonClientConfig(
            cookies = cookies ?: data.cookies,
            userAgent = data.userAgent,
            onCookieChanged = {
                if (onCookieChanged != null) {
                    onCookieChanged()
                } else if (cookies == null) {
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
        onCookieChanged: (() -> Unit)? = null,
    ): HttpClient = if (engine == null) {
        HttpClient {
            applyCommonConfiguration(context, cookies, onCookieChanged)
        }
    } else {
        HttpClient(engine) {
            applyCommonConfiguration(context, cookies, onCookieChanged)
        }
    }

    @Synchronized
    fun overrideHttpClientFactoryForTesting(factory: ((Context, MutableMap<String, String>?) -> HttpClient)?) {
        accountClient?.invalidateHttpClient()
        accountClient = null
        observedLifecycleClient = null
        httpClientFactoryOverride = factory
    }

    fun httpClient(context: Context, cookies: MutableMap<String, String>? = null): HttpClient {
        if (cookies != null) {
            return httpClientFactoryOverride?.invoke(context, cookies)
                ?: accountClient(context).temporaryHttpClient(cookies)
        }
        val client = accountClient(context).httpClient()
        if (context is LifecycleOwner && observedLifecycleClient !== client) {
            ContextCompat.getMainExecutor(context).execute {
                context.lifecycle.addObserver(object : DefaultLifecycleObserver {
                    override fun onDestroy(owner: LifecycleOwner) {
                        accountClient(context).invalidateHttpClient()
                        observedLifecycleClient = null
                    }
                })
                observedLifecycleClient = client
            }
        }
        return client
    }

    suspend fun verifyLogin(context: Context, cookies: Map<String, String>): Boolean =
        accountClient(context).verifyAndSave(cookies.toMutableMap())

    fun delete(context: Context) {
        accountClient(context).clear()
    }

    private fun accountClient(context: Context): ZhihuAccountClient {
        accountClient?.let { return it }
        val appContext = context.applicationContext
        return ZhihuAccountClient(
            repository = ZhihuAccountRepository(
                AndroidAccountSessionStore(File(appContext.filesDir, "account.json")),
            ),
            createClient = { cookies, _, onCookieChanged, isTemporary ->
                httpClientFactoryOverride?.invoke(appContext, if (isTemporary) cookies else null)
                    ?: createConfiguredHttpClient(
                        context = appContext,
                        cookies = cookies,
                        onCookieChanged = onCookieChanged,
                    )
            },
            onSessionChanged = { session ->
                dataState.value = session.toAndroidData()
            },
        ).also {
            accountClient = it
        }
    }

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
}
