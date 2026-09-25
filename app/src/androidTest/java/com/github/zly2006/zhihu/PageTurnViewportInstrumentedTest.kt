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

package com.github.zly2006.zhihu

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.components.LocalPageTurnDispatcher
import com.github.zly2006.zhihu.ui.components.PageTurnCommand
import com.github.zly2006.zhihu.ui.components.PageTurnDispatcher
import com.github.zly2006.zhihu.ui.components.pageTurnViewportWithGuide
import com.github.zly2006.zhihu.ui.components.rememberPageTurnTarget
import com.github.zly2006.zhihu.ui.subscreens.PREF_PAGE_TURN_PERCENT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PageTurnViewportInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/630
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/728
     */
    @Test
    fun modifierReportsViewportAndDisabledTargetReturnsKeysToSystem() {
        composeRule.resetAppPreferences()
        composeRule.activity
            .getSharedPreferences(PREFERENCE_NAME, 0)
            .edit()
            .putInt(PREF_PAGE_TURN_PERCENT, 90)
            .commit()

        val dispatcher = PageTurnDispatcher()
        val enabled = mutableStateOf(true)
        lateinit var scrollState: ScrollState
        composeRule.setScreenContent {
            CompositionLocalProvider(LocalPageTurnDispatcher provides dispatcher) {
                scrollState = rememberScrollState()
                val target = rememberPageTurnTarget(
                    scrollState = scrollState,
                    enabled = enabled.value,
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .pageTurnViewportWithGuide(target)
                        .verticalScroll(scrollState),
                ) {
                    repeat(80) { Text("第 $it 行", fontSize = 20.sp) }
                }
            }
        }

        assertTrue(dispatcher.dispatch(PageTurnCommand.PageDown))
        composeRule.waitUntil(5_000) { scrollState.value > 0 }
        val firstPage = scrollState.value
        val expectedPage = 270 * composeRule.activity.resources.displayMetrics.density
        assertEquals(expectedPage.toDouble(), firstPage.toDouble(), 2.0)

        composeRule.runOnIdle { enabled.value = false }
        composeRule.waitUntil(5_000) { !dispatcher.hasActiveTarget }
        assertFalse(dispatcher.dispatch(PageTurnCommand.PageDown))
    }

    /**
     * Regression: 编程式翻页（Page Down 键、翻页悬浮按钮、音量键翻页）不产生嵌套滚动事件，
     * 基于 NestedScrollConnection 的自动隐藏 UI（主 tab 底栏、文章页顶栏收起）收不到通知。
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/760
     */
    @Test
    fun pageTurnCommandsDispatchNestedScrollEventsToAncestors() {
        composeRule.resetAppPreferences()
        composeRule.activity
            .getSharedPreferences(PREFERENCE_NAME, 0)
            .edit()
            .putInt(PREF_PAGE_TURN_PERCENT, 90)
            .commit()

        val dispatcher = PageTurnDispatcher()
        val preScrollYs = mutableListOf<Float>()
        val userInputOnlyEvents = mutableListOf<Float>()
        lateinit var scrollState: ScrollState
        composeRule.setScreenContent {
            val recordingConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        preScrollYs += available.y
                        return Offset.Zero
                    }
                }
            }
            // 模仿回答切换（AnswerVerticalOverscroll）：只响应用户直接输入。
            val userInputOnlyConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        if (source == NestedScrollSource.UserInput) userInputOnlyEvents += available.y
                        return Offset.Zero
                    }

                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource,
                    ): Offset {
                        if (source == NestedScrollSource.UserInput) userInputOnlyEvents += available.y
                        return Offset.Zero
                    }
                }
            }
            CompositionLocalProvider(LocalPageTurnDispatcher provides dispatcher) {
                scrollState = rememberScrollState()
                val target = rememberPageTurnTarget(
                    scrollState = scrollState,
                    enabled = true,
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .nestedScroll(recordingConnection)
                        .nestedScroll(userInputOnlyConnection)
                        .pageTurnViewportWithGuide(target)
                        .verticalScroll(scrollState),
                ) {
                    repeat(80) { Text("第 $it 行", fontSize = 20.sp) }
                }
            }
        }

        assertTrue(dispatcher.dispatch(PageTurnCommand.PageDown))
        composeRule.waitUntil(5_000) { scrollState.value > 0 }
        composeRule.waitUntil(5_000) { preScrollYs.any { it < 0f } }

        assertTrue(dispatcher.dispatch(PageTurnCommand.PageUp))
        composeRule.waitUntil(5_000) { scrollState.value <= 2 }
        composeRule.waitUntil(5_000) { preScrollYs.any { it > 0f } }

        assertTrue(userInputOnlyEvents.isEmpty())
    }

    /**
     * Regression: 编程式翻页跳过顶栏收起阶段直接滚动正文，正文中间仍显示展开版顶栏。
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/760
     */
    @Test
    fun pageTurnDefersToAncestorConsumingEntirePreScroll() {
        composeRule.resetAppPreferences()
        composeRule.activity
            .getSharedPreferences(PREFERENCE_NAME, 0)
            .edit()
            .putInt(PREF_PAGE_TURN_PERCENT, 90)
            .commit()

        val dispatcher = PageTurnDispatcher()
        var outerConsumed = false
        val greedyConnection = object : NestedScrollConnection {
            // 模仿 PreferCollapsedExitUntilCollapsedScrollBehavior：收起期间把整个 delta 作为已消费返回。
            private var collapseBudget = Float.MAX_VALUE

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0f && collapseBudget > 0f) {
                    collapseBudget = 0f
                    outerConsumed = true
                    return available.copy(x = 0f)
                }
                return Offset.Zero
            }
        }
        lateinit var scrollState: ScrollState
        composeRule.setScreenContent {
            CompositionLocalProvider(LocalPageTurnDispatcher provides dispatcher) {
                scrollState = rememberScrollState()
                val target = rememberPageTurnTarget(
                    scrollState = scrollState,
                    enabled = true,
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .nestedScroll(greedyConnection)
                        .pageTurnViewportWithGuide(target)
                        .verticalScroll(scrollState),
                ) {
                    repeat(80) { Text("第 $it 行", fontSize = 20.sp) }
                }
            }
        }

        assertTrue(dispatcher.dispatch(PageTurnCommand.PageDown))
        composeRule.waitUntil(5_000) { outerConsumed }
        assertEquals(0, scrollState.value)

        assertTrue(dispatcher.dispatch(PageTurnCommand.PageDown))
        val expectedPage = 270 * composeRule.activity.resources.displayMetrics.density
        composeRule.waitUntil(5_000) { scrollState.value >= expectedPage.toInt() - 2 }
        assertEquals(expectedPage.toDouble(), scrollState.value.toDouble(), 2.0)
    }

    /**
     * Regression: 标题区域收起范围超过一页翻页距离时，一次编程式翻页只收起其中一部分，
     * 顶栏停在中间位置。
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/760
     */
    @Test
    fun pageTurnDrivesOversizedToolbarCollapseToCompletion() {
        composeRule.resetAppPreferences()
        composeRule.activity
            .getSharedPreferences(PREFERENCE_NAME, 0)
            .edit()
            .putInt(PREF_PAGE_TURN_PERCENT, 90)
            .commit()

        val expectedPage = 270 * composeRule.activity.resources.displayMetrics.density
        val dispatcher = PageTurnDispatcher()
        // 模仿标题区域超高的 PreferCollapsedExitUntilCollapsedScrollBehavior：收起范围为两页翻页距离，
        // 收起期间把整个 delta 作为已消费返回。
        var collapseBudget = 2 * expectedPage
        val tallToolbarConnection = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0f && collapseBudget > 0f) {
                    val take = minOf(-available.y, collapseBudget)
                    collapseBudget -= take
                    return available.copy(x = 0f)
                }
                return Offset.Zero
            }
        }
        lateinit var scrollState: ScrollState
        composeRule.setScreenContent {
            CompositionLocalProvider(LocalPageTurnDispatcher provides dispatcher) {
                scrollState = rememberScrollState()
                val target = rememberPageTurnTarget(
                    scrollState = scrollState,
                    enabled = true,
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .nestedScroll(tallToolbarConnection)
                        .pageTurnViewportWithGuide(target)
                        .verticalScroll(scrollState),
                ) {
                    repeat(80) { Text("第 $it 行", fontSize = 20.sp) }
                }
            }
        }

        assertTrue(dispatcher.dispatch(PageTurnCommand.PageDown))
        composeRule.waitUntil(5_000) { collapseBudget < 0.5f }
        assertEquals(0, scrollState.value)

        assertTrue(dispatcher.dispatch(PageTurnCommand.PageDown))
        composeRule.waitUntil(5_000) { scrollState.value >= expectedPage.toInt() - 2 }
        assertEquals(expectedPage.toDouble(), scrollState.value.toDouble(), 2.0)
    }
}
