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
