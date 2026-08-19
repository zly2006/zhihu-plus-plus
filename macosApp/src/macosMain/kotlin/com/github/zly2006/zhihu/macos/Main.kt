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

@file:OptIn(kotlinx.cinterop.BetaInteropApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package com.github.zly2006.zhihu.macos

import androidx.compose.ui.window.Window
import com.github.zly2006.zhihu.account.MacosQrLoginScreen
import com.github.zly2006.zhihu.account.generateQrLoginBitmap
import com.github.zly2006.zhihu.util.hmacSha1Hex
import kotlinx.cinterop.autoreleasepool
import platform.AppKit.NSApplication
import platform.AppKit.NSApplicationActivationPolicy
import platform.AppKit.NSImage
import platform.Foundation.NSBundle
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.processUnhandledException
import kotlin.native.setUnhandledExceptionHook

@OptIn(ExperimentalNativeApi::class)
fun main(args: Array<String>) {
    var handledExceptionSmokeTest = false
    setUnhandledExceptionHook { error ->
        if (error.message == UNHANDLED_EXCEPTION_SMOKE_TEST_MESSAGE) {
            handledExceptionSmokeTest = true
        } else {
            runCatching {
                println("Unhandled coroutine exception: ${error.message ?: error::class.simpleName}")
                error.printStackTrace()
            }
        }
    }

    if ("--smoke-test" in args) {
        check(hmacSha1Hex("key", "The quick brown fox jumps over the lazy dog") == "de7c9b85b8b78aa6bc8a7a36f70a90701c9db4d9")
        autoreleasepool {
            val qrBitmap = generateQrLoginBitmap("https://www.zhihu.com/")
            check(qrBitmap.width > 0 && qrBitmap.height > 0)
        }
        processUnhandledException(IllegalStateException(UNHANDLED_EXCEPTION_SMOKE_TEST_MESSAGE))
        check(handledExceptionSmokeTest)
        println("Zhihu++ macOS Kotlin/Native ZhihuMain host smoke test passed")
        return
    }

    autoreleasepool {
        val application = NSApplication.sharedApplication()
        application.setActivationPolicy(NSApplicationActivationPolicy.NSApplicationActivationPolicyRegular)
        NSBundle.mainBundle
            .pathForResource("desktop-icon", ofType = "png")
            ?.let { iconPath -> NSImage(contentsOfFile = iconPath) }
            ?.let(application::setApplicationIconImage)
        Window(
            title = "Zhihu++",
        ) {
            MacosQrLoginScreen()
        }
        application.activateIgnoringOtherApps(true)
        application.run()
    }
}

private const val UNHANDLED_EXCEPTION_SMOKE_TEST_MESSAGE = "native-unhandled-exception-smoke-test"
