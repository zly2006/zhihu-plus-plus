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
