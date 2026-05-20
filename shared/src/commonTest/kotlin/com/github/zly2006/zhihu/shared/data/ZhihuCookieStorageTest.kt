package com.github.zly2006.zhihu.shared.data

import io.ktor.http.Cookie
import io.ktor.http.Url
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ZhihuCookieStorageTest {
    @Test
    fun addCookieSkipsBlankZC0() = runTest {
        val cookies = mutableMapOf("z_c0" to "existing")
        val storage = ZhihuCookieStorage(cookies)

        storage.addCookie(Url("https://www.zhihu.com/"), Cookie("z_c0", "", domain = ".zhihu.com"))

        assertEquals("existing", cookies["z_c0"])
    }

    @Test
    fun addCookieRejectsNonZhihuDomain() = runTest {
        val cookies = mutableMapOf<String, String>()
        val storage = ZhihuCookieStorage(cookies)

        storage.addCookie(Url("https://example.com/"), Cookie("z_c0", "token", domain = "example.com"))

        assertFalse("z_c0" in cookies)
    }

    @Test
    fun getReturnsCookiesForZhihuDomain() = runTest {
        val storage = ZhihuCookieStorage(mutableMapOf("_xsrf" to "token"))

        val cookies = storage.get(Url("https://www.zhihu.com/"))

        assertEquals("_xsrf", cookies.single().name)
        assertEquals("token", cookies.single().value)
        assertEquals("www.zhihu.com", cookies.single().domain)
    }
}
