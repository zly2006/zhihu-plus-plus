package com.github.zly2006.zhihu.shared.desktop

import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.fetchVerifiedZhihuProfile
import com.github.zly2006.zhihu.shared.data.installZhihuCommonClientConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.serialization.Serializable
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

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
        val savedData = load()
        installZhihuCommonClientConfig(
            cookies = cookies,
            userAgent = savedData.userAgent,
            onCookieChanged = {
                save(
                    savedData.copy(
                        cookies = cookies.toMutableMap(),
                    ),
                )
            },
        )
    }

    suspend fun verifyAndSave(cookies: MutableMap<String, String>): Boolean {
        createHttpClient(cookies).use { client ->
            val profile = fetchVerifiedZhihuProfile(client) ?: return false
            save(
                DesktopAccountData(
                    login = true,
                    username = profile.name,
                    cookies = cookies,
                ),
            )
            return true
        }
    }
}

private fun defaultAccountFile(): Path {
    val home = System.getProperty("user.home")
    return Path.of(home, ".zhihu-plus-plus", "account.json")
}
