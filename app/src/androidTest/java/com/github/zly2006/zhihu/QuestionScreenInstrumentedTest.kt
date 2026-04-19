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
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.RecordingNavigator
import com.github.zly2006.zhihu.test.performHorizontalSwipeCycle
import com.github.zly2006.zhihu.test.performVerticalSwipeCycle
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.QUESTION_COMMENTS_BUTTON_TAG
import com.github.zly2006.zhihu.ui.QUESTION_DETAIL_CONTENT_TAG
import com.github.zly2006.zhihu.ui.QUESTION_DETAIL_PREVIEW_TAG
import com.github.zly2006.zhihu.ui.QUESTION_DETAIL_TOGGLE_TAG
import com.github.zly2006.zhihu.ui.QUESTION_FOLLOW_BUTTON_TAG
import com.github.zly2006.zhihu.ui.QUESTION_SCREEN_LIST_TAG
import com.github.zly2006.zhihu.ui.QUESTION_SHARE_BUTTON_TAG
import com.github.zly2006.zhihu.ui.QUESTION_SORT_DEFAULT_TAG
import com.github.zly2006.zhihu.ui.QUESTION_SORT_UPDATED_TAG
import com.github.zly2006.zhihu.ui.QUESTION_STATS_TAG
import com.github.zly2006.zhihu.ui.QUESTION_TITLE_TAG
import com.github.zly2006.zhihu.ui.QUESTION_VIEW_LOG_BUTTON_TAG
import com.github.zly2006.zhihu.ui.QuestionScreen
import com.github.zly2006.zhihu.ui.QuestionScreenTestOverrides
import com.github.zly2006.zhihu.ui.QuestionScreenUiState
import com.github.zly2006.zhihu.ui.questionFeedItemTag
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.QuestionFeedViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuestionScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
    }

    @Test
    fun headerActionsDetailToggleSortAndDialogEntrancesRemainOffline() {
        /*
         * Expected behavior:
         * 1. The seeded title, statistics, and detail markdown must render entirely from injected
         *    local state without loading question detail from the network.
         * 2. Tapping the detail toggle should collapse the markdown body into the preview snippet,
         *    then allow expanding back to the full content.
         * 3. Sort buttons must call the injected refresh callback whenever the order actually
         *    changes, and the follow button must toggle between follow and unfollow states through
         *    the injected follow callback.
         * 4. View-log, share, and comments actions should all route through injected offline hooks
         *    and custom test content rather than opening real webviews or bottom sheets.
         */
        val followStates = mutableListOf<Boolean>()
        var refreshCount = 0
        var openLogCount = 0
        var shareCount = 0
        val overrides = createQuestionOverrides(
            onRefreshAnswers = { refreshCount++ },
            onFollowQuestion = { followStates += it },
            onOpenLog = { openLogCount++ },
            onShareAction = { shareCount++ },
        )

        setScreen(overrides)

        composeRule.onNodeWithTag(QUESTION_TITLE_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("离线问题标题").assertIsDisplayed()
        composeRule.onNodeWithTag(QUESTION_STATS_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("12 个回答  345 次浏览  7 条评论  89 人关注").assertIsDisplayed()
        composeRule.onNodeWithTag(QUESTION_DETAIL_CONTENT_TAG).assertIsDisplayed()

        composeRule.onNodeWithTag(QUESTION_DETAIL_TOGGLE_TAG).performClick()
        composeRule.onNodeWithTag(QUESTION_DETAIL_PREVIEW_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("离线问题详情用于 QuestionScreen instrumented test。").assertIsDisplayed()
        composeRule.onNodeWithTag(QUESTION_DETAIL_TOGGLE_TAG).performClick()
        composeRule.onNodeWithTag(QUESTION_DETAIL_CONTENT_TAG).assertIsDisplayed()

        composeRule.onNodeWithTag(QUESTION_SORT_UPDATED_TAG).performClick()
        composeRule.onNodeWithTag(QUESTION_SORT_DEFAULT_TAG).performClick()
        assertEquals(2, refreshCount)

        composeRule.onNodeWithTag(QUESTION_FOLLOW_BUTTON_TAG).performClick()
        composeRule.onNodeWithText("已关注").assertIsDisplayed()
        composeRule.onNodeWithTag(QUESTION_FOLLOW_BUTTON_TAG).performClick()
        composeRule.onNodeWithText("关注问题").assertIsDisplayed()
        assertEquals(listOf(true, false), followStates)

        composeRule.onNodeWithTag(QUESTION_VIEW_LOG_BUTTON_TAG).performClick()
        composeRule.onNodeWithTag(QUESTION_SHARE_BUTTON_TAG).performClick()
        composeRule.onNodeWithText("离线分享面板").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag(QUESTION_COMMENTS_BUTTON_TAG).performClick()
        composeRule.onNodeWithText("离线评论面板").assertIsDisplayed().performClick()

        assertEquals(1, openLogCount)
        assertEquals(1, shareCount)
    }

    @Test
    fun seededAnswerListSupportsScrollSwipesPaginationAndRowNavigationOffline() {
        /*
         * Expected behavior:
         * 1. A locally seeded answer list should render in the paginated list immediately, without
         *    waiting for QuestionFeedViewModel to fetch real answers.
         * 2. Scrolling to a deep row should keep list semantics intact, and vertical plus horizontal
         *    swipe cycles must not break the list or remove the visible seeded item.
         * 3. Reaching the lower part of the list should trigger the injected load-more callback at
         *    least once, proving pagination can be exercised offline.
         * 4. Clicking a seeded row must navigate to its deterministic destination exactly once.
         */
        var loadMoreCount = 0
        val navigator = setScreen(
            createQuestionOverrides(
                itemCount = 24,
                isEnd = false,
                onLoadMore = { loadMoreCount++ },
            ),
        )

        composeRule.onNodeWithTag(QUESTION_SCREEN_LIST_TAG).assertIsDisplayed()
        composeRule
            .onNodeWithTag(QUESTION_SCREEN_LIST_TAG)
            .performScrollToNode(hasTestTag(questionFeedItemTag("offline-question-item-18")))
        composeRule.onNodeWithTag(questionFeedItemTag("offline-question-item-18")).assertIsDisplayed()
        composeRule.onNodeWithTag(QUESTION_SCREEN_LIST_TAG).performVerticalSwipeCycle()
        composeRule.onNodeWithTag(QUESTION_SCREEN_LIST_TAG).performHorizontalSwipeCycle()
        composeRule
            .onNodeWithTag(QUESTION_SCREEN_LIST_TAG)
            .performScrollToNode(hasTestTag(questionFeedItemTag("offline-question-item-18")))
        composeRule.onNodeWithTag(questionFeedItemTag("offline-question-item-18")).assertIsDisplayed()

        composeRule
            .onNodeWithTag(QUESTION_SCREEN_LIST_TAG)
            .performScrollToNode(hasTestTag(questionFeedItemTag("offline-question-item-3")))
        composeRule.onNodeWithTag(questionFeedItemTag("offline-question-item-3")).performClick()

        assertTrue("Scrolling near the end should trigger the offline load-more seam", loadMoreCount > 0)
        assertEquals(
            listOf(
                Article(
                    type = ArticleType.Answer,
                    id = 7003L,
                ),
            ),
            navigator.destinations,
        )
    }

    private fun setScreen(overrides: QuestionScreenTestOverrides): RecordingNavigator = composeRule.setScreenContent {
        QuestionScreen(
            question = Question(questionId = 123456789L, title = "离线问题标题"),
            testOverrides = overrides,
        )
    }

    private fun createQuestionOverrides(
        itemCount: Int = 8,
        isEnd: Boolean = true,
        onRefreshAnswers: (() -> Unit)? = null,
        onLoadMore: (() -> Unit)? = null,
        onFollowQuestion: ((Boolean) -> Unit)? = null,
        onOpenLog: (() -> Unit)? = null,
        onShareAction: (() -> Unit)? = null,
    ): QuestionScreenTestOverrides {
        val viewModel = QuestionFeedViewModel(123456789L)
        viewModel.addDisplayItems(seededItems(itemCount))
        return QuestionScreenTestOverrides(
            viewModel = viewModel,
            initialUiState = QuestionScreenUiState(
                questionContent = "<p>离线问题详情用于 QuestionScreen instrumented test。</p>",
                answerCount = 12,
                visitCount = 345,
                commentCount = 7,
                followerCount = 89,
                title = "离线问题标题",
                isFollowing = false,
                isQuestionDetailExpanded = true,
            ),
            isEnd = isEnd,
            onRefreshAnswers = onRefreshAnswers,
            onLoadMore = onLoadMore,
            onFollowQuestion = onFollowQuestion,
            onOpenLog = onOpenLog,
            onShareAction = onShareAction,
            commentSheetContent = { onDismiss ->
                Text(
                    text = "离线评论面板",
                    modifier = Modifier.clickable { onDismiss() },
                )
            },
            shareDialogContent = { onDismissRequest ->
                Text(
                    text = "离线分享面板",
                    modifier = Modifier.clickable { onDismissRequest() },
                )
            },
        )
    }

    private fun seededItems(count: Int): List<BaseFeedViewModel.FeedDisplayItem> = List(count) { index ->
        val id = index + 1L
        BaseFeedViewModel.FeedDisplayItem(
            title = "离线回答条目 $id",
            summary = "这是第 $id 条离线回答摘要。",
            details = "离线作者 $id · 赞同 ${(id * 3)}",
            feed = null,
            navDestination = Article(
                type = ArticleType.Answer,
                id = 7000L + id,
            ),
            authorName = "离线作者 $id",
            localFeedId = "offline-question-item-$id",
        )
    }
}
