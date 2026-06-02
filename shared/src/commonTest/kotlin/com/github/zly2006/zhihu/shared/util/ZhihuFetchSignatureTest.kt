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

package com.github.zly2006.zhihu.shared.util

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.takeFrom
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

    @Test
    fun signZhihuFetchRequestAddsRequiredHeaders() {
        val builder = HttpRequestBuilder().apply {
            url.takeFrom("https://www.zhihu.com/api/v4/me")
            signZhihuFetchRequest(
                dc0 = "token",
                body = """{"hello":"world"}""",
            )
        }

        assertEquals("101_3_3.0", builder.headers["x-zse-93"])
        assertEquals("fetch", builder.headers["x-requested-with"])
        assertTrue(builder.headers["x-zse-96"].orEmpty().startsWith("2.0_"))
        assertTrue(HttpHeaders.UserAgent !in builder.headers.names())
    }
}
