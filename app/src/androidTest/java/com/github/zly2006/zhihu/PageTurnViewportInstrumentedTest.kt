/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
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
import androidx.compose.ui.Modifier
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
}
