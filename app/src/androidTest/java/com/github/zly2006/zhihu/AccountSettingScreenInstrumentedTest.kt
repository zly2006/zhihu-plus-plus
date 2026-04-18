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
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Collections
import com.github.zly2006.zhihu.navigation.Daily
import com.github.zly2006.zhihu.navigation.Follow
import com.github.zly2006.zhihu.navigation.Home
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.OnlineHistory
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.RecordingNavigator
import com.github.zly2006.zhihu.test.performVerticalSwipeCycle
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.ACCOUNT_SETTINGS_SCROLL_TAG
import com.github.zly2006.zhihu.ui.AccountSettingScreen
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.subscreens.BOTTOM_BAR_ITEMS_PREFERENCE_KEY
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger
import com.github.zly2006.zhihu.data.Person as AccountPerson

@RunWith(AndroidJUnit4::class)
class AccountSettingScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    private val preferences: SharedPreferences
        get() = composeRule.activity.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
        AccountData.delete(composeRule.activity)
    }

    @After
    fun tearDown() {
        AccountData.delete(composeRule.activity)
        composeRule.resetAppPreferences()
    }

    @Test
    fun loggedOutLoginEntryLaunchesLoginActivityWithoutDependingOnExistingAccountState() {
        // Expected behavior:
        // 1. When local account storage is empty, AccountSettingScreen must render the logged-out
        //    entry instead of any logged-in shortcut cluster.
        // 2. Tapping that entry should launch LoginActivity directly from the screen contract,
        //    without requiring a network response or a pre-existing authenticated account file.
        // 3. The assertion uses an ActivityMonitor so the test verifies the real startActivity path
        //    rather than only checking that the row exists in the semantics tree.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val loginMonitor = instrumentation.addMonitor(LoginActivity::class.java.name, null, false)

        try {
            showScreen()

            clickRow("登录知乎")

            val startedActivity = instrumentation.waitForMonitorWithTimeout(loginMonitor, 5_000)
            assertNotNull("Tapping the logged-out entry should launch LoginActivity", startedActivity)
            startedActivity?.finish()
            instrumentation.waitForIdleSync()
        } finally {
            instrumentation.removeMonitor(loginMonitor)
        }
    }

    @Test
    fun coreSettingsRowsNavigateInOrderAndBottomScrollRemainsStableAcrossRoundTrips() {
        // Expected behavior:
        // 1. The always-available local settings rows must navigate to their corresponding account
        //    sub-destinations in a deterministic order: appearance, recommendation, system/update,
        //    and open-source licenses.
        // 2. Reaching the about section should rely on scroll semantics from the tagged scroll
        //    container instead of raw coordinates, so the test remains stable on different devices.
        // 3. A forward-and-reverse swipe cycle plus an explicit scroll back to the top should leave
        //    the screen usable, proving that the bottom portion of the page remains reachable after
        //    repeated navigation-row interaction and scrolling.
        val navigator = showScreen()

        clickRow("外观与阅读体验")
        clickRow("推荐系统与内容过滤")
        clickRow("系统与更新")

        scrollContainer().performVerticalSwipeCycle()
        scrollContainer().performScrollToNode(hasText("开源许可"))
        composeRule.onNodeWithText("开源许可").assertIsDisplayed()
        clickRow("开源许可")

        composeRule.waitUntil(timeoutMillis = 5_000) {
            navigator.destinations.size == 4
        }
        assertEquals(
            listOf(
                Account.AppearanceSettings(),
                Account.RecommendSettings(),
                Account.SystemAndUpdateSettings,
                Account.OpenSourceLicenses,
            ),
            navigator.destinations,
        )

        scrollContainer().performScrollToNode(hasText("登录知乎"))
        composeRule.onNodeWithText("登录知乎").assertIsDisplayed()
    }

    @Test
    fun loggedInShortcutClusterNavigatesOfflineAndDismissesOnlyForOverlayDestinations() {
        // Expected behavior:
        // 1. A fully local seeded account plus SharedPreferences should be enough to render the
        //    logged-in shortcut cluster without touching the `/me` refresh path or assuming a real
        //    session cookie is valid on the network.
        // 2. The favorites shortcut should navigate to the seeded Collections destination and must
        //    not close the surrounding account surface.
        // 3. The notification and history shortcuts represent overlay-style exits from the account
        //    surface, so each one must call onDismissRequest exactly once before recording the
        //    navigation event.
        preferences
            .edit()
            .putBoolean("duo3_home_account", true)
            .putStringSet(
                BOTTOM_BAR_ITEMS_PREFERENCE_KEY,
                linkedSetOf(Home.name, Follow.name, Daily.name),
            ).commit()
        seedLoggedInAccount()

        val dismissCount = AtomicInteger(0)
        val navigator = showScreen(
            unreadCount = 7,
            onDismissRequest = { dismissCount.incrementAndGet() },
        )

        composeRule.onNodeWithText(SEEDED_ACCOUNT_NAME).assertIsDisplayed()
        composeRule.onNodeWithText("收藏夹").assertIsDisplayed()
        composeRule.onNodeWithText("通知").assertIsDisplayed()
        composeRule.onNodeWithText("浏览历史").assertIsDisplayed()

        clickRow("收藏夹")
        composeRule.waitUntil(timeoutMillis = 5_000) {
            navigator.destinations.size == 1
        }
        assertEquals(0, dismissCount.get())

        clickRow("通知")
        composeRule.waitUntil(timeoutMillis = 5_000) {
            navigator.destinations.size == 2 && dismissCount.get() == 1
        }

        clickRow("浏览历史")
        composeRule.waitUntil(timeoutMillis = 5_000) {
            navigator.destinations.size == 3 && dismissCount.get() == 2
        }

        assertEquals(
            listOf(
                Collections(SEEDED_ACCOUNT_URL_TOKEN),
                Notification,
                OnlineHistory,
            ),
            navigator.destinations,
        )
        assertEquals(2, dismissCount.get())
    }

    @Test
    fun developerEntryAppearsOnlyWhenPreferenceIsEnabledAndThenNavigatesDeterministically() {
        // Expected behavior:
        // 1. Developer options should stay absent for a default local install so the screen does
        //    not expose hidden functionality unless SharedPreferences explicitly enable it.
        // 2. Recreating the screen after enabling the developer preference must reveal the row in
        //    the same scrollable list, again without any server dependency.
        // 3. Once visible, tapping the row should emit exactly the DeveloperSettings destination.
        showScreen()
        scrollContainer().performScrollToNode(hasText("开源许可"))
        assertNodeDoesNotExist(settingRowMatcher("开发者选项"))

        preferences.edit().putBoolean("developer", true).commit()

        val navigator = showScreen()
        scrollContainer().performScrollToNode(hasText("开发者选项"))
        composeRule.onNodeWithText("开发者选项").assertIsDisplayed()
        clickRow("开发者选项")

        composeRule.waitUntil(timeoutMillis = 5_000) {
            navigator.destinations.size == 1
        }
        assertEquals(listOf(Account.DeveloperSettings), navigator.destinations)
    }

    private fun showScreen(
        unreadCount: Int = 0,
        onDismissRequest: () -> Unit = {},
    ): RecordingNavigator = composeRule.setScreenContent {
        AccountSettingScreen(
            innerPadding = PaddingValues(),
            unreadCount = unreadCount,
            onDismissRequest = onDismissRequest,
            refreshAccountProfileOnEnter = false,
        )
    }

    private fun seedLoggedInAccount() {
        AccountData.saveData(
            composeRule.activity,
            AccountData.Data(
                login = true,
                username = SEEDED_ACCOUNT_NAME,
                self = AccountPerson(
                    id = "offline-account-id",
                    url = "https://www.zhihu.com/people/$SEEDED_ACCOUNT_URL_TOKEN",
                    userType = "people",
                    urlToken = SEEDED_ACCOUNT_URL_TOKEN,
                    name = SEEDED_ACCOUNT_NAME,
                    headline = "用于 AccountSettingScreen 仪器测试的离线账号",
                    avatarUrl = "",
                ),
            ),
        )
        composeRule.waitForIdle()
    }

    private fun scrollContainer() = composeRule.onNodeWithTag(ACCOUNT_SETTINGS_SCROLL_TAG)

    private fun clickRow(title: String) {
        composeRule.onNode(settingRowMatcher(title), useUnmergedTree = true).performScrollTo().performClick()
    }

    private fun settingRowMatcher(title: String): SemanticsMatcher =
        hasAnyDescendant(hasText(title)) and hasClickAction()

    private fun assertNodeDoesNotExist(matcher: SemanticsMatcher) {
        assertEquals(
            0,
            composeRule
                .onAllNodes(matcher, useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .size,
        )
    }

    private companion object {
        const val SEEDED_ACCOUNT_NAME = "离线测试账号"
        const val SEEDED_ACCOUNT_URL_TOKEN = "offline-seeded-user"
    }
}
