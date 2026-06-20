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

package com.github.zly2006.zhihu.shared.account

import com.github.zly2006.zhihu.shared.data.installZhihuCommonClientConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class ZhihuAccountClientTest {
    @Test
    fun reusesCachedHttpClientUntilSessionChanges() {
        val store = ClientInMemoryAccountSessionStore()
        val createdClients = mutableListOf<HttpClient>()
        val accountClient = ZhihuAccountClient(
            repository = ZhihuAccountRepository(store),
            createClient = { cookies, session, onCookieChanged, _ ->
                testHttpClient(cookies, session, onCookieChanged).also(createdClients::add)
            },
        )

        val firstClient = accountClient.httpClient()

        assertSame(firstClient, accountClient.httpClient())

        accountClient.save(
            ZhihuAccountSession(
                login = true,
                username = "alice",
                cookies = mutableMapOf("d_c0" to "new-dc0"),
            ),
        )
        val secondClient = accountClient.httpClient()

        assertNotSame(firstClient, secondClient)
        assertEquals(2, createdClients.size)
        secondClient.close()
    }

    @Test
    fun cookieChangePersistsSessionWithoutInvalidatingClient() {
        val store = ClientInMemoryAccountSessionStore()
        val repository = ZhihuAccountRepository(store)
        repository.save(
            ZhihuAccountSession(
                login = true,
                username = "alice",
                cookies = mutableMapOf("z_c0" to "old-token"),
            ),
        )
        var factoryCookies: MutableMap<String, String>? = null
        var factoryOnCookieChanged: (() -> Unit)? = null
        val accountClient = ZhihuAccountClient(
            repository = repository,
            createClient = { cookies, session, onCookieChanged, _ ->
                factoryCookies = cookies
                factoryOnCookieChanged = onCookieChanged
                testHttpClient(cookies, session, onCookieChanged)
            },
        )
        val client = accountClient.httpClient()

        factoryCookies?.set("z_c0", "new-token")
        factoryOnCookieChanged?.invoke()

        assertEquals("new-token", repository.load().cookies["z_c0"])
        assertSame(client, accountClient.httpClient())
        client.close()
    }

    @Test
    fun inPlaceCookieMutationInvalidatesCachedHttpClientOnSave() {
        val store = ClientInMemoryAccountSessionStore()
        val repository = ZhihuAccountRepository(store)
        repository.save(
            ZhihuAccountSession(
                login = true,
                username = "alice",
                cookies = mutableMapOf("z_c0" to "old-token"),
            ),
        )
        val accountClient = ZhihuAccountClient(
            repository = repository,
            createClient = { cookies, session, onCookieChanged, _ ->
                testHttpClient(cookies, session, onCookieChanged)
            },
        )
        val firstClient = accountClient.httpClient()
        val session = accountClient.load()
        session.cookies["z_c0"] = "new-token"

        accountClient.save(session)

        val secondClient = accountClient.httpClient()
        assertNotSame(firstClient, secondClient)
        secondClient.close()
    }

    private fun testHttpClient(
        cookies: MutableMap<String, String>,
        session: ZhihuAccountSession,
        onCookieChanged: () -> Unit,
    ): HttpClient = HttpClient(
        MockEngine {
            respond(
                content = "{}",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        },
    ) {
        installZhihuCommonClientConfig(
            cookies = cookies,
            userAgent = session.userAgent,
            onCookieChanged = onCookieChanged,
        )
    }
}

private class ClientInMemoryAccountSessionStore(
    var text: String? = null,
) : ZhihuAccountSessionStore {
    override fun readText(): String? = text

    override fun writeText(text: String) {
        this.text = text
    }

    override fun delete() {
        text = null
    }
}
