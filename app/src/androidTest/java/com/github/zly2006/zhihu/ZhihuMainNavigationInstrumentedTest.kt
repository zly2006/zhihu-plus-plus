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

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Daily
import com.github.zly2006.zhihu.navigation.Follow
import com.github.zly2006.zhihu.navigation.Home
import com.github.zly2006.zhihu.navigation.OnlineHistory
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.performHorizontalSwipeCycle
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setZhihuMainContent
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.subscreens.BOTTOM_BAR_ITEMS_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.START_DESTINATION_PREFERENCE_KEY
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ZhihuMainNavigationInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val deterministicBottomBarItems = linkedSetOf(
        Home.name,
        Follow.name,
        Daily.name,
        OnlineHistory.name,
        Account.name,
    )

    @Before
    fun resetPreferences() {
        composeRule.resetAppPreferences()
    }

    @Test
    fun bottomTabs_followDeterministicPreferenceOrderAndSelectionState() {
        // This test pins the bottom-bar preferences to a known set so the shell cannot inherit
        // whatever tabs the developer last selected locally. The expected behavior is:
        // 1. only the configured tabs are rendered;
        // 2. the configured start destination is selected on launch; and
        // 3. tapping another bottom tab updates the selected state without reintroducing hidden tabs.
        composeRule.launchZhihuMain(startDestination = Home.name)

        composeRule.waitUntilTabSelected("nav_tab_home")
        composeRule
            .onNodeWithTag("nav_tab_home")
            .assertExists()
            .assertIsDisplayed()
            .assertIsSelected()
        composeRule
            .onNodeWithTag("nav_tab_follow")
            .assertExists()
            .assertIsDisplayed()
            .assertIsNotSelected()
        composeRule
            .onNodeWithTag("nav_tab_daily")
            .assertExists()
            .assertIsDisplayed()
            .assertIsNotSelected()
        composeRule
            .onNodeWithTag("nav_tab_onlinehistory")
            .assertExists()
            .assertIsDisplayed()
            .assertIsNotSelected()
        composeRule
            .onNodeWithTag("nav_tab_account")
            .assertExists()
            .assertIsDisplayed()
            .assertIsNotSelected()
        composeRule.onNodeWithTag("nav_tab_hotlist").assertDoesNotExist()

        composeRule.onNodeWithTag("nav_tab_follow").performClick()

        composeRule.waitUntilTabSelected("nav_tab_follow")
        composeRule.onNodeWithTag("nav_tab_home").assertIsNotSelected()
        composeRule.onNodeWithTag("nav_tab_follow").assertIsSelected()
        composeRule
            .onNodeWithText("推荐")
            .assertExists()
            .assertIsDisplayed()
            .assertIsSelected()
        composeRule
            .onNodeWithText("动态")
            .assertExists()
            .assertIsDisplayed()
            .assertIsNotSelected()
        composeRule.onNodeWithTag("nav_tab_hotlist").assertDoesNotExist()

        composeRule.onNodeWithTag("nav_tab_account").performClick()

        composeRule.waitUntilTabSelected("nav_tab_account")
        composeRule.onNodeWithTag("nav_tab_follow").assertIsNotSelected()
        composeRule.onNodeWithTag("nav_tab_account").assertIsSelected()
        composeRule.onNodeWithTag("nav_tab_hotlist").assertDoesNotExist()
    }

    @Test
    fun followScreen_swipesChangeInnerPageWithoutLosingBottomTabSelection() {
        // This test enters the Follow top-level destination from a deterministic start state and
        // verifies two shell-level guarantees. First, a horizontal swipe inside the Follow content
        // must advance the inner pager from "推荐" to "动态". Second, even after those swipes,
        // the bottom navigation shell must remain on the Follow tab instead of drifting back home
        // or selecting another tab because of gesture side effects.
        composeRule.launchZhihuMain(startDestination = Home.name)
        composeRule.onNodeWithTag("nav_tab_follow").performClick()

        composeRule.waitUntilTabSelected("nav_tab_follow")
        composeRule
            .onNodeWithText("推荐")
            .assertExists()
            .assertIsDisplayed()
            .assertIsSelected()
        composeRule
            .onNodeWithText("动态")
            .assertExists()
            .assertIsDisplayed()
            .assertIsNotSelected()

        composeRule.onRoot().performTouchInput { swipeLeft() }

        composeRule.waitUntilTextSelected("动态")
        composeRule.onNodeWithText("动态").assertIsSelected()
        composeRule.onNodeWithText("推荐").assertIsNotSelected()
        composeRule.onNodeWithTag("nav_tab_follow").assertIsSelected()

        composeRule.onRoot().performHorizontalSwipeCycle()

        composeRule.waitUntilTextSelected("推荐")
        composeRule.onNodeWithText("推荐").assertIsSelected()
        composeRule.onNodeWithText("动态").assertIsNotSelected()
        composeRule.onNodeWithTag("nav_tab_follow").assertIsSelected()
        composeRule.onNodeWithTag("nav_tab_home").assertIsNotSelected()
    }

    private fun MainActivityComposeRule.launchZhihuMain(
        startDestination: String,
        bottomBarItems: Set<String> = deterministicBottomBarItems,
    ) {
        activity.getSharedPreferences(PREFERENCE_NAME, android.content.Context.MODE_PRIVATE).edit(commit = true) {
            putString(START_DESTINATION_PREFERENCE_KEY, startDestination)
            putStringSet(BOTTOM_BAR_ITEMS_PREFERENCE_KEY, bottomBarItems)
            putBoolean("duo3_home_account", false)
            putBoolean("duo3_nav_style", false)
            putBoolean("bottomBarTapScrollToTop", false)
            putBoolean("autoHideBottomBar", false)
        }
        setZhihuMainContent()
    }

    private fun MainActivityComposeRule.waitUntilTabSelected(tag: String) {
        waitUntil(timeoutMillis = 5_000) {
            onAllNodes(hasTestTag(tag).and(isSelectedMatcher()))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun MainActivityComposeRule.waitUntilTextSelected(text: String) {
        waitUntil(timeoutMillis = 5_000) {
            onAllNodes(hasText(text).and(isSelectedMatcher()))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun isSelectedMatcher(): SemanticsMatcher = SemanticsMatcher.expectValue(
        SemanticsProperties.Selected,
        true,
    )
}
