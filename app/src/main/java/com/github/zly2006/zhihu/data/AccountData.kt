package com.github.zly2006.zhihu.data

import android.content.Context
import io.ktor.client.*
import io.ktor.client.call.*
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
        val cookies: Map<String, String> = mutableMapOf(),
    )

    private var data = Data()
    fun getData(context: Context): Data {
        val file = File(context.filesDir, "account.json")
        if (file.exists()) {
            data = json.decodeFromString<Data>(file.readText())
        }
        return data
    }

    fun saveData(context: Context, data: Data) {
        this.data = data
        val file = File(context.filesDir, "account.json")
        file.writeText(json.encodeToString(data))
    }

    suspend fun verifyLogin(context: Context, cookies: Map<String, String>): Boolean {
        val httpClient = HttpClient {
            install(HttpCookies) {
                storage = ConstantCookiesStorage(
                    *cookies.map {
                        Cookie(it.key, it.value, CookieEncoding.RAW, domain = "www.zhihu.com")
                    }.toTypedArray()
                )
            }
            install(ContentNegotiation) {
                json()
            }
        }
        val response = httpClient.get("https://www.zhihu.com/api/v4/me")
        if (response.status == HttpStatusCode.OK) {
            val body = response.body<JsonObject>()
            saveData(
                context, Data(
                    login = true,
                    cookies = cookies,
                    username = body["name"]!!.jsonPrimitive.content
                )
            )
            return true
        }
        return false
    }
}
