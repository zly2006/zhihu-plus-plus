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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.RecordingNavigator
import com.github.zly2006.zhihu.test.performHorizontalSwipeCycle
import com.github.zly2006.zhihu.test.performVerticalSwipeCycle
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.BlocklistSettingsScreen
import com.github.zly2006.zhihu.ui.BlocklistSettingsTestTags
import com.github.zly2006.zhihu.viewmodel.filter.BlockedQuestionAuthor
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlocklistSettingsScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
        runBlocking {
            getContentFilterDatabase(composeRule.activity).blockedQuestionAuthorDao().apply {
                clearAllUsers()
                seededQuestionAuthors().forEach { insertUser(it) }
            }
        }
    }

    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/403
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/454
     */
    @Test
    fun questionAuthorTabSupportsNavigationDeleteClearAndDialogPathsOffline() {
        /*
         * Expected behavior:
         * 1. Switching to the question-author tab should render the seeded list, preserve row
         *    visibility under swipe cycles, and keep the add FAB visible for this tab.
         * 2. Clicking a row must navigate to that asker's Person destination with the seeded id,
         *    urlToken, and display name.
         * 3. Individual delete, clear-all, and add actions must mutate the real persistent
         *    blocklist used by the production screen.
         * 4. The add dialog should support both cancel and confirm paths deterministically.
         */
        val dao = getContentFilterDatabase(composeRule.activity).blockedQuestionAuthorDao()
        val navigator = setScreen()

        composeRule.onNode(hasText("屏蔽提问者") and hasClickAction()).performClick()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.FAB).assertIsDisplayed()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(BlocklistSettingsTestTags.QUESTION_AUTHOR_LIST).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.QUESTION_AUTHOR_LIST).assertIsDisplayed()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.QUESTION_AUTHOR_LIST).performVerticalSwipeCycle()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.QUESTION_AUTHOR_LIST).performHorizontalSwipeCycle()
        navigator.reset()
        composeRule.onNodeWithTag("blocklistSettings:questionAuthors:item:offline-asker-1").performClick()
        assertEquals(
            listOf(
                Person(
                    id = "offline-asker-1",
                    urlToken = "offline-asker-token-1",
                    name = "离线提问者一",
                ),
            ),
            navigator.destinations,
        )

        composeRule.onNodeWithTag("blocklistSettings:questionAuthors:delete:offline-asker-1").performClick()
        composeRule.waitUntil(5_000) { runBlocking { dao.getUserCount() == 1 } }
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.QUESTION_AUTHOR_CLEAR_BUTTON).performClick()
        composeRule.waitUntil(5_000) { runBlocking { dao.getUserCount() == 0 } }

        composeRule.onNodeWithTag(BlocklistSettingsTestTags.FAB).performClick()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.USER_DIALOG_DISMISS).performClick()
        assertEquals(0, runBlocking { dao.getUserCount() })

        composeRule.onNodeWithTag(BlocklistSettingsTestTags.FAB).performClick()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.USER_DIALOG_ID_INPUT).performTextInput("new-asker-id")
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.USER_DIALOG_NAME_INPUT).performTextInput("新提问者")
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.USER_DIALOG_CONFIRM).performClick()
        composeRule.waitUntil(5_000) { runBlocking { dao.getUserCount() == 1 } }
        assertEquals(
            listOf("new-asker-id" to "新提问者"),
            runBlocking { dao.getAllUsers().map { it.userId to it.userName } },
        )
    }

    private fun setScreen(): RecordingNavigator = composeRule.setScreenContent {
        BlocklistSettingsScreen()
    }

    private fun seededQuestionAuthors(): List<BlockedQuestionAuthor> = listOf(
        BlockedQuestionAuthor(
            userId = "offline-asker-1",
            userName = "离线提问者一",
            urlToken = "offline-asker-token-1",
        ),
        BlockedQuestionAuthor(
            userId = "offline-asker-2",
            userName = "离线提问者二",
            urlToken = "offline-asker-token-2",
        ),
    )
}
