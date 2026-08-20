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

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Daily
import com.github.zly2006.zhihu.navigation.Follow
import com.github.zly2006.zhihu.navigation.Home
import com.github.zly2006.zhihu.navigation.HotList
import com.github.zly2006.zhihu.navigation.OnlineHistory
import com.github.zly2006.zhihu.test.performVerticalSwipeCycle
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.subscreens.APPEARANCE_SETTINGS_BOTTOM_BAR_SECTION_KEY
import com.github.zly2006.zhihu.ui.subscreens.APPEARANCE_SETTINGS_COLLECTION_DIRECT_BROWSE_TAG
import com.github.zly2006.zhihu.ui.subscreens.APPEARANCE_SETTINGS_SCROLL_TAG
import com.github.zly2006.zhihu.ui.subscreens.AppearanceSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.BOTTOM_BAR_ITEM_ORDER_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.COLLECTION_DIRECT_BROWSE_PREFERENCE_KEY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppearanceSettingsScreenInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val preferences: SharedPreferences
        get() = composeRule.activity.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/354
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/409
     */
    @Test
    fun bottomBarRowsKeepUniformHeightAndMoveOrderPersists() {
        // The bottom-bar editor mixes selected rows, unselected rows, and the non-removable account
        // row. They should keep the same touch target height while reorder actions still persist.
        setUpScreen(setting = APPEARANCE_SETTINGS_BOTTOM_BAR_SECTION_KEY)

        scrollUntilTagDisplayed("appearanceSettings:bottomBar:item:${HotList.name}")
        val selectedHeight = boundsHeightForTag("appearanceSettings:bottomBar:item:${Daily.name}")
        val unselectedHeight = boundsHeightForTag("appearanceSettings:bottomBar:item:${HotList.name}")
        val lockedHeight = boundsHeightForTag("appearanceSettings:bottomBar:item:${Account.name}")

        assertEquals(selectedHeight.toDouble(), unselectedHeight.toDouble(), 0.5)
        assertEquals(selectedHeight.toDouble(), lockedHeight.toDouble(), 0.5)

        composeRule.onNodeWithTag("appearanceSettings:bottomBar:moveDown:${Daily.name}").performClick()
        waitUntilStringPreference(
            BOTTOM_BAR_ITEM_ORDER_PREFERENCE_KEY,
            expected = listOf(Home.name, Follow.name, OnlineHistory.name, Daily.name, Account.name).joinToString(","),
        )
    }

    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/609
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/611
     */
    @Test
    fun collectionDirectBrowseIsOptInAndClearlyMarkedAsExperimental() {
        setUpScreen(setting = COLLECTION_DIRECT_BROWSE_PREFERENCE_KEY)

        scrollUntilTagDisplayed(APPEARANCE_SETTINGS_COLLECTION_DIRECT_BROWSE_TAG)
        assertFalse(preferences.getBoolean(COLLECTION_DIRECT_BROWSE_PREFERENCE_KEY, false))
        composeRule
            .onNodeWithTag(APPEARANCE_SETTINGS_COLLECTION_DIRECT_BROWSE_TAG)
            .assertTextContains("收藏直达浏览（测试）")
        composeRule
            .onNode(hasText("请谨慎开启", substring = true), useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(APPEARANCE_SETTINGS_COLLECTION_DIRECT_BROWSE_TAG).performClick()

        waitUntilBooleanPreference(COLLECTION_DIRECT_BROWSE_PREFERENCE_KEY, expected = true)
    }

    private fun setUpScreen(setting: String = "", resetPreferences: Boolean = true) {
        if (resetPreferences) {
            composeRule.resetAppPreferences()
        }
        composeRule.setScreenContent {
            AppearanceSettingsScreen(
                onExit = {},
                setting = setting,
            )
        }
    }

    private fun scrollContainer() = composeRule.onNodeWithTag(APPEARANCE_SETTINGS_SCROLL_TAG)

    private fun scrollUntilDisplayed(matcher: SemanticsMatcher, maxSwipes: Int = 12) {
        repeat(maxSwipes) {
            if (isDisplayed(matcher)) {
                return
            }
            scrollContainer().performTouchInput { swipeUp() }
            composeRule.waitForIdle()
        }
        composeRule.onNode(matcher, useUnmergedTree = true).assertIsDisplayed()
    }

    private fun clickSettingRow(title: String) {
        val rowMatcher = hasAnyDescendant(hasText(title)) and hasClickAction()
        scrollUntilDisplayed(rowMatcher)
        composeRule.onNode(rowMatcher, useUnmergedTree = true).performClick()
    }

    private fun scrollUntilTagDisplayed(tag: String, maxSwipes: Int = 12) {
        repeat(maxSwipes) {
            if (isTagDisplayed(tag)) {
                return
            }
            scrollContainer().performTouchInput { swipeUp() }
            composeRule.waitForIdle()
        }
        repeat(maxSwipes) {
            if (isTagDisplayed(tag)) {
                return
            }
            scrollContainer().performVerticalSwipeCycle()
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithTag(tag).assertIsDisplayed()
    }

    private fun waitUntilDisplayed(matcher: SemanticsMatcher, timeoutMillis: Long = 5_000) {
        composeRule.waitUntil(timeoutMillis) { isDisplayed(matcher) }
        composeRule.onNode(matcher, useUnmergedTree = true).assertIsDisplayed()
    }

    private fun waitUntilTagDisplayed(tag: String, timeoutMillis: Long = 5_000) {
        composeRule.waitUntil(timeoutMillis) { isTagDisplayed(tag) }
        composeRule.onNodeWithTag(tag).assertIsDisplayed()
    }

    private fun waitUntilTagExists(tag: String, timeoutMillis: Long = 5_000) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() ||
                composeRule
                    .onAllNodesWithTag(tag, useUnmergedTree = true)
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
        }
    }

    private fun boundsHeightForTag(tag: String): Float = composeRule
        .onNodeWithTag(tag)
        .fetchSemanticsNode()
        .boundsInRoot
        .height

    private fun waitUntilNodeDoesNotExist(matcher: SemanticsMatcher, timeoutMillis: Long = 5_000) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule
                .onAllNodes(matcher, useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty()
        }
    }

    private fun assertNodeDoesNotExist(matcher: SemanticsMatcher) {
        assertTrue(
            composeRule
                .onAllNodes(matcher, useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty(),
        )
    }

    private fun assertTagDoesNotExist(tag: String) {
        assertTrue(
            composeRule
                .onAllNodesWithTag(tag)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty() &&
                composeRule
                    .onAllNodesWithTag(tag, useUnmergedTree = true)
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isEmpty(),
        )
    }

    private fun waitUntilTagDoesNotExist(tag: String, timeoutMillis: Long = 5_000) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule
                .onAllNodesWithTag(tag)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty() &&
                composeRule
                    .onAllNodesWithTag(tag, useUnmergedTree = true)
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isEmpty()
        }
    }

    private fun waitUntilBooleanPreference(key: String, expected: Boolean, timeoutMillis: Long = 5_000) {
        composeRule.waitUntil(timeoutMillis) { preferences.getBoolean(key, !expected) == expected }
    }

    private fun waitUntilStringPreference(key: String, expected: String, timeoutMillis: Long = 5_000) {
        composeRule.waitUntil(timeoutMillis) { preferences.getString(key, null) == expected }
    }

    private fun waitUntilStringSetPreference(
        key: String,
        expected: Set<String>,
        timeoutMillis: Long = 5_000,
    ) {
        composeRule.waitUntil(timeoutMillis) {
            preferences.getStringSet(key, emptySet())?.toSet() == expected
        }
    }

    private fun isDisplayed(matcher: SemanticsMatcher): Boolean = runCatching {
        composeRule.onNode(matcher, useUnmergedTree = true).assertIsDisplayed()
    }.isSuccess

    private fun isTagDisplayed(tag: String): Boolean = runCatching {
        composeRule.onNodeWithTag(tag).assertIsDisplayed()
    }.isSuccess ||
        runCatching {
            composeRule.onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed()
        }.isSuccess
}
