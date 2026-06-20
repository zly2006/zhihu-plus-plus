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
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.data.CommonFeed
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.toFeedDisplayItemNavDestinationJson
import com.github.zly2006.zhihu.test.InstrumentedTestEnvironment
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.RecordingNavigator
import com.github.zly2006.zhihu.test.ZhihuMockApi
import com.github.zly2006.zhihu.test.performHorizontalSwipeCycle
import com.github.zly2006.zhihu.test.performVerticalSwipeCycle
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.seedViewModel
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.COMMENT_SCREEN_LIST_TAG
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
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.feed.QuestionFeedViewModel
import com.github.zly2006.zhihu.viewmodel.filter.BlockedUser
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.paginationEnvironment
import io.ktor.http.HttpMethod
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.github.zly2006.zhihu.shared.data.Person as FeedPerson

@RunWith(AndroidJUnit4::class)
class QuestionScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() = runBlocking {
        composeRule.resetAppPreferences()
        ZhihuMockApi.install(enabled = true)
        ZhihuMockApi.reset()
        val database = getContentFilterDatabase(composeRule.activity)
        database.blockedUserDao().clearAllUsers()
    }

    @After
    fun tearDown() = runBlocking {
        val database = getContentFilterDatabase(composeRule.activity)
        database.blockedUserDao().clearAllUsers()
        ZhihuMockApi.install(enabled = InstrumentedTestEnvironment.isMockMode())
    }

    @Test
    fun headerActionsDetailToggleSortAndDialogEntrancesRemainOffline() {
        /*
         * Expected behavior:
         * 1. The seeded title, statistics, and detail markdown must render from the mocked question
         *    detail endpoint through the production loadQuestion path.
         * 2. Tapping the detail toggle should collapse the markdown body into the preview snippet,
         *    then allow expanding back to the full content.
         * 3. Sort buttons must call the seeded ViewModel refresh path whenever the order actually
         *    changes, and the follow button must toggle through mocked production POST/DELETE calls.
         * 4. View-log, share, and comments actions should exercise the real platform/dialog entry
         *    points while staying offline through ActivityMonitor and mocked HTTP.
         */
        mockQuestionDetail()
        mockQuestionFollowActions()
        val viewModel = seedQuestionViewModel()

        setScreen()

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val webviewMonitor = instrumentation.addMonitor(WebviewActivity::class.java.name, null, false)
        try {
            composeRule.waitUntilTextExists("12 个回答  345 次浏览  7 条评论  89 人关注")
            composeRule.onNodeWithTag(QUESTION_TITLE_TAG).assertIsDisplayed()
            composeRule.onNodeWithText("离线问题标题").assertIsDisplayed()
            composeRule.onNodeWithTag(QUESTION_STATS_TAG).assertIsDisplayed()
            composeRule.onNodeWithText("12 个回答  345 次浏览  7 条评论  89 人关注").assertIsDisplayed()
            composeRule.onNodeWithTag(QUESTION_SCREEN_LIST_TAG).performScrollToNode(hasTestTag(QUESTION_DETAIL_CONTENT_TAG))
            composeRule.waitUntilTagIsDisplayed(QUESTION_DETAIL_CONTENT_TAG)
            composeRule.onNodeWithTag(QUESTION_DETAIL_CONTENT_TAG).assertIsDisplayed()

            composeRule.onNodeWithTag(QUESTION_DETAIL_TOGGLE_TAG).performClick()
            composeRule.waitUntilTagIsDisplayed(QUESTION_DETAIL_PREVIEW_TAG)
            composeRule.onNodeWithTag(QUESTION_DETAIL_PREVIEW_TAG).assertIsDisplayed()
            composeRule.onNodeWithText("离线问题详情用于 QuestionScreen instrumented test。").assertIsDisplayed()
            composeRule.onNodeWithTag(QUESTION_DETAIL_TOGGLE_TAG).performClick()
            composeRule.onNodeWithTag(QUESTION_SCREEN_LIST_TAG).performScrollToNode(hasTestTag(QUESTION_DETAIL_CONTENT_TAG))
            composeRule.waitUntilTagIsDisplayed(QUESTION_DETAIL_CONTENT_TAG)
            composeRule.onNodeWithTag(QUESTION_DETAIL_CONTENT_TAG).assertIsDisplayed()

            composeRule.onNodeWithTag(QUESTION_SORT_UPDATED_TAG).performClick()
            composeRule.onNodeWithTag(QUESTION_SORT_DEFAULT_TAG).performClick()
            assertEquals(2, viewModel.refreshCount)

            composeRule.onNodeWithTag(QUESTION_FOLLOW_BUTTON_TAG).performClick()
            composeRule.onNodeWithText("已关注").assertIsDisplayed()
            composeRule.onNodeWithTag(QUESTION_FOLLOW_BUTTON_TAG).performClick()
            composeRule.onNodeWithText("关注问题").assertIsDisplayed()
            assertEquals(1, ZhihuMockApi.requestCount(HttpMethod.Post, "questions/123456789/followers"))
            assertEquals(1, ZhihuMockApi.requestCount(HttpMethod.Delete, "questions/123456789/followers"))

            composeRule.onNodeWithTag(QUESTION_SHARE_BUTTON_TAG).performClick()
            composeRule.onNodeWithText("复制链接").assertIsDisplayed().performClick()

            composeRule.onNodeWithTag(QUESTION_VIEW_LOG_BUTTON_TAG).performClick()
            val startedActivity = instrumentation.waitForMonitorWithTimeout(webviewMonitor, 5_000)
            assertNotNull("日志按钮应打开知乎网页日志页", startedActivity)
            startedActivity?.finish()
            instrumentation.waitForIdleSync()

            composeRule.onNodeWithTag(QUESTION_COMMENTS_BUTTON_TAG).performClick()
            composeRule.onNodeWithTag(COMMENT_SCREEN_LIST_TAG).assertIsDisplayed()
        } finally {
            instrumentation.removeMonitor(webviewMonitor)
        }
    }

    @Test
    fun seededAnswerListSupportsScrollSwipesPaginationAndRowNavigationOffline() {
        /*
         * Expected behavior:
         * 1. A locally seeded answer list should render in the paginated list immediately, without
         *    waiting for QuestionFeedViewModel to fetch real answers.
         * 2. Scrolling to a deep row should keep list semantics intact, and vertical plus horizontal
         *    swipe cycles must not break the list or remove the visible seeded item.
         * 3. Reaching the lower part of the list should trigger the seeded ViewModel load-more path
         *    at least once, proving pagination can be exercised offline.
         * 4. Clicking a seeded row must navigate to its deterministic destination exactly once.
         */
        val viewModel = seedQuestionViewModel(
            itemCount = 24,
            isEnd = false,
        )
        mockQuestionDetail()
        val navigator = setScreen()

        composeRule.onNodeWithTag(QUESTION_SCREEN_LIST_TAG).assertIsDisplayed()
        composeRule
            .onNodeWithTag(QUESTION_SCREEN_LIST_TAG)
            .performScrollToNode(hasTestTag("question_feed_item_offline-question-item-18"))
        composeRule.onNodeWithTag("question_feed_item_offline-question-item-18").assertIsDisplayed()
        composeRule.onNodeWithTag(QUESTION_SCREEN_LIST_TAG).performVerticalSwipeCycle()
        composeRule.onNodeWithTag(QUESTION_SCREEN_LIST_TAG).performHorizontalSwipeCycle()
        composeRule
            .onNodeWithTag(QUESTION_SCREEN_LIST_TAG)
            .performScrollToNode(hasTestTag("question_feed_item_offline-question-item-18"))
        composeRule.onNodeWithTag("question_feed_item_offline-question-item-18").assertIsDisplayed()

        composeRule
            .onNodeWithTag(QUESTION_SCREEN_LIST_TAG)
            .performScrollToNode(hasTestTag("question_feed_item_offline-question-item-3"))
        composeRule.onNodeWithTag("question_feed_item_offline-question-item-3").performClick()

        assertTrue("Scrolling near the end should trigger the seeded load-more path", viewModel.loadMoreCount > 0)
        assertEquals(
            listOf(
                Article(
                    type = ArticleType.Answer,
                    id = 7003L,
                ),
            ),
            navigator.destinations,
        )
        val pendingNavigator = composeRule.activity.articleAnswerSwitchState.pendingNavigator
        assertEquals(7002L, pendingNavigator?.previousAnswerPreview?.article?.id)
        assertEquals(7004L, runBlocking { pendingNavigator?.loadNext()?.id })
    }

    @Test
    fun blockedUserAnswersAreRemovedFromQuestionFeedProcessing() {
        /*
         * Expected behavior:
         * 1. Question answer feeds should honor the same blocked-user switch used by the rest of the
         *    content filter stack.
         * 2. A blocked answer author should be removed before display items are created, while
         *    unblocked answers continue through the existing display mapping.
         */
        val viewModel = TestableQuestionFeedViewModel(123456789L)
        runBlocking {
            val database = getContentFilterDatabase(composeRule.activity)
            database.blockedUserDao().insertUser(BlockedUser("blocked-answer-author", "被屏蔽回答作者"))
            viewModel.processForTest(
                composeRule.activity,
                listOf(
                    seedAnswerFeed(
                        id = 1L,
                        authorId = "blocked-answer-author",
                        authorName = "被屏蔽回答作者",
                        excerpt = "这条回答不应展示",
                    ),
                    seedAnswerFeed(
                        id = 2L,
                        authorId = "allowed-answer-author",
                        authorName = "可见回答作者",
                        excerpt = "这条回答应展示",
                    ),
                ),
            )
        }

        assertEquals(listOf("可见回答作者"), viewModel.displayItems.map { it.authorName })
        assertEquals(listOf("这条回答应展示"), viewModel.displayItems.map { it.summary })
    }

    private fun setScreen(): RecordingNavigator = composeRule.setScreenContent {
        QuestionScreen(
            question = Question(questionId = 123456789L, title = "离线问题标题"),
        )
    }

    private fun seedQuestionViewModel(
        itemCount: Int = 8,
        isEnd: Boolean = true,
    ): SeededQuestionFeedViewModel {
        val viewModel = composeRule.seedViewModel<SeededQuestionFeedViewModel>(key = "question_123456789") {
            SeededQuestionFeedViewModel(123456789L, isEnd)
        }
        viewModel.addDisplayItems(seededItems(itemCount))
        return viewModel
    }

    private fun mockQuestionDetail(questionId: Long = 123456789L) {
        ZhihuMockApi.mockJsonPrefix(
            method = HttpMethod.Get,
            urlPrefix = "https://www.zhihu.com/api/v4/questions/$questionId?",
            body = ZhihuJson.json.encodeToString(seededQuestionDetail(questionId)),
        )
    }

    private fun mockQuestionFollowActions(questionId: Long = 123456789L) {
        val url = "https://www.zhihu.com/api/v4/questions/$questionId/followers"
        ZhihuMockApi.mockJson(method = HttpMethod.Post, url = url, body = "{}")
        ZhihuMockApi.mockJson(method = HttpMethod.Delete, url = url, body = "{}")
    }

    private class SeededQuestionFeedViewModel(
        questionId: Long,
        private val seededIsEnd: Boolean,
    ) : QuestionFeedViewModel(questionId) {
        var refreshCount = 0
            private set
        var loadMoreCount = 0
            private set

        override val isEnd: Boolean
            get() = seededIsEnd

        override fun refresh(environment: PaginationEnvironment) {
            refreshCount += 1
        }

        override fun loadMore(environment: PaginationEnvironment) {
            loadMoreCount += 1
        }
    }

    private fun MainActivityComposeRule.waitUntilTextExists(text: String) {
        waitUntil("Expected text $text", timeoutMillis = 5_000) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun MainActivityComposeRule.waitUntilTagIsDisplayed(tag: String) {
        waitUntil("Expected tag $tag to be displayed", timeoutMillis = 5_000) {
            runCatching {
                onNodeWithTag(tag).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private fun seededQuestionDetail(questionId: Long): DataHolder.Question = DataHolder.Question(
        type = "question",
        id = questionId,
        title = "离线问题标题",
        questionType = "normal",
        created = 1_713_456_789L,
        updatedTime = 1_713_456_999L,
        url = "https://www.zhihu.com/question/$questionId",
        answerCount = 12,
        visitCount = 345,
        commentCount = 7,
        followerCount = 89,
        detail = "<p>离线问题详情用于 QuestionScreen instrumented test。</p>",
        relationship = DataHolder.QuestionRelationship(isFollowing = false),
        topics = emptyList(),
        author = DataHolder.Author(
            avatarUrl = "",
            gender = 0,
            headline = "离线提问者简介",
            id = "question-author-id",
            isAdvertiser = false,
            isOrg = false,
            name = "离线提问者",
            type = "people",
            url = "https://www.zhihu.com/people/question-author-token",
            urlToken = "question-author-token",
            userType = "people",
        ),
        voteupCount = 0,
    )

    private fun seededItems(count: Int): List<FeedDisplayItem> = List(count) { index ->
        val id = index + 1L
        FeedDisplayItem(
            title = "离线回答条目 $id",
            summary = "这是第 $id 条离线回答摘要。",
            details = "离线作者 $id · 赞同 ${(id * 3)}",
            feed = null,
            navDestinationJson = Article(
                type = ArticleType.Answer,
                id = 7000L + id,
            ).toFeedDisplayItemNavDestinationJson(),
            authorName = "离线作者 $id",
            localFeedId = "offline-question-item-$id",
        )
    }

    private class TestableQuestionFeedViewModel(
        questionId: Long,
    ) : QuestionFeedViewModel(questionId) {
        suspend fun processForTest(context: android.content.Context, data: List<Feed>) {
            processResponse(paginationEnvironment(context), data, JsonArray(emptyList()))
        }
    }

    private fun seedAnswerFeed(
        id: Long,
        authorId: String,
        authorName: String,
        excerpt: String,
    ): Feed = CommonFeed(
        id = "answer-feed-$id",
        target = Feed.AnswerTarget(
            id = id,
            url = "https://www.zhihu.com/answer/$id",
            author = FeedPerson(
                id = authorId,
                url = "https://www.zhihu.com/people/$authorId",
                userType = "people",
                urlToken = "$authorId-token",
                name = authorName,
                headline = "$authorName 的离线签名",
                avatarUrl = "https://example.invalid/avatar/$authorId.png",
            ),
            voteupCount = 10,
            commentCount = 1,
            question = Feed.QuestionTarget(
                id = 123456789L,
                _title = "离线问题标题",
                url = "https://www.zhihu.com/question/123456789",
                type = "question",
            ),
            excerpt = excerpt,
        ),
    )
}
