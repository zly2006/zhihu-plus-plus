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

package com.chloemlla.zhplus.ui

import com.chloemlla.zhplus.navigation.Account
import com.chloemlla.zhplus.navigation.Daily
import com.chloemlla.zhplus.navigation.Follow
import com.chloemlla.zhplus.navigation.Home
import com.chloemlla.zhplus.navigation.HotList
import com.chloemlla.zhplus.navigation.MyCollections
import com.chloemlla.zhplus.navigation.OnlineHistory
import com.chloemlla.zhplus.ui.subscreens.bottomBarItemOrderFromPreference
import com.chloemlla.zhplus.ui.subscreens.defaultBottomBarSelectionKeys
import com.chloemlla.zhplus.ui.subscreens.normalizeBottomBarItemOrder
import com.chloemlla.zhplus.ui.subscreens.normalizeBottomBarSelection
import com.chloemlla.zhplus.ui.subscreens.resolveValidStartDestinationKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZhihuMainPreferencesTest {
    @Test
    fun defaultBottomBarSelectionMatchesNavigationMode() {
        assertEquals(
            linkedSetOf(Home.name, Follow.name, Daily.name),
            defaultBottomBarSelectionKeys(duo3HomeAccount = true),
        )
        assertEquals(
            linkedSetOf(Home.name, Follow.name, Daily.name, OnlineHistory.name, Account.name),
            defaultBottomBarSelectionKeys(duo3HomeAccount = false),
        )
    }

    @Test
    fun normalizeBottomBarSelectionKeepsAccountAsSeparateTabWhenHomeAccountIsOff() {
        val normalized = normalizeBottomBarSelection(
            selectedKeys = linkedSetOf(Home.name, Follow.name, HotList.name, Daily.name, OnlineHistory.name),
            duo3HomeAccount = false,
        )

        assertEquals(5, normalized.size)
        assertTrue(Account.name in normalized)
        assertFalse(HotList.name in normalized)
    }

    @Test
    fun normalizeBottomBarSelectionReplacesAccountWithHomeAccountWhenEnabled() {
        val normalized = normalizeBottomBarSelection(
            selectedKeys = linkedSetOf(Home.name, Account.name),
            duo3HomeAccount = true,
            enforceMinimumSelection = true,
        )

        assertTrue(Home.name in normalized)
        assertFalse(Account.name in normalized)
        assertEquals(3, normalized.size)
    }

    @Test
    fun resolveValidStartDestinationFallsBackToFirstAvailableDestination() {
        assertEquals(
            Follow.name,
            resolveValidStartDestinationKey(
                preferredKey = HotList.name,
                availableKeysInOrder = listOf(Follow.name, Daily.name),
            ),
        )
    }

    @Test
    fun normalizeBottomBarSelectionAllowsCollectionsEntry() {
        val normalized = normalizeBottomBarSelection(
            selectedKeys = linkedSetOf(Home.name, HotList.name, MyCollections.name),
            duo3HomeAccount = true,
            enforceMinimumSelection = true,
        )

        assertTrue(MyCollections.name in normalized)
        assertEquals(3, normalized.size)
    }

    @Test
    fun normalizeBottomBarItemOrderKeepsPreferredOrderAndAppendsMissingSelectedItems() {
        val normalized = normalizeBottomBarItemOrder(
            preferredOrderKeys = listOf(HotList.name, MyCollections.name, HotList.name, "Unknown"),
            selectedKeys = linkedSetOf(Home.name, HotList.name, MyCollections.name, Daily.name),
        )

        assertEquals(
            listOf(HotList.name, MyCollections.name, Home.name, Daily.name),
            normalized,
        )
    }

    @Test
    fun bottomBarItemOrderFromPreferenceIgnoresUnselectedAndUnknownKeys() {
        val normalized = bottomBarItemOrderFromPreference(
            preferenceValue = "${MyCollections.name}, Unknown, ${HotList.name}, ${OnlineHistory.name}",
            selectedKeys = linkedSetOf(Home.name, HotList.name, MyCollections.name),
        )

        assertEquals(
            listOf(MyCollections.name, HotList.name, Home.name),
            normalized,
        )
    }
}
