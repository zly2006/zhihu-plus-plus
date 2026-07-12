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

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.Daily
import com.github.zly2006.zhihu.navigation.Follow
import com.github.zly2006.zhihu.navigation.Home
import com.github.zly2006.zhihu.navigation.MainTabs
import com.github.zly2006.zhihu.navigation.OnlineHistory
import com.github.zly2006.zhihu.shared.filter.ContentOpenFrom
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setZhihuMainContent
import com.github.zly2006.zhihu.ui.FOLLOW_SCREEN_PAGER_TAG
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.subscreens.BOTTOM_BAR_ITEMS_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.START_DESTINATION_PREFERENCE_KEY
import org.junit.Assert.assertEquals
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
    fun followInnerPager_swipesBetweenTabsWithoutLosingBottomTabSelection() {
        // Follow is now a single main-tab page that owns its own inner pager. Swiping inside the
        // follow pager must switch between "推荐" and "动态" while keeping the bottom-bar Follow
        // item selected, and leaving Follow should still require switching the main tab itself.
        composeRule.launchZhihuMain(startDestination = Home.name)
        composeRule.onNodeWithTag("nav_tab_follow").performClick()

        composeRule.waitUntilTabSelected("nav_tab_follow")
        composeRule.waitUntilTabSelected("follow_screen_tab_0")
        composeRule.onNodeWithTag("follow_screen_tab_0").assertIsSelected()
        composeRule.onNodeWithTag("follow_screen_tab_1").assertIsNotSelected()
        composeRule.onNodeWithText("推荐").assertExists().assertIsDisplayed()
        composeRule.onNodeWithText("动态").assertExists().assertIsDisplayed()

        composeRule.onNodeWithTag(FOLLOW_SCREEN_PAGER_TAG).performTouchInput { swipeLeft() }

        composeRule.waitUntilTabSelected("follow_screen_tab_1")
        composeRule.onNodeWithTag("follow_screen_tab_1").assertIsSelected()
        composeRule.onNodeWithTag("follow_screen_tab_0").assertIsNotSelected()
        composeRule.onNodeWithTag("nav_tab_follow").assertIsSelected()

        composeRule.onNodeWithTag("nav_tab_daily").performClick()
        composeRule.onNodeWithTag("nav_tab_daily").assertIsSelected()
        composeRule.onNodeWithTag("nav_tab_follow").assertIsNotSelected()

        composeRule.onNodeWithTag("nav_tab_follow").performClick()

        composeRule.waitUntilTabSelected("nav_tab_follow")
        composeRule.waitUntilTabSelected("follow_screen_tab_1")
        composeRule.onNodeWithTag("follow_screen_tab_1").assertIsSelected()
        composeRule.onNodeWithTag("follow_screen_tab_0").assertIsNotSelected()
        composeRule.onNodeWithTag("nav_tab_follow").assertIsSelected()

        composeRule.onNodeWithTag(FOLLOW_SCREEN_PAGER_TAG).performTouchInput { swipeRight() }

        composeRule.waitUntilTabSelected("follow_screen_tab_0")
        composeRule.onNodeWithTag("follow_screen_tab_0").assertIsSelected()
        composeRule.onNodeWithTag("follow_screen_tab_1").assertIsNotSelected()
        composeRule.onNodeWithTag("nav_tab_follow").assertIsSelected()
        composeRule.onNodeWithTag("nav_tab_home").assertIsNotSelected()
    }

    @Test
    fun startDestinationAndHiddenBottomTabsRemainCompatibleWithFlattenedPager() {
        // The flattened main pager now treats Follow as a single main tab with an internal pager.
        // Startup still opens the configured visible tab, hidden tabs stay hidden, and entering
        // Follow from the main pager lands on the Follow page while its inner pager starts from
        // the default recommendation tab.
        composeRule.launchZhihuMain(
            startDestination = Daily.name,
            bottomBarItems = linkedSetOf(Follow.name, Daily.name, Account.name),
        )

        composeRule.waitUntilTabSelected("nav_tab_daily")
        composeRule.onNodeWithTag("nav_tab_daily").assertIsSelected()
        composeRule.onNodeWithTag("nav_tab_follow").assertIsNotSelected()
        composeRule.onNodeWithTag("nav_tab_account").assertIsNotSelected()
        composeRule.onNodeWithTag("nav_tab_home").assertDoesNotExist()
        composeRule.onNodeWithTag("nav_tab_hotlist").assertDoesNotExist()
        composeRule.onNodeWithTag("nav_tab_onlinehistory").assertDoesNotExist()

        composeRule.onRoot().performTouchInput { swipeRight() }

        composeRule.waitUntilTabSelected("follow_screen_tab_0")
        composeRule.onNodeWithTag("nav_tab_follow").assertIsSelected()
        composeRule.onNodeWithTag("follow_screen_tab_0").assertIsSelected()
        composeRule.onNodeWithTag("follow_screen_tab_1").assertIsNotSelected()

        composeRule.onNodeWithTag(FOLLOW_SCREEN_PAGER_TAG).performTouchInput { swipeLeft() }

        composeRule.waitUntilTabSelected("follow_screen_tab_1")
        composeRule.onNodeWithTag("nav_tab_follow").assertIsSelected()
        composeRule.onNodeWithTag("follow_screen_tab_1").assertIsSelected()

        composeRule.activity.runOnUiThread {
            composeRule.activity.navigate(MainTabs, popup = true)
        }

        composeRule.waitUntilTabSelected("nav_tab_daily")
        composeRule.onNodeWithTag("nav_tab_daily").assertIsSelected()
        composeRule.onNodeWithTag("nav_tab_home").assertDoesNotExist()

        composeRule.activity.runOnUiThread {
            composeRule.activity.navigateMainTab(Follow)
        }

        composeRule.waitUntilTabSelected("nav_tab_follow")
        composeRule.onNodeWithTag("nav_tab_follow").assertIsSelected()
        composeRule.waitUntilTabSelected("follow_screen_tab_1")
    }

    @Test
    fun homeTabOpenContent_recordsHomeFeedOpenFrom() {
        composeRule.launchZhihuMain(startDestination = Home.name)

        composeRule.waitUntilTabSelected("nav_tab_home")

        val article = Article(type = ArticleType.Answer, id = 318L)
        var openFrom: String? = null
        composeRule.runOnIdle {
            composeRule.activity.navigate(article)
            openFrom = composeRule.activity.consumePendingContentOpenFrom(article)
        }

        assertEquals(ContentOpenFrom.HOME_FEED, openFrom)
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

    private fun isSelectedMatcher(): SemanticsMatcher = SemanticsMatcher.expectValue(
        SemanticsProperties.Selected,
        true,
    )
}
