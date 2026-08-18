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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock

class QrLoginClientTest {
    @Test
    fun parsesAndSyncsQrCookies() {
        val cookies = parseCookieAssignments("z_c0=abc; Path=/; Domain=.zhihu.com; _xsrf=def; HttpOnly")
        assertEquals(mapOf("z_c0" to "abc", "_xsrf" to "def"), cookies)

        val syncedCookies = mutableMapOf<String, String>()
        syncCookiesFromScanInfo(syncedCookies, ZhihuQrScanInfo(zC0 = "token"))

        assertEquals("token", syncedCookies["z_c0"])
    }

    @Test
    fun recognizesQrTerminalStates() {
        assertTrue(isQrLoginSuccessful(ZhihuQrScanInfo(loginStatus = "login_success")))
        assertFalse(isQrLoginSuccessful(ZhihuQrScanInfo(loginStatus = "waiting")))
        assertTrue(isQrLoginExpired(ZhihuQrScanInfo(status = 2)))
        assertTrue(isQrLoginExpired(ZhihuQrScanInfo(loginStatus = "qr_code_expired")))
        assertFalse(isQrLoginExpired(ZhihuQrScanInfo(status = 1, loginStatus = "waiting")))
    }

    @Test
    fun normalizesQrDeadlineUnitsAndStaleValues() {
        val before = Clock.System.now().toEpochMilliseconds()
        val epochSeconds = Clock.System.now().toEpochMilliseconds() / 1000 + 600
        val epochMillis = Clock.System.now().toEpochMilliseconds() + 600_000

        listOf(600L, 600_000L).forEach { expiresAt ->
            val deadline = normalizeDeadline(expiresAt)
            assertTrue(deadline >= before + 600_000)
            assertTrue(deadline < before + 601_000)
        }
        val staleDeadline = normalizeDeadline(1_000_000_000)
        assertTrue(staleDeadline >= before + 120_000)
        assertTrue(staleDeadline < before + 121_000)
        assertEquals(epochSeconds * 1000, normalizeDeadline(epochSeconds))
        assertEquals(epochMillis, normalizeDeadline(epochMillis))
    }
}
