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
import platform.AppKit.NSToolbarItemGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MacosNativeToolbarTest {
    @Test
    fun navigationItemsFollowConfiguredOrder() {
        val items = macosToolbarNavigationItems(
            listOf("Home", "Follow", "HotList", "Daily", "OnlineHistory", "MyCollections", "Account"),
        )

        assertEquals(
            listOf("Home", "Follow", "HotList", "Daily", "OnlineHistory", "MyCollections", "Account"),
            items.map(MacosToolbarNavigationItem::destinationName),
        )
        assertEquals(
            listOf("首页", "关注", "热榜", "日报", "历史", "收藏", "账号"),
            items.map(MacosToolbarNavigationItem::label),
        )
    }

    @Test
    fun nativeActionsDispatchToSharedNavigation() = autoreleasepool {
        val destinations = macosToolbarNavigationItems(listOf("Home", "Daily"))
        val controller = MacosNativeToolbarController(destinations)
        var navigation = ""
        var action = ""
        controller.onNavigate = { navigation = it }
        controller.onSearch = { action = "search" }
        controller.onNotifications = { action = "notifications" }

        val identifiers = controller.toolbarDefaultItemIdentifiers(controller.toolbar)
        assertEquals(3, identifiers.size)
        assertTrue(
            controller.toolbar(controller.toolbar, identifiers[0] as String, false) is NSToolbarItemGroup,
        )
        assertTrue(
            controller.toolbar(controller.toolbar, identifiers[2] as String, false) is NSToolbarItemGroup,
        )

        controller.performNavigation(1)
        assertEquals("Daily", navigation)
        controller.performAction(0)
        assertEquals("search", action)
        controller.performAction(1)
        assertEquals("notifications", action)
    }
}
