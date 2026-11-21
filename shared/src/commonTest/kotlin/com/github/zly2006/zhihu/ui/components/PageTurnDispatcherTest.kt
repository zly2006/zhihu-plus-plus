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

package com.github.zly2006.zhihu.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PageTurnDispatcherTest {
    @Test
    fun dispatchesOnlyToMostRecentlyRegisteredTarget() {
        val dispatcher = PageTurnDispatcher()
        val article = dispatcher.registerTarget()
        val comments = dispatcher.registerTarget()

        assertTrue(dispatcher.dispatch(PageTurnCommand.PageDown))
        assertNull(article.commands.tryReceive().getOrNull())
        assertEquals(PageTurnCommand.PageDown, comments.commands.tryReceive().getOrNull())

        comments.close()
        assertTrue(dispatcher.dispatch(PageTurnCommand.PageUp))
        assertEquals(PageTurnCommand.PageUp, article.commands.tryReceive().getOrNull())
    }

    @Test
    fun higherPriorityDetailPaneWinsOverListPaneAndFallsBackWhenClosed() {
        val dispatcher = PageTurnDispatcher()
        val detailArticle = dispatcher.registerTarget(priority = 1)
        // 即使左栏列表由于生命周期/Tab 切换在右栏之后重新注册，priority=1 的右栏详情依然优先响应翻页
        val listFeed = dispatcher.registerTarget(priority = 0)

        assertTrue(dispatcher.dispatch(PageTurnCommand.PageDown))
        assertEquals(PageTurnCommand.PageDown, detailArticle.commands.tryReceive().getOrNull())
        assertNull(listFeed.commands.tryReceive().getOrNull())

        // 当右栏没有详情（例如回退到 EmptyDetail）关闭目标后，自动回退给左栏列表
        detailArticle.close()
        assertTrue(dispatcher.dispatch(PageTurnCommand.PageDown))
        assertEquals(PageTurnCommand.PageDown, listFeed.commands.tryReceive().getOrNull())
    }

    @Test
    fun dispatchesAnswerSwitchAndFeedRefreshTargets() {
        val dispatcher = PageTurnDispatcher()
        assertFalse(dispatcher.hasAnswerSwitchTarget)
        assertFalse(dispatcher.hasRefreshTarget)
        assertFalse(dispatcher.dispatchAnswerSwitch(next = true))
        assertFalse(dispatcher.dispatchRefresh())

        var switchedDirection: String? = null
        val answerTarget = dispatcher.registerAnswerSwitchTarget(
            priority = 1,
            onPrevious = {
                switchedDirection = "prev"
                true
            },
            onNext = {
                switchedDirection = "next"
                true
            },
        )
        assertTrue(dispatcher.hasAnswerSwitchTarget)
        assertTrue(dispatcher.dispatchAnswerSwitch(next = true))
        assertEquals("next", switchedDirection)
        assertTrue(dispatcher.dispatchAnswerSwitch(next = false))
        assertEquals("prev", switchedDirection)

        answerTarget.close()
        assertFalse(dispatcher.hasAnswerSwitchTarget)

        var refreshCount = 0
        val refreshTarget = dispatcher.registerRefreshTarget { refreshCount++ }
        assertTrue(dispatcher.hasRefreshTarget)
        assertTrue(dispatcher.dispatchRefresh())
        assertEquals(1, refreshCount)

        refreshTarget.close()
        assertFalse(dispatcher.hasRefreshTarget)
    }

    @Test
    fun fallsBackWhenNoWhitelistedTargetIsActive() {
        val dispatcher = PageTurnDispatcher()

        assertFalse(dispatcher.hasActiveTarget)
        assertFalse(dispatcher.dispatch(PageTurnCommand.PageDown))

        val target = dispatcher.registerTarget()
        assertTrue(dispatcher.hasActiveTarget)
        target.close()
        assertFalse(dispatcher.hasActiveTarget)
        assertFalse(dispatcher.dispatch(PageTurnCommand.PageDown))
    }
}
