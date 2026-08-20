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
import kotlinx.cinterop.autoreleasepool
import platform.AppKit.NSApplication
import platform.AppKit.NSApplicationActivationPolicy
import platform.AppKit.NSImage
import platform.Foundation.NSBundle
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.setUnhandledExceptionHook

@OptIn(ExperimentalNativeApi::class)
fun main() {
    setUnhandledExceptionHook { error ->
        runCatching {
            println("Unhandled coroutine exception: ${error.message ?: error::class.simpleName}")
            error.printStackTrace()
        }
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
