/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
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

package com.github.zly2006.zhihu.shared.account

import com.github.zly2006.zhihu.shared.data.executeZhihuAuthenticatedRequest
import com.github.zly2006.zhihu.shared.data.fetchVerifiedZhihuSession
import com.github.zly2006.zhihu.shared.data.fetchZhihuAuthenticatedJson
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.json.JsonObject

class ZhihuAccountClient(
    private val repository: ZhihuAccountRepository,
    private val createClient: (
        cookies: MutableMap<String, String>,
        session: ZhihuAccountSession,
        onCookieChanged: () -> Unit,
        isTemporary: Boolean,
    ) -> HttpClient,
    private val onSessionChanged: (ZhihuAccountSession) -> Unit = {},
) {
    private var session: ZhihuAccountSession? = null
    private var httpClient: HttpClient? = null
    private var httpClientCookies: Map<String, String>? = null
    private var httpClientUserAgent: String? = null
    private var lastRefreshMillis = 0L

    fun load(): ZhihuAccountSession {
        val cached = session
        if (cached != null) return cached
        return repository.load().also {
            session = it
            onSessionChanged(it)
        }
    }

    fun save(session: ZhihuAccountSession) {
        val shouldInvalidateClient = httpClient != null &&
            (httpClientCookies != session.cookies || httpClientUserAgent != session.userAgent)
        this.session = session
        onSessionChanged(session)
        if (shouldInvalidateClient) {
            invalidateHttpClient()
        }
        repository.save(session)
    }

    fun clear() {
        val emptySession = ZhihuAccountSession()
        session = emptySession
        onSessionChanged(emptySession)
        invalidateHttpClient()
        repository.clear()
    }

    fun httpClient(): HttpClient {
        httpClient?.let { return it }
        val currentSession = load()
        return createClient(currentSession.cookies, currentSession, { persistCurrentSession() }, false).also {
            httpClient = it
            httpClientCookies = currentSession.cookies.toMap()
            httpClientUserAgent = currentSession.userAgent
        }
    }

    fun temporaryHttpClient(cookies: MutableMap<String, String>): HttpClient =
        createClient(cookies, load(), {}, true)

    fun invalidateHttpClient() {
        httpClient?.close()
        httpClient = null
        httpClientCookies = null
        httpClientUserAgent = null
    }

    suspend fun verifyAndSave(cookies: MutableMap<String, String>): Boolean {
        val temporaryClient = temporaryHttpClient(cookies)
        try {
            val verified = fetchVerifiedZhihuSession(temporaryClient, cookies, load().userAgent) ?: return false
            save(verified)
            return true
        } finally {
            temporaryClient.close()
        }
    }

    suspend fun refreshAndSaveProfile(): ZhihuAccountSession? {
        val currentSession = load()
        val refreshed = fetchVerifiedZhihuSession(httpClient(), currentSession.cookies, currentSession.userAgent)
        if (refreshed != null) {
            save(refreshed)
        }
        return refreshed
    }

    suspend fun fetchAuthenticatedJson(
        url: String,
        block: suspend HttpRequestBuilder.() -> Unit = {},
    ): JsonObject? =
        fetchZhihuAuthenticatedJson(
            client = httpClient(),
            url = url,
            lastRefreshMillis = lastRefreshMillis,
            updateLastRefreshMillis = { lastRefreshMillis = it },
            block = block,
        )

    suspend fun <T> withAuthenticatedResponse(
        url: String,
        block: suspend HttpRequestBuilder.() -> Unit = {},
        transform: suspend (HttpResponse) -> T,
    ): T {
        val response = executeZhihuAuthenticatedRequest(
            client = httpClient(),
            url = url,
            lastRefreshMillis = lastRefreshMillis,
            updateLastRefreshMillis = { lastRefreshMillis = it },
            block = block,
        )
        return transform(response)
    }

    suspend fun <T> withAuthenticatedClient(
        block: suspend (client: HttpClient, cookies: Map<String, String>) -> T,
    ): T {
        val currentSession = load()
        return block(httpClient(), currentSession.cookies)
    }

    private fun persistCurrentSession() {
        val currentSession = load()
        val updated = currentSession.copy(cookies = currentSession.cookies.toMutableMap())
        session = updated
        httpClientCookies = updated.cookies.toMap()
        httpClientUserAgent = updated.userAgent
        onSessionChanged(updated)
        repository.save(updated)
    }
}
