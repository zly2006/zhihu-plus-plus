package com.github.zly2006.zhihu.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ZhihuFetchSignatureTest {
    @Test
    fun md5HexMatchesKnownVectors() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", ZhihuFetchSignature.md5Hex(""))
        assertEquals("900150983cd24fb0d6963f7d28e17f72", ZhihuFetchSignature.md5Hex("abc"))
        assertEquals("5d41402abc4b2a76b9719d911017c592", ZhihuFetchSignature.md5Hex("hello"))
    }

    @Test
    fun createZse96HeaderUsesZhihuPrefix() {
        val header = ZhihuFetchSignature.createZse96Header(
            zse93 = "101_3_3.0",
            url = "https://www.zhihu.com/api/v4/me",
            dc0 = "token",
        )

        assertTrue(header.startsWith("2.0_"))
    }
}
