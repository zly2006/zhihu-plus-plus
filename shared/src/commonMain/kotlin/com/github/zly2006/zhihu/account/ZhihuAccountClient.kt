/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under the GNU Affero General Public License version 3.
 */

package com.github.zly2006.zhihu.account

import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.data.fetchVerifiedZhihuSession
import com.github.zly2006.zhihu.data.installZhihuCommonClientConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ZhihuAccountClient internal constructor(
    private val repository: ZhihuAccountRepository,
    initialSession: ZhihuAccountSession,
    private val mutableSession: MutableStateFlow<ZhihuAccountSession>,
    private val engineProvider: AccountHttpClientEngineProvider?,
) : AutoCloseable {
    constructor(
        repository: ZhihuAccountRepository,
        initialSession: ZhihuAccountSession,
        mutableSession: MutableStateFlow<ZhihuAccountSession>,
    ) : this(repository, initialSession, mutableSession, null)

    private val cookies = initialSession.cookies.toMutableMap()
    private val client = createAccountHttpClient(cookies, initialSession.userAgent, engineProvider) {
        val refreshed = mutableSession.value.copy(cookies = cookies.toMutableMap())
        mutableSession.value = refreshed
        repository.save(refreshed)
    }

    val sessionState: StateFlow<ZhihuAccountSession> = mutableSession.asStateFlow()

    fun load(): ZhihuAccountSession = mutableSession.value

    fun save(session: ZhihuAccountSession) {
        require(load().hasSameIdentityAs(session)) {
            "ZhihuAccountClient 只能刷新同一账号的 session；切换身份必须替换 client"
        }
        persistSession(session)
    }

    private fun persistSession(session: ZhihuAccountSession) {
        val updatedCookies = session.cookies.toMap()
        cookies.clear()
        cookies.putAll(updatedCookies)
        mutableSession.value = session.copy(cookies = cookies.toMutableMap())
        repository.save(mutableSession.value)
    }

    fun httpClient(): HttpClient = client

    fun temporaryHttpClient(cookies: MutableMap<String, String>): HttpClient =
        createAccountHttpClient(cookies, load().userAgent, engineProvider)

    suspend fun refreshAndSaveProfile(): ZhihuAccountSession? {
        val current = load()
        val refreshed = fetchVerifiedZhihuSession(client, current.cookies, current.userAgent) ?: return null
        return refreshed
            .copy(
                mobileAccessToken = current.mobileAccessToken,
                mobileRefreshToken = current.mobileRefreshToken,
                mobileTokenType = current.mobileTokenType,
                mobileTokenExpiresAt = current.mobileTokenExpiresAt,
            ).also(::persistSession)
    }

    suspend fun <T> withAuthenticatedClient(
        block: suspend (client: HttpClient, cookies: Map<String, String>) -> T,
    ): T = block(client, cookies)

    override fun close() {
        client.close()
    }
}

class ZhihuAccountStore internal constructor(
    private val repository: ZhihuAccountRepository,
    private val engineProvider: AccountHttpClientEngineProvider?,
) : AutoCloseable {
    constructor(repository: ZhihuAccountRepository) : this(repository, null)

    private val mutableSession = MutableStateFlow(repository.load())
    private var mutableClient = ZhihuAccountClient(
        repository,
        mutableSession.value,
        mutableSession,
        engineProvider,
    )

    val sessionState: StateFlow<ZhihuAccountSession> = mutableSession.asStateFlow()
    val session: ZhihuAccountSession
        get() = mutableSession.value
    val client: ZhihuAccountClient
        get() = mutableClient

    fun save(session: ZhihuAccountSession) {
        if (this.session.hasSameIdentityAs(session)) {
            mutableClient.save(session)
        } else {
            replaceSession(session)
        }
    }

    fun replaceSession(session: ZhihuAccountSession) {
        repository.save(session)
        replaceClient(session)
    }

    fun clear() {
        repository.clear()
        replaceClient(ZhihuAccountSession())
    }

    suspend fun login(cookies: MutableMap<String, String>): Boolean {
        val session = verifyCandidateSession(cookies) ?: return false
        replaceSession(session)
        return true
    }

    suspend fun login(token: ZhihuMobileLoginToken): Boolean {
        val session = verifyCandidateSession(token.cookies.toMutableMap())?.copy(
            mobileAccessToken = token.accessToken,
            mobileRefreshToken = token.refreshToken,
            mobileTokenType = token.tokenType,
            mobileTokenExpiresAt = token.expiresAt,
        ) ?: return false
        replaceSession(session)
        return true
    }

    private suspend fun verifyCandidateSession(cookies: MutableMap<String, String>): ZhihuAccountSession? {
        val client = createAccountHttpClient(cookies, session.userAgent, engineProvider)
        return try {
            fetchVerifiedZhihuSession(client, cookies, session.userAgent)
        } finally {
            client.close()
        }
    }

    private fun replaceClient(session: ZhihuAccountSession) {
        mutableClient.close()
        mutableSession.value = session
        mutableClient = ZhihuAccountClient(repository, session, mutableSession, engineProvider)
    }

    override fun close() = mutableClient.close()
}

private fun createAccountHttpClient(
    cookies: MutableMap<String, String>,
    userAgent: String,
    engineProvider: AccountHttpClientEngineProvider?,
    onCookieChanged: () -> Unit = {},
): HttpClient {
    val configure: io.ktor.client.HttpClientConfig<*>.() -> Unit = {
        installZhihuCommonClientConfig(cookies, userAgent, onCookieChanged)
    }
    return engineProvider?.create()?.let { HttpClient(it, configure) }
        ?: HttpClient(accountHttpClientEngineFactory, configure)
}

private fun ZhihuAccountSession.hasSameIdentityAs(other: ZhihuAccountSession): Boolean {
    if (login != other.login) return false
    if (!login) return true
    val identity = profile?.id?.takeIf(String::isNotBlank)
        ?: profile?.urlToken?.takeIf(String::isNotBlank)
        ?: username.takeIf(String::isNotBlank)
    val otherIdentity = other.profile?.id?.takeIf(String::isNotBlank)
        ?: other.profile?.urlToken?.takeIf(String::isNotBlank)
        ?: other.username.takeIf(String::isNotBlank)
    return identity != null && identity == otherIdentity
}

internal expect val accountHttpClientEngineFactory: HttpClientEngineFactory<*>

internal interface AccountHttpClientEngineProvider {
    fun create(): HttpClientEngine
}

@Composable
expect fun rememberZhihuAccountStore(): ZhihuAccountStore
