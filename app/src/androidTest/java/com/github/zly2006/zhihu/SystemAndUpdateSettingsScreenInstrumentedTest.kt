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
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.performVerticalSwipeCycle
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.subscreens.SystemAndUpdateSettingsScreen
import com.github.zly2006.zhihu.updater.SchematicVersion
import com.github.zly2006.zhihu.updater.UpdateManager
import com.github.zly2006.zhihu.updater.UpdateManager.UpdateState
import com.github.zly2006.zhihu.util.ContinuousUsageReminderManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SystemAndUpdateSettingsScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    private val preferences: SharedPreferences
        get() = composeRule.activity.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
        UpdateManager.setAutoCheckEnabled(composeRule.activity, true)
        preferences
            .edit()
            .putBoolean(CHECK_NIGHTLY_UPDATES_PREFERENCE_KEY, false)
            .putBoolean(ALLOW_TELEMETRY_PREFERENCE_KEY, true)
            .putInt(ContinuousUsageReminderManager.KEY_CONTINUOUS_USAGE_REMINDER_INTERVAL_MINUTES, 0)
            .commit()
        UpdateManager.updateState.value = UpdateState.NoUpdate
        composeRule.waitForIdle()
    }

    @After
    fun tearDown() {
        UpdateManager.updateState.value = UpdateState.NoUpdate
        composeRule.waitForIdle()
    }

    @Test
    fun updateBannerButtonsAndBackNavigationStayDeterministicWithoutNetwork() {
        // This test seeds the banner entirely from the in-memory UpdateManager state flow so the
        // screen renders a fixed "update available" path without touching GitHub or any other
        // network source, then verifies that the skip and reset buttons both produce the exact
        // local state transitions the settings screen is responsible for handling.
        val seededVersion = SchematicVersion.fromString("9.9.9")
        UpdateManager.updateState.value = UpdateState.UpdateAvailable(
            version = seededVersion,
            releaseNotes = "修复若干设置项细节\nhttps://github.com/zly2006/zhihu-plus-plus/pull/321",
            downloadUrl = "https://example.com/app-lite-debug.apk",
            cnDownloadUrl = null,
        )
        val navigator = setUpScreen()

        waitUntilDisplayed(hasText("新版本：\n9.9.9"))
        waitUntilDisplayed(hasText("更新内容"))
        waitUntilDisplayed(hasText("修复若干设置项细节", substring = true))

        composeRule.onNodeWithText("跳过此版本").performClick()
        waitUntil(timeoutMillis = 5_000) {
            preferences.getString(SKIPPED_VERSION_PREFERENCE_KEY, null) == seededVersion.toString()
        }
        waitUntil(timeoutMillis = 5_000) { UpdateManager.updateState.value == UpdateState.Latest }
        composeRule.onNodeWithText("已经是最新版本").assertIsDisplayed()

        // Tapping the "already latest" button must only clear the locally-seeded Latest state back
        // to NoUpdate. The test intentionally does not tap the real network-backed check button.
        composeRule.onNodeWithText("已经是最新版本").performClick()
        waitUntil(timeoutMillis = 5_000) { UpdateManager.updateState.value == UpdateState.NoUpdate }
        composeRule.onNodeWithText("检查更新").assertIsDisplayed()

        // Back navigation is part of the same screen contract, so one final click proves the shared
        // navigator still receives exactly one back event after the banner state changes above.
        composeRule.onNodeWithContentDescription("返回").performClick()
        composeRule.runOnIdle {
            assertEquals(1, navigator.backCount)
        }
    }

    @Test
    fun reminderIntervalDropdownPersistsSelectionAcrossDeterministicScrolls() {
        // This test exercises the lower "continuous usage reminder" section using only seeded local
        // preferences: scroll to the dropdown with semantics-driven scrolling, change the selection,
        // scroll away to a bottom link, and then scroll back to prove that both the visible label and
        // the stored interval remain aligned without depending on account state or live responses.
        setUpScreen()
        val scrollContainer = scrollContainer()

        waitUntilDisplayed(hasText("检查更新"))
        scrollContainer.performScrollToNode(hasText("防沉迷提醒"))
        composeRule.onNodeWithText("防沉迷提醒").assertIsDisplayed()
        composeRule.onNode(hasText("关闭") and hasClickAction()).performClick()
        composeRule.onNode(hasText("每 30 分钟"), useUnmergedTree = true).performClick()

        waitUntilIntPreference(
            ContinuousUsageReminderManager.KEY_CONTINUOUS_USAGE_REMINDER_INTERVAL_MINUTES,
            expected = 30,
        )
        composeRule.onNodeWithText("每 30 分钟").assertIsDisplayed()

        scrollContainer.performScrollToNode(hasText("Github issue"))
        composeRule.onNodeWithText("Github issue").assertIsDisplayed()
        scrollContainer.performScrollToNode(hasText("防沉迷提醒"))
        composeRule.onNodeWithText("每 30 分钟").assertIsDisplayed()
        assertEquals(
            30,
            preferences.getInt(
                ContinuousUsageReminderManager.KEY_CONTINUOUS_USAGE_REMINDER_INTERVAL_MINUTES,
                -1,
            ),
        )
    }

    @Test
    fun toggleRowsRemainStableAfterSwipeCycleAndSemanticsScrolls() {
        // This test seeds all toggle-backed preferences to known values, flips each row through the
        // settings screen itself, performs both gesture-based and semantics-based scrolling, and then
        // verifies that the final persisted values still match the exact toggles the user selected.
        UpdateManager.setAutoCheckEnabled(composeRule.activity, false)
        preferences
            .edit()
            .putBoolean(CHECK_NIGHTLY_UPDATES_PREFERENCE_KEY, false)
            .putBoolean(ALLOW_TELEMETRY_PREFERENCE_KEY, true)
            .commit()
        setUpScreen()
        val scrollContainer = scrollContainer()

        waitUntilDisplayed(hasText("自动检查更新"))
        assertFalse(UpdateManager.isAutoCheckEnabled(composeRule.activity))
        assertFalse(preferences.getBoolean(CHECK_NIGHTLY_UPDATES_PREFERENCE_KEY, true))
        assertTrue(preferences.getBoolean(ALLOW_TELEMETRY_PREFERENCE_KEY, false))

        clickSettingRow("自动检查更新")
        waitUntil(timeoutMillis = 5_000) { UpdateManager.isAutoCheckEnabled(composeRule.activity) }

        clickSettingRow("检查 Nightly 版本更新")
        waitUntilBooleanPreference(CHECK_NIGHTLY_UPDATES_PREFERENCE_KEY, expected = true)

        clickSettingRow("允许发送遥测统计数据")
        waitUntilBooleanPreference(ALLOW_TELEMETRY_PREFERENCE_KEY, expected = false)

        scrollContainer.performVerticalSwipeCycle()
        scrollContainer.performScrollToNode(hasText("Github issue"))
        composeRule.onNodeWithText("Github issue").assertIsDisplayed()

        scrollContainer.performScrollToNode(hasText("允许发送遥测统计数据"))
        composeRule.onNodeWithText("允许发送遥测统计数据").assertIsDisplayed()
        assertTrue(UpdateManager.isAutoCheckEnabled(composeRule.activity))
        assertTrue(preferences.getBoolean(CHECK_NIGHTLY_UPDATES_PREFERENCE_KEY, false))
        assertFalse(preferences.getBoolean(ALLOW_TELEMETRY_PREFERENCE_KEY, true))
    }

    private fun setUpScreen() = composeRule.setScreenContent {
        SystemAndUpdateSettingsScreen(innerPadding = PaddingValues())
    }

    private fun scrollContainer() = composeRule.onNode(hasScrollAction())

    private fun clickSettingRow(title: String) {
        val rowMatcher = hasAnyDescendant(hasText(title)) and hasClickAction()
        waitUntilDisplayed(rowMatcher)
        composeRule.onNode(rowMatcher, useUnmergedTree = true).performClick()
    }

    private fun waitUntilDisplayed(matcher: SemanticsMatcher, timeoutMillis: Long = 5_000) {
        waitUntil(timeoutMillis) { isDisplayed(matcher) }
        composeRule.onNode(matcher, useUnmergedTree = true).assertIsDisplayed()
    }

    private fun waitUntilBooleanPreference(key: String, expected: Boolean, timeoutMillis: Long = 5_000) {
        waitUntil(timeoutMillis) { preferences.getBoolean(key, !expected) == expected }
    }

    private fun waitUntilIntPreference(key: String, expected: Int, timeoutMillis: Long = 5_000) {
        waitUntil(timeoutMillis) { preferences.getInt(key, Int.MIN_VALUE) == expected }
    }

    private fun waitUntil(timeoutMillis: Long = 5_000, condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis) { condition() }
    }

    private fun isDisplayed(matcher: SemanticsMatcher): Boolean = runCatching {
        composeRule.onNode(matcher, useUnmergedTree = true).assertIsDisplayed()
    }.isSuccess

    private companion object {
        const val ALLOW_TELEMETRY_PREFERENCE_KEY = "allowTelemetry"
        const val CHECK_NIGHTLY_UPDATES_PREFERENCE_KEY = "checkNightlyUpdates"
        const val SKIPPED_VERSION_PREFERENCE_KEY = "skippedVersion"
    }
}
