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
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.SentenceSimilarityTest
import com.github.zly2006.zhihu.test.RecordingNavigator
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.subscreens.DEVELOPER_SETTINGS_BACK_BUTTON_TAG
import com.github.zly2006.zhihu.ui.subscreens.DEVELOPER_SETTINGS_COLOR_SCHEME_TAG
import com.github.zly2006.zhihu.ui.subscreens.DEVELOPER_SETTINGS_MODE_TAG
import com.github.zly2006.zhihu.ui.subscreens.DEVELOPER_SETTINGS_SENTENCE_SIMILARITY_TAG
import com.github.zly2006.zhihu.ui.subscreens.DeveloperSettingsScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeveloperSettingsScreenInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
    }

    @Test
    fun developerModeToggle_updatesPreferenceAndOnlyNavigatesBackWhenDisabling() {
        // Expected behavior:
        // 1. The screen should read the stored preference value when it is first composed.
        // 2. Enabling developer mode should only update SharedPreferences and keep the user on the screen.
        // 3. Disabling developer mode should update SharedPreferences again and emit exactly one navigate-back event.
        assertFalse(isDeveloperModeEnabled())

        val navigator = setDeveloperSettingsContent()

        composeRule.onNodeWithText("开发者模式").assertIsDisplayed()
        composeRule.onNodeWithTag(DEVELOPER_SETTINGS_MODE_TAG).performClick()

        composeRule.waitUntil(5_000) {
            isDeveloperModeEnabled()
        }
        assertTrue(isDeveloperModeEnabled())
        assertEquals(0, navigator.backCount)

        composeRule.onNodeWithTag(DEVELOPER_SETTINGS_MODE_TAG).performClick()

        composeRule.waitUntil(5_000) {
            !isDeveloperModeEnabled() && navigator.backCount == 1
        }
        assertFalse(isDeveloperModeEnabled())
        assertEquals(1, navigator.backCount)
    }

    @Test
    fun scrollTargets_canBeReachedDeterministicallyWithoutCoordinateGestures() {
        // Expected behavior:
        // 1. The test should be able to scroll directly to the deep TTS section using semantics rather than raw swipes.
        // 2. After reaching the lower section, the same test should be able to scroll back to the action buttons deterministically.
        // 3. Both targets need to become visible so later click assertions can rely on stable node lookup.
        setDeveloperSettingsContent()

        composeRule.onNodeWithText("引擎列表").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag(DEVELOPER_SETTINGS_COLOR_SCHEME_TAG).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(DEVELOPER_SETTINGS_SENTENCE_SIMILARITY_TAG).assertIsDisplayed()
    }

    @Test
    fun navigationButtons_andTopAppBarBack_dispatchExpectedNavigatorEvents() {
        // Expected behavior:
        // 1. The top app bar back button should emit a navigate-back event without adding a forward destination.
        // 2. The sentence similarity button should navigate to SentenceSimilarityTest.
        // 3. The color scheme button should navigate to Account.DeveloperSettings.ColorScheme in the recorded order.
        val navigator = setDeveloperSettingsContent()

        composeRule.onNodeWithTag(DEVELOPER_SETTINGS_BACK_BUTTON_TAG).performClick()

        composeRule.waitUntil(5_000) {
            navigator.backCount == 1
        }
        assertEquals(1, navigator.backCount)
        assertTrue(navigator.destinations.isEmpty())

        composeRule.onNodeWithTag(DEVELOPER_SETTINGS_SENTENCE_SIMILARITY_TAG).performScrollTo().performClick()
        composeRule.onNodeWithTag(DEVELOPER_SETTINGS_COLOR_SCHEME_TAG).performScrollTo().performClick()

        composeRule.waitUntil(5_000) {
            navigator.destinations.size == 2
        }
        assertEquals(
            listOf(
                SentenceSimilarityTest,
                Account.DeveloperSettings.ColorScheme,
            ),
            navigator.destinations,
        )
    }

    private fun setDeveloperSettingsContent(): RecordingNavigator = composeRule.setScreenContent {
        DeveloperSettingsScreen(innerPadding = PaddingValues(0.dp))
    }

    private fun isDeveloperModeEnabled(): Boolean = composeRule.activity
        .getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        .getBoolean("developer", false)
}
