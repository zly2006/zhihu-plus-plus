/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
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

package com.github.zly2006.zhihu.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TopLevelReselectActionTest {
    @Test
    fun returnsNullWhenThereIsNoNewTap() {
        assertNull(topLevelReselectAction(triggerDelta = 0, isAtTop = false))
        assertNull(topLevelReselectAction(triggerDelta = -1, isAtTop = true))
    }

    @Test
    fun returnsScrollToTopForSingleTapAwayFromTop() {
        assertEquals(
            TopLevelReselectAction.ScrollToTop,
            topLevelReselectAction(triggerDelta = 1, isAtTop = false),
        )
    }

    @Test
    fun returnsRefreshWhenAlreadyAtTopOrRapidRetap() {
        assertEquals(
            TopLevelReselectAction.Refresh,
            topLevelReselectAction(triggerDelta = 1, isAtTop = true),
        )
        assertEquals(
            TopLevelReselectAction.Refresh,
            topLevelReselectAction(triggerDelta = 2, isAtTop = false),
        )
    }
}
