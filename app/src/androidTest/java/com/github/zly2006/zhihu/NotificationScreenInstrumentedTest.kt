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
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.data.NotificationActor
import com.github.zly2006.zhihu.data.NotificationContent
import com.github.zly2006.zhihu.data.NotificationItem
import com.github.zly2006.zhihu.data.NotificationLink
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.RecordingNavigator
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.NotificationScreen
import com.github.zly2006.zhihu.viewmodel.NotificationViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
    }

    @Test
    fun notificationScreen_showsStableToolbarActionsWithoutLiveData() {
        /*
         * Expected behavior:
         * 1. The test preloads one local notification before composing the screen so NotificationScreen
         *    does not need to fetch live notification data just to render its scaffold.
         * 2. The toolbar should always show the page title plus clickable back and settings actions.
         * 3. The "mark all as read" action should stay hidden while unreadCount remains at its default zero.
         */
        setNotificationScreenContent()

        composeRule.onNodeWithText("通知").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("返回").assertExists().assertHasClickAction()
        composeRule.onNodeWithContentDescription("设置").assertExists().assertHasClickAction()
        composeRule.onNodeWithContentDescription("已读").assertDoesNotExist()
    }

    @Test
    fun notificationScreen_backButton_delegatesToNavigatorBackCallback() {
        /*
         * Expected behavior:
         * 1. Pressing the toolbar back button should invoke the injected navigator back callback exactly once.
         * 2. The screen should not record any forward navigation destination when the user only requests back.
         * 3. This interaction must remain deterministic even when the notification list itself is seeded locally.
         */
        val recordingNavigator = setNotificationScreenContent()

        composeRule.onNodeWithContentDescription("返回").performClick()

        assertEquals(1, recordingNavigator.backCount)
        assertTrue(recordingNavigator.destinations.isEmpty())
    }

    @Test
    fun notificationScreen_settingsButton_navigatesToNotificationSettings() {
        /*
         * Expected behavior:
         * 1. Pressing the toolbar settings button should navigate to Notification.NotificationSettings.
         * 2. This action should not trigger a back event because it is a forward navigation path.
         * 3. The recorded destination list should contain exactly the settings destination after one click.
         */
        val recordingNavigator = setNotificationScreenContent()

        composeRule.onNodeWithContentDescription("设置").performClick()

        assertEquals(0, recordingNavigator.backCount)
        assertEquals(listOf(Notification.NotificationSettings), recordingNavigator.destinations)
    }

    private fun setNotificationScreenContent(): RecordingNavigator {
        composeRule.seedNotificationViewModel()
        return composeRule.setScreenContent {
            NotificationScreen(innerPadding = PaddingValues())
        }
    }

    private fun notificationFixture() = NotificationItem(
        id = "local-notification",
        type = "notification",
        isRead = true,
        createTime = 1_713_420_000L,
        content = NotificationContent(
            verb = "voteup_answer",
            actors = listOf(
                NotificationActor(
                    name = "测试用户",
                    type = "people",
                    link = "https://www.zhihu.com/people/test-user",
                    urlToken = "test-user",
                ),
            ),
            target = NotificationLink(
                text = "测试回答",
                link = "https://www.zhihu.com/question/1/answer/2",
            ),
        ),
    )

    private fun MainActivityComposeRule.seedNotificationViewModel() {
        activity.runOnUiThread {
            val viewModel = ViewModelProvider(activity)[NotificationViewModel::class.java]
            viewModel.allData.clear()
            viewModel.allData += notificationFixture()
        }
        waitForIdle()
    }
}
