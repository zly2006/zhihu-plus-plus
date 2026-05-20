package com.github.zly2006.zhihu.shared.account

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
