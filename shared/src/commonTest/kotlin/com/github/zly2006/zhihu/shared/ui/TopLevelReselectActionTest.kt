package com.github.zly2006.zhihu.shared.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
