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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.NotificationPreferences
import com.github.zly2006.zhihu.ui.NotificationSettingsScreen
import com.github.zly2006.zhihu.ui.NotificationType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationSettingsScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule()

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
    }

    @Test
    fun notificationSwitchesScrollAndPersistDeterministicallyAcrossForwardAndReversePasses() {
        // Start from cleared SharedPreferences so every row begins at its documented default value.
        // This makes the assertions deterministic for all nine switches and prevents cross-test
        // leakage from previous runs from changing the expected toggle target.
        val toggleCases = notificationToggleCases()
        toggleCases.forEach { toggleCase ->
            assertEquals(toggleCase.defaultValue, readPreference(toggleCase))
        }

        showScreen()
        composeRule.onNodeWithText("通知设置").assertIsDisplayed()

        // Walk the page from top to bottom, scrolling each row into view before tapping it. Every
        // click must invert the persisted value for that exact setting, including rows with the same
        // visible label that appear once under "系统通知" and once under "应用内显示".
        toggleCases.forEach { toggleCase ->
            scrollToRow(toggleCase)
            clickRow(toggleCase)
            waitUntilPreferenceValue(toggleCase, expected = !toggleCase.defaultValue)
        }
        toggleCases.forEach { toggleCase ->
            assertEquals(!toggleCase.defaultValue, readPreference(toggleCase))
        }

        // Recreate the screen without clearing preferences, then walk back from bottom to top. This
        // proves the screen re-reads persisted state on a fresh composition and that reverse scrolling
        // stays stable even after the first pass already moved the column deep into the page.
        showScreen()
        toggleCases.asReversed().forEach { toggleCase ->
            scrollToRow(toggleCase)
            clickRow(toggleCase)
            waitUntilPreferenceValue(toggleCase, expected = toggleCase.defaultValue)
        }
        toggleCases.forEach { toggleCase ->
            assertEquals(toggleCase.defaultValue, readPreference(toggleCase))
        }
    }

    @Test
    fun backButtonDelegatesExactlyOneNavigateBackEventAfterScrolling() {
        // Open the screen inside the shared recording host, scroll all the way to the last duplicate
        // notification row to exercise the nested-scroll app bar state, and then verify that the top
        // app bar back button still emits exactly one navigate-back event with no unexpected forward navigation.
        val navigator = showScreen()
        scrollToRow(notificationToggleCases().last())

        composeRule.onNodeWithContentDescription("返回").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(1, navigator.backCount)
            assertEquals(0, navigator.destinations.size)
        }
    }

    private fun showScreen() = composeRule.setScreenContent {
        NotificationSettingsScreen(innerPadding = PaddingValues())
    }

    private fun scrollToRow(toggleCase: ToggleCase) {
        val row = settingRow(toggleCase)
        row.performScrollTo()
        row.assertIsDisplayed()
    }

    private fun clickRow(toggleCase: ToggleCase) {
        settingRow(toggleCase).performClick()
    }

    private fun settingRow(toggleCase: ToggleCase) =
        composeRule.onAllNodes(settingRowMatcher(toggleCase.title), useUnmergedTree = true).also {
            it.assertCountEquals(toggleCase.labelOccurrenceCount)
        }[toggleCase.labelOccurrenceIndex]

    private fun settingRowMatcher(title: String): SemanticsMatcher =
        hasAnyDescendant(hasText(title)) and hasClickAction()

    private fun waitUntilPreferenceValue(
        toggleCase: ToggleCase,
        expected: Boolean,
        timeoutMillis: Long = 5_000,
    ) {
        composeRule.waitUntil(timeoutMillis) { readPreference(toggleCase) == expected }
    }

    private fun readPreference(toggleCase: ToggleCase): Boolean = when (toggleCase.group) {
        ToggleGroup.AutoMarkAsRead -> NotificationPreferences.getAutoMarkAsReadEnabled(composeRule.activity)
        ToggleGroup.SystemNotification -> NotificationPreferences.getSystemNotificationEnabled(
            composeRule.activity,
            checkNotNull(toggleCase.type),
        )

        ToggleGroup.DisplayInApp -> NotificationPreferences.getDisplayInAppEnabled(
            composeRule.activity,
            checkNotNull(toggleCase.type),
        )
    }

    private fun notificationToggleCases(): List<ToggleCase> = buildList {
        add(
            ToggleCase(
                title = "打开通知自动已读",
                group = ToggleGroup.AutoMarkAsRead,
                defaultValue = true,
                labelOccurrenceCount = 1,
                labelOccurrenceIndex = 0,
            ),
        )

        NotificationType.entries.forEach { type ->
            add(
                ToggleCase(
                    title = type.displayName,
                    group = ToggleGroup.SystemNotification,
                    type = type,
                    defaultValue = false,
                    labelOccurrenceCount = 2,
                    labelOccurrenceIndex = 0,
                ),
            )
        }

        NotificationType.entries.forEach { type ->
            add(
                ToggleCase(
                    title = type.displayName,
                    group = ToggleGroup.DisplayInApp,
                    type = type,
                    defaultValue = type.defaultValue,
                    labelOccurrenceCount = 2,
                    labelOccurrenceIndex = 1,
                ),
            )
        }
    }

    private data class ToggleCase(
        val title: String,
        val group: ToggleGroup,
        val defaultValue: Boolean,
        val labelOccurrenceCount: Int,
        val labelOccurrenceIndex: Int,
        val type: NotificationType? = null,
    )

    private enum class ToggleGroup {
        AutoMarkAsRead,
        SystemNotification,
        DisplayInApp,
    }
}
