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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.navigation.History
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.RecordingNavigator
import com.github.zly2006.zhihu.test.performHorizontalSwipeCycle
import com.github.zly2006.zhihu.test.performVerticalSwipeCycle
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.OnlineHistoryScreen
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.OnlineHistoryViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnlineHistoryScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule()

    private val onlineHistoryViewModel: OnlineHistoryViewModel
        get() = ViewModelProvider(composeRule.activity)[OnlineHistoryViewModel::class.java]

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
        clearDisplayItems()
    }

    @After
    fun tearDown() {
        clearDisplayItems()
    }

    @Test
    fun overflowMenuShowsStableActionsAndBackDismissesWithoutNavigation() {
        // Seed deterministic fake rows before composition so OnlineHistoryScreen skips the
        // automatic refresh path. This keeps the test fully local and makes the toolbar/menu
        // assertions independent from login state, network reachability, and remote history data.
        val navigator = setOnlineHistoryScreen()

        composeRule.onNodeWithText("历史记录").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("更多选项").performClick()

        // The overflow menu is the only stable toolbar entry point on this screen, so it must
        // always expose both supported actions and be dismissible with the system back gesture
        // without emitting any app-level navigation callbacks.
        composeRule.onNodeWithText("查看本地历史记录").assertIsDisplayed()
        composeRule.onNodeWithText("清除历史记录").assertIsDisplayed()
        pressBack()
        composeRule.onNodeWithText("查看本地历史记录").assertDoesNotExist()
        composeRule.onNodeWithText("清除历史记录").assertDoesNotExist()

        composeRule.runOnIdle {
            assertEquals(0, navigator.destinations.size)
            assertEquals(0, navigator.backCount)
        }
    }

    @Test
    fun overflowMenuNavigatesLocallyAndClearDialogCancelsWithoutMutatingSeededRows() {
        // Populate the activity-scoped ViewModel with stable placeholder rows so the screen renders
        // a real lazy list but never depends on zhihu read-history responses during this test.
        val navigator = setOnlineHistoryScreen()

        // Choosing the local-history action should synchronously record one navigation event to the
        // History destination and close the menu, while keeping the current screen mounted because
        // the test host only records navigation instead of replacing the content tree.
        composeRule.onNodeWithContentDescription("更多选项").performClick()
        composeRule.onNodeWithText("查看本地历史记录").performClick()
        composeRule.runOnIdle {
            assertEquals(listOf(History), navigator.destinations)
            assertEquals(0, navigator.backCount)
        }

        // Opening the clear-history flow must show the confirmation copy and both dialog buttons,
        // but cancelling via the explicit secondary action should keep the seeded rows intact and
        // must not create any extra navigation event or trigger a destructive clear.
        composeRule.onNodeWithContentDescription("更多选项").performClick()
        composeRule.onNodeWithText("清除历史记录").performClick()
        composeRule.onNodeWithText("确认清除历史记录").assertIsDisplayed()
        composeRule.onNodeWithText("此操作会清除当前账号的在线和本地的全部历史记录。").assertIsDisplayed()
        composeRule.onNodeWithText("确认").assertIsDisplayed()
        composeRule.onNodeWithText("我再想想").assertIsDisplayed()
        composeRule.onNodeWithText("我再想想").performClick()
        composeRule.onNodeWithText("确认清除历史记录").assertDoesNotExist()
        composeRule.onNodeWithText(seedTitle(1)).assertExists()

        composeRule.runOnIdle {
            assertEquals(listOf(History), navigator.destinations)
            assertEquals(0, navigator.backCount)
        }
    }

    @Test
    fun listSwipeCyclesKeepToolbarMenuAndDialogInteractionsStable() {
        // Use enough fake rows to guarantee a scrollable lazy list. The swipe assertions then
        // exercise gesture handling on the actual list container without ever approaching the end
        // of pagination or invoking the network-backed clear-history confirmation path.
        val navigator = setOnlineHistoryScreen(itemCount = 24)

        composeRule.onNodeWithTag(LIST_TAG).assertExists()
        composeRule.onNodeWithTag(LIST_TAG).performVerticalSwipeCycle()
        composeRule.onNodeWithTag(LIST_TAG).performHorizontalSwipeCycle()

        // After both gesture cycles, the toolbar should remain interactive, the overflow menu
        // should still open normally, and dismissing the confirmation dialog with system back
        // should restore the untouched list state instead of navigating away or corrupting UI.
        composeRule.onNodeWithText("历史记录").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("更多选项").performClick()
        composeRule.onNodeWithText("清除历史记录").performClick()
        composeRule.onNodeWithText("确认清除历史记录").assertIsDisplayed()
        pressBack()
        composeRule.onNodeWithText("确认清除历史记录").assertDoesNotExist()
        composeRule.onNodeWithTag(LIST_TAG).assertExists()

        composeRule.runOnIdle {
            assertEquals(0, navigator.destinations.size)
            assertEquals(0, navigator.backCount)
        }
    }

    private fun setOnlineHistoryScreen(itemCount: Int = 24): RecordingNavigator {
        seedDisplayItems(itemCount)
        return composeRule.setScreenContent {
            OnlineHistoryScreen(PaddingValues())
        }
    }

    private fun seedDisplayItems(itemCount: Int) {
        composeRule.activity.runOnUiThread {
            onlineHistoryViewModel.displayItems.clear()
            onlineHistoryViewModel.displayItems.addAll(
                List(itemCount) { index ->
                    BaseFeedViewModel.FeedDisplayItem(
                        title = seedTitle(index + 1),
                        summary = "用于 OnlineHistoryScreen 仪器测试的固定摘要 ${index + 1}",
                        details = "固定详情 ${index + 1}",
                        feed = null,
                        authorName = "作者 ${index + 1}",
                    )
                },
            )
        }
        composeRule.waitForIdle()
    }

    private fun clearDisplayItems() {
        composeRule.activity.runOnUiThread {
            onlineHistoryViewModel.displayItems.clear()
        }
        composeRule.waitForIdle()
    }

    private companion object {
        const val LIST_TAG = "online_history_list"

        fun seedTitle(index: Int) = "固定在线历史条目 $index"
    }
}
