/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.github.zly2006.zhihu.account

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class ZhihuAccountClientTest {
    @Test
    fun sameIdentitySessionRefreshKeepsTheBoundClient() = runTest {
        withAccountEngine(emptyMockEngine()) {
            val repository = ZhihuAccountRepository(ClientInMemoryAccountSessionStore()).apply {
                save(
                    ZhihuAccountSession(
                        login = true,
                        username = "alice",
                        cookies = mutableMapOf("d_c0" to "old-dc0"),
                    ),
                )
            }
            val accountData = ZhihuAccountStore(repository)
            val firstClient = accountData.client
            val nextSession = ZhihuAccountSession(
                login = true,
                username = "alice",
                cookies = mutableMapOf("d_c0" to "new-dc0"),
            )

            accountData.save(nextSession)

            assertEquals(nextSession.username, accountData.session.username)
            assertEquals(nextSession.username, accountData.client.load().username)
            assertSame(firstClient, accountData.client)
            accountData.close()
        }
    }

    @Test
    fun cookieChangesPersistTheBoundSessionWithoutCreatingAnotherState() = runTest {
        val store = ClientInMemoryAccountSessionStore()
        val repository = ZhihuAccountRepository(store)
        repository.save(
            ZhihuAccountSession(
                login = true,
                username = "alice",
                cookies = mutableMapOf("z_c0" to "old-token"),
            ),
        )
        withAccountEngine(
            MockEngine {
                respond(
                    content = "{}",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.SetCookie, "z_c0=new-token; Path=/"),
                )
            },
        ) {
            val accountData = ZhihuAccountStore(repository)
            val session = accountData.session
            val client = accountData.client

            client.httpClient().get("https://www.zhihu.com/")

            assertNotSame(session, accountData.session)
            assertSame(client, accountData.client)
            assertEquals("new-token", accountData.session.cookies["z_c0"])
            assertEquals("new-token", repository.load().cookies["z_c0"])
            accountData.close()
        }
    }

    @Test
    fun profileRefreshKeepsClientAndPreservesMobileTokens() = runTest {
        val repository = ZhihuAccountRepository(ClientInMemoryAccountSessionStore())
        repository.save(
            ZhihuAccountSession(
                login = true,
                cookies = mutableMapOf("z_c0" to "token"),
                mobileAccessToken = "access",
                mobileRefreshToken = "refresh",
                mobileTokenType = "bearer",
                mobileTokenExpiresAt = 1234,
            ),
        )
        withAccountEngine(
            MockEngine {
                respond(
                    content = """{"id":"1","name":"alice","url_token":"alice","user_type":"people"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        ) {
            val accountData = ZhihuAccountStore(repository)
            val oldClient = accountData.client

            val refreshed = accountData.client.refreshAndSaveProfile()

            assertEquals("access", refreshed?.mobileAccessToken)
            assertEquals("refresh", accountData.session.mobileRefreshToken)
            assertSame(oldClient, accountData.client)
            accountData.close()
        }
    }

    @Test
    fun mobileLoginVerifiesProfileAndAtomicallyReplacesSessionAndClient() = runTest {
        val store = ClientInMemoryAccountSessionStore()
        val repository = ZhihuAccountRepository(store)
        withAccountEngine(
            MockEngine { request ->
                assertEquals("/api/v4/me", request.url.encodedPath)
                respond(
                    content = """{"id":"1","name":"alice","url_token":"alice","user_type":"people"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        ) {
            val accountData = ZhihuAccountStore(repository)
            val oldClient = accountData.client

            val verified = accountData.login(
                ZhihuMobileLoginToken(
                    accessToken = "mobile-access",
                    refreshToken = "mobile-refresh",
                    tokenType = "bearer",
                    expiresAt = 1_700_003_600L,
                    cookies = mapOf(
                        "q_c0" to "q-cookie",
                        "z_c0" to "z-cookie",
                        "d_c0" to "device-cookie",
                    ),
                ),
            )

            assertEquals(true, verified)
            assertEquals(1, store.writeCount)
            assertEquals("alice", accountData.session.username)
            assertEquals("z-cookie", accountData.session.cookies["z_c0"])
            assertEquals("device-cookie", accountData.session.cookies["d_c0"])
            assertEquals("mobile-access", accountData.session.mobileAccessToken)
            assertEquals("mobile-refresh", accountData.session.mobileRefreshToken)
            assertNotSame(oldClient, accountData.client)
            assertSame(accountData.session, accountData.client.load())
            accountData.close()
        }
    }

    @Test
    fun logoutDestroysTheBoundClientAndPublishesOneGuestSession() = runTest {
        withAccountEngine(emptyMockEngine()) {
            val repository = ZhihuAccountRepository(ClientInMemoryAccountSessionStore()).apply {
                save(
                    ZhihuAccountSession(
                        login = true,
                        username = "alice",
                        cookies = mutableMapOf("z_c0" to "token"),
                    ),
                )
            }
            val accountData = ZhihuAccountStore(repository)
            val signedInClient = accountData.client

            accountData.clear()

            assertEquals(false, accountData.session.login)
            assertSame(accountData.session, accountData.client.load())
            assertNotSame(signedInClient, accountData.client)
            accountData.close()
        }
    }

    private fun emptyMockEngine(): HttpClientEngine =
        MockEngine {
            respond(
                content = "{}",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
}

private suspend fun <T> withAccountEngine(
    engine: HttpClientEngine,
    block: suspend () -> T,
): T {
    check(accountHttpClientEngineForTesting == null)
    accountHttpClientEngineForTesting = engine
    return try {
        block()
    } finally {
        accountHttpClientEngineForTesting = null
        engine.close()
    }
}

private class ClientInMemoryAccountSessionStore(
    var text: String? = null,
) : ZhihuAccountSessionStore {
    var writeCount: Int = 0

    override fun readText(): String? = text

    override fun writeText(text: String) {
        this.text = text
        writeCount++
    }

    override fun delete() {
        text = null
    }
}
