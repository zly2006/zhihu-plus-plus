package com.github.zly2006.zhihu.ui

import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Daily
import com.github.zly2006.zhihu.navigation.Follow
import com.github.zly2006.zhihu.navigation.Home
import com.github.zly2006.zhihu.navigation.HotList
import com.github.zly2006.zhihu.navigation.OnlineHistory
import com.github.zly2006.zhihu.ui.subscreens.defaultBottomBarSelectionKeys
import com.github.zly2006.zhihu.ui.subscreens.normalizeBottomBarSelection
import com.github.zly2006.zhihu.ui.subscreens.resolveValidStartDestinationKey
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
}
