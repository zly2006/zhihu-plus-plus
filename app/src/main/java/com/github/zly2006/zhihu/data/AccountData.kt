package com.github.zly2006.zhihu.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.get
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.File

object AccountData {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    internal val ANDROID_HEADERS = mapOf(
        "x-api-version" to "3.1.8",
        "x-app-version" to "10.61.0",
        "x-app-za" to "OS=Android&Release=12&Model=sdk_gphone64_arm64&VersionName=10.61.0&VersionCode=26107&Product=com.zhihu.android&Width=1440&Height=2952&Installer=%E7%81%B0%E5%BA%A6&DeviceType=AndroidPhone&Brand=google",
    )

    const val ANDROID_USER_AGENT = "com.zhihu.android/Futureve/10.61.0 Mozilla/5.0 (Linux; Android 12; sdk_gphone64_arm64 Build/SE1A.220630.001.A1; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/57.0.1000.10 Mobile Safari/537.36"

    @Serializable
    data class Data(
        val login: Boolean = false,
        val username: String = "",
        val cookies: MutableMap<String, String> = mutableMapOf(),
        val userAgent: String = "Mozilla/5.0 (X11; U; Linux x86_64; en-US) AppleWebKit/540.0 (KHTML, like Gecko) Ubuntu/10.10 Chrome/9.1.0.0 Safari/540.0",
        val self: Person? = null,
    )

    fun loadData(context: Context): Data {
        val file = File(context.filesDir, "account.json")
        runCatching {
            if (file.exists()) {
                dataState.value = json.decodeFromString<Data>(file.readText())
            }
        }
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
        val file = File(context.filesDir, "account.json")
        file.writeText(json.encodeToString(data))
    }

    fun httpClient(context: Context, cookies: MutableMap<String, String>? = null): HttpClient = HttpClient {
        install(HttpCache)
        install(HttpCookies) {
            storage = object : CookiesStorage {
                override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
                    // https://github.com/zly2006/zhihu-plus-plus/issues/25#issuecomment-3311926550
                    if (cookie.name == "z_c0" && cookie.value.isBlank()) {
                        // 避免被登出
                        return
                    }
                    if (cookie.domain?.endsWith("zhihu.com") != false) {
                        if (cookies == null) {
                            data.cookies[cookie.name] = cookie.value
                            saveData(context, data)
                        } else {
                            cookies[cookie.name] = cookie.value
                        }
                    }
                }

                override fun close() {
                }

                override suspend fun get(requestUrl: Url): List<Cookie> = (cookies ?: data.cookies).map {
                    Cookie(it.key, it.value, CookieEncoding.RAW, domain = "www.zhihu.com")
                }
            }
        }
        install(ContentNegotiation) {
            json(json)
        }
        install(UserAgent) {
            agent = data.userAgent
        }
    }

    suspend fun verifyLogin(context: Context, cookies: Map<String, String>): Boolean {
        val map = cookies.toMutableMap()
        val httpClient = httpClient(context, map)
        val response = httpClient.get("https://www.zhihu.com/api/v4/me")
        if (response.status == HttpStatusCode.OK) {
            val jojo = response.body<JsonObject>()
            val person = decodeJson<Person>(jojo)
            saveData(
                context,
                Data(
                    login = true,
                    cookies = map,
                    username = person.name,
                    self = person,
                ),
            )
            return true
        }
        return false
    }

    fun delete(context: Context) {
        saveData(context, Data())
    }

    internal inline fun <reified T> decodeJson(json: JsonElement): T {
        val json = snake_case2camelCase(json)
        try {
            return this.json.decodeFromJsonElement<T>(json)
        } catch (e: SerializationException) {
            Log.e("AccountData", "Failed to parse JSON: $json", e)
            throw SerializationException("Failed to parse JSON: $json", e)
        }
    }

    internal fun <T> decodeJson(serializer: KSerializer<T>, json: JsonElement): T {
        val json = snake_case2camelCase(json)
        try {
            return this.json.decodeFromJsonElement(serializer, json)
        } catch (e: SerializationException) {
            Log.e("AccountData", "Failed to parse JSON: $json", e)
            throw SerializationException("Failed to parse JSON: $json", e)
        }
    }

    @Suppress("FunctionName")
    fun snake_case2camelCase(snakeCase: String): String = snakeCase
        .split("_")
        .joinToString("") { it.replaceFirstChar { char -> char.uppercase() } }
        .replaceFirstChar { it.lowercase() }

    @Suppress("FunctionName")
    fun snake_case2camelCase(json: JsonElement): JsonElement = when (json) {
        is JsonObject -> buildJsonObject {
            for ((key, value) in json) {
                put(snake_case2camelCase(key), snake_case2camelCase(value))
            }
        }
        is JsonArray -> buildJsonArray {
            for (item in json) {
                add(snake_case2camelCase(item))
            }
        }
        else -> json
    }
}
