@file:OptIn(kotlinx.cinterop.BetaInteropApi::class)

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

package com.github.zly2006.zhihu.macos

import kotlinx.cinterop.autoreleasepool
import platform.AppKit.NSToolbarItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MacosNativeToolbarTest {
    @Test
    fun toolbarUsesIndependentNativeActions() = autoreleasepool {
        val selectedActions = mutableListOf<String>()
        val controller = MacosNativeToolbarController(
            leadingActions = listOf(
                MacosNativeToolbarAction("sidebar", "侧栏", "sidebar.left") {
                    selectedActions += "sidebar"
                },
            ),
            trailingActions = listOf(
                MacosNativeToolbarAction("search", "搜索", "magnifyingglass") {
                    selectedActions += "search"
                },
                MacosNativeToolbarAction("notifications", "通知", "bell") {
                    selectedActions += "notifications"
                },
            ),
        )

        val identifiers = controller.toolbarDefaultItemIdentifiers(controller.toolbar)
        assertEquals(4, identifiers.size)
        assertTrue(
            controller.toolbar(controller.toolbar, identifiers[0] as String, false) is NSToolbarItem,
        )
        assertTrue(
            controller.toolbar(controller.toolbar, identifiers[2] as String, false) is NSToolbarItem,
        )
        assertTrue(
            controller.toolbar(controller.toolbar, identifiers[3] as String, false) is NSToolbarItem,
        )

        val sidebarItem = controller.toolbar(controller.toolbar, identifiers[0] as String, false)
        val searchItem = controller.toolbar(controller.toolbar, identifiers[2] as String, false)
        val notificationsItem = controller.toolbar(controller.toolbar, identifiers[3] as String, false)
        controller.selectAction(sidebarItem!!)
        controller.selectAction(searchItem!!)
        controller.selectAction(notificationsItem!!)
        assertEquals(listOf("sidebar", "search", "notifications"), selectedActions)

        controller.updateActions(
            leadingActions = emptyList(),
            trailingActions = listOf(
                MacosNativeToolbarAction("search", "搜索", "magnifyingglass") {
                    selectedActions += "updated search"
                },
            ),
        )
        controller.selectAction(searchItem)
        assertEquals("updated search", selectedActions.last())
    }
}
