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

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.CommentHolder
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.RecordingNavigator
import com.github.zly2006.zhihu.test.ZhihuMockApi
import com.github.zly2006.zhihu.test.performHorizontalSwipeCycle
import com.github.zly2006.zhihu.test.performVerticalSwipeCycle
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.COMMENT_CANCEL_REPLY_TAG
import com.github.zly2006.zhihu.ui.COMMENT_IMAGE_MENU_BROWSER_TAG
import com.github.zly2006.zhihu.ui.COMMENT_IMAGE_MENU_OPEN_TAG
import com.github.zly2006.zhihu.ui.COMMENT_IMAGE_MENU_SAVE_TAG
import com.github.zly2006.zhihu.ui.COMMENT_IMAGE_MENU_SHARE_TAG
import com.github.zly2006.zhihu.ui.COMMENT_INPUT_TAG
import com.github.zly2006.zhihu.ui.COMMENT_REPLY_BANNER_TAG
import com.github.zly2006.zhihu.ui.COMMENT_SCREEN_LIST_TAG
import com.github.zly2006.zhihu.ui.COMMENT_SEND_BUTTON_TAG
import com.github.zly2006.zhihu.ui.COMMENT_SORT_SCORE_TAG
import com.github.zly2006.zhihu.ui.COMMENT_SORT_TIME_TAG
import com.github.zly2006.zhihu.ui.CommentImageMenuAction
import com.github.zly2006.zhihu.ui.CommentScreen
import com.github.zly2006.zhihu.ui.CommentScreenTestOverrides
import com.github.zly2006.zhihu.ui.commentAuthorTag
import com.github.zly2006.zhihu.ui.commentChildButtonTag
import com.github.zly2006.zhihu.ui.commentImageTag
import com.github.zly2006.zhihu.ui.commentLikeButtonTag
import com.github.zly2006.zhihu.ui.commentReplyButtonTag
import com.github.zly2006.zhihu.ui.commentReplyToAuthorTag
import com.github.zly2006.zhihu.ui.commentRowTag
import com.github.zly2006.zhihu.viewmodel.CommentItem
import com.github.zly2006.zhihu.viewmodel.comment.BaseCommentViewModel
import com.github.zly2006.zhihu.viewmodel.comment.CommentSortOrder
import io.ktor.client.HttpClient
import io.ktor.http.HttpMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommentScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
        ZhihuMockApi.mockJsonPrefix(
            method = HttpMethod.Post,
            urlPrefix = "https://www.zhihu.com/api/v4/comments/",
            body = "{}",
        )
        ZhihuMockApi.mockJsonPrefix(
            method = HttpMethod.Delete,
            urlPrefix = "https://www.zhihu.com/api/v4/comments/",
            body = "{}",
        )
    }

    @Test
    fun rootCommentsSupportOfflineSortingScrollingSwipesAndClickableRows() {
        /*
         * Expected behavior:
         * 1. A seeded root-comment list must render entirely from the injected fake ViewModel, so
         *    sort changes, pagination thresholds, and row gestures stay deterministic offline.
         * 2. Tapping both sort chips should update the fake ViewModel state and invoke refresh only
         *    when the order actually changes, preserving the production sort contract.
         * 3. Vertical and horizontal list swipe cycles plus a deep scroll should keep the seeded row
         *    visible, and scrolling near the tail must trigger the offline load-more seam.
         * 4. The obvious row actions on the stable seam must all work locally: right-swipe archive,
         *    left-swipe reply-entry, author / reply-to-author navigation, reply button, child-list
         *    button, and like button.
         */
        val archivedCommentIds = mutableListOf<String>()
        val childEntryCommentIds = mutableListOf<String>()
        val seededComments = seedRootComments(count = 24)
        val viewModel = SeededRootCommentViewModel(
            article = ROOT_ARTICLE,
            seededComments = seededComments,
        )

        val navigator = setCommentScreen(
            viewModel = viewModel,
            onChildCommentClick = { childEntryCommentIds += it.item.id },
            testOverrides = CommentScreenTestOverrides(
                viewModel = viewModel,
                skipInitialLoad = true,
                onArchiveComment = { archivedCommentIds += it.item.id },
            ),
        )

        composeRule.onNodeWithTag(COMMENT_SCREEN_LIST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(COMMENT_SORT_TIME_TAG).performClick()
        composeRule.onNodeWithTag(COMMENT_SORT_SCORE_TAG).performClick()
        assertEquals(listOf(CommentSortOrder.TIME, CommentSortOrder.SCORE), viewModel.refreshHistory)

        composeRule
            .onNodeWithTag(COMMENT_SCREEN_LIST_TAG)
            .performScrollToNode(hasTestTag(commentRowTag("root-20")))
        composeRule.onNodeWithTag(commentRowTag("root-20")).assertIsDisplayed()
        composeRule.onNodeWithTag(COMMENT_SCREEN_LIST_TAG).performVerticalSwipeCycle()
        composeRule.onNodeWithTag(COMMENT_SCREEN_LIST_TAG).performHorizontalSwipeCycle()
        composeRule
            .onNodeWithTag(COMMENT_SCREEN_LIST_TAG)
            .performScrollToNode(hasTestTag(commentRowTag("root-20")))
        composeRule.onNodeWithTag(commentRowTag("root-20")).assertIsDisplayed()
        assertTrue(viewModel.loadMoreCount > 0)

        composeRule
            .onNodeWithTag(COMMENT_SCREEN_LIST_TAG)
            .performScrollToNode(hasTestTag(commentRowTag("root-1")))
        composeRule.onNodeWithTag(commentRowTag("root-1")).assertIsDisplayed()
        composeRule.onNodeWithTag(commentRowTag("root-1")).performTouchInput { swipeRight() }
        composeRule.waitForIdle()
        composeRule
            .onNodeWithTag(COMMENT_SCREEN_LIST_TAG)
            .performScrollToNode(hasTestTag(commentRowTag("root-2")))
        composeRule.onNodeWithTag(commentRowTag("root-2")).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(commentAuthorTag("root-1")).performClick()
        composeRule.onNodeWithTag(commentReplyToAuthorTag("root-2")).performClick()
        composeRule.onNodeWithTag(commentReplyButtonTag("root-1")).performClick()
        composeRule.onNodeWithTag(commentChildButtonTag("root-1")).performClick()
        composeRule.onNodeWithTag(commentLikeButtonTag("root-1")).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            seededComments.first().likeCount == 6 && seededComments.first().liked
        }

        assertEquals(listOf("root-1"), archivedCommentIds)
        assertEquals(listOf("root-2", "root-1"), childEntryCommentIds)
        assertEquals(
            listOf(
                Person(id = "author-root-1", urlToken = "author-root-1-token", name = "离线作者 1"),
                Person(id = "reply-to-root-2", urlToken = "reply-to-root-2-token", name = "被回复作者 2"),
            ),
            navigator.destinations,
        )
    }

    @Test
    fun commentImageMenuSupportsCancelAndAllStableOfflineActions() {
        /*
         * Expected behavior:
         * 1. Long-pressing the seeded comment image should always open the stable context menu.
         * 2. Pressing system back while the menu is open should dismiss only the menu, which is the
         *    cancel path available in this offline seam.
         * 3. Reopening the menu and choosing each action should invoke the injected image handler in
         *    deterministic order instead of starting real dialogs, intents, or storage writes.
         */
        val imageActions = mutableListOf<CommentImageMenuAction>()
        val viewModel = SeededRootCommentViewModel(
            article = ROOT_ARTICLE,
            seededComments = seedRootComments(count = 4),
        )

        setCommentScreen(
            viewModel = viewModel,
            testOverrides = CommentScreenTestOverrides(
                viewModel = viewModel,
                skipInitialLoad = true,
                onImageMenuAction = { action, _ -> imageActions += action },
            ),
        )

        composeRule
            .onNodeWithTag(COMMENT_SCREEN_LIST_TAG)
            .performScrollToNode(hasTestTag(commentImageTag("root-1")))
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag(commentImageTag("root-1"), useUnmergedTree = true).assertIsDisplayed()
            }.isSuccess
        }
        composeRule
            .onNodeWithTag(commentImageTag("root-1"), useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.OnLongClick)
        composeRule.onNodeWithTag(COMMENT_IMAGE_MENU_OPEN_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag(COMMENT_IMAGE_MENU_BROWSER_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag(COMMENT_IMAGE_MENU_SAVE_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag(COMMENT_IMAGE_MENU_SHARE_TAG, useUnmergedTree = true).assertIsDisplayed()

        pressBack()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(COMMENT_IMAGE_MENU_OPEN_TAG, useUnmergedTree = true).assertCountEquals(0)

        composeRule.onNodeWithTag(commentImageTag("root-1"), useUnmergedTree = true).performClick()
        composeRule
            .onNodeWithTag(commentImageTag("root-1"), useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.OnLongClick)
        composeRule.onNodeWithTag(COMMENT_IMAGE_MENU_OPEN_TAG, useUnmergedTree = true).performClick()
        composeRule
            .onNodeWithTag(commentImageTag("root-1"), useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.OnLongClick)
        composeRule.onNodeWithTag(COMMENT_IMAGE_MENU_BROWSER_TAG, useUnmergedTree = true).performClick()
        composeRule
            .onNodeWithTag(commentImageTag("root-1"), useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.OnLongClick)
        composeRule.onNodeWithTag(COMMENT_IMAGE_MENU_SAVE_TAG, useUnmergedTree = true).performClick()
        composeRule
            .onNodeWithTag(commentImageTag("root-1"), useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.OnLongClick)
        composeRule.onNodeWithTag(COMMENT_IMAGE_MENU_SHARE_TAG, useUnmergedTree = true).performClick()

        assertEquals(
            listOf(
                CommentImageMenuAction.Open,
                CommentImageMenuAction.Open,
                CommentImageMenuAction.OpenInBrowser,
                CommentImageMenuAction.Save,
                CommentImageMenuAction.Share,
            ),
            imageActions,
        )
    }

    @Test
    fun childCommentViewSupportsReplyCancelAndOfflineSendFlow() {
        /*
         * Expected behavior:
         * 1. In child-comment mode, swiping left on a reply row should enter reply state locally and
         *    show the reply banner plus reply-specific placeholder text.
         * 2. The explicit cancel button must clear that reply state without mutating the seeded
         *    replies, restoring the default placeholder.
         * 3. Entering reply mode again, typing text, and sending should add the locally generated
         *    reply to the injected fake ViewModel, clear the reply banner, and reset the input.
         */
        val viewModel = SeededChildCommentViewModel(
            content = CommentHolder(commentId = "root-parent", article = ROOT_ARTICLE),
            seededComments = seedChildComments(count = 3),
        )
        val activeCommentItem = CommentItem(
            item = seedRootComment(index = 99, childCommentCount = 3),
            clickTarget = null,
        )

        setCommentScreen(
            viewModel = viewModel,
            activeCommentItem = activeCommentItem,
            testOverrides = CommentScreenTestOverrides(
                viewModel = viewModel,
                skipInitialLoad = true,
            ),
        )

        composeRule.onNodeWithTag(commentRowTag("child-1")).performTouchInput { swipeLeft() }
        composeRule.onNodeWithTag(COMMENT_REPLY_BANNER_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("回复 子回复作者 1").assertIsDisplayed()
        composeRule.onNodeWithText("回复 子回复作者 1...").assertIsDisplayed()

        composeRule.onNodeWithTag(COMMENT_CANCEL_REPLY_TAG).performClick()
        composeRule.onAllNodesWithTag(COMMENT_REPLY_BANNER_TAG).assertCountEquals(0)
        composeRule.onNodeWithText("写下你的评论...").assertIsDisplayed()

        composeRule.onNodeWithTag(commentRowTag("child-1")).performTouchInput { swipeLeft() }
        composeRule.onNodeWithTag(COMMENT_INPUT_TAG).performTextInput("离线发送的回复")
        composeRule.onNodeWithTag(COMMENT_SEND_BUTTON_TAG).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.submissions.size == 1
        }

        composeRule.onNodeWithText("离线发送的回复").assertIsDisplayed()
        composeRule.onAllNodesWithTag(COMMENT_REPLY_BANNER_TAG).assertCountEquals(0)
        composeRule.onNodeWithText("回复 子回复作者 1...").assertDoesNotExist()
        composeRule.onNodeWithText("写下你的评论...").assertIsDisplayed()
        assertEquals(listOf(SeededChildCommentViewModel.Submission("离线发送的回复", "child-1")), viewModel.submissions)
    }

    private fun setCommentScreen(
        viewModel: BaseCommentViewModel,
        activeCommentItem: CommentItem? = null,
        onChildCommentClick: (CommentItem) -> Unit = {},
        testOverrides: CommentScreenTestOverrides,
    ): RecordingNavigator = composeRule.setScreenContent {
        CommentScreen(
            httpClient = httpClient(),
            content = { viewModel.article },
            activeCommentItem = activeCommentItem,
            onChildCommentClick = onChildCommentClick,
            testOverrides = testOverrides,
        )
    }

    private fun httpClient(): HttpClient = AccountData.httpClient(composeRule.activity)

    private class SeededRootCommentViewModel(
        article: NavDestination,
        seededComments: List<DataHolder.Comment>,
    ) : BaseCommentViewModel(article) {
        override val initialUrl: String = "https://example.invalid/root_comments"
        var loadMoreCount = 0
            private set
        val refreshHistory = mutableListOf<CommentSortOrder>()

        init {
            allData.addAll(seededComments)
            seededComments.forEach { comment ->
                commentsMap[comment.id] = createCommentItem(comment, article)
                comment.childComments.forEach { child ->
                    commentsMap[child.id] = CommentItem(child, null)
                }
            }
        }

        override fun createCommentItem(comment: DataHolder.Comment, article: NavDestination): CommentItem =
            CommentItem(comment, CommentHolder(comment.id, article))

        override fun loadMore(context: android.content.Context) {
            loadMoreCount += 1
        }

        override fun refresh(context: android.content.Context) {
            refreshHistory += sortOrder
        }

        override fun submitComment(
            content: NavDestination,
            commentText: String,
            httpClient: HttpClient,
            context: android.content.Context,
            replyToCommentId: String?,
            onSuccess: () -> Unit,
        ) = Unit
    }

    private class SeededChildCommentViewModel(
        content: CommentHolder,
        seededComments: List<DataHolder.Comment>,
    ) : BaseCommentViewModel(content) {
        data class Submission(
            val text: String,
            val replyToCommentId: String?,
        )

        override val initialUrl: String = "https://example.invalid/child_comments"
        val submissions = mutableListOf<Submission>()

        init {
            allData.addAll(seededComments)
            seededComments.forEach { comment ->
                commentsMap[comment.id] = CommentItem(comment, null)
            }
        }

        override fun createCommentItem(comment: DataHolder.Comment, article: NavDestination): CommentItem =
            CommentItem(comment, null)

        override fun loadMore(context: android.content.Context) = Unit

        override fun submitComment(
            content: NavDestination,
            commentText: String,
            httpClient: HttpClient,
            context: android.content.Context,
            replyToCommentId: String?,
            onSuccess: () -> Unit,
        ) {
            submissions += Submission(commentText, replyToCommentId)
            allData.add(
                0,
                DataHolder.Comment(
                    id = "child-submitted-${submissions.size}",
                    type = "comment",
                    resourceType = "answer",
                    url = "https://www.zhihu.com/comment/child-submitted-${submissions.size}",
                    content = "<p>$commentText</p>",
                    createdTime = 1_713_500_000L,
                    isDelete = false,
                    collapsed = false,
                    reviewing = false,
                    liked = false,
                    likeCount = 0,
                    isAuthor = false,
                    author = DataHolder.Comment.Author(
                        id = "submitted-author-${submissions.size}",
                        urlToken = "submitted-author-${submissions.size}-token",
                        name = "当前用户",
                        avatarUrl = "https://example.invalid/avatar/submitted-${submissions.size}.png",
                        avatarUrlTemplate = "",
                        isOrg = false,
                        type = "people",
                        url = "https://www.zhihu.com/people/submitted-author-${submissions.size}-token",
                        userType = "people",
                        headline = "当前用户的离线签名",
                        gender = 0,
                        isAdvertiser = false,
                    ),
                    replyToAuthor = null,
                    childCommentCount = 0,
                    childComments = emptyList(),
                ),
            )
            onSuccess()
        }
    }

    private fun seedRootComments(count: Int): List<DataHolder.Comment> = List(count) { index ->
        when (index) {
            0 -> seedRootComment(index = 1, childCommentCount = 2, withImage = true)
            1 -> seedRootComment(index = 2, replyToAuthor = seedAuthor("reply-to-root-2", "reply-to-root-2-token", "被回复作者 2"))
            else -> seedRootComment(index = index + 1)
        }
    }

    private fun seedChildComments(count: Int): List<DataHolder.Comment> = List(count) { index ->
        seedComment(
            id = "child-${index + 1}",
            authorId = "child-author-${index + 1}",
            authorName = "子回复作者 ${index + 1}",
            content = "子回复内容 ${index + 1}",
            likeCount = index + 1,
        )
    }

    private fun seedRootComment(
        index: Int,
        childCommentCount: Int = 0,
        withImage: Boolean = false,
        replyToAuthor: DataHolder.Comment.Author? = null,
    ): DataHolder.Comment = seedComment(
        id = "root-$index",
        authorId = "author-root-$index",
        authorName = "离线作者 $index",
        content = if (withImage) {
            "<p>根评论内容 $index</p><a class=\"comment_img\" href=\"https://example.invalid/comment-$index.jpg\">image</a>"
        } else {
            "根评论内容 $index"
        },
        likeCount = if (index == 1) 5 else index,
        childCommentCount = childCommentCount,
        childComments = if (childCommentCount > 0) {
            listOf(
                seedComment(
                    id = "root-$index-child-1",
                    authorId = "root-$index-child-author-1",
                    authorName = "子评论作者 1",
                    content = "内嵌子评论 1",
                    likeCount = 1,
                ),
            )
        } else {
            emptyList()
        },
        replyToAuthor = replyToAuthor,
    )

    private fun seedComment(
        id: String,
        authorId: String,
        authorName: String,
        content: String,
        likeCount: Int = 0,
        childCommentCount: Int = 0,
        childComments: List<DataHolder.Comment> = emptyList(),
        replyToAuthor: DataHolder.Comment.Author? = null,
    ): DataHolder.Comment = DataHolder.Comment(
        id = id,
        type = "comment",
        resourceType = "answer",
        url = "https://www.zhihu.com/comment/$id",
        content = if (content.trimStart().startsWith("<")) content else "<p>$content</p>",
        createdTime = 1_713_500_000L,
        isDelete = false,
        collapsed = false,
        reviewing = false,
        liked = false,
        likeCount = likeCount,
        isAuthor = false,
        author = seedAuthor(authorId, "$authorId-token", authorName),
        replyToAuthor = replyToAuthor,
        childCommentCount = childCommentCount,
        childComments = childComments,
    )

    private fun seedAuthor(id: String, urlToken: String, name: String): DataHolder.Comment.Author = DataHolder.Comment.Author(
        id = id,
        urlToken = urlToken,
        name = name,
        avatarUrl = "https://example.invalid/avatar/$id.png",
        avatarUrlTemplate = "",
        isOrg = false,
        type = "people",
        url = "https://www.zhihu.com/people/$urlToken",
        userType = "people",
        headline = "$name 的离线签名",
        gender = 0,
        isAdvertiser = false,
    )

    private companion object {
        val ROOT_ARTICLE = Article(
            type = ArticleType.Answer,
            id = 9001L,
            title = "离线评论宿主回答",
        )
    }
}
