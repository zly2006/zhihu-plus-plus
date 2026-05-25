package com.github.zly2006.zhihu.shared.desktop

import com.github.zly2006.zhihu.shared.account.ZhihuAccountRepository
import com.github.zly2006.zhihu.shared.account.ZhihuAccountSession
import com.github.zly2006.zhihu.shared.account.ZhihuAccountSessionStore
import com.github.zly2006.zhihu.shared.data.fetchVerifiedZhihuSession
import com.github.zly2006.zhihu.shared.data.fetchZhihuAuthenticatedJson
import com.github.zly2006.zhihu.shared.data.installZhihuCommonClientConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.HttpRequestBuilder
import kotlinx.serialization.json.JsonObject
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

typealias DesktopAccountData = ZhihuAccountSession

class DesktopAccountStore(
    private val accountFile: Path = defaultAccountFile(),
) {
    private val repository = ZhihuAccountRepository(PathAccountSessionStore(accountFile))
    private var lastRefreshCookie = 0L

    fun load(): DesktopAccountData = repository.load()

    fun save(data: DesktopAccountData) = repository.save(data)

    fun clear() = repository.clear()

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

    suspend fun fetchAuthenticatedJson(
        url: String,
        block: suspend HttpRequestBuilder.() -> Unit = {},
    ): JsonObject? {
        val account = load()
        return createHttpClient(account.cookies).use { client ->
            fetchZhihuAuthenticatedJson(
                client = client,
                url = url,
                lastRefreshMillis = lastRefreshCookie,
                updateLastRefreshMillis = { lastRefreshCookie = it },
                block = block,
            )
        }
    }

    suspend fun verifyAndSave(cookies: MutableMap<String, String>): Boolean {
        createHttpClient(cookies).use { client ->
            val session = fetchVerifiedZhihuSession(client, cookies, load().userAgent) ?: return false
            save(session)
            return true
        }
    }
}

private fun defaultAccountFile(): Path {
    val home = System.getProperty("user.home")
    return Path.of(home, ".zhihu-plus-plus", "account.json")
}

private class PathAccountSessionStore(
    private val accountFile: Path,
) : ZhihuAccountSessionStore {
    override fun readText(): String? = if (accountFile.exists()) {
        accountFile.readText()
    } else {
        null
    }

    override fun writeText(text: String) {
        accountFile.parent.createDirectories()
        accountFile.writeText(text)
    }

    override fun delete() {
        accountFile.deleteIfExists()
    }
}
