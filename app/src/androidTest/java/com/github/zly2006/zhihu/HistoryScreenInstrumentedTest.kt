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

package com.github.zly2006.zhihu

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.data.HistoryStorage
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.performHorizontalSwipeCycle
import com.github.zly2006.zhihu.test.performVerticalSwipeCycle
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.HistoryScreen
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class HistoryScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.replaceHistoryWithEmptyState()
    }

    @Test
    fun historyScreen_emptyHistoryRendersDeterministicallyAndKeepsSwipeGesturesStable() {
        // Replace the activity content with HistoryScreen after clearing the persisted history file.
        // The expected behavior is a deterministic empty/default render that depends only on local
        // in-memory state, not on whatever history items a previous developer session may have left.
        composeRule.setScreenContent {
            HistoryScreen(innerPadding = PaddingValues())
        }

        // Wait for the initial LaunchedEffect-driven refresh to finish, then verify the empty
        // history footer is rendered exactly once and remains visible as the only deterministic
        // piece of content available with default data.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText(END_OF_LIST_TEXT)).fetchSemanticsNodes().size == 1
        }
        val historyList = composeRule.onNode(hasScrollToIndexAction())
        historyList.assertExists()
        composeRule.onNodeWithText(END_OF_LIST_TEXT).assertIsDisplayed()
        composeRule.onAllNodes(hasText(END_OF_LIST_TEXT)).assertCountEquals(1)
        composeRule.runOnIdle {
            assertTrue(
                composeRule.activity.history.history
                    .isEmpty(),
            )
        }

        // A vertical swipe cycle is the only meaningful gesture in the empty state because the list
        // can overscroll for pull-to-refresh even without feed cards. The footer must stay visible
        // and the local history source must remain empty after the gesture-triggered refresh path.
        historyList.performVerticalSwipeCycle()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(END_OF_LIST_TEXT).assertIsDisplayed()
        composeRule.onAllNodes(hasText(END_OF_LIST_TEXT)).assertCountEquals(1)
        composeRule.runOnIdle {
            assertTrue(
                composeRule.activity.history.history
                    .isEmpty(),
            )
        }

        // A horizontal swipe is effectively a no-op for this screen, but it should still be safe:
        // the gesture must not crash the list, duplicate the footer, or mutate the empty history
        // backing store just because the user brushed sideways on the surface.
        historyList.performHorizontalSwipeCycle()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(END_OF_LIST_TEXT).assertIsDisplayed()
        composeRule.onAllNodes(hasText(END_OF_LIST_TEXT)).assertCountEquals(1)
        composeRule.runOnIdle {
            assertTrue(
                composeRule.activity.history.history
                    .isEmpty(),
            )
        }
    }

    private fun MainActivityComposeRule.replaceHistoryWithEmptyState() {
        val historyFile = File(activity.filesDir, "history.json")

        activity.runOnUiThread {
            historyFile.delete()
            activity.history = HistoryStorage(activity)
        }
        waitForIdle()
        runOnIdle {
            assertTrue(activity.history.history.isEmpty())
        }
    }

    private companion object {
        const val END_OF_LIST_TEXT = "已经到底啦"
    }
}
