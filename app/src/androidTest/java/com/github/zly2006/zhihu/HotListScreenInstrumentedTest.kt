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

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.RecordingNavigator
import com.github.zly2006.zhihu.test.performHorizontalSwipeCycle
import com.github.zly2006.zhihu.test.performVerticalSwipeCycle
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.HOT_LIST_LIST_TAG
import com.github.zly2006.zhihu.ui.HOT_LIST_REFRESH_BUTTON_TAG
import com.github.zly2006.zhihu.ui.HotListScreen
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HotListViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class HotListScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule()

    private val hotListViewModel: HotListViewModel
        get() = ViewModelProvider(composeRule.activity)[HotListViewModel::class.java]

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
        clearDisplayItems()
    }

    @After
    fun tearDown() {
        composeRule.resetAppPreferences()
        clearDisplayItems()
    }

    @Test
    fun seededRowsRenderAndScrollOfflineWithoutUnexpectedNavigationOrPagination() {
        // Expected behavior:
        // 1. The screen must render from the activity-scoped HotListViewModel displayItems so the
        //    test is deterministic and does not depend on a live hot-list response.
        // 2. A mid-list programmatic scroll plus manual swipe cycles must keep seeded rows visible
        //    without accidentally entering the real pagination path.
        // 3. Because these interactions are non-navigational, they must not emit navigator events.
        val loadMoreCalls = AtomicInteger(0)
        val navigator = setHotListScreen(
            itemCount = 18,
            onTestLoadMore = { loadMoreCalls.incrementAndGet() },
        )

        composeRule.onNodeWithTag(HOT_LIST_LIST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(seedTitle(1)).assertIsDisplayed()

        composeRule.onNodeWithTag(HOT_LIST_LIST_TAG).performScrollToIndex(8)
        composeRule.onNodeWithText(seedTitle(9)).assertIsDisplayed()

        composeRule.onNodeWithTag(HOT_LIST_LIST_TAG).performVerticalSwipeCycle()
        composeRule.onNodeWithTag(HOT_LIST_LIST_TAG).performHorizontalSwipeCycle()
        composeRule.onNodeWithText(seedTitle(9)).assertIsDisplayed()

        composeRule.runOnIdle {
            assertEquals(0, loadMoreCalls.get())
            assertEquals(0, navigator.destinations.size)
            assertEquals(0, navigator.backCount)
        }
    }

    @Test
    fun refreshFabVisibilityAndClicksStayStableOffline() {
        // Expected behavior:
        // 1. The refresh FAB is visible by default on top of a locally seeded list.
        // 2. Clicking it must route through the injected test callback so the test can prove click
        //    stability without invoking the real network-backed refresh implementation.
        // 3. Repeated clicks must preserve the seeded rows and never create navigation side effects.
        val refreshClicks = AtomicInteger(0)
        val navigator = setHotListScreen(
            onTestRefreshClick = { refreshClicks.incrementAndGet() },
        )

        composeRule.onNodeWithTag(HOT_LIST_REFRESH_BUTTON_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(HOT_LIST_REFRESH_BUTTON_TAG).performClick()
        composeRule.onNodeWithTag(HOT_LIST_REFRESH_BUTTON_TAG).performClick()
        composeRule.onNodeWithText(seedTitle(1)).assertIsDisplayed()

        composeRule.runOnIdle {
            assertEquals(2, refreshClicks.get())
            assertEquals(0, navigator.destinations.size)
            assertEquals(0, navigator.backCount)
            assertEquals(12, hotListViewModel.displayItems.size)
        }
    }

    @Test
    fun hotListRowsRemainAvatarFreeAndFabCanBeHiddenForDeterministicTesting() {
        // Expected behavior:
        // 1. HotListViewModel removes author/avatar data, so seeded rows should never expose avatar
        //    semantics even after the list is composed.
        // 2. Disabling the preference before composition must remove the FAB entirely while keeping
        //    the underlying list content stable and usable.
        // 3. This visibility change is purely presentational and must not generate navigation events.
        setShowRefreshFabPreference(false)
        val navigator = setHotListScreen(itemCount = 10)

        composeRule.onNodeWithTag(HOT_LIST_LIST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(seedTitle(1)).assertIsDisplayed()
        composeRule.onAllNodesWithTag(HOT_LIST_REFRESH_BUTTON_TAG).assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription("Avatar").assertCountEquals(0)

        composeRule.runOnIdle {
            assertEquals(0, navigator.destinations.size)
            assertEquals(0, navigator.backCount)
            assertEquals(10, hotListViewModel.displayItems.size)
        }
    }

    private fun setHotListScreen(
        itemCount: Int = 12,
        onTestRefreshClick: (() -> Unit)? = null,
        onTestLoadMore: (() -> Unit)? = null,
    ): RecordingNavigator {
        seedDisplayItems(itemCount)
        return composeRule.setScreenContent {
            HotListScreen(
                innerPadding = PaddingValues(),
                onTestRefreshClick = onTestRefreshClick,
                onTestLoadMore = onTestLoadMore,
            )
        }
    }

    private fun seedDisplayItems(itemCount: Int) {
        composeRule.activity.runOnUiThread {
            hotListViewModel.displayItems.clear()
            hotListViewModel.displayItems.addAll(
                List(itemCount) { index ->
                    BaseFeedViewModel.FeedDisplayItem(
                        title = seedTitle(index + 1),
                        summary = "用于 HotListScreen 仪器测试的固定摘要 ${index + 1}",
                        details = "固定热榜详情 ${index + 1}",
                        feed = null,
                        authorName = null,
                        avatarSrc = null,
                    )
                },
            )
        }
        composeRule.waitForIdle()
    }

    private fun clearDisplayItems() {
        composeRule.activity.runOnUiThread {
            hotListViewModel.displayItems.clear()
        }
        composeRule.waitForIdle()
    }

    private fun setShowRefreshFabPreference(enabled: Boolean) {
        composeRule.activity
            .getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("showRefreshFab", enabled)
            .commit()
        composeRule.waitForIdle()
    }

    private companion object {
        fun seedTitle(index: Int) = "固定热榜条目 $index"
    }
}
