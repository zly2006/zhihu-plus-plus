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
import com.github.zly2006.zhihu.data.installZhihuMobileClientProfile
import com.github.zly2006.zhihu.util.raiseForStatus
import com.github.zly2006.zhihu.util.signZhihuFetchRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

enum class ZhihuRequestProfile { Web, Mobile }

class ZhihuAccountClient internal constructor(
    private val accountStore: ZhihuAccountStore,
) : AutoCloseable {
    private val accountState: ZhihuAccountState
        get() = accountStore.accountState
    private val accountId = accountState.value.activeAccountId
    private var closed = false
    internal val isCurrent: Boolean
        get() = !closed && accountState.value.activeAccountId == accountId
    private val cookies = accountState.value.session.cookies
        .toMutableMap()
    private val client = createAccountHttpClient(cookies, accountState.value.session.userAgent, accountStore.sessionLock) {
        if (isCurrent) {
            val current = load()
            // 以已保存会话为准；写入失败后的同值 Cookie 仍需重试保存。
            if (current.cookies != cookies) accountState.save(current.copy(cookies = cookies.toMutableMap()))
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

    suspend fun request(
        url: String,
        method: HttpMethod = HttpMethod.Get,
        profile: ZhihuRequestProfile = ZhihuRequestProfile.Web,
        block: suspend HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse {
        check(isCurrent) { "账号客户端已失效" }
        // 仅复制 Cookie 时持锁；签名计算和网络等待保持在锁外。
        val requestCookies = accountStore.sessionLock.withLock { cookies.toMap() }
        val requestCookie = requestCookies["z_c0"]
        val transport = httpClient(profile)
        val response = transport.request(url) {
            this.method = method
            block()
            if (profile == ZhihuRequestProfile.Web) {
                requestCookies["_xsrf"]?.takeIf(String::isNotBlank)?.let { headers["x-xsrftoken"] = it }
                signZhihuFetchRequest(requestCookies)
            }
        }
        if (method != HttpMethod.Get || response.status != HttpStatusCode.Unauthorized) return response

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
        if (!accountStore.refreshCredentials(force = true, rejectedCookie = requestCookie, sourceClient = this) || !isCurrent) return response
        val retryCookies = accountStore.sessionLock.withLock { cookies.toMap() }
        return transport
            .request(url) {
                this.method = method
                block()
                if (profile == ZhihuRequestProfile.Web) {
                    retryCookies["_xsrf"]?.takeIf(String::isNotBlank)?.let { headers["x-xsrftoken"] = it }
                    signZhihuFetchRequest(retryCookies)
                }
            }.raiseForStatus()
    }

    suspend fun get(url: String, profile: ZhihuRequestProfile = ZhihuRequestProfile.Web, block: suspend HttpRequestBuilder.() -> Unit = {}): HttpResponse = request(url, HttpMethod.Get, profile, block)

    suspend fun post(url: String, profile: ZhihuRequestProfile = ZhihuRequestProfile.Web, block: suspend HttpRequestBuilder.() -> Unit = {}): HttpResponse = request(url, HttpMethod.Post, profile, block)

    suspend fun put(url: String, profile: ZhihuRequestProfile = ZhihuRequestProfile.Web, block: suspend HttpRequestBuilder.() -> Unit = {}): HttpResponse = request(url, HttpMethod.Put, profile, block)

    suspend fun patch(url: String, profile: ZhihuRequestProfile = ZhihuRequestProfile.Web, block: suspend HttpRequestBuilder.() -> Unit = {}): HttpResponse = request(url, HttpMethod.Patch, profile, block)

    suspend fun delete(url: String, profile: ZhihuRequestProfile = ZhihuRequestProfile.Web, block: suspend HttpRequestBuilder.() -> Unit = {}): HttpResponse = request(url, HttpMethod.Delete, profile, block)

    private val mobileClient = lazy { client.config { installZhihuMobileClientProfile() } }

    fun httpClient(profile: ZhihuRequestProfile = ZhihuRequestProfile.Web): HttpClient {
        check(isCurrent) { "账号客户端已失效" }
        return if (profile == ZhihuRequestProfile.Mobile) mobileClient.value else client
    }

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

    override fun close() = accountStore.sessionLock.withLock {
        closed = true
        if (mobileClient.isInitialized()) mobileClient.value.close()
        client.close()
    }
}

class ZhihuAccountStore internal constructor(
    repository: ZhihuAccountRepository,
) : AutoCloseable {
    internal val sessionLock = reentrantLock()
    internal val accountState = ZhihuAccountState(repository)

    // 每个账号只保存续期互斥和最近尝试时间；锁实例同时标记本次登录生命周期。
    private val credentialRefreshes = mutableMapOf<String, Pair<Mutex, TimeMark?>>()
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
        credentialRefreshes.remove(accountState.value.activeAccountId)
        accountState.save(session)
        replaceClient()
    }

    fun clear() = sessionLock.withLock {
        credentialRefreshes.remove(accountState.value.activeAccountId)
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
        credentialRefreshes.remove(id)
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

    suspend fun completeIdentityChange(
        sourceClient: ZhihuAccountClient,
        body: String,
        expectedAccountId: String? = null,
    ): ZhihuIdentityChangeResult {
        val token = ZhihuJson.decodeJson<ZhihuIdentityToken>(ZhihuJson.json.parseToJsonElement(body))
        check(token.accessToken.isNotBlank()) { "服务器未返回新账号凭证" }
        check(token.cookie["z_c0"].isNullOrBlank().not()) { "服务器未返回新账号 Cookie" }
        val oldSession = sessionLock.withLock {
            check(sourceClient.isCurrent) { "账号已切换，请重新操作" }
            sourceClient.load()
        }
        val newCookies = oldSession.cookies.toMutableMap().apply { putAll(token.cookie) }
        val client = sourceClient.temporaryHttpClient(newCookies)
        return try {
            val response = client.get("https://api.zhihu.com/people/self") {
                applyIdentityHeaders(
                    oldSession.copy(
                        mobileAccessToken = token.accessToken,
                        mobileTokenType = token.tokenType,
                    ),
                )
            }
            val rawProfile = ZhihuJson.json
                .parseToJsonElement(
                    response.identitySuccessBody("初始化新账号"),
                ).jsonObject
            val profile = ZhihuJson.decodeJson<ZhihuIdentityProfile>(rawProfile)
            check(profile.id.isNotBlank() && profile.name.isNotBlank()) { "服务器返回的账号资料不完整" }
            check(expectedAccountId == null || profile.id == expectedAccountId) { "服务器返回的账号与目标账号不一致" }
            val nextSession = oldSession.copy(
                login = true,
                username = profile.name,
                cookies = newCookies,
                profile = ZhihuAccountProfileSnapshot(
                    id = profile.id,
                    name = profile.name,
                    urlToken = profile.urlToken,
                    userType = profile.userType,
                    avatarUrl = profile.avatarUrl,
                ),
                self = rawProfile,
                mobileAccessToken = token.accessToken,
                mobileRefreshToken = token.refreshToken,
                mobileTokenType = token.tokenType,
                mobileTokenExpiresAt = token.expiresAt,
            )
            sessionLock.withLock {
                check(sourceClient.isCurrent) { "账号已切换，请重新操作" }
                replaceSession(nextSession)
            }
            ZhihuIdentityChangeResult(
                account = ZhihuIdentityAccount(
                    id = profile.id,
                    urlToken = profile.urlToken,
                    name = profile.name,
                    avatarUrl = profile.avatarUrl,
                    isActive = true,
                    canCreateSubAccount = profile.canCreateSubAccount,
                    accountType = profile.accountType,
                    subAccountControlStatus = profile.subAccountControlStatus,
                ),
                session = nextSession,
            )
        } finally {
            client.close()
        }
    }

    private fun selectVerifiedSession(session: ZhihuAccountSession) = sessionLock.withLock {
        accountState.selectOrAdd(session)
        credentialRefreshes.remove(accountState.value.activeAccountId)
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

    suspend fun refreshCredentials(
        force: Boolean = false,
        rejectedCookie: String? = null,
        sourceClient: ZhihuAccountClient = client,
    ): Boolean {
        val (accountId, mutex) = sessionLock.withLock {
            if (sourceClient !== mutableClient || !sourceClient.isCurrent) return false
            val accountId = accountState.value.activeAccountId ?: return false
            accountId to credentialRefreshes.getOrPut(accountId) { Mutex() to null }.first
        }
        return mutex.withLock {
            val current = sessionLock.withLock {
                val attempt = credentialRefreshes[accountId] ?: return false
                if (attempt.first !== mutex || sourceClient !== mutableClient || !sourceClient.isCurrent) return false
                val current = session
                if (!current.login || current.profile?.id.isNullOrBlank() || current.mobileRefreshToken.isNullOrBlank()) return false
                if (rejectedCookie != null && !current.cookies["z_c0"].isNullOrBlank() && current.cookies["z_c0"] != rejectedCookie) return true
                if (!force) {
                    val expiresAt = current.mobileTokenExpiresAt ?: return false
                    if (expiresAt <= 0 || expiresAt > Clock.System.now().epochSeconds + 1.days.inWholeSeconds) return false
                }
                if (attempt.second?.elapsedNow()?.let { it < 1.minutes } == true) return false
                credentialRefreshes[accountId] = mutex to TimeSource.Monotonic.markNow()
                current
            }
            val refreshToken = requireNotNull(current.mobileRefreshToken)
            val token = try {
                // 续期使用独立 Cookie jar；校验完成前不覆盖已保存的会话。
                ZhihuPhoneLoginClient().use { it.refresh(refreshToken) }
            } finally {
                sessionLock.withLock {
                    if (credentialRefreshes[accountId]?.first === mutex) {
                        credentialRefreshes[accountId] = mutex to TimeSource.Monotonic.markNow()
                    }
                }
            }
            check(token.accountId == current.profile?.id) { "刷新响应的账号与当前登录账号不一致" }
            sessionLock.withLock {
                if (credentialRefreshes[accountId]?.first !== mutex) return false
                val latest = accountState.value.accounts
                    .firstOrNull { it.id == accountId }
                    ?.session ?: return false
                if (!latest.login || latest.mobileRefreshToken != refreshToken || latest.profile?.id != token.accountId) return false
                val updated = latest.copy(
                    cookies = (latest.cookies + token.cookies).toMutableMap(),
                    mobileAccessToken = token.accessToken,
                    mobileRefreshToken = token.refreshToken,
                    mobileTokenType = token.tokenType,
                    mobileTokenExpiresAt = token.expiresAt,
                )
                if (accountState.value.activeAccountId == accountId) mutableClient.save(updated) else accountState.saveAccount(accountId, updated)
                true
            }
        }
    }

    override fun close() = sessionLock.withLock {
        mutableClient.close()
        credentialRefreshes.clear()
    }
}

internal class ZhihuAccountState(
    private val repository: ZhihuAccountRepository,
) {
    val state = MutableStateFlow(repository.loadAccounts())
    val value: ZhihuAccounts
        get() = state.value

    fun save(session: ZhihuAccountSession) {
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
    cookieLock: ReentrantLock = reentrantLock(),
    onCookieChanged: () -> Unit = {},
): HttpClient {
    val configure: io.ktor.client.HttpClientConfig<*>.() -> Unit = {
        installZhihuCommonClientConfig(cookies, userAgent, onCookieChanged, cookieLock = cookieLock)
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
