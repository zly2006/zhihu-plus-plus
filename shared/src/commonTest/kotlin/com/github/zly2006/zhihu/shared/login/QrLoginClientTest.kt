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
import kotlin.time.Clock

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

    @Test
    fun qrExpiredRecognizesStatusCodeAndString() {
        assertTrue(isQrLoginExpired(ZhihuQrScanInfo(status = 2)))
        assertTrue(isQrLoginExpired(ZhihuQrScanInfo(loginStatus = "qr_code_expired")))
        assertFalse(isQrLoginExpired(ZhihuQrScanInfo(status = 1, loginStatus = "waiting")))
    }

    @Test
    fun normalizeDeadlineTreatsSmallExpiresAtAsTtlSeconds() {
        val before = Clock.System.now().toEpochMilliseconds()
        val deadline = normalizeDeadline(600)

        assertTrue(deadline >= before + 600_000)
        assertTrue(deadline < before + 601_000)
    }

    @Test
    fun normalizeDeadlineTreatsSmallExpiresAtAsTtlMillis() {
        val before = Clock.System.now().toEpochMilliseconds()
        val deadline = normalizeDeadline(600_000)

        assertTrue(deadline >= before + 600_000)
        assertTrue(deadline < before + 601_000)
    }

    @Test
    fun normalizeDeadlineFallsBackForStaleEpochSeconds() {
        val before = Clock.System.now().toEpochMilliseconds()
        val deadline = normalizeDeadline(1_000_000_000)

        assertTrue(deadline >= before + 120_000)
        assertTrue(deadline < before + 121_000)
    }

    @Test
    fun normalizeDeadlineKeepsAbsoluteEpochSeconds() {
        val epochSeconds = Clock.System.now().toEpochMilliseconds() / 1000 + 600

        assertEquals(epochSeconds * 1000, normalizeDeadline(epochSeconds))
    }

    @Test
    fun normalizeDeadlineKeepsAbsoluteEpochMillis() {
        val epochMillis = Clock.System.now().toEpochMilliseconds() + 600_000

        assertEquals(epochMillis, normalizeDeadline(epochMillis))
    }
}
