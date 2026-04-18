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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.data.RecommendationMode
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.subscreens.ContentFilterSettingsScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContentFilterSettingsScreenInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun preferenceBackedSelectionsPersistAcrossScreenRecreation() {
        // This screen stores its state in SharedPreferences, so every user-facing selection must update
        // persistent storage immediately instead of only mutating transient Compose state. After the
        // screen is recreated, the recommendation mode and switch states should still reflect the last
        // saved values.
        composeRule.resetAppPreferences()

        composeRule.setScreenContent {
            ContentFilterSettingsScreen(innerPadding = PaddingValues())
        }

        composeRule
            .onNodeWithTag(RECOMMENDATION_MODE_FIELD_TAG, useUnmergedTree = true)
            .assertTextContains(RecommendationMode.MIXED.displayName)
        toggleFor(LOGIN_FOR_RECOMMENDATION_TAG).assertIsOn()
        toggleFor(FILTER_FOLLOWED_USER_CONTENT_TAG).assertIsOff()

        composeRule.onNodeWithTag(RECOMMENDATION_MODE_FIELD_TAG, useUnmergedTree = true).performClick()
        composeRule.onNodeWithText(RecommendationMode.LOCAL.displayName).performClick()
        assertEquals(RecommendationMode.LOCAL.key, preferences().getString("recommendationMode", null))

        composeRule.onNodeWithTag(LOGIN_FOR_RECOMMENDATION_TAG).performClick()
        toggleFor(LOGIN_FOR_RECOMMENDATION_TAG).assertIsOff()
        assertEquals(false, preferences().getBoolean("loginForRecommendation", true))

        composeRule.onNodeWithTag(ENABLE_CONTENT_FILTER_TAG).performClick()
        toggleFor(ENABLE_CONTENT_FILTER_TAG).assertIsOff()
        assertEquals(false, preferences().getBoolean("enableContentFilter", true))
        composeRule.onNodeWithTag(FILTER_FOLLOWED_USER_CONTENT_TAG).assertIsNotEnabled()

        composeRule.onNodeWithTag(ENABLE_CONTENT_FILTER_TAG).performClick()
        toggleFor(ENABLE_CONTENT_FILTER_TAG).assertIsOn()
        assertEquals(true, preferences().getBoolean("enableContentFilter", false))
        composeRule.onNodeWithTag(FILTER_FOLLOWED_USER_CONTENT_TAG).assertIsEnabled()

        composeRule.onNodeWithTag(FILTER_FOLLOWED_USER_CONTENT_TAG).performClick()
        toggleFor(FILTER_FOLLOWED_USER_CONTENT_TAG).assertIsOn()
        assertEquals(true, preferences().getBoolean("filterFollowedUserContent", false))

        composeRule.setScreenContent {
            ContentFilterSettingsScreen(innerPadding = PaddingValues())
        }

        composeRule
            .onNodeWithTag(RECOMMENDATION_MODE_FIELD_TAG, useUnmergedTree = true)
            .assertTextContains(RecommendationMode.LOCAL.displayName)
        toggleFor(LOGIN_FOR_RECOMMENDATION_TAG).assertIsOff()
        toggleFor(ENABLE_CONTENT_FILTER_TAG).assertIsOn()
        toggleFor(FILTER_FOLLOWED_USER_CONTENT_TAG).assertIsOn()
    }

    @Test
    fun deterministicScrollReachesBottomActionsAndEmitsExpectedNavigationEvents() {
        // This screen uses a regular vertically scrollable Column, so off-screen actions near the bottom
        // must remain reachable through semantics-driven scrolling in tests. Scrolling to those actions,
        // clicking them, and then scrolling back upward should produce stable navigation events without
        // depending on device-specific coordinates.
        composeRule.resetAppPreferences()

        val recordingNavigator = composeRule.setScreenContent {
            ContentFilterSettingsScreen(innerPadding = PaddingValues())
        }

        scrollTo(BLOCKLIST_TAG)
        composeRule.onNodeWithTag(BLOCKLIST_TAG).assertIsDisplayed().performClick()

        scrollTo(BLOCKED_FEED_HISTORY_TAG)
        composeRule.onNodeWithTag(BLOCKED_FEED_HISTORY_TAG).assertIsDisplayed().performClick()

        scrollTo(LOGIN_FOR_RECOMMENDATION_TAG)
        composeRule.onNodeWithTag(LOGIN_FOR_RECOMMENDATION_TAG).assertIsDisplayed()

        composeRule.onNodeWithContentDescription("返回").performClick()

        assertEquals(
            listOf(
                Account.RecommendSettings.Blocklist,
                Account.RecommendSettings.BlockedFeedHistory,
            ),
            recordingNavigator.destinations,
        )
        assertEquals(1, recordingNavigator.backCount)
    }

    private fun preferences() =
        composeRule.activity.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    private fun scrollTo(tag: String) {
        composeRule.onNodeWithTag(SCROLL_TAG).performScrollToNode(hasTestTag(tag))
        composeRule.waitForIdle()
    }

    private fun toggleFor(tag: String) = composeRule.onNode(
        matcher = isToggleable() and hasAnyAncestor(hasTestTag(tag)),
        useUnmergedTree = true,
    )

    private companion object {
        const val SCROLL_TAG = "contentFilterSettings:scroll"
        const val RECOMMENDATION_MODE_FIELD_TAG = "contentFilterSettings:recommendationModeField"
        const val LOGIN_FOR_RECOMMENDATION_TAG = "contentFilterSettings:loginForRecommendation"
        const val ENABLE_CONTENT_FILTER_TAG = "contentFilterSettings:enableContentFilter"
        const val FILTER_FOLLOWED_USER_CONTENT_TAG = "contentFilterSettings:filterFollowedUserContent"
        const val BLOCKLIST_TAG = "contentFilterSettings:blocklist"
        const val BLOCKED_FEED_HISTORY_TAG = "contentFilterSettings:blockedFeedHistory"
    }
}
