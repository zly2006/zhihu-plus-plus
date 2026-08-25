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

@file:OptIn(
    kotlinx.cinterop.BetaInteropApi::class,
    kotlinx.cinterop.ExperimentalForeignApi::class,
)

package com.github.zly2006.zhihu.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.github.zly2006.zhihu.account.NativeAccountStore
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import platform.AppKit.NSApplication
import platform.AppKit.NSBackingStoreBuffered
import platform.AppKit.NSColor
import platform.AppKit.NSEvent
import platform.AppKit.NSFloatingWindowLevel
import platform.AppKit.NSImage
import platform.AppKit.NSImageAlignCenter
import platform.AppKit.NSImageScaleProportionallyUpOrDown
import platform.AppKit.NSImageView
import platform.AppKit.NSScreen
import platform.AppKit.NSTextAlignmentCenter
import platform.AppKit.NSTextField
import platform.AppKit.NSView
import platform.AppKit.NSViewHeightSizable
import platform.AppKit.NSViewWidthSizable
import platform.AppKit.NSWindow
import platform.AppKit.NSWindowStyleMaskBorderless
import platform.AppKit.labelWithString
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes

private const val VIEWER_SCREEN_FRACTION = 0.8

@Composable
actual fun rememberImageGalleryOpener(): (List<String>, Int) -> Unit {
    val scope = rememberCoroutineScope()
    val accountStore = remember { NativeAccountStore() }
    val userMessages = rememberUserMessageSink()
    val controller = remember(scope, accountStore, userMessages) {
        MacosMarkdownImageViewerController(scope, accountStore, userMessages)
    }

    DisposableEffect(controller) {
        onDispose(controller::dispose)
    }

    return remember(controller) {
        { urls, initialIndex ->
            if (urls.isNotEmpty()) {
                controller.toggle(urls[initialIndex.coerceIn(0, urls.lastIndex)])
            }
        }
    }
}

internal class MacosMarkdownImageViewerController(
    private val scope: CoroutineScope,
    private val accountStore: NativeAccountStore,
    private val userMessages: UserMessageSink,
) {
    private var window: NSWindow? = null
    private var activeImageUrl: String? = null
    private var loadJob: Job? = null

    fun toggle(imageUrl: String) {
        if (activeImageUrl == imageUrl && window?.isVisible() == true) {
            dismiss()
            return
        }

        dismiss()
        activeImageUrl = imageUrl
        val viewerFrame = viewerFrame()
        val contentFrame = viewerFrame.useContents {
            CGRectMake(0.0, 0.0, size.width, size.height)
        }
        val viewerWindow = window ?: createWindow(viewerFrame).also { window = it }
        viewerWindow.setFrame(viewerFrame, display = true)
        viewerWindow.contentView = ClosingLoadingView(
            frame = contentFrame,
            onClose = ::dismiss,
        )
        viewerWindow.makeKeyAndOrderFront(null)

        loadJob = scope.launch {
            runCatching {
                val imageBytes = accountStore.httpClient().get(imageUrl).body<ByteArray>()
                val imageData = imageBytes.usePinned { pinned ->
                    NSData.dataWithBytes(pinned.addressOf(0), imageBytes.size.toULong())
                }
                NSImage(data = imageData)
            }.onSuccess { image ->
                if (activeImageUrl == imageUrl) {
                    viewerWindow.contentView = ClosingImageView(
                        frame = viewerWindow.contentView?.bounds ?: CGRectMake(0.0, 0.0, 0.0, 0.0),
                        image = image,
                        onClose = ::dismiss,
                    )
                }
            }.onFailure { error ->
                if (activeImageUrl == imageUrl) {
                    userMessages.showShortMessage("图片加载失败: ${error.message}")
                    dismiss()
                }
            }
        }
    }

    fun dispose() {
        dismiss()
        val viewerWindow = window
        window = null
        viewerWindow?.close()
    }

    private fun viewerFrame(): kotlinx.cinterop.CValue<CGRect> {
        val screen = NSApplication.sharedApplication.keyWindow?.screen ?: NSScreen.mainScreen
        val visibleFrame = screen?.visibleFrame ?: CGRectMake(0.0, 0.0, 960.0, 720.0)
        return visibleFrame.useContents {
            val viewerWidth = size.width * VIEWER_SCREEN_FRACTION
            val viewerHeight = size.height * VIEWER_SCREEN_FRACTION
            CGRectMake(
                origin.x + (size.width - viewerWidth) / 2.0,
                origin.y + (size.height - viewerHeight) / 2.0,
                viewerWidth,
                viewerHeight,
            )
        }
    }

    private fun createWindow(viewerFrame: kotlinx.cinterop.CValue<CGRect>): NSWindow =
        NSWindow(
            contentRect = viewerFrame,
            styleMask = NSWindowStyleMaskBorderless,
            backing = NSBackingStoreBuffered,
            defer = false,
        ).apply {
            backgroundColor = NSColor.blackColor.colorWithAlphaComponent(0.92)
            opaque = false
            hasShadow = true
            level = NSFloatingWindowLevel
            movableByWindowBackground = true
            releasedWhenClosed = false
        }

    private fun dismiss() {
        loadJob?.cancel()
        loadJob = null
        window?.orderOut(null)
        activeImageUrl = null
    }
}

private class ClosingLoadingView(
    frame: kotlinx.cinterop.CValue<CGRect>,
    private val onClose: () -> Unit,
) : NSView(frame = frame) {
    init {
        val label = NSTextField.labelWithString("正在加载图片…").apply {
            textColor = NSColor.whiteColor
            alignment = NSTextAlignmentCenter
            this.frame = this@ClosingLoadingView.bounds
            setAutoresizingMask(NSViewWidthSizable or NSViewHeightSizable)
        }
        addSubview(label)
    }

    override fun mouseDown(event: NSEvent) {
        onClose()
    }
}

private class ClosingImageView(
    frame: kotlinx.cinterop.CValue<CGRect>,
    image: NSImage,
    private val onClose: () -> Unit,
) : NSImageView(frame = frame) {
    init {
        this.image = image
        imageAlignment = NSImageAlignCenter
        imageScaling = NSImageScaleProportionallyUpOrDown
        setAutoresizingMask(NSViewWidthSizable or NSViewHeightSizable)
    }

    override fun mouseDown(event: NSEvent) {
        onClose()
    }
}
