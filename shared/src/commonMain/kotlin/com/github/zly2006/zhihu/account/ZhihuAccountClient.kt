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
    private val accountState: ZhihuAccountState,
) : AutoCloseable {
    private val cookies = accountState.session.value.cookies
        .toMutableMap()
    private val client = createAccountHttpClient(cookies, accountState.session.value.userAgent) {
        accountState.save(load().copy(cookies = cookies.toMutableMap()))
    }

    val sessionState: StateFlow<ZhihuAccountSession> = accountState.session.asStateFlow()

    fun load(): ZhihuAccountSession = accountState.session.value

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
        accountState.save(session.copy(cookies = cookies.toMutableMap()))
    }

    fun httpClient(): HttpClient = client

    fun temporaryHttpClient(cookies: MutableMap<String, String>): HttpClient =
        createAccountHttpClient(cookies, load().userAgent)

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
    repository: ZhihuAccountRepository,
) : AutoCloseable {
    private val accountState = ZhihuAccountState(repository)
    private var mutableClient = ZhihuAccountClient(accountState)

    val sessionState: StateFlow<ZhihuAccountSession> = accountState.session.asStateFlow()
    val session: ZhihuAccountSession
        get() = accountState.session.value
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
        accountState.save(session)
        replaceClient()
    }

    fun clear() {
        accountState.clear()
        replaceClient()
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
        val client = createAccountHttpClient(cookies, session.userAgent)
        return try {
            fetchVerifiedZhihuSession(client, cookies, session.userAgent)
        } finally {
            client.close()
        }
    }

    private fun replaceClient() {
        mutableClient.close()
        mutableClient = ZhihuAccountClient(accountState)
    }

    override fun close() = mutableClient.close()
}

internal class ZhihuAccountState(
    private val repository: ZhihuAccountRepository,
) {
    val session = MutableStateFlow(repository.load())

    fun save(session: ZhihuAccountSession) {
        this.session.value = session
        repository.save(session)
    }

    fun clear() {
        session.value = ZhihuAccountSession()
        repository.clear()
    }
}

internal fun createAccountHttpClient(
    cookies: MutableMap<String, String>,
    userAgent: String,
    onCookieChanged: () -> Unit = {},
): HttpClient {
    val configure: io.ktor.client.HttpClientConfig<*>.() -> Unit = {
        installZhihuCommonClientConfig(cookies, userAgent, onCookieChanged)
    }
    return accountHttpClientEngineForTesting?.let { HttpClient(it, configure) }
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

var accountHttpClientEngineForTesting: HttpClientEngine? = null

@Composable
expect fun rememberZhihuAccountStore(): ZhihuAccountStore
