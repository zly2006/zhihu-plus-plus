package com.github.zly2006.zhihu.desktop

import com.github.zly2006.zhihu.shared.data.ZhihuJson
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.get
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

private const val ME_URL = "https://www.zhihu.com/api/v4/me"
private const val DEFAULT_USER_AGENT = "Mozilla/5.0 (X11; U; Linux x86_64; en-US) AppleWebKit/540.0 (KHTML, like Gecko) Ubuntu/10.10 Chrome/9.1.0.0 Safari/540.0"

@Serializable
data class DesktopAccountData(
    val login: Boolean = false,
    val username: String = "",
    val cookies: MutableMap<String, String> = mutableMapOf(),
    val userAgent: String = DEFAULT_USER_AGENT,
)

class DesktopAccountStore(
    private val accountFile: Path = defaultAccountFile(),
) {
    fun load(): DesktopAccountData = runCatching {
        if (accountFile.exists()) {
            ZhihuJson.json.decodeFromString<DesktopAccountData>(accountFile.readText())
        } else {
            DesktopAccountData()
        }
    }.getOrDefault(DesktopAccountData())

    fun save(data: DesktopAccountData) {
        accountFile.parent.createDirectories()
        accountFile.writeText(ZhihuJson.json.encodeToString(data))
    }

    fun createHttpClient(cookies: MutableMap<String, String>): HttpClient = HttpClient(CIO) {
        install(HttpCookies) {
            storage = mapBackedCookieStorage(cookies)
        }
        install(ContentNegotiation) {
            json(ZhihuJson.json)
        }
        install(UserAgent) {
            agent = load().userAgent
        }
    }

    suspend fun verifyAndSave(cookies: MutableMap<String, String>): Boolean {
        createHttpClient(cookies).use { client ->
            val response = client.get(ME_URL)
            if (response.status != HttpStatusCode.OK) {
                return false
            }
            val body = response.body<JsonObject>()
            val username = body["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
            save(
                DesktopAccountData(
                    login = true,
                    username = username,
                    cookies = cookies,
                ),
            )
            return true
        }
    }
}

private fun mapBackedCookieStorage(cookies: MutableMap<String, String>) = object : CookiesStorage {
    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        if (cookie.name == "z_c0" && cookie.value.isBlank()) {
            return
        }
        if (cookie.domain?.endsWith("zhihu.com") != false) {
            cookies[cookie.name] = cookie.value
        }
    }

    override fun close() {
    }

    override suspend fun get(requestUrl: Url): List<Cookie> = cookies.map {
        Cookie(it.key, it.value, CookieEncoding.RAW, domain = "www.zhihu.com")
    }
}

private fun defaultAccountFile(): Path {
    val home = System.getProperty("user.home")
    return Path.of(home, ".zhihu-plus-plus", "account.json")
}
