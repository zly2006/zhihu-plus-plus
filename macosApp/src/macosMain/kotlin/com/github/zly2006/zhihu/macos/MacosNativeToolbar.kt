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
import com.github.zly2006.zhihu.ui.MacosWindowChromeState
import kotlinx.cinterop.ObjCAction
import platform.AppKit.NSImage
import platform.AppKit.NSSegmentedControl
import platform.AppKit.NSToolbar
import platform.AppKit.NSToolbarDelegateProtocol
import platform.AppKit.NSToolbarFlexibleSpaceItemIdentifier
import platform.AppKit.NSToolbarItem
import platform.AppKit.NSToolbarItemGroup
import platform.AppKit.NSToolbarItemGroupSelectionModeMomentary
import platform.AppKit.NSToolbarItemGroupSelectionModeSelectOne
import platform.AppKit.NSToolbarItemVisibilityPriorityHigh
import platform.AppKit.NSWindow
import platform.AppKit.NSWindowToolbarStyle.NSWindowToolbarStyleUnified
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSSelectorFromString
import platform.darwin.NSObject

private const val TOOLBAR_IDENTIFIER = "com.github.zly2006.zhihu.macos.toolbar"
private const val NAVIGATION_ITEM_IDENTIFIER = "com.github.zly2006.zhihu.macos.toolbar.navigation"
private const val ACTIONS_ITEM_IDENTIFIER = "com.github.zly2006.zhihu.macos.toolbar.actions"

internal data class MacosToolbarNavigationItem(
    val destinationName: String,
    val label: String,
)

internal fun macosToolbarNavigationItems(destinationKeys: List<String>): List<MacosToolbarNavigationItem> {
    val labels = mapOf(
        "Home" to "首页",
        "Follow" to "关注",
        "HotList" to "热榜",
        "Daily" to "日报",
        "OnlineHistory" to "历史",
        "MyCollections" to "收藏",
        "Account" to "账号",
    )
    return destinationKeys.mapNotNull { key ->
        labels[key]?.let { label -> MacosToolbarNavigationItem(key, label) }
    }
}

@Composable
internal fun MacosNativeToolbar(
    window: NSWindow,
    chromeState: MacosWindowChromeState,
) {
    val destinations = macosToolbarNavigationItems(chromeState.destinationKeys)
    if (destinations.isEmpty()) return

    val controller = remember(window, destinations) {
        MacosNativeToolbarController(destinations)
    }

    SideEffect {
        controller.onNavigate = chromeState::navigate
        controller.onSearch = chromeState::search
        controller.onNotifications = chromeState::notifications
        controller.updateSelectedDestination(chromeState.selectedDestinationName)
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
    private val destinations: List<MacosToolbarNavigationItem>,
) : NSObject(),
    NSToolbarDelegateProtocol {
    var onNavigate: (String) -> Unit = {}
    var onSearch: () -> Unit = {}
    var onNotifications: () -> Unit = {}

    private var selectedDestinationName: String? = null
    private var navigationGroup: NSToolbarItemGroup? = null

    val toolbar = NSToolbar(TOOLBAR_IDENTIFIER).apply {
        delegate = this@MacosNativeToolbarController
        allowsUserCustomization = false
        autosavesConfiguration = false
        showsBaselineSeparator = false
    }

    override fun toolbarDefaultItemIdentifiers(toolbar: NSToolbar): List<*> = listOf(
        NAVIGATION_ITEM_IDENTIFIER,
        NSToolbarFlexibleSpaceItemIdentifier,
        ACTIONS_ITEM_IDENTIFIER,
    )

    override fun toolbarAllowedItemIdentifiers(toolbar: NSToolbar): List<*> = toolbarDefaultItemIdentifiers(toolbar)

    override fun toolbar(
        toolbar: NSToolbar,
        itemForItemIdentifier: String?,
        willBeInsertedIntoToolbar: Boolean,
    ): NSToolbarItem? = when (itemForItemIdentifier) {
        NAVIGATION_ITEM_IDENTIFIER -> navigationItemGroup()
        ACTIONS_ITEM_IDENTIFIER -> actionItemGroup()
        else -> null
    }

    internal fun updateSelectedDestination(destinationName: String) {
        selectedDestinationName = destinationName
        navigationGroup?.selectedIndex = destinations
            .indexOfFirst {
                it.destinationName == destinationName
            }.toLong()
    }

    internal fun performNavigation(index: Int) {
        destinations.getOrNull(index)?.destinationName?.let(onNavigate)
    }

    internal fun performAction(index: Int) {
        when (index) {
            0 -> onSearch()
            1 -> onNotifications()
        }
    }

    @ObjCAction
    fun selectNavigation(sender: NSObject) {
        selectedIndex(sender)?.let(::performNavigation)
    }

    @ObjCAction
    fun selectAction(sender: NSObject) {
        selectedIndex(sender)?.let(::performAction)
    }

    private fun selectedIndex(sender: NSObject): Int? = when (sender) {
        is NSToolbarItemGroup -> sender.selectedIndex.toInt()
        is NSSegmentedControl -> sender.selectedSegment.toInt()
        else -> null
    }

    private fun navigationItemGroup(): NSToolbarItemGroup {
        val labels = destinations.map(MacosToolbarNavigationItem::label)
        return NSToolbarItemGroup
            .groupWithItemIdentifier(
                itemIdentifier = NAVIGATION_ITEM_IDENTIFIER,
                titles = labels,
                selectionMode = NSToolbarItemGroupSelectionModeSelectOne,
                labels = labels,
                target = this,
                action = NSSelectorFromString("selectNavigation:"),
            ).apply {
                label = ""
                paletteLabel = ""
                visibilityPriority = NSToolbarItemVisibilityPriorityHigh
                selectedIndex = destinations
                    .indexOfFirst {
                        it.destinationName == selectedDestinationName
                    }.toLong()
                navigationGroup = this
            }
    }

    private fun actionItemGroup(): NSToolbarItemGroup {
        val labels = listOf("搜索", "通知")
        val images = listOf(
            requireNotNull(NSImage.imageWithSystemSymbolName("magnifyingglass", labels[0])),
            requireNotNull(NSImage.imageWithSystemSymbolName("bell", labels[1])),
        )
        return NSToolbarItemGroup
            .groupWithItemIdentifier(
                itemIdentifier = ACTIONS_ITEM_IDENTIFIER,
                images = images,
                selectionMode = NSToolbarItemGroupSelectionModeMomentary,
                labels = labels,
                target = this,
                action = NSSelectorFromString("selectAction:"),
            ).apply {
                label = ""
                paletteLabel = ""
                visibilityPriority = NSToolbarItemVisibilityPriorityHigh
            }
    }
}
