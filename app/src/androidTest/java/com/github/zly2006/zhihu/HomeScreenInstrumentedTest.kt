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
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.core.content.edit
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.RecommendationMode
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.Search
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.RecordingNavigator
import com.github.zly2006.zhihu.test.performVerticalSwipeCycle
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.HOME_ACCOUNT_BUTTON_TAG
import com.github.zly2006.zhihu.ui.HOME_FEED_LIST_TAG
import com.github.zly2006.zhihu.ui.HOME_NOTIFICATION_BUTTON_TAG
import com.github.zly2006.zhihu.ui.HOME_REFRESH_BUTTON_TAG
import com.github.zly2006.zhihu.ui.HOME_SEARCH_BUTTON_TAG
import com.github.zly2006.zhihu.ui.HOME_TOP_ACTIONS_TAG
import com.github.zly2006.zhihu.ui.HomeScreen
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.QQ_GROUP_DISMISSED_PREFERENCE_KEY
import com.github.zly2006.zhihu.updater.UpdateManager
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedViewModel
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
        composeRule.activity.runOnUiThread {
            AccountData.delete(composeRule.activity)
            UpdateManager.updateState.value = UpdateManager.UpdateState.NoUpdate
            clearHomeFeedViewModel()
        }
        composeRule.waitForIdle()
    }

    @Test
    fun classicTopButtons_showSearchAndNotification_andHideAccount() {
        /*
         * Expected behavior:
         * 1. With duo3 account mode disabled, the classic toolbar should show the search surface and
         *    the notification action, but it must not render the duo3-only account button.
         * 2. Clicking search should navigate to an empty Search destination without relying on live data.
         * 3. Clicking notification should append exactly one Notification destination and must not emit
         *    any back navigation event.
         */
        val recordingNavigator = composeRule.launchHomeScreen(
            duo3HomeAccount = false,
            showRefreshFab = false,
            displayItems = homeFeedFixtureItems(),
        )

        composeRule.onNodeWithTag(HOME_TOP_ACTIONS_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(HOME_SEARCH_BUTTON_TAG).assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithTag(HOME_NOTIFICATION_BUTTON_TAG).assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithTag(HOME_ACCOUNT_BUTTON_TAG).assertDoesNotExist()

        composeRule.onNodeWithTag(HOME_SEARCH_BUTTON_TAG).performClick()
        composeRule.onNodeWithTag(HOME_NOTIFICATION_BUTTON_TAG).performClick()

        assertEquals(listOf(Search(query = ""), Notification), recordingNavigator.destinations)
        assertEquals(0, recordingNavigator.backCount)
    }

    @Test
    fun duo3TopButtons_showSearchAndAccountSheet_andHideNotification() {
        /*
         * Expected behavior:
         * 1. With duo3 account mode enabled, the top action area should switch to the account-entry layout,
         *    so the account button is visible and the classic notification icon is absent.
         * 2. The search affordance should still navigate to Search(query = "") exactly once.
         * 3. Clicking the account button in a forced logged-out state should open the offline account sheet,
         *    which is verified by the stable "登录知乎" entry instead of any network-backed profile UI.
         */
        val recordingNavigator = composeRule.launchHomeScreen(
            duo3HomeAccount = true,
            showRefreshFab = false,
            displayItems = homeFeedFixtureItems(),
        )

        composeRule.onNodeWithTag(HOME_TOP_ACTIONS_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(HOME_SEARCH_BUTTON_TAG).assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithTag(HOME_ACCOUNT_BUTTON_TAG).assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithTag(HOME_NOTIFICATION_BUTTON_TAG).assertDoesNotExist()

        composeRule.onNodeWithTag(HOME_SEARCH_BUTTON_TAG).performClick()
        composeRule.onNodeWithTag(HOME_ACCOUNT_BUTTON_TAG).performClick()

        composeRule.onNodeWithText("登录知乎").assertIsDisplayed()
        assertEquals(listOf(Search(query = "")), recordingNavigator.destinations)
        assertEquals(0, recordingNavigator.backCount)
    }

    @Test
    fun refreshButton_staysPresentAfterClick_inSeededOfflineState() {
        /*
         * Expected behavior:
         * 1. Enabling the floating refresh action through SharedPreferences must render a dedicated refresh FAB.
         * 2. The test seeds non-empty feed items first so HomeScreen skips its initial auto-refresh path.
         * 3. Tapping the refresh FAB should remain stable from the test perspective: the button stays present
         *    and the interaction does not create any navigation side effects.
         */
        val recordingNavigator = composeRule.launchHomeScreen(
            duo3HomeAccount = false,
            showRefreshFab = true,
            displayItems = homeFeedFixtureItems(),
        )

        composeRule.onNodeWithTag(HOME_REFRESH_BUTTON_TAG).assertIsDisplayed().assertHasClickAction()

        composeRule.onNodeWithTag(HOME_REFRESH_BUTTON_TAG).performClick()

        composeRule.onNodeWithTag(HOME_REFRESH_BUTTON_TAG).assertExists()
        assertEquals(0, recordingNavigator.destinations.size)
        assertEquals(0, recordingNavigator.backCount)
    }

    @Test
    fun seededOfflineList_scrollsToFarItemsAndBack_stably() {
        /*
         * Expected behavior:
         * 1. A fixed list of locally seeded display items should render immediately without waiting for feeds.
         * 2. The LazyColumn must scroll to a far-away injected item by text matcher, proving the list stays
         *    addressable even when HomeScreen wraps it with pull-to-refresh and announcement content.
         * 3. A swipe cycle and a return scroll back to the first injected item should keep the deterministic
         *    offline content intact instead of dropping or corrupting the seeded rows.
         */
        composeRule.launchHomeScreen(
            duo3HomeAccount = false,
            showRefreshFab = false,
            displayItems = homeFeedFixtureItems(count = 30),
        )

        composeRule.onNodeWithTag(HOME_FEED_LIST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("离线条目 00").assertIsDisplayed()

        composeRule.onNodeWithTag(HOME_FEED_LIST_TAG).performScrollToNode(hasText("离线条目 24"))
        composeRule.onNodeWithText("离线条目 24").assertIsDisplayed()

        composeRule.onNodeWithTag(HOME_FEED_LIST_TAG).performVerticalSwipeCycle()
        composeRule.onNodeWithTag(HOME_FEED_LIST_TAG).performScrollToNode(hasText("离线条目 00"))
        composeRule.onNodeWithText("离线条目 00").assertIsDisplayed()
    }

    private fun MainActivityComposeRule.launchHomeScreen(
        duo3HomeAccount: Boolean,
        showRefreshFab: Boolean,
        displayItems: List<BaseFeedViewModel.FeedDisplayItem>,
    ): RecordingNavigator {
        activity.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).edit(commit = true) {
            putBoolean("duo3_home_account", duo3HomeAccount)
            putBoolean("showRefreshFab", showRefreshFab)
            putBoolean("loginForRecommendation", false)
            putBoolean("filterExplainDialogShown", true)
            putBoolean(QQ_GROUP_DISMISSED_PREFERENCE_KEY, true)
            putBoolean("survey_feedback_done", true)
            putBoolean("autoCheckUpdates", false)
            putString("recommendationMode", RecommendationMode.WEB.key)
        }
        activity.runOnUiThread {
            AccountData.delete(activity)
            UpdateManager.updateState.value = UpdateManager.UpdateState.NoUpdate
            clearHomeFeedViewModel()
            seedHomeFeedViewModel(displayItems)
        }
        waitForIdle()

        val recordingNavigator = setScreenContent {
            HomeScreen(innerPadding = PaddingValues())
        }
        activity.runOnUiThread {
            UpdateManager.updateState.value = UpdateManager.UpdateState.NoUpdate
        }
        waitForIdle()
        return recordingNavigator
    }

    private fun clearHomeFeedViewModel() {
        val viewModel = ViewModelProvider(composeRule.activity)[HomeFeedViewModel::class.java]
        viewModel.allData.clear()
        viewModel.debugData.clear()
        viewModel.displayItems.clear()
    }

    private fun seedHomeFeedViewModel(items: List<BaseFeedViewModel.FeedDisplayItem>) {
        val viewModel = ViewModelProvider(composeRule.activity)[HomeFeedViewModel::class.java]
        viewModel.addDisplayItems(items)
    }

    private fun homeFeedFixtureItems(count: Int = 8): List<BaseFeedViewModel.FeedDisplayItem> = List(count) { index ->
        BaseFeedViewModel.FeedDisplayItem(
            title = "离线条目 ${index.toString().padStart(2, '0')}",
            summary = "这是第 ${index + 1} 条用于 HomeScreen instrumented test 的离线摘要。",
            details = "离线验证 · 固定假数据",
            feed = null,
            navDestination = Search(query = "fixture-$index"),
        )
    }
}
