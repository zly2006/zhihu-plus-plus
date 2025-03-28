package com.github.zly2006.zhihu.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.File

object AccountData {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Serializable
    data class Data(
        val login: Boolean = false,
        val username: String = "",
        val cookies: MutableMap<String, String> = mutableMapOf(),
        val userAgent: String = "Mozilla/5.0 (X11; U; Linux x86_64; en-US) AppleWebKit/540.0 (KHTML, like Gecko) Ubuntu/10.10 Chrome/9.1.0.0 Safari/540.0",
        val self: Person? = null
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
        val file = File(context.filesDir, "account.json")
        file.writeText(json.encodeToString(data))
    }

    fun httpClient(context: Context, cookies: MutableMap<String, String>? = null): HttpClient {
        return HttpClient {
            install(HttpCache)
            install(HttpCookies) {
                storage = object : CookiesStorage {
                    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
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

                    override suspend fun get(requestUrl: Url): List<Cookie> {
                        return (cookies ?: data.cookies).map {
                            Cookie(it.key, it.value, CookieEncoding.RAW, domain = "www.zhihu.com")
                        }
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
    }

    suspend fun verifyLogin(context: Context, cookies: Map<String, String>): Boolean {
        val map = cookies.toMutableMap()
        val httpClient = httpClient(context, map)
        val response = httpClient.get("https://www.zhihu.com/api/v4/me")
        if (response.status == HttpStatusCode.OK) {
            val person = response.body<Person>()
            saveData(
                context, Data(
                    login = true,
                    cookies = map,
                    username = person.name,
                    self = person
                )
            )
            return true
        }
        return false
    }

    fun delete(context: Context) {
        saveData(context, Data())
    }

    internal inline fun <reified T> decodeJson(json: JsonElement): T {
        try {
            return this.json.decodeFromJsonElement<T>(json)
        } catch (e: SerializationException) {
            Log.e("AccountData", "Failed to parse JSON: $json", e)
            throw SerializationException("Failed to parse JSON: $json", e)
        }
    }
}
