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

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.RecordingNavigator
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.subscreens.SETTINGS_SEARCH_INPUT_TAG
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsSearchScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
    }

    @Test
    fun searchScreen_filtersAppearanceResultsAndNavigatesToTargetSetting() {
        val navigator = setSearchScreenContent()

        composeRule.onNodeWithTag(SETTINGS_SEARCH_INPUT_TAG).performTextInput("热搜")
        composeRule.onNodeWithTag("settingsSearch.result.appearance.showSearchHotSearch")
            .assertIsDisplayed()
            .performClick()

        assertEquals(
            listOf(Account.AppearanceSettings(setting = "showSearchHotSearch")),
            navigator.destinations,
        )
    }

    @Test
    fun searchScreen_canJumpToSystemAndNotificationSettingsEntries() {
        val navigator = setSearchScreenContent()

        composeRule.onNodeWithTag(SETTINGS_SEARCH_INPUT_TAG).performTextInput("GitHub Token")
        composeRule.onNodeWithTag("settingsSearch.result.system.githubToken")
            .assertIsDisplayed()
            .performClick()
        assertEquals(
            listOf(Account.SystemAndUpdateSettings(setting = "githubToken")),
            navigator.destinations,
        )

        composeRule.onNodeWithTag(SETTINGS_SEARCH_INPUT_TAG).performTextClearance()
        composeRule.onNodeWithTag(SETTINGS_SEARCH_INPUT_TAG).performTextInput("系统通知")
        composeRule.onNodeWithTag("settingsSearch.result.notification.displayInAppNotifications")
            .assertDoesNotExist()
        composeRule.onNodeWithTag("settingsSearch.result.notification.systemNotifications")
            .assertIsDisplayed()
            .performClick()

        assertEquals(
            listOf(
                Account.SystemAndUpdateSettings(setting = "githubToken"),
                Notification.NotificationSettings(setting = "systemNotifications"),
            ),
            navigator.destinations,
        )
    }

    private fun setSearchScreenContent(): RecordingNavigator = composeRule.setScreenContent {
        com.github.zly2006.zhihu.ui.subscreens.SettingsSearchScreen()
    }
}
