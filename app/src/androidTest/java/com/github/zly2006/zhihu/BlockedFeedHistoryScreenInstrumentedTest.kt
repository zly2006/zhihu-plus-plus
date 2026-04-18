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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.subscreens.BlockedFeedHistoryScreen
import com.github.zly2006.zhihu.viewmodel.filter.BlockedFeedRecord
import com.github.zly2006.zhihu.viewmodel.filter.BlockedFeedRecordDao
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlockedFeedHistoryScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    private val blockedFeedRecordDao: BlockedFeedRecordDao
        get() = ContentFilterDatabase.getDatabase(composeRule.activity).blockedFeedRecordDao()

    @Before
    fun setUp() = runBlocking {
        composeRule.resetAppPreferences()
        blockedFeedRecordDao.clearAll()
    }

    @After
    fun tearDown() = runBlocking {
        blockedFeedRecordDao.clearAll()
    }

    @Test
    fun emptyHistoryShowsPlaceholderAndBackButtonStillWorks() {
        // Start from a fully empty local database so the screen must render the placeholder copy
        // and hide destructive actions. This keeps the test independent from account state,
        // network state, and any records created by previous runs.
        val navigator = composeRule.setScreenContent {
            BlockedFeedHistoryScreen()
        }

        composeRule.onNodeWithText("暂无屏蔽记录").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("清空记录").assertDoesNotExist()

        // Even without history rows, the screen is opened from settings and must always let the
        // user navigate back deterministically with one tap on the top app bar button.
        composeRule.onNodeWithContentDescription("返回").performClick()
        composeRule.runOnIdle {
            assertEquals(1, navigator.backCount)
        }
    }

    @Test
    fun seededHistorySupportsDeterministicScrollClickDeleteAndClearFlows() {
        // Seed only local Room data with fixed timestamps so row order is deterministic and the
        // test can verify scrolling and click handling without depending on remote feeds or login.
        val seededHistory = seedHistory()
        val navigator = composeRule.setScreenContent {
            BlockedFeedHistoryScreen()
        }

        // The newest record intentionally has a blank title and no navigation target. The screen
        // should render the fallback title, allow tapping the row, and still avoid emitting any
        // navigation event because default/empty record data cannot be opened anywhere.
        composeRule.onNodeWithText("（无标题）").assertIsDisplayed()
        composeRule.onNodeWithTag(itemTag(seededHistory.placeholderRecordId)).performClick()
        composeRule.runOnIdle {
            assertEquals(emptyList<NavDestination>(), navigator.destinations)
        }

        // The target row is seeded deep in the list so the test exercises real lazy list scrolling
        // before clicking. After the row is brought into view, tapping it must navigate to the
        // exact serialized destination stored in the history record.
        composeRule
            .onNodeWithTag(LIST_TAG)
            .performScrollToNode(hasTestTag(itemTag(seededHistory.navigableRecordId)))
        composeRule.onNodeWithTag(itemTag(seededHistory.navigableRecordId)).assertIsDisplayed()
        composeRule.onNodeWithTag(itemTag(seededHistory.navigableRecordId)).performClick()
        composeRule.runOnIdle {
            assertEquals(listOf(seededHistory.expectedDestination), navigator.destinations)
        }

        // Deleting a single row should remove only that record from both the database-backed list
        // and the rendered UI, while keeping the rest of the seeded history intact.
        composeRule.onNodeWithTag(deleteTag(seededHistory.deletableRecordId)).performClick()
        waitForRecordToDisappear(seededHistory.deletableRecordId)
        composeRule.onNodeWithText(seededHistory.deletableTitle).assertDoesNotExist()
        composeRule.onNodeWithText(seededHistory.survivingTitle).assertExists()

        // The clear-all confirmation dialog should be reversible on cancel and destructive only on
        // explicit confirmation, so the test exercises both dialog buttons in sequence.
        composeRule.onNodeWithContentDescription("清空记录").performClick()
        composeRule.onNodeWithText("确定要清空所有屏蔽记录吗？此操作不可撤销。").assertIsDisplayed()
        composeRule.onNodeWithText("取消").performClick()
        composeRule.onNodeWithText(seededHistory.survivingTitle).assertExists()

        composeRule.onNodeWithContentDescription("清空记录").performClick()
        composeRule.onNodeWithText("清空").performClick()
        composeRule.onNodeWithText("暂无屏蔽记录").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("清空记录").assertDoesNotExist()
    }

    private fun seedHistory(): SeededHistory {
        val destination = Question(questionId = 4242, title = "被屏蔽的问题")
        val baseTime = 1_710_000_000_000L
        var currentTime = baseTime

        return runBlocking {
            val placeholderRecordId = blockedFeedRecordDao.insert(
                BlockedFeedRecord(
                    title = "",
                    questionId = null,
                    authorName = null,
                    authorId = null,
                    url = null,
                    content = null,
                    blockedReason = "默认占位记录",
                    navDestinationJson = null,
                    feedJson = null,
                    blockedTime = currentTime--,
                ),
            )

            repeat(10) { index ->
                blockedFeedRecordDao.insert(
                    BlockedFeedRecord(
                        title = "填充记录 ${index + 1}",
                        questionId = null,
                        authorName = "作者 ${index + 1}",
                        authorId = "author-$index",
                        url = null,
                        content = null,
                        blockedReason = "用于滚动验证",
                        navDestinationJson = null,
                        feedJson = null,
                        blockedTime = currentTime--,
                    ),
                )
            }

            val navigableRecordId = blockedFeedRecordDao.insert(
                BlockedFeedRecord(
                    title = "可点击跳转记录",
                    questionId = destination.questionId,
                    authorName = "可导航作者",
                    authorId = "navigable-author",
                    url = null,
                    content = null,
                    blockedReason = "带导航目标",
                    navDestinationJson = Json.encodeToString<NavDestination>(destination),
                    feedJson = null,
                    blockedTime = currentTime--,
                ),
            )

            val survivingTitle = "保留到清空前的记录"
            blockedFeedRecordDao.insert(
                BlockedFeedRecord(
                    title = survivingTitle,
                    questionId = null,
                    authorName = "保留作者",
                    authorId = "survivor",
                    url = null,
                    content = null,
                    blockedReason = "用于验证取消清空",
                    navDestinationJson = null,
                    feedJson = null,
                    blockedTime = currentTime--,
                ),
            )

            val deletableTitle = "待删除记录"
            val deletableRecordId = blockedFeedRecordDao.insert(
                BlockedFeedRecord(
                    title = deletableTitle,
                    questionId = null,
                    authorName = "待删除作者",
                    authorId = "delete-me",
                    url = null,
                    content = null,
                    blockedReason = "用于删除验证",
                    navDestinationJson = null,
                    feedJson = null,
                    blockedTime = currentTime,
                ),
            )

            SeededHistory(
                placeholderRecordId = placeholderRecordId,
                navigableRecordId = navigableRecordId,
                deletableRecordId = deletableRecordId,
                deletableTitle = deletableTitle,
                survivingTitle = survivingTitle,
                expectedDestination = destination,
            )
        }
    }

    private fun waitForRecordToDisappear(recordId: Long) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasTestTag(itemTag(recordId))).fetchSemanticsNodes().isEmpty()
        }
    }

    private data class SeededHistory(
        val placeholderRecordId: Long,
        val navigableRecordId: Long,
        val deletableRecordId: Long,
        val deletableTitle: String,
        val survivingTitle: String,
        val expectedDestination: Question,
    )

    private companion object {
        const val LIST_TAG = "blocked_feed_history_list"

        fun itemTag(recordId: Long) = "blocked_feed_history_item_$recordId"

        fun deleteTag(recordId: Long) = "blocked_feed_history_delete_$recordId"
    }
}
