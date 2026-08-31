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

package com.github.zly2006.zhihu.macos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import kotlinx.cinterop.ObjCAction
import platform.AppKit.NSImage
import platform.AppKit.NSToolbar
import platform.AppKit.NSToolbarDelegateProtocol
import platform.AppKit.NSToolbarFlexibleSpaceItemIdentifier
import platform.AppKit.NSToolbarItem
import platform.AppKit.NSToolbarItemVisibilityPriorityHigh
import platform.AppKit.NSWindow
import platform.AppKit.NSWindowToolbarStyle.NSWindowToolbarStyleUnified
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSSelectorFromString
import platform.darwin.NSObject

private const val TOOLBAR_IDENTIFIER = "com.github.zly2006.zhihu.macos.toolbar"

internal data class MacosNativeToolbarAction(
    val identifier: String,
    val label: String,
    val systemSymbolName: String,
    val action: () -> Unit,
)

@Composable
internal fun MacosNativeToolbar(
    window: NSWindow,
    leadingActions: List<MacosNativeToolbarAction>,
    trailingActions: List<MacosNativeToolbarAction>,
) {
    val actionIdentifiers = (leadingActions + trailingActions).map(MacosNativeToolbarAction::identifier)
    val controller = remember(window, actionIdentifiers) {
        MacosNativeToolbarController(leadingActions, trailingActions)
    }

    SideEffect {
        controller.updateActions(leadingActions, trailingActions)
    }

    DisposableEffect(window, controller) {
        val previousToolbar = window.toolbar
        val previousToolbarStyle = window.toolbarStyle
        var active = true
        NSOperationQueue.mainQueue.addOperationWithBlock {
            if (active && window.toolbar !== controller.toolbar) {
                window.toolbarStyle = NSWindowToolbarStyleUnified
                window.toolbar = controller.toolbar
            }
        }

        onDispose {
            active = false
            NSOperationQueue.mainQueue.addOperationWithBlock {
                if (window.toolbar === controller.toolbar) {
                    window.toolbar = previousToolbar
                    window.toolbarStyle = previousToolbarStyle
                }
                controller.toolbar.delegate = null
            }
        }
    }
}

internal class MacosNativeToolbarController(
    leadingActions: List<MacosNativeToolbarAction>,
    trailingActions: List<MacosNativeToolbarAction>,
) : NSObject(),
    NSToolbarDelegateProtocol {
    private var leadingActions = leadingActions
    private var trailingActions = trailingActions

    val toolbar = NSToolbar(TOOLBAR_IDENTIFIER).apply {
        delegate = this@MacosNativeToolbarController
        allowsUserCustomization = false
        autosavesConfiguration = false
        showsBaselineSeparator = false
    }

    override fun toolbarDefaultItemIdentifiers(toolbar: NSToolbar): List<*> = buildList {
        addAll(leadingActions.map(MacosNativeToolbarAction::identifier))
        if (leadingActions.isNotEmpty() && trailingActions.isNotEmpty()) {
            add(NSToolbarFlexibleSpaceItemIdentifier)
        }
        addAll(trailingActions.map(MacosNativeToolbarAction::identifier))
    }

    override fun toolbarAllowedItemIdentifiers(toolbar: NSToolbar): List<*> = toolbarDefaultItemIdentifiers(toolbar)

    override fun toolbar(
        toolbar: NSToolbar,
        itemForItemIdentifier: String?,
        willBeInsertedIntoToolbar: Boolean,
    ): NSToolbarItem? = (leadingActions + trailingActions)
        .firstOrNull { it.identifier == itemForItemIdentifier }
        ?.let(::buttonItem)

    @ObjCAction
    fun selectAction(sender: NSObject) {
        val identifier = (sender as? NSToolbarItem)?.itemIdentifier ?: return
        (leadingActions + trailingActions).firstOrNull { it.identifier == identifier }?.action?.invoke()
    }

    fun updateActions(
        leadingActions: List<MacosNativeToolbarAction>,
        trailingActions: List<MacosNativeToolbarAction>,
    ) {
        this.leadingActions = leadingActions
        this.trailingActions = trailingActions
    }

    private fun buttonItem(action: MacosNativeToolbarAction): NSToolbarItem = NSToolbarItem(action.identifier).apply {
        label = action.label
        paletteLabel = action.label
        toolTip = action.label
        image = NSImage.imageWithSystemSymbolName(action.systemSymbolName, action.label)
        target = this@MacosNativeToolbarController
        this.action = NSSelectorFromString("selectAction:")
        visibilityPriority = NSToolbarItemVisibilityPriorityHigh
    }
}
