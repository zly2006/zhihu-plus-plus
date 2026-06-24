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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.components.LocalVerticalPagerScrollGate
import com.github.zly2006.zhihu.ui.components.rememberVerticalPagerScrollGate
import com.github.zly2006.zhihu.ui.components.verticalPagerScrollGate
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class AnswerVerticalPagerScrollGateInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
    }

    @Test
    fun bottomBoundarySwipeUpTriggersNextPageNavigation() {
        val nextNavigationCount = AtomicInteger(0)

        composeRule.setScreenContent {
            val scrollState = rememberScrollState()
            val gate = rememberVerticalPagerScrollGate(
                onNavigatePrevious = {},
                onNavigateNext = { nextNavigationCount.incrementAndGet() },
                canGoPrevious = { false },
                canGoNext = { true },
            )
            CompositionLocalProvider(LocalVerticalPagerScrollGate provides gate) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalPagerScrollGate(scrollState = scrollState, enabled = true)
                        .verticalScroll(scrollState)
                        .testTag(ANSWER_CONTENT_TAG),
                ) {
                    Text("离线回答正文顶部")
                    Spacer(Modifier.height(1800.dp))
                    Text(
                        text = "离线回答正文底部",
                        modifier = Modifier.testTag(ANSWER_BOTTOM_TAG),
                    )
                }
            }
        }

        composeRule
            .onNodeWithTag(ANSWER_BOTTOM_TAG)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(ANSWER_CONTENT_TAG).performTouchInput {
            swipeUp(durationMillis = 700)
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            nextNavigationCount.get() == 1
        }
    }

    private companion object {
        const val ANSWER_CONTENT_TAG = "answer_vertical_pager_gate_content"
        const val ANSWER_BOTTOM_TAG = "answer_vertical_pager_gate_bottom"
    }
}
