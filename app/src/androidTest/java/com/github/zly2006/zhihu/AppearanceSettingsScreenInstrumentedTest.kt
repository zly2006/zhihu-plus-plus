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
import android.content.SharedPreferences
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import com.github.zly2006.zhihu.test.performVerticalSwipeCycle
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.ARTICLE_USE_WEBVIEW_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.AnswerDoubleTapAction
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.subscreens.APPEARANCE_SETTINGS_ANSWER_DOUBLE_TAP_TAG
import com.github.zly2006.zhihu.ui.subscreens.APPEARANCE_SETTINGS_SCROLL_TAG
import com.github.zly2006.zhihu.ui.subscreens.APPEARANCE_SETTINGS_START_DESTINATION_TAG
import com.github.zly2006.zhihu.ui.subscreens.AppearanceSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.BOTTOM_BAR_ITEMS_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.START_DESTINATION_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.appearanceSettingsStartDestinationOptionTag
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

    @Test
    fun togglingWebViewModePersistsAndKeepsDependentControlsStableAfterScroll() {
        // This test verifies a full user path in the answer section: scroll to the WebView setting,
        // toggle it on, confirm the dependent font setting appears, then scroll away and back to
        // prove that both the UI and the persisted preference stay in sync before toggling it off again.
        setUpScreen()

        scrollUntilDisplayed(hasText("使用 WebView 显示文章"))
        assertFalse(preferences.getBoolean(ARTICLE_USE_WEBVIEW_PREFERENCE_KEY, false))
        assertNodeDoesNotExist(hasText("WebView 自定义字体"))

        clickSettingRow("使用 WebView 显示文章")
        waitUntilBooleanPreference(ARTICLE_USE_WEBVIEW_PREFERENCE_KEY, expected = true)
        waitUntilDisplayed(hasText("WebView 自定义字体"))

        scrollContainer().performVerticalSwipeCycle()
        waitUntilDisplayed(hasText("WebView 自定义字体"))
        assertTrue(preferences.getBoolean(ARTICLE_USE_WEBVIEW_PREFERENCE_KEY, false))

        clickSettingRow("使用 WebView 显示文章")
        waitUntilBooleanPreference(ARTICLE_USE_WEBVIEW_PREFERENCE_KEY, expected = false)
        waitUntilNodeDoesNotExist(hasText("WebView 自定义字体"))
    }

    @Test
    fun selectingAnswerDoubleTapActionUpdatesDropdownTextAndPreference() {
        // This test verifies the dropdown click path for the answer double-tap action: after scrolling
        // to the control, opening the anchored menu, and picking a different option, the visible label
        // and the stored preference must both change to the same deterministic value.
        setUpScreen()

        scrollUntilDisplayed(hasText("双击回答动作"))
        waitUntilStringPreference(
            ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY,
            expected = AnswerDoubleTapAction.Ask.preferenceValue,
        )

        composeRule.onNodeWithTag(APPEARANCE_SETTINGS_ANSWER_DOUBLE_TAP_TAG).performClick()
        composeRule.onNode(hasText(AnswerDoubleTapAction.VoteUp.label), useUnmergedTree = true).performClick()

        waitUntilStringPreference(
            ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY,
            expected = AnswerDoubleTapAction.VoteUp.preferenceValue,
        )
        composeRule
            .onNodeWithTag(APPEARANCE_SETTINGS_ANSWER_DOUBLE_TAP_TAG)
            .assertTextContains(AnswerDoubleTapAction.VoteUp.label)
    }

    @Test
    fun bottomBarSelectionAndStartDestinationRemainStableAcrossScrollAndClicks() {
        // This test verifies a multi-step bottom-bar configuration flow: remove one selected item,
        // add another one, pick the new entry as the startup destination, and then perform an extra
        // scroll cycle to ensure the rendered state still matches the persisted SharedPreferences.
        setUpScreen()

        scrollUntilDisplayed(hasText("底部导航栏"))
        clickSettingRow("历史")
        waitUntilStringSetPreference(
            BOTTOM_BAR_ITEMS_PREFERENCE_KEY,
            expected = setOf(Home.name, Follow.name, Daily.name, Account.name),
        )

        clickSettingRow("热榜")
        waitUntilStringSetPreference(
            BOTTOM_BAR_ITEMS_PREFERENCE_KEY,
            expected = setOf(Home.name, Follow.name, Daily.name, HotList.name, Account.name),
        )

        composeRule.onNodeWithTag(APPEARANCE_SETTINGS_START_DESTINATION_TAG).performClick()
        composeRule.onNodeWithTag(appearanceSettingsStartDestinationOptionTag(HotList.name)).performClick()

        waitUntilStringPreference(START_DESTINATION_PREFERENCE_KEY, expected = HotList.name)
        scrollContainer().performVerticalSwipeCycle()
        composeRule
            .onNodeWithTag(APPEARANCE_SETTINGS_START_DESTINATION_TAG)
            .assertTextContains("热榜")
        assertEquals(
            setOf(Home.name, Follow.name, Daily.name, HotList.name, Account.name),
            preferences.getStringSet(BOTTOM_BAR_ITEMS_PREFERENCE_KEY, emptySet())?.toSet(),
        )
    }

    private fun setUpScreen() {
        composeRule.resetAppPreferences()
        composeRule.setScreenContent {
            AppearanceSettingsScreen(innerPadding = PaddingValues())
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

    private fun waitUntilDisplayed(matcher: SemanticsMatcher, timeoutMillis: Long = 5_000) {
        composeRule.waitUntil(timeoutMillis) { isDisplayed(matcher) }
        composeRule.onNode(matcher, useUnmergedTree = true).assertIsDisplayed()
    }

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
}
