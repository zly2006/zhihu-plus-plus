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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState
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
import com.github.zly2006.zhihu.ui.components.rememberPreferCollapsedExitUntilCollapsedScrollBehavior
import com.github.zly2006.zhihu.ui.subscreens.PREF_PAGE_TURN_PERCENT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

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
            // 验证祖先完整消费翻页距离时，正文不滚动。
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
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/630
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/760
     * 祖先部分消费翻页距离时，正文移动剩余距离，后滚动事件不能重复计算祖先的消费量。
     */
    @Test
    fun pageTurnPassesPartialPreScrollRemainderToContent() {
        composeRule.resetAppPreferences()
        composeRule.activity
            .getSharedPreferences(PREFERENCE_NAME, 0)
            .edit()
            .putInt(PREF_PAGE_TURN_PERCENT, 90)
            .commit()

        val expectedPage = 270 * composeRule.activity.resources.displayMetrics.density
        val toolbarRemainder = expectedPage / 3f
        val dispatcher = PageTurnDispatcher()
        var postConsumed = Float.NaN
        val toolbarConnection = object : NestedScrollConnection {
            var remaining = toolbarRemainder

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val taken = minOf(-available.y, remaining)
                remaining -= taken
                return Offset(0f, -taken)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                postConsumed = consumed.y
                return Offset.Zero
            }
        }
        lateinit var scrollState: ScrollState
        composeRule.setScreenContent {
            CompositionLocalProvider(LocalPageTurnDispatcher provides dispatcher) {
                scrollState = rememberScrollState()
                val target = rememberPageTurnTarget(scrollState = scrollState, enabled = true)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .nestedScroll(toolbarConnection)
                        .pageTurnViewportWithGuide(target)
                        .verticalScroll(scrollState),
                ) {
                    repeat(80) { Text("第 $it 行", fontSize = 20.sp) }
                }
            }
        }

        assertTrue(dispatcher.dispatch(PageTurnCommand.PageDown))
        composeRule.waitUntil(5_000) { !postConsumed.isNaN() }
        val expectedContentScroll = expectedPage - toolbarRemainder
        assertEquals(expectedContentScroll.toDouble(), scrollState.value.toDouble(), 2.0)
        assertEquals(-expectedContentScroll.toDouble(), postConsumed.toDouble(), 2.0)
        assertEquals(0.0, toolbarConnection.remaining.toDouble(), 0.01)
    }

    /**
     * Regression: 翻页结束未触发真实顶栏的吸附，超高标题会停在半收起状态。
     * 验证首次翻页收完顶栏且正文不动，第二次才滚动正文。
     * 关联 issue 未提出这项两阶段翻页行为；维护者 zly2006 已于 2026-09-25 明确认可其作为交互契约。
     * Related issue: https://github.com/zly2006/zhihu-plus-plus/issues/630
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/760
     */
    @Test
    @OptIn(ExperimentalMaterial3Api::class)
    fun pageTurnDrivesOversizedToolbarCollapseToCompletion() {
        composeRule.resetAppPreferences()
        composeRule.activity
            .getSharedPreferences(PREFERENCE_NAME, 0)
            .edit()
            .putInt(PREF_PAGE_TURN_PERCENT, 90)
            .commit()

        val expectedPage = (300 * composeRule.activity.resources.displayMetrics.density).roundToInt() * 0.9f
        val dispatcher = PageTurnDispatcher()
        lateinit var toolbarState: TopAppBarState
        lateinit var scrollState: ScrollState
        composeRule.setScreenContent {
            CompositionLocalProvider(LocalPageTurnDispatcher provides dispatcher) {
                toolbarState = rememberTopAppBarState(initialHeightOffsetLimit = -2 * expectedPage)
                val toolbarBehavior = rememberPreferCollapsedExitUntilCollapsedScrollBehavior(toolbarState)
                scrollState = rememberScrollState()
                val target = rememberPageTurnTarget(
                    scrollState = scrollState,
                    enabled = true,
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .nestedScroll(toolbarBehavior.nestedScrollConnection)
                        .pageTurnViewportWithGuide(target)
                        .verticalScroll(scrollState),
                ) {
                    repeat(80) { Text("第 $it 行", fontSize = 20.sp) }
                }
            }
        }

        assertTrue(dispatcher.dispatch(PageTurnCommand.PageDown))
        composeRule.waitUntil(5_000) { toolbarState.collapsedFraction == 1f }
        assertEquals(0, scrollState.value)

        assertTrue(dispatcher.dispatch(PageTurnCommand.PageDown))
        composeRule.waitUntil(5_000) { scrollState.value >= expectedPage.toInt() - 2 }
        assertEquals(expectedPage.toDouble(), scrollState.value.toDouble(), 2.0)
    }
}
