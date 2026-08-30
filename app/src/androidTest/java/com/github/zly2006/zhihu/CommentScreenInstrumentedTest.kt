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

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.data.CommentSortOrder
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.ZhihuJson
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.CommentHolder
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Navigator
import com.github.zly2006.zhihu.navigation.SegmentCommentHolder
import com.github.zly2006.zhihu.test.InstrumentedTestEnvironment
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.RecordingNavigator
import com.github.zly2006.zhihu.test.ZhihuMockApi
import com.github.zly2006.zhihu.test.mockCommentDetail
import com.github.zly2006.zhihu.test.mockRootComments
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.seedViewModel
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.AndroidArticleNavigationHandoff
import com.github.zly2006.zhihu.ui.COMMENT_EMOJI_BUTTON_TAG
import com.github.zly2006.zhihu.ui.COMMENT_EMOJI_ITEM_TAG_PREFIX
import com.github.zly2006.zhihu.ui.COMMENT_EMOJI_PICKER_TAG
import com.github.zly2006.zhihu.ui.COMMENT_INPUT_TAG
import com.github.zly2006.zhihu.ui.COMMENT_SCREEN_LIST_TAG
import com.github.zly2006.zhihu.ui.CommentScreen
import com.github.zly2006.zhihu.ui.components.CommentScreenComponent
import com.github.zly2006.zhihu.viewmodel.CommentItem
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import com.github.zly2006.zhihu.viewmodel.comment.BaseCommentViewModel
import com.github.zly2006.zhihu.viewmodel.filter.BlockedUser
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.paginationEnvironment
import io.ktor.http.HttpMethod
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import org.junit.After
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
    fun setUp() = runBlocking {
        composeRule.resetAppPreferences()
        ZhihuMockApi.install(enabled = true)
        ZhihuMockApi.reset()
        val database = getContentFilterDatabase(composeRule.activity)
        database.blockedUserDao().clearAllUsers()
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

    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/569
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/606
     */
    @Test
    fun articleHostPendingCommentOpensChildListAndTargetsNestedComment() {
        mockCommentDetail(
            commentId = "liked-child-comment",
            resourceType = "answer",
            replyRootCommentId = "liked-root-comment",
        )
        mockCommentDetail(
            commentId = "liked-root-comment",
            resourceType = "answer",
        )
        mockRootComments(
            urlPrefix = "https://www.zhihu.com/api/v4/comment_v5/answers/9001/root_comment",
            commentId = "liked-root-comment",
        )
        mockRootComments(
            urlPrefix = "https://www.zhihu.com/api/v4/comment_v5/comment/liked-root-comment/child_comment",
            commentId = "other-child-comment",
        )
        AndroidArticleNavigationHandoff.prepareComment(CommentHolder("liked-child-comment", ROOT_ARTICLE))

        composeRule.setScreenContent {
            CommentScreenComponent(
                showComments = false,
                onDismiss = {},
                content = ROOT_ARTICLE,
            )
        }

        composeRule.waitUntil("Expected pending child comment holder to open both comment sheets", timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(COMMENT_SCREEN_LIST_TAG).fetchSemanticsNodes().size == 2
        }
        composeRule.onAllNodesWithTag(COMMENT_SCREEN_LIST_TAG).assertCountEquals(2)
        composeRule.onNodeWithTag("comment_row_liked-child-comment").assertIsDisplayed()
        composeRule.waitUntil("Expected target/root details and root child list requests", timeoutMillis = 5_000) {
            ZhihuMockApi.requestCount(HttpMethod.Get, "comment/liked-child-comment") == 1 &&
                ZhihuMockApi.requestCount(HttpMethod.Get, "comment/liked-root-comment") == 2 &&
                ZhihuMockApi.requestCount(HttpMethod.Get, "comment/liked-root-comment/child_comment") == 1
        }
        assertEquals(0, ZhihuMockApi.requestCount(HttpMethod.Get, "comment/liked-child-comment/child_comment"))
    }

    @After
    fun tearDown() = runBlocking {
        val database = getContentFilterDatabase(composeRule.activity)
        database.blockedUserDao().clearAllUsers()
        ZhihuMockApi.install(enabled = InstrumentedTestEnvironment.isMockMode())
    }

    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/598
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/601
     */
    @Test
    fun emojiPickerInsertsPlaceholderAtCursor() {
        seedRootCommentViewModel(seedRootComments(count = 1))
        setCommentScreen()

        composeRule.onNodeWithTag(COMMENT_INPUT_TAG).performTextInput("已有草稿")
        composeRule
            .onNodeWithTag(COMMENT_INPUT_TAG)
            .performSemanticsAction(SemanticsActions.SetSelection) { setSelection ->
                setSelection(0, 0, false)
            }
        composeRule.onNodeWithTag(COMMENT_EMOJI_BUTTON_TAG).performClick()
        composeRule.onNodeWithTag(COMMENT_EMOJI_PICKER_TAG).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("切换到键盘").assertIsDisplayed()
        composeRule.onNodeWithTag(COMMENT_EMOJI_ITEM_TAG_PREFIX + "[惊喜]").performClick()
        composeRule.onNodeWithTag(COMMENT_INPUT_TAG).assertTextEquals("[惊喜]已有草稿")
        composeRule.onNodeWithTag(COMMENT_EMOJI_BUTTON_TAG).performClick()
        composeRule.onNodeWithContentDescription("选择表情").assertIsDisplayed()
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/595
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/683
     */
    @Test
    fun spanFragmentsWithTheSameSegmentIdsReuseTheCommentThread() {
        val firstFragment = SegmentCommentHolder(
            contentId = "1907864533831225689",
            contentType = "answer",
            segmentId = "1993195116945487118,1978217501180568395",
            segmentContent = "吃柠檬，怎么吃？谁吃的？吃的哪儿？什么时候吃的？吃的心情怎么样？",
            paragraphId = "nX5RAoeG",
            startOffset = 0,
            endOffset = 32,
        )
        val secondFragment = firstFragment.copy(
            segmentContent = "柠檬，是怎样的檬？这个檬是否从事正当行业？这个檬是活着还是死的？",
            paragraphId = "CANw6uZN",
        )
        val currentFragment = mutableStateOf<NavDestination>(firstFragment)
        val urlPrefix =
            "https://www.zhihu.com/api/v4/comment_v5/answers/${firstFragment.contentId}/segment/root_comment" +
                "?segment_id=${firstFragment.segmentId}"
        mockRootComments(
            urlPrefix = urlPrefix,
            commentId = "root-1",
        )

        composeRule.setScreenContent {
            val commentInput = remember { mutableStateOf("") }
            CommentScreen(
                content = { currentFragment.value },
                onChildCommentClick = {},
                commentInput = commentInput.value,
                onCommentInputChange = { commentInput.value = it },
            )
        }
        composeRule.onNodeWithTag("comment_row_root-1").assertIsDisplayed()

        composeRule.runOnIdle { currentFragment.value = secondFragment }

        composeRule.onNodeWithTag("comment_row_root-1").assertIsDisplayed()
        assertEquals(1, ZhihuMockApi.requestCount(HttpMethod.Get, urlPrefix))
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/572
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/580
     */
    @Test
    fun childCommentTargetAndScrollSurviveExternalNavigation() {
        /*
         * Expected behavior:
         * 1. Opening a root comment's reply sheet and scrolling deep into its child replies records
         *    both the active root comment and the child list position in the host back-stack state.
         * 2. Navigating to a child comment author's profile removes the comment host from composition.
         * 3. Returning to the saved host must reopen the same reply sheet at the same child reply,
         *    instead of falling back to the root comment list or resetting the child list to the top.
         */
        val rootComment = seedRootComment(index = 99, childCommentCount = 24)
        val childComments = seedChildComments(count = 24)
        ZhihuMockApi.mockJsonPrefix(
            method = HttpMethod.Get,
            urlPrefix = "https://www.zhihu.com/api/v4/comment_v5/answers/9001/root_comment",
            body =
                """
                {
                  "data": ${ZhihuJson.json.encodeToString(listOf(rootComment))},
                  "paging": {"is_end": true, "is_start": true, "totals": 1, "next": ""}
                }
                """.trimIndent(),
        )
        ZhihuMockApi.mockJsonPrefix(
            method = HttpMethod.Get,
            urlPrefix = "https://www.zhihu.com/api/v4/comment_v5/comment/root-99/child_comment",
            body =
                """
                {
                  "data": ${ZhihuJson.json.encodeToString(childComments)},
                  "paging": {"is_end": true, "is_start": true, "totals": 24, "next": ""}
                }
                """.trimIndent(),
        )

        val showCommentHost = mutableStateOf(true)
        composeRule.setScreenContent {
            val stateHolder = rememberSaveableStateHolder()
            val navigator = remember {
                Navigator(
                    onNavigate = { showCommentHost.value = false },
                    onNavigateBack = {},
                )
            }
            if (showCommentHost.value) {
                stateHolder.SaveableStateProvider("comment-host") {
                    CompositionLocalProvider(LocalNavigator provides navigator) {
                        CommentScreenComponent(
                            showComments = true,
                            onDismiss = {},
                            content = ROOT_ARTICLE,
                        )
                    }
                }
            } else {
                androidx.compose.material3.Text(
                    text = "外部页面",
                    modifier = Modifier.testTag("external_page"),
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("comment_child_button_root-99").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("comment_child_button_root-99").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(COMMENT_SCREEN_LIST_TAG).fetchSemanticsNodes().size == 2
        }
        composeRule
            .onAllNodesWithTag(COMMENT_SCREEN_LIST_TAG)[1]
            .performScrollToNode(hasTestTag("comment_row_child-20"))
        composeRule.onNodeWithTag("comment_row_child-20").assertIsDisplayed()
        composeRule.onNodeWithTag("comment_author_child-20").performClick()
        composeRule.onNodeWithTag("external_page").assertIsDisplayed()

        composeRule.runOnIdle { showCommentHost.value = true }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag(COMMENT_SCREEN_LIST_TAG).fetchSemanticsNodes().size == 2
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithTag("comment_row_child-20").assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithTag("comment_row_child-20").assertIsDisplayed()
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/369
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/381
     */
    @Test
    fun commentWithImageKeepsReplyAndLikeActionsVisible() {
        /*
         * Expected behavior:
         * 1. A comment containing an inline image should still render the same bottom action row as
         *    text-only comments.
         * 2. The reply and like buttons must remain visible and clickable after the image content is
         *    laid out.
         */
        val childEntryCommentIds = mutableListOf<String>()
        val seededComments = seedRootComments(count = 4)
        mockRootComments(
            urlPrefix = "https://www.zhihu.com/api/v4/comment_v5/answers/9001/root_comment",
            comments = seededComments,
        )

        setCommentScreen(
            onChildCommentClick = { childEntryCommentIds += it.item.id },
        )

        composeRule
            .onNodeWithTag(COMMENT_SCREEN_LIST_TAG)
            .performScrollToNode(hasTestTag("comment_image_root-1"))
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("comment_image_root-1", useUnmergedTree = true).assertIsDisplayed()
            }.isSuccess
        }

        composeRule.onNodeWithTag("comment_reply_button_root-1").assertIsDisplayed()
        composeRule.onNodeWithTag("comment_like_button_root-1").assertIsDisplayed()
        val rowBounds = composeRule
            .onAllNodesWithTag("comment_row_root-1")
            .fetchSemanticsNodes()
            .single()
            .boundsInRoot
        val imageBounds = composeRule
            .onAllNodesWithTag("comment_image_root-1", useUnmergedTree = true)
            .fetchSemanticsNodes()
            .single()
            .boundsInRoot
        val replyBounds = composeRule
            .onAllNodesWithTag("comment_reply_button_root-1")
            .fetchSemanticsNodes()
            .single()
            .boundsInRoot
        val likeBounds = composeRule
            .onAllNodesWithTag("comment_like_button_root-1")
            .fetchSemanticsNodes()
            .single()
            .boundsInRoot
        val expectedBottom = maxOf(imageBounds.bottom, replyBounds.bottom, likeBounds.bottom)
        assertTrue(
            "Comment row should include image and action row bounds, but row bottom was " +
                "${rowBounds.bottom} and content bottom was $expectedBottom",
            rowBounds.bottom >= expectedBottom,
        )
        composeRule.onNodeWithTag("comment_reply_button_root-1").performClick()
        composeRule.onNodeWithTag("comment_like_button_root-1").performClick()

        assertEquals(listOf("root-1"), childEntryCommentIds)
    }

    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/357
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/363
     */
    @Test
    fun blockedUsersAreRemovedFromRootAndEmbeddedChildComments() {
        /*
         * Expected behavior:
         * 1. Production response processing should remove a root comment authored by a blocked user.
         * 2. Kept root comments should also drop embedded child comments from blocked users before
         *    the screen receives them.
         */
        runBlocking {
            val database = getContentFilterDatabase(composeRule.activity)
            database.blockedUserDao().insertUser(BlockedUser("blocked-root-author", "被屏蔽根评论作者"))
            database.blockedUserDao().insertUser(BlockedUser("blocked-child-author", "被屏蔽子评论作者"))
            mockRootComments(
                urlPrefix = "https://www.zhihu.com/api/v4/comment_v5/answers/9001/root_comment",
                comments = listOf(
                    seedComment(
                        id = "blocked-root",
                        authorId = "blocked-root-author",
                        authorName = "被屏蔽根评论作者",
                        content = "这条根评论不应展示",
                    ),
                    seedComment(
                        id = "allowed-root",
                        authorId = "allowed-root-author",
                        authorName = "可见根评论作者",
                        content = "这条根评论应展示",
                        childCommentCount = 2,
                        childComments = listOf(
                            seedComment(
                                id = "blocked-child",
                                authorId = "blocked-child-author",
                                authorName = "被屏蔽子评论作者",
                                content = "这条内嵌子评论不应展示",
                            ),
                            seedComment(
                                id = "allowed-child",
                                authorId = "allowed-child-author",
                                authorName = "可见子评论作者",
                                content = "这条内嵌子评论应展示",
                            ),
                        ),
                    ),
                ),
            )
        }

        setCommentScreen()

        composeRule.onNodeWithTag("comment_row_allowed-root").assertIsDisplayed()
        composeRule.onNodeWithText("可见根评论作者").assertIsDisplayed()
        composeRule.onNodeWithText("这条内嵌子评论应展示").assertIsDisplayed()
        composeRule.onAllNodesWithTag("comment_row_blocked-root").assertCountEquals(0)
        composeRule.onAllNodesWithTag("comment_row_blocked-child").assertCountEquals(0)
        composeRule.onAllNodesWithText("被屏蔽根评论作者").assertCountEquals(0)
        composeRule.onAllNodesWithText("被屏蔽子评论作者").assertCountEquals(0)
    }

    private fun setCommentScreen(
        content: NavDestination = ROOT_ARTICLE,
        activeCommentItem: CommentItem? = null,
        onChildCommentClick: (CommentItem) -> Unit = {},
    ): RecordingNavigator = composeRule.setScreenContent {
        val commentInput = remember { mutableStateOf("") }
        CommentScreen(
            content = { content },
            activeCommentItem = activeCommentItem,
            onChildCommentClick = onChildCommentClick,
            commentInput = commentInput.value,
            onCommentInputChange = { commentInput.value = it },
        )
    }

    private fun seedRootCommentViewModel(seededComments: List<DataHolder.Comment>): SeededRootCommentViewModel =
        composeRule.seedViewModel<SeededRootCommentViewModel>(
            key = ROOT_ARTICLE_COMMENT_VIEW_MODEL_KEY,
        ) {
            SeededRootCommentViewModel(
                article = ROOT_ARTICLE,
                seededComments = seededComments,
            )
        }

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

        suspend fun processForTest(context: android.content.Context, data: List<DataHolder.Comment>) {
            processResponse(paginationEnvironment(context), data, JsonArray(emptyList()))
        }

        override fun loadMore(environment: PaginationEnvironment) {
            loadMoreCount += 1
        }

        override fun refresh(environment: PaginationEnvironment) {
            refreshHistory += sortOrder
        }

        override fun submitComment(
            content: NavDestination,
            commentText: String,
            environment: ZhihuApiEnvironment,
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

        override fun loadMore(environment: PaginationEnvironment) = Unit

        override fun submitComment(
            content: NavDestination,
            commentText: String,
            environment: ZhihuApiEnvironment,
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
        canDelete: Boolean = false,
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
        canDelete = canDelete,
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
        val ROOT_ARTICLE_COMMENT_VIEW_MODEL_KEY = "article:${ROOT_ARTICLE.type}:${ROOT_ARTICLE.id}"
    }
}
