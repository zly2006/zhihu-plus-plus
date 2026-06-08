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

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ZhihuAccountRepositoryTest {
    @Test
    fun loadReturnsDefaultSessionWhenStoreIsEmpty() {
        val repository = ZhihuAccountRepository(InMemoryAccountSessionStore())

        val session = repository.load()

        assertFalse(session.login)
        assertEquals("", session.username)
        assertTrue(session.cookies.isEmpty())
        assertEquals(DEFAULT_ZHIHU_USER_AGENT, session.userAgent)
        assertNull(session.profile)
    }

    @Test
    fun saveAndLoadRoundTripsSession() {
        val store = InMemoryAccountSessionStore()
        val repository = ZhihuAccountRepository(store)
        val session = ZhihuAccountSession(
            login = true,
            username = "alice",
            cookies = mutableMapOf("z_c0" to "token", "d_c0" to "dc0"),
            profile = ZhihuAccountProfileSnapshot(
                id = "123",
                name = "alice",
                urlToken = "alice-token",
                userType = "people",
            ),
            self = buildJsonObject {
                put("id", "123")
                put("name", "alice")
            },
        )

        repository.save(session)

        assertEquals(session, repository.load())
    }

    @Test
    fun loadFallsBackToDefaultSessionForInvalidJson() {
        val repository = ZhihuAccountRepository(InMemoryAccountSessionStore("not json"))

        val session = repository.load()

        assertFalse(session.login)
        assertTrue(session.cookies.isEmpty())
    }

    @Test
    fun clearDeletesStoredSession() {
        val store = InMemoryAccountSessionStore()
        val repository = ZhihuAccountRepository(store)
        repository.save(
            ZhihuAccountSession(
                login = true,
                username = "alice",
                cookies = mutableMapOf("z_c0" to "token"),
            ),
        )

        repository.clear()

        assertNull(store.text)
        assertFalse(repository.load().login)
    }
}

private class InMemoryAccountSessionStore(
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
