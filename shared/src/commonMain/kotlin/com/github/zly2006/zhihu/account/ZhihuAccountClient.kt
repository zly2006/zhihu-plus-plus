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
    private val cookies = accountState.value.session.cookies
        .toMutableMap()
    private val client = createAccountHttpClient(cookies, accountState.value.session.userAgent) {
        accountState.save(load().copy(cookies = cookies.toMutableMap()))
    }

    fun load(): ZhihuAccountSession = accountState.value.session

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

    val accountsState: StateFlow<ZhihuAccounts> = accountState.state.asStateFlow()
    val session: ZhihuAccountSession
        get() = accountState.value.session
    val accounts: List<ZhihuSavedAccount>
        get() = accountState.value.accounts
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
        accountState.saveCurrent(session)
        replaceClient()
    }

    fun clear() {
        accountState.removeCurrent()
        replaceClient()
    }

    suspend fun switchAccount(id: String): Boolean {
        if (id == accountState.value.activeAccountId) return true
        val savedSession = accountState.value.accounts
            .firstOrNull { it.id == id }
            ?.session ?: return false
        val verified = verifyCandidateSession(savedSession.cookies.toMutableMap(), savedSession.userAgent)
            ?.copy(
                mobileAccessToken = savedSession.mobileAccessToken,
                mobileRefreshToken = savedSession.mobileRefreshToken,
                mobileTokenType = savedSession.mobileTokenType,
                mobileTokenExpiresAt = savedSession.mobileTokenExpiresAt,
            ) ?: return false
        accountState.select(id, verified)
        replaceClient()
        return true
    }

    fun removeAccount(id: String) {
        val activeAccountId = accountState.value.activeAccountId
        accountState.remove(id)
        if (id == activeAccountId) replaceClient()
    }

    suspend fun login(cookies: MutableMap<String, String>): Boolean {
        val session = verifyCandidateSession(cookies) ?: return false
        selectVerifiedSession(session)
        return true
    }

    suspend fun login(token: ZhihuMobileLoginToken): Boolean {
        val session = verifyCandidateSession(token.cookies.toMutableMap())?.copy(
            mobileAccessToken = token.accessToken,
            mobileRefreshToken = token.refreshToken,
            mobileTokenType = token.tokenType,
            mobileTokenExpiresAt = token.expiresAt,
        ) ?: return false
        selectVerifiedSession(session)
        return true
    }

    private fun selectVerifiedSession(session: ZhihuAccountSession) {
        accountState.selectOrAdd(session)
        replaceClient()
    }

    private suspend fun verifyCandidateSession(
        cookies: MutableMap<String, String>,
        userAgent: String = session.userAgent,
    ): ZhihuAccountSession? {
        val client = createAccountHttpClient(cookies, userAgent)
        return try {
            fetchVerifiedZhihuSession(client, cookies, userAgent)
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
    val state = MutableStateFlow(repository.loadAccounts())
    val value: ZhihuAccounts
        get() = state.value

    fun save(session: ZhihuAccountSession) {
        saveCurrent(session)
    }

    fun saveCurrent(session: ZhihuAccountSession) {
        val current = value
        val activeId = current.activeAccountId
        val updated = if (activeId == null) {
            if (!session.login) {
                ZhihuAccounts()
            } else {
                val id = session.accountSlotId()
                ZhihuAccounts(id, listOf(ZhihuSavedAccount(id, session)))
            }
        } else {
            current.copy(
                accounts = current.accounts.map { account ->
                    if (account.id == activeId) account.copy(session = session) else account
                },
            )
        }
        persist(updated)
    }

    fun selectOrAdd(session: ZhihuAccountSession) {
        val existing = value.accounts.firstOrNull { it.session.hasSameIdentityAs(session) }
        if (existing != null) {
            persist(
                value.copy(
                    activeAccountId = existing.id,
                    accounts = value.accounts.map { if (it.id == existing.id) it.copy(session = session) else it },
                ),
            )
        } else {
            val id = session.accountSlotId()
            persist(ZhihuAccounts(id, value.accounts + ZhihuSavedAccount(id, session)))
        }
    }

    fun select(id: String, session: ZhihuAccountSession) {
        require(value.accounts.any { it.id == id }) { "找不到要切换的登录账号" }
        persist(
            value.copy(
                activeAccountId = id,
                accounts = value.accounts.map { if (it.id == id) it.copy(session = session) else it },
            ),
        )
    }

    fun remove(id: String) {
        val remaining = value.accounts.filterNot { it.id == id }
        val activeId = if (value.activeAccountId == id) remaining.firstOrNull()?.id else value.activeAccountId
        persist(ZhihuAccounts(activeId, remaining))
    }

    fun removeCurrent() {
        value.activeAccountId?.let(::remove) ?: repository.clear()
    }

    private fun persist(accounts: ZhihuAccounts) {
        if (accounts.accounts.isEmpty()) repository.clear() else repository.saveAccounts(accounts)
        state.value = accounts
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
    val identity = accountIdentityKey()
    val otherIdentity = other.accountIdentityKey()
    return identity != null && identity == otherIdentity
}

internal expect val accountHttpClientEngineFactory: HttpClientEngineFactory<*>

var accountHttpClientEngineForTesting: HttpClientEngine? = null

@Composable
expect fun rememberZhihuAccountStore(): ZhihuAccountStore
