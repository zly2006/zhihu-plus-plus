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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
import com.github.zly2006.zhihu.ui.BlocklistSettingsTestConfig
import com.github.zly2006.zhihu.ui.BlocklistSettingsTestTags
import com.github.zly2006.zhihu.viewmodel.filter.BlockedKeyword
import com.github.zly2006.zhihu.viewmodel.filter.BlockedTopic
import com.github.zly2006.zhihu.viewmodel.filter.BlockedUser
import com.github.zly2006.zhihu.viewmodel.filter.BlocklistStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    }

    @Test
    fun statsImportExportAndNlpTabBackFlowRemainDeterministicOffline() {
        /*
         * Expected behavior:
         * 1. The screen must render the locally injected stats card, import button, export button,
         *    and keyword tab contents without reaching BlocklistManager or storage.
         * 2. Import and export buttons should call their injected callbacks exactly once each,
         *    which proves the top action row remains interactive in test mode.
         * 3. Switching to the NLP tab should hide the add FAB, render injected custom content, and
         *    allow that content to trigger navigator.onNavigateBack() exactly once.
         * 4. A vertical and horizontal swipe cycle on the root should not break the currently
         *    selected tab or remove the injected NLP content.
         */
        var importCount = 0
        var exportCount = 0
        val navigator = setScreen(
            testConfig = BlocklistSettingsTestConfig(
                blockedKeywords = seededKeywords(),
                stats = BlocklistStats(keywordCount = 2, userCount = 1, topicCount = 1),
                onImportRequested = { importCount++ },
                onExportRequested = { exportCount++ },
                nlpContent = { onNavigateBack ->
                    Text(
                        text = "离线 NLP 内容",
                        modifier = Modifier
                            .clickable { onNavigateBack() },
                    )
                },
            ),
        )

        composeRule.onNodeWithTag(BlocklistSettingsTestTags.STATS_CARD).assertIsDisplayed()
        composeRule.onNodeWithText("屏蔽统计").assertIsDisplayed()
        composeRule.onNodeWithText("2").assertIsDisplayed()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.IMPORT_BUTTON).performClick()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.EXPORT_BUTTON).performClick()
        assertEquals(1, importCount)
        assertEquals(1, exportCount)

        composeRule.onNodeWithTag(BlocklistSettingsTestTags.tab(1)).performClick()
        assertTagAbsent(BlocklistSettingsTestTags.FAB)
        composeRule.onNodeWithText("离线 NLP 内容").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.ROOT).performVerticalSwipeCycle()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.ROOT).performHorizontalSwipeCycle()
        composeRule.onNodeWithText("离线 NLP 内容").assertIsDisplayed()
        assertEquals(1, navigator.backCount)
        assertTrue(navigator.destinations.isEmpty())
    }

    @Test
    fun keywordTabSupportsDialogToggleDeleteClearAndSwipeCyclesOffline() {
        /*
         * Expected behavior:
         * 1. The keyword tab should render seeded rows, expose the clear-all action, and keep the
         *    list stable under vertical and horizontal swipe cycles.
         * 2. Tapping an individual delete button must route through the injected delete callback
         *    with the exact seeded keyword object instead of touching the real database.
         * 3. Opening the add dialog should allow cancel without side effects, then allow a second
         *    open where the keyword text, case-sensitive toggle, and regex toggle are all applied.
         * 4. Clearing all keywords must use the injected callback exactly once.
         */
        val addedKeywords = mutableListOf<Triple<String, Boolean, Boolean>>()
        val deletedKeywordIds = mutableListOf<Long>()
        var clearCount = 0

        setScreen(
            testConfig = BlocklistSettingsTestConfig(
                blockedKeywords = seededKeywords(),
                onAddKeyword = { keyword, caseSensitive, isRegex ->
                    addedKeywords += Triple(keyword, caseSensitive, isRegex)
                },
                onDeleteKeyword = { keyword -> deletedKeywordIds += keyword.id },
                onClearKeywords = { clearCount++ },
            ),
        )

        composeRule.onNodeWithTag(BlocklistSettingsTestTags.KEYWORD_LIST).assertIsDisplayed()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.KEYWORD_LIST).performVerticalSwipeCycle()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.KEYWORD_LIST).performHorizontalSwipeCycle()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.keywordDelete(1)).performClick()
        assertEquals(listOf(1L), deletedKeywordIds)

        composeRule.onNodeWithTag(BlocklistSettingsTestTags.FAB).performClick()
        composeRule.onNodeWithText("添加屏蔽关键词").assertIsDisplayed()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.KEYWORD_DIALOG_DISMISS).performClick()
        assertTrue(addedKeywords.isEmpty())

        composeRule.onNodeWithTag(BlocklistSettingsTestTags.FAB).performClick()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.KEYWORD_DIALOG_INPUT).performTextInput("剧透")
        composeRule
            .onNodeWithTag(BlocklistSettingsTestTags.KEYWORD_DIALOG_CASE_SENSITIVE)
            .assertIsOff()
            .performClick()
            .assertIsOn()
        composeRule
            .onNodeWithTag(BlocklistSettingsTestTags.KEYWORD_DIALOG_REGEX)
            .assertIsOff()
            .performClick()
            .assertIsOn()
        composeRule.onNodeWithText("提示：使用正则表达式可以实现更灵活的匹配，但语法错误会导致该关键词无效").assertIsDisplayed()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.KEYWORD_DIALOG_CONFIRM).performClick()

        assertEquals(listOf(Triple("剧透", true, true)), addedKeywords)

        composeRule.onNodeWithTag(BlocklistSettingsTestTags.KEYWORD_CLEAR_BUTTON).performClick()
        assertEquals(1, clearCount)
    }

    @Test
    fun userTabSupportsNavigationDeleteClearAndDialogPathsOffline() {
        /*
         * Expected behavior:
         * 1. Switching to the user tab should render the seeded blocked users list, preserve row
         *    visibility under swipe cycles, and keep the add FAB visible for this tab.
         * 2. Clicking a user row must navigate to that user's Person destination with the seeded
         *    id, urlToken, and display name.
         * 3. Individual delete and clear-all actions must call their injected callbacks instead of
         *    mutating the persistent blocklist.
         * 4. The add-user dialog should support both cancel and confirm paths deterministically.
         */
        val addedUsers = mutableListOf<Pair<String, String>>()
        val deletedUserIds = mutableListOf<String>()
        var clearCount = 0
        val navigator = setScreen(
            testConfig = BlocklistSettingsTestConfig(
                blockedUsers = seededUsers(),
                onAddUser = { userId, userName -> addedUsers += userId to userName },
                onDeleteUser = { user -> deletedUserIds += user.userId },
                onClearUsers = { clearCount++ },
            ),
        )

        composeRule.onNodeWithTag(BlocklistSettingsTestTags.tab(2)).performClick()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.FAB).assertIsDisplayed()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.USER_LIST).assertIsDisplayed()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.USER_LIST).performVerticalSwipeCycle()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.USER_LIST).performHorizontalSwipeCycle()
        navigator.reset()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.userItem("offline-user-1")).performClick()
        assertEquals(
            listOf(
                Person(
                    id = "offline-user-1",
                    urlToken = "offline-user-token-1",
                    name = "离线用户一",
                ),
            ),
            navigator.destinations,
        )

        composeRule.onNodeWithTag(BlocklistSettingsTestTags.userDelete("offline-user-1")).performClick()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.USER_CLEAR_BUTTON).performClick()
        assertEquals(listOf("offline-user-1"), deletedUserIds)
        assertEquals(1, clearCount)

        composeRule.onNodeWithTag(BlocklistSettingsTestTags.FAB).performClick()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.USER_DIALOG_DISMISS).performClick()
        assertTrue(addedUsers.isEmpty())

        composeRule.onNodeWithTag(BlocklistSettingsTestTags.FAB).performClick()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.USER_DIALOG_ID_INPUT).performTextInput("new-user-id")
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.USER_DIALOG_NAME_INPUT).performTextInput("新用户")
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.USER_DIALOG_CONFIRM).performClick()
        assertEquals(listOf("new-user-id" to "新用户"), addedUsers)
    }

    @Test
    fun topicTabSupportsDeleteClearDialogStatesAndTopicCreationOffline() {
        /*
         * Expected behavior:
         * 1. The topic tab should render seeded topics and keep them interactive after vertical and
         *    horizontal swipe cycles on the list.
         * 2. Tapping a topic delete icon must invoke the injected delete callback with that exact
         *    topic id instead of mutating the persistent store.
         * 3. The clear-all dialog must support both dismiss and confirm states, with confirm
         *    invoking the injected clear callback exactly once.
         * 4. The add-topic dialog should allow typing both topic id and topic name, then confirm
         *    through the injected add callback.
         */
        val addedTopics = mutableListOf<Pair<String, String>>()
        val deletedTopicIds = mutableListOf<String>()
        var clearCount = 0

        setScreen(
            testConfig = BlocklistSettingsTestConfig(
                blockedTopics = seededTopics(),
                onAddTopic = { topicId, topicName -> addedTopics += topicId to topicName },
                onDeleteTopic = { topic -> deletedTopicIds += topic.topicId },
                onClearTopics = { clearCount++ },
            ),
        )

        composeRule.onNodeWithTag(BlocklistSettingsTestTags.tab(3)).performClick()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.TOPIC_LIST).assertIsDisplayed()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.TOPIC_LIST).performVerticalSwipeCycle()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.TOPIC_LIST).performHorizontalSwipeCycle()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.topicDelete("topic-1")).performClick()
        assertEquals(listOf("topic-1"), deletedTopicIds)

        composeRule.onNodeWithTag(BlocklistSettingsTestTags.TOPIC_CLEAR_BUTTON).performClick()
        composeRule.onNodeWithText("确认清空").assertIsDisplayed()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.TOPIC_CLEAR_DISMISS).performClick()
        assertEquals(0, clearCount)

        composeRule.onNodeWithTag(BlocklistSettingsTestTags.TOPIC_CLEAR_BUTTON).performClick()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.TOPIC_CLEAR_CONFIRM).performClick()
        assertEquals(1, clearCount)

        composeRule.onNodeWithTag(BlocklistSettingsTestTags.FAB).performClick()
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.TOPIC_DIALOG_ID_INPUT).performTextInput("topic-3")
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.TOPIC_DIALOG_NAME_INPUT).performTextInput("离线主题三")
        composeRule.onNodeWithTag(BlocklistSettingsTestTags.TOPIC_DIALOG_CONFIRM).performClick()
        assertEquals(listOf("topic-3" to "离线主题三"), addedTopics)
    }

    private fun setScreen(testConfig: BlocklistSettingsTestConfig): RecordingNavigator = composeRule.setScreenContent {
        BlocklistSettingsScreen(
            innerPadding = PaddingValues(),
            testConfig = testConfig,
        )
    }

    private fun assertTagAbsent(tag: String) {
        composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).assertCountEquals(0)
    }

    private fun seededKeywords(): List<BlockedKeyword> = listOf(
        BlockedKeyword(id = 1, keyword = "标题党"),
        BlockedKeyword(id = 2, keyword = "剧透", caseSensitive = true),
    )

    private fun seededUsers(): List<BlockedUser> = listOf(
        BlockedUser(
            userId = "offline-user-1",
            userName = "离线用户一",
            urlToken = "offline-user-token-1",
        ),
        BlockedUser(
            userId = "offline-user-2",
            userName = "离线用户二",
            urlToken = "offline-user-token-2",
        ),
    )

    private fun seededTopics(): List<BlockedTopic> = listOf(
        BlockedTopic(topicId = "topic-1", topicName = "离线主题一"),
        BlockedTopic(topicId = "topic-2", topicName = "离线主题二"),
    )
}
