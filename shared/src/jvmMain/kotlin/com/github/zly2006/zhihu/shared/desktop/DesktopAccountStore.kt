package com.github.zly2006.zhihu.shared.desktop

import com.github.zly2006.zhihu.shared.account.ZhihuAccountRepository
import com.github.zly2006.zhihu.shared.account.ZhihuAccountSession
import com.github.zly2006.zhihu.shared.account.ZhihuAccountSessionStore
import com.github.zly2006.zhihu.shared.data.ZHIHU_READ_HISTORY_ADD_URL
import com.github.zly2006.zhihu.shared.data.buildZhihuReadHistoryBody
import com.github.zly2006.zhihu.shared.data.executeZhihuAuthenticatedRequest
import com.github.zly2006.zhihu.shared.data.fetchVerifiedZhihuSession
import com.github.zly2006.zhihu.shared.data.fetchZhihuAuthenticatedJson
import com.github.zly2006.zhihu.shared.data.installZhihuCommonClientConfig
import com.github.zly2006.zhihu.shared.util.signZhihuFetchRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
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

    suspend fun <T> withAuthenticatedResponse(
        url: String,
        block: suspend HttpRequestBuilder.() -> Unit = {},
        transform: suspend (HttpResponse) -> T,
    ): T {
        val account = load()
        return createHttpClient(account.cookies).use { client ->
            val response = executeZhihuAuthenticatedRequest(
                client = client,
                url = url,
                lastRefreshMillis = lastRefreshCookie,
                updateLastRefreshMillis = { lastRefreshCookie = it },
                block = block,
            )
            transform(response)
        }
    }

    suspend fun addReadHistory(
        contentToken: String,
        contentTypeName: String,
    ) {
        runCatching {
            val account = load()
            val dc0 = account.cookies["d_c0"] ?: return
            val body = buildZhihuReadHistoryBody(contentToken, contentTypeName)
            fetchAuthenticatedJson(ZHIHU_READ_HISTORY_ADD_URL) {
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                setBody(body)
                signDesktopRequest(account.cookies, body)
            }
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

private fun defaultAccountFile(): Path = desktopZhihuLegacyAccountFile()

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

/**
 * JVM 签名便捷函数：从 cookies 提取 d_c0 并签名。
 * 对应 Android 端 Utils.kt 的 signFetchRequest()。
 */
fun HttpRequestBuilder.signDesktopRequest(
    cookies: Map<String, String>,
    body: String? = null,
) {
    cookies["d_c0"]?.let { dc0 ->
        signZhihuFetchRequest(dc0 = dc0, body = body)
    }
}
