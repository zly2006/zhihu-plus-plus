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

@file:OptIn(kotlinx.cinterop.BetaInteropApi::class)

package com.github.zly2006.zhihu.macos

import com.github.zly2006.zhihu.account.generateQrLoginBitmap
import com.github.zly2006.zhihu.util.hmacSha1Hex
import kotlinx.cinterop.autoreleasepool
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.processUnhandledException
import kotlin.native.setUnhandledExceptionHook
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MacosNativeRuntimeTest {
    @Test
    fun cryptoAndQrRenderingAreAvailable() {
        assertEquals(
            "de7c9b85b8b78aa6bc8a7a36f70a90701c9db4d9",
            hmacSha1Hex("key", "The quick brown fox jumps over the lazy dog"),
        )
        autoreleasepool {
            val qrBitmap = generateQrLoginBitmap("https://www.zhihu.com/")
            assertTrue(qrBitmap.width > 0)
            assertTrue(qrBitmap.height > 0)
        }
    }

    @OptIn(ExperimentalNativeApi::class)
    @Test
    fun unhandledExceptionHookReturnsControlToTheRuntime() {
        var capturedMessage: String? = null
        setUnhandledExceptionHook { error ->
            capturedMessage = error.message
        }

        processUnhandledException(IllegalStateException("native-unhandled-exception-test"))

        assertEquals("native-unhandled-exception-test", capturedMessage)
    }
}
