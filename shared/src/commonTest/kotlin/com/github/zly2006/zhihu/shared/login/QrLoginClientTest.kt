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

package com.github.zly2006.zhihu.shared.login

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QrLoginClientTest {
    @Test
    fun parseCookieAssignmentsSkipsAttributes() {
        val cookies = parseCookieAssignments("z_c0=abc; Path=/; Domain=.zhihu.com; _xsrf=def; HttpOnly")

        assertEquals(mapOf("z_c0" to "abc", "_xsrf" to "def"), cookies)
    }

    @Test
    fun syncCookiesFromScanInfoAcceptsSnakeCaseZC0() {
        val cookies = mutableMapOf<String, String>()

        syncCookiesFromScanInfo(cookies, ZhihuQrScanInfo(zC0 = "token"))

        assertEquals("token", cookies["z_c0"])
    }

    @Test
    fun qrSuccessRecognizesStatusString() {
        assertTrue(isQrLoginSuccessful(ZhihuQrScanInfo(loginStatus = "login_success")))
        assertFalse(isQrLoginSuccessful(ZhihuQrScanInfo(loginStatus = "waiting")))
    }
}
