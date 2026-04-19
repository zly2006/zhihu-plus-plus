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
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.core.content.edit
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.Search
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.RecordingNavigator
import com.github.zly2006.zhihu.test.performVerticalSwipeCycle
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.FOLLOW_DYNAMIC_LIST_TAG
import com.github.zly2006.zhihu.ui.FOLLOW_DYNAMIC_REFRESH_BUTTON_TAG
import com.github.zly2006.zhihu.ui.FOLLOW_RECOMMEND_LIST_TAG
import com.github.zly2006.zhihu.ui.FOLLOW_RECOMMEND_REFRESH_BUTTON_TAG
import com.github.zly2006.zhihu.ui.FOLLOW_SCREEN_PAGER_TAG
import com.github.zly2006.zhihu.ui.FollowScreen
import com.github.zly2006.zhihu.ui.FollowScreenData
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.followDynamicItemTag
import com.github.zly2006.zhihu.ui.followRecommendItemTag
import com.github.zly2006.zhihu.ui.followScreenTabTag
import com.github.zly2006.zhihu.ui.followingUserItemTag
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.FollowRecommendViewModel
import com.github.zly2006.zhihu.viewmodel.feed.FollowViewModel
import com.github.zly2006.zhihu.viewmodel.feed.RecentMomentsViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FollowScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    private val recommendViewModel: FollowRecommendViewModel
        get() = ViewModelProvider(composeRule.activity)[FollowRecommendViewModel::class.java]

    private val dynamicViewModel: FollowViewModel
        get() = ViewModelProvider(composeRule.activity)[FollowViewModel::class.java]

    private val recentMomentsViewModel: RecentMomentsViewModel
        get() = ViewModelProvider(composeRule.activity)[RecentMomentsViewModel::class.java]

    private val followScreenData: FollowScreenData
        get() = ViewModelProvider(composeRule.activity)[FollowScreenData::class.java]

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
        clearFollowScreenState()
    }

    @After
    fun tearDown() {
        composeRule.resetAppPreferences()
        clearFollowScreenState()
    }

    @Test
    fun tabsSwitchByClickAndPagerSwipes_withoutLosingDeterministicSeededRows() {
        /*
         * Expected behavior:
         * 1. FollowScreen must boot into the "推荐" tab from a fully local seed so the test never
         *    waits for real follow-feed responses.
         * 2. Tapping the "动态" tab should update the selected state and reveal the seeded dynamic rows.
         * 3. Pager swipes must move back and forth between the two tabs while preserving the selected
         *    bottom-page state and the deterministic offline row content on each side.
         * 4. Tab switching alone must not emit any navigator events because no row was clicked yet.
         */
        val navigator = setFollowScreen(
            showRefreshFab = false,
            recommendItems = seededRecommendItems(count = 8),
            dynamicItems = seededDynamicItems(count = 8),
            recentUsers = emptyList(),
        )

        composeRule.waitUntilTagSelected(followScreenTabTag(0))
        composeRule.onNodeWithTag(followScreenTabTag(0)).assertIsSelected()
        composeRule.onNodeWithTag(followScreenTabTag(1)).assertIsNotSelected()
        composeRule.onNodeWithTag(followRecommendItemTag("recommend-item-1")).assertIsDisplayed()

        composeRule.onNodeWithTag(followScreenTabTag(1)).performClick()

        composeRule.waitUntilTagSelected(followScreenTabTag(1))
        composeRule.onNodeWithTag(followScreenTabTag(1)).assertIsSelected()
        composeRule.onNodeWithTag(followScreenTabTag(0)).assertIsNotSelected()
        composeRule.onNodeWithTag(followDynamicItemTag("dynamic-item-1")).assertIsDisplayed()

        composeRule.onNodeWithTag(FOLLOW_SCREEN_PAGER_TAG).performTouchInput { swipeRight() }

        composeRule.waitUntilTagSelected(followScreenTabTag(0))
        composeRule.onNodeWithTag(followRecommendItemTag("recommend-item-1")).assertIsDisplayed()

        composeRule.onNodeWithTag(FOLLOW_SCREEN_PAGER_TAG).performTouchInput { swipeLeft() }

        composeRule.waitUntilTagSelected(followScreenTabTag(1))
        composeRule.onNodeWithTag(followDynamicItemTag("dynamic-item-1")).assertIsDisplayed()
        assertTrue(navigator.destinations.isEmpty())
        assertEquals(0, navigator.backCount)
    }

    @Test
    fun recommendTab_exposesRefreshAndClickableRows_withDeterministicNavigation() {
        /*
         * Expected behavior:
         * 1. The recommendation page should render the following-users strip and the refresh FAB from
         *    seeded local state without triggering the real refresh path.
         * 2. Clicking the refresh affordance must route through the injected test seam so the test can
         *    prove stable interaction without clearing the seeded list.
         * 3. A following-user chip must navigate to Person(..., jumpTo = "动态") with the seeded identity.
         * 4. A seeded recommendation feed row must remain clickable and navigate to its deterministic
         *    offline destination exactly once.
         */
        var recommendRefreshClicks = 0
        val navigator = setFollowScreen(
            showRefreshFab = true,
            recommendItems = seededRecommendItems(count = 6),
            dynamicItems = seededDynamicItems(count = 4),
            recentUsers = seededRecentUsers(count = 2),
            onTestRecommendRefreshClick = { recommendRefreshClicks++ },
        )

        composeRule.onNodeWithTag(FOLLOW_RECOMMEND_REFRESH_BUTTON_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(followingUserItemTag("follow-user-1")).assertIsDisplayed()
        composeRule.onNodeWithTag(followRecommendItemTag("recommend-item-1")).assertIsDisplayed()

        composeRule.onNodeWithTag(FOLLOW_RECOMMEND_REFRESH_BUTTON_TAG).performClick()
        composeRule.onNodeWithTag(FOLLOW_RECOMMEND_REFRESH_BUTTON_TAG).performClick()
        composeRule.onNodeWithTag(followingUserItemTag("follow-user-1")).performClick()
        composeRule.onNodeWithTag(followRecommendItemTag("recommend-item-1")).performClick()

        assertEquals(2, recommendRefreshClicks)
        assertEquals(
            listOf(
                Person(
                    id = "follow-user-1",
                    urlToken = "follow-user-token-1",
                    name = "关注用户 1",
                    jumpTo = "动态",
                ),
                Search(query = "follow-recommend-1"),
            ),
            navigator.destinations,
        )
        assertEquals(0, navigator.backCount)
    }

    @Test
    fun recommendAndDynamicLists_scrollSwipeLoadMoreAndDynamicRowClickStayOffline() {
        /*
         * Expected behavior:
         * 1. Both follow lists must remain scrollable with large deterministic seeds, including the
         *    recommendation page's top user strip and the dynamic page's own list content.
         * 2. Scrolling near the end of either list should trigger the injected load-more seam instead of
         *    starting the real pagination flow, which keeps the test deterministic.
         * 3. Vertical swipe cycles on the visible list must not corrupt the currently visible seeded row.
         * 4. Clicking a dynamic row after deep scrolling must still navigate to the seeded destination.
         */
        var recommendLoadMoreCount = 0
        var dynamicLoadMoreCount = 0
        val navigator = setFollowScreen(
            showRefreshFab = true,
            recommendItems = seededRecommendItems(count = 28),
            dynamicItems = seededDynamicItems(count = 28),
            recentUsers = seededRecentUsers(count = 1),
            onTestRecommendLoadMore = { recommendLoadMoreCount++ },
            onTestDynamicLoadMore = { dynamicLoadMoreCount++ },
        )

        composeRule.onNodeWithTag(FOLLOW_RECOMMEND_LIST_TAG).assertIsDisplayed()
        composeRule
            .onNodeWithTag(FOLLOW_RECOMMEND_LIST_TAG)
            .performScrollToNode(hasTestTag(followRecommendItemTag("recommend-item-24")))
        composeRule.onNodeWithTag(followRecommendItemTag("recommend-item-24")).assertIsDisplayed()
        composeRule.onNodeWithTag(FOLLOW_RECOMMEND_LIST_TAG).performVerticalSwipeCycle()
        composeRule
            .onNodeWithTag(FOLLOW_RECOMMEND_LIST_TAG)
            .performScrollToNode(hasTestTag(followRecommendItemTag("recommend-item-24")))
        composeRule.onNodeWithTag(followRecommendItemTag("recommend-item-24")).assertIsDisplayed()

        composeRule.onNodeWithTag(followScreenTabTag(1)).performClick()
        composeRule.waitUntilTagSelected(followScreenTabTag(1))
        composeRule.onNodeWithTag(FOLLOW_DYNAMIC_REFRESH_BUTTON_TAG).assertIsDisplayed()
        composeRule
            .onNodeWithTag(FOLLOW_DYNAMIC_LIST_TAG)
            .performScrollToNode(hasTestTag(followDynamicItemTag("dynamic-item-24")))
        composeRule.onNodeWithTag(followDynamicItemTag("dynamic-item-24")).assertIsDisplayed()
        composeRule.onNodeWithTag(FOLLOW_DYNAMIC_LIST_TAG).performVerticalSwipeCycle()
        composeRule
            .onNodeWithTag(FOLLOW_DYNAMIC_LIST_TAG)
            .performScrollToNode(hasTestTag(followDynamicItemTag("dynamic-item-24")))
        composeRule.onNodeWithTag(followDynamicItemTag("dynamic-item-24")).assertIsDisplayed()

        composeRule
            .onNodeWithTag(FOLLOW_DYNAMIC_LIST_TAG)
            .performScrollToNode(hasTestTag(followDynamicItemTag("dynamic-item-3")))
        composeRule.onNodeWithTag(followDynamicItemTag("dynamic-item-3")).performClick()

        composeRule.runOnIdle {
            assertTrue(recommendLoadMoreCount > 0)
            assertTrue(dynamicLoadMoreCount > 0)
        }
        assertEquals(listOf(Search(query = "follow-dynamic-3")), navigator.destinations)
    }

    private fun setFollowScreen(
        showRefreshFab: Boolean,
        recommendItems: List<BaseFeedViewModel.FeedDisplayItem>,
        dynamicItems: List<BaseFeedViewModel.FeedDisplayItem>,
        recentUsers: List<RecentMomentsViewModel.FollowingUserItem>,
        onTestRecommendRefreshClick: (() -> Unit)? = null,
        onTestRecommendLoadMore: (() -> Unit)? = null,
        onTestDynamicRefreshClick: (() -> Unit)? = null,
        onTestDynamicLoadMore: (() -> Unit)? = null,
    ): RecordingNavigator {
        setShowRefreshFabPreference(showRefreshFab)
        seedFollowScreenState(
            recommendItems = recommendItems,
            dynamicItems = dynamicItems,
            recentUsers = recentUsers,
        )
        return composeRule.setScreenContent {
            FollowScreen(
                innerPadding = PaddingValues(),
                onTestRecommendRefreshClick = onTestRecommendRefreshClick,
                onTestRecommendLoadMore = onTestRecommendLoadMore,
                onTestDynamicRefreshClick = onTestDynamicRefreshClick,
                onTestDynamicLoadMore = onTestDynamicLoadMore,
            )
        }
    }

    private fun clearFollowScreenState() {
        composeRule.activity.runOnUiThread {
            recommendViewModel.allData.clear()
            recommendViewModel.debugData.clear()
            recommendViewModel.displayItems.clear()

            dynamicViewModel.allData.clear()
            dynamicViewModel.debugData.clear()
            dynamicViewModel.displayItems.clear()

            recentMomentsViewModel.users.clear()
            recentMomentsViewModel.isLoading = false
            recentMomentsViewModel.errorMessage = null

            followScreenData.selectedTabIndex = 0
        }
        composeRule.waitForIdle()
    }

    private fun seedFollowScreenState(
        recommendItems: List<BaseFeedViewModel.FeedDisplayItem>,
        dynamicItems: List<BaseFeedViewModel.FeedDisplayItem>,
        recentUsers: List<RecentMomentsViewModel.FollowingUserItem>,
    ) {
        composeRule.activity.runOnUiThread {
            recommendViewModel.displayItems.clear()
            recommendViewModel.addDisplayItems(recommendItems)

            dynamicViewModel.displayItems.clear()
            dynamicViewModel.addDisplayItems(dynamicItems)

            recentMomentsViewModel.users.clear()
            recentMomentsViewModel.users.addAll(recentUsers)
            recentMomentsViewModel.isLoading = false
            recentMomentsViewModel.errorMessage = null

            followScreenData.selectedTabIndex = 0
        }
        composeRule.waitForIdle()
    }

    private fun setShowRefreshFabPreference(enabled: Boolean) {
        composeRule.activity.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).edit(commit = true) {
            putBoolean("showRefreshFab", enabled)
            putBoolean("loginForRecommendation", false)
        }
        composeRule.waitForIdle()
    }

    private fun MainActivityComposeRule.waitUntilTagSelected(tag: String) {
        waitUntil(timeoutMillis = 5_000) {
            this.onAllNodes(hasTestTag(tag).and(isSelectedMatcher())).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun isSelectedMatcher(): SemanticsMatcher = SemanticsMatcher.expectValue(
        SemanticsProperties.Selected,
        true,
    )

    private fun seededRecommendItems(count: Int): List<BaseFeedViewModel.FeedDisplayItem> = List(count) { index ->
        val itemId = index + 1
        BaseFeedViewModel.FeedDisplayItem(
            title = "推荐离线条目 ${itemId.toString().padStart(2, '0')}",
            summary = "这是第 $itemId 条 Follow 推荐页离线摘要。",
            details = "离线推荐详情 $itemId",
            feed = null,
            navDestination = Search(query = "follow-recommend-$itemId"),
            localFeedId = "recommend-item-$itemId",
        )
    }

    private fun seededDynamicItems(count: Int): List<BaseFeedViewModel.FeedDisplayItem> = List(count) { index ->
        val itemId = index + 1
        BaseFeedViewModel.FeedDisplayItem(
            title = "动态离线条目 ${itemId.toString().padStart(2, '0')}",
            summary = "这是第 $itemId 条 Follow 动态页离线摘要。",
            details = "离线动态详情 $itemId",
            feed = null,
            navDestination = Search(query = "follow-dynamic-$itemId"),
            localFeedId = "dynamic-item-$itemId",
        )
    }

    private fun seededRecentUsers(count: Int): List<RecentMomentsViewModel.FollowingUserItem> = List(count) { index ->
        val itemId = index + 1
        RecentMomentsViewModel.FollowingUserItem(
            actor = RecentMomentsViewModel.Actor(
                id = "follow-user-$itemId",
                urlToken = "follow-user-token-$itemId",
                name = "关注用户 $itemId",
                avatarUrl = "",
            ),
            unreadCount = itemId,
        )
    }
}
