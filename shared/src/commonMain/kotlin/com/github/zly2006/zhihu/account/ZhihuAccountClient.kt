/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.account

import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.data.ZhihuJson
import com.github.zly2006.zhihu.data.fetchVerifiedZhihuSession
import com.github.zly2006.zhihu.data.installZhihuCommonClientConfig
import com.github.zly2006.zhihu.util.ZhihuCredentialRefresher
import com.github.zly2006.zhihu.util.raiseForStatus
import com.github.zly2006.zhihu.util.signZhihuFetchRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ZhihuAccountClient internal constructor(
    private val accountStore: ZhihuAccountStore,
) : AutoCloseable {
    private val accountState: ZhihuAccountState
        get() = accountStore.accountState
    private val accountId = accountState.value.activeAccountId
    private var closed = false
    internal val isCurrent: Boolean
        get() = !closed && accountState.value.activeAccountId == accountId
    val credentialRefresher: ZhihuCredentialRefresher
        get() = accountStore.credentialRefresherFor(accountId)
    private val cookies = accountState.value.session.cookies
        .toMutableMap()
    private val client = createAccountHttpClient(cookies, accountState.value.session.userAgent) {
        accountStore.sessionLock.withLock {
            if (isCurrent) accountState.save(load().copy(cookies = cookies.toMutableMap()))
        }
    }

    fun load(): ZhihuAccountSession {
        val accounts = accountState.value
        return accounts.accounts.firstOrNull { it.id == accountId }?.session
            ?: accounts.session.takeIf { accountId == null && isCurrent }
            ?: error("账号客户端已失效")
    }

    fun save(session: ZhihuAccountSession) = accountStore.sessionLock.withLock {
        require(isCurrent && load().hasSameIdentityAs(session)) {
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

    suspend fun get(
        url: String,
        block: suspend HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse {
        check(isCurrent) { "账号客户端已失效" }
        val requestCookie = cookies["z_c0"]
        val response = client.get(url) {
            block()
            signZhihuFetchRequest(cookies)
        }
        if (response.status != HttpStatusCode.Unauthorized) return response

        if (!isCurrent) return response
        val body = response.bodyAsText()
        val ticketExpired = runCatching {
            val error = ZhihuJson.json
                .parseToJsonElement(body)
                .jsonObject["error"]
                ?.jsonObject
            error?.get("code")?.jsonPrimitive?.intOrNull == 100 &&
                error["message"]?.jsonPrimitive?.content in setOf("ERR_LOGIN_TICKET_EXPIRED", "ERR_TICKET_NOT_EXIST")
        }.getOrDefault(false)
        if (!ticketExpired) return response
        if (!credentialRefresher.refresh(force = true, rejectedCookie = requestCookie) || !isCurrent) return response
        return client
            .get(url) {
                block()
                signZhihuFetchRequest(cookies)
            }.raiseForStatus()
    }

    fun httpClient(): HttpClient = client

    fun temporaryHttpClient(cookies: MutableMap<String, String>): HttpClient =
        createAccountHttpClient(cookies, load().userAgent)

    suspend fun refreshAndSaveProfile(): ZhihuAccountSession? {
        val current = load()
        val refreshed = fetchVerifiedZhihuSession(client, current.cookies, current.userAgent) ?: return null
        return accountStore.sessionLock.withLock {
            if (!isCurrent) return@withLock null
            val latest = load()
            if (latest.profile?.id != null && latest.profile.id != refreshed.profile?.id) return@withLock null
            latest
                .copy(
                    username = refreshed.username,
                    profile = refreshed.profile,
                    self = refreshed.self,
                ).also(::persistSession)
        }
    }

    suspend fun <T> withAuthenticatedClient(
        block: suspend (client: HttpClient, cookies: Map<String, String>) -> T,
    ): T = block(client, cookies)

    override fun close() = accountStore.sessionLock.withLock {
        closed = true
        client.close()
    }
}

class ZhihuAccountStore internal constructor(
    repository: ZhihuAccountRepository,
) : AutoCloseable {
    internal val sessionLock = reentrantLock()
    internal val accountState = ZhihuAccountState(repository)
    private val credentialRefreshers = mutableMapOf<String?, ZhihuCredentialRefresher>()
    private var mutableClient = ZhihuAccountClient(this)

    val accountsState: StateFlow<ZhihuAccounts> = accountState.state.asStateFlow()
    val session: ZhihuAccountSession
        get() = accountState.value.session
    val accounts: List<ZhihuSavedAccount>
        get() = accountState.value.accounts
    val client: ZhihuAccountClient
        get() = sessionLock.withLock { mutableClient }

    fun save(session: ZhihuAccountSession) = sessionLock.withLock {
        if (this.session.hasSameIdentityAs(session)) {
            mutableClient.save(session)
        } else {
            replaceSession(session)
        }
    }

    fun replaceSession(session: ZhihuAccountSession) = sessionLock.withLock {
        credentialRefreshers.remove(accountState.value.activeAccountId)
        accountState.saveCurrent(session)
        replaceClient()
    }

    fun clear() = sessionLock.withLock {
        credentialRefreshers.remove(accountState.value.activeAccountId)
        accountState.removeCurrent()
        replaceClient()
    }

    suspend fun switchAccount(id: String): Boolean {
        if (id == accountState.value.activeAccountId) return true
        val savedSession = accountState.value.accounts
            .firstOrNull { it.id == id }
            ?.session ?: return false
        val verified = verifyCandidateSession(savedSession.cookies.toMutableMap(), savedSession.userAgent) ?: return false
        return sessionLock.withLock {
            val latest = accountState.value.accounts
                .firstOrNull { it.id == id }
                ?.session ?: return@withLock false
            if (!latest.hasSameIdentityAs(verified)) return@withLock false
            val selected = latest.copy(
                username = verified.username,
                profile = verified.profile,
                self = verified.self,
                cookies = if (latest.cookies == savedSession.cookies) verified.cookies else latest.cookies,
            )
            accountState.select(id, selected)
            replaceClient()
            true
        }
    }

    fun removeAccount(id: String) = sessionLock.withLock {
        credentialRefreshers.remove(id)
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

    private fun selectVerifiedSession(session: ZhihuAccountSession) = sessionLock.withLock {
        accountState.selectOrAdd(session)
        credentialRefreshers.remove(accountState.value.activeAccountId)
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
        mutableClient = ZhihuAccountClient(this)
    }

    internal fun credentialRefresherFor(accountId: String?): ZhihuCredentialRefresher = sessionLock.withLock {
        credentialRefreshers.getOrPut(accountId) { ZhihuCredentialRefresher(this, accountId) }
    }

    internal fun renewalSession(accountId: String?, refresher: ZhihuCredentialRefresher): ZhihuAccountSession? = sessionLock.withLock {
        if (!mutableClient.isCurrent || accountState.value.activeAccountId != accountId || credentialRefreshers[accountId] !== refresher) {
            return@withLock null
        }
        session
    }

    internal fun saveRenewedSession(
        accountId: String?,
        refresher: ZhihuCredentialRefresher,
        expectedRefreshToken: String,
        token: ZhihuMobileLoginToken,
    ): Boolean = sessionLock.withLock {
        if (!mutableClient.isCurrent || accountId == null || credentialRefreshers[accountId] !== refresher) return@withLock false
        val latest = accountState.value.accounts
            .firstOrNull { it.id == accountId }
            ?.session ?: return@withLock false
        if (!latest.login || latest.mobileRefreshToken != expectedRefreshToken || latest.profile?.id != token.accountId) return@withLock false
        val updated = latest.copy(
            cookies = (latest.cookies + token.cookies).toMutableMap(),
            mobileAccessToken = token.accessToken,
            mobileRefreshToken = token.refreshToken,
            mobileTokenType = token.tokenType,
            mobileTokenExpiresAt = token.expiresAt,
        )
        if (accountState.value.activeAccountId == accountId) {
            mutableClient.save(updated)
        } else {
            accountState.saveAccount(accountId, updated)
        }
        true
    }

    override fun close() = sessionLock.withLock {
        mutableClient.close()
        credentialRefreshers.clear()
    }
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

    fun saveAccount(id: String, session: ZhihuAccountSession) {
        val current = value
        persist(current.copy(accounts = current.accounts.map { if (it.id == id) it.copy(session = session) else it }))
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
