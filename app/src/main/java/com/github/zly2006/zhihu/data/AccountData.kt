package com.github.zly2006.zhihu.data

import android.content.Context
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

object AccountData {
    val json = Json {
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
    )

    private var data = Data()
    fun getData(context: Context): Data {
        val file = File(context.filesDir, "account.json")
        runCatching {
            if (file.exists()) {
                data = json.decodeFromString<Data>(file.readText())
            }
        }
        return data
    }

    fun saveData(context: Context, data: Data) {
        this.data = data
        val file = File(context.filesDir, "account.json")
        file.writeText(json.encodeToString(data))
    }

    fun httpClient(context: Context, cookies: MutableMap<String, String>? = null): HttpClient {
        return HttpClient {
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
            val body = response.body<JsonObject>()
            saveData(
                context, Data(
                    login = true,
                    cookies = map,
                    username = body["name"]!!.jsonPrimitive.content
                )
            )
            return true
        }
        return false
    }

    fun delete(requireContext: Context) {
        saveData(requireContext, Data())
    }
}
