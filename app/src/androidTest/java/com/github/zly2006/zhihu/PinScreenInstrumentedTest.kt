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

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.ZhihuJson
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.test.InstrumentedTestEnvironment
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.ZhihuMockApi
import com.github.zly2006.zhihu.test.mockRootComments
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.COMMENT_INPUT_TAG
import com.github.zly2006.zhihu.ui.COMMENT_SCREEN_LIST_TAG
import com.github.zly2006.zhihu.ui.components.CommentScreenComponent
import io.ktor.http.HttpMethod
import kotlinx.serialization.encodeToString
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PinScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
        ZhihuMockApi.install(enabled = true)
        ZhihuMockApi.reset()
    }

    @After
    fun tearDown() {
        ZhihuMockApi.install(enabled = InstrumentedTestEnvironment.isMockMode())
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/534
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/546
     */
    @Test
    fun commentDraftSurvivesSheetDismissAndReopen() {
        /*
         * Expected behavior:
         * 1. A long draft typed in the comment sheet remains stored after its production Back handler dismisses it.
         * 2. Reopening the same content's comments must restore the entire unsent draft.
         */
        mockRootComments("https://www.zhihu.com/api/v4/comment_v5/pins/101/root_comment")
        val showComments = mutableStateOf(true)
        composeRule.setScreenContent {
            CommentScreenComponent(
                showComments = showComments.value,
                onDismiss = { showComments.value = false },
                content = Pin(101),
            )
        }
        val draft = "这是一段尚未发送的长评论，用来验证关闭评论区后重新打开仍然保留全部内容。".repeat(8)

        composeRule.waitUntilTagExists(COMMENT_INPUT_TAG)
        composeRule.onNodeWithTag(COMMENT_INPUT_TAG).performTextInput(draft)
        composeRule.runOnIdle {
            showComments.value = false
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(COMMENT_SCREEN_LIST_TAG).fetchSemanticsNodes().isEmpty()
        }

        composeRule.runOnIdle {
            showComments.value = true
        }
        composeRule.waitUntilTagExists(COMMENT_INPUT_TAG)
        composeRule.onNodeWithTag(COMMENT_INPUT_TAG).assertTextEquals(draft)
    }

    private fun mockPinPollVote(pollId: String = "poll-101") {
        ZhihuMockApi.mockJson(
            method = HttpMethod.Post,
            url = "https://www.zhihu.com/api/v4/polls/$pollId",
            body = "{}",
        )
    }

    private fun mockPinLike(
        pinId: Long = 101,
        likedCount: Int,
    ) {
        ZhihuMockApi.mockJson(
            method = HttpMethod.Post,
            url = "https://www.zhihu.com/api/v4/pins/$pinId/voters/up",
            body = """{"liked_count":$likedCount}""",
        )
    }

    private fun mockPinDetail(
        pinId: Long = 101,
        content: DataHolder.Pin,
    ) {
        mockPinDetailBody(
            pinId = pinId,
            body = ZhihuJson.json.encodeToString(content),
        )
    }

    private fun mockPinDetailBody(
        pinId: Long,
        body: String,
        beforeRespond: suspend () -> Unit = {},
    ) {
        ZhihuMockApi.mockJson(
            method = HttpMethod.Get,
            url = "https://www.zhihu.com/api/v4/pins/$pinId?include=topics",
            body = body,
            beforeRespond = beforeRespond,
        )
    }

    private fun MainActivityComposeRule.waitUntilTagExists(tag: String) {
        waitUntil("Expected node with tag $tag", timeoutMillis = 5_000) {
            onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun MainActivityComposeRule.waitUntilRequestCount(
        method: HttpMethod,
        urlSubstring: String,
        count: Int,
    ) {
        waitUntil("Expected $count $method requests containing $urlSubstring", timeoutMillis = 5_000) {
            ZhihuMockApi.requestCount(method, urlSubstring) == count
        }
    }

    private fun seededPinContent(): DataHolder.Pin = DataHolder.Pin(
        id = "101",
        url = "https://www.zhihu.com/pin/101",
        author = DataHolder.Author(
            avatarUrl = "",
            gender = 0,
            headline = "离线作者简介",
            id = "pin-author-id",
            isAdvertiser = false,
            isOrg = false,
            name = "离线想法作者",
            type = "people",
            url = "https://www.zhihu.com/people/pin-author-token",
            urlToken = "pin-author-token",
            userType = "people",
            badgeV2 = DataHolder.BadgeV2(
                title = "离线优秀答主",
                icon = DataHolder.ZH_PLUS_AUTHOR_BADGE_ICON,
                detailBadges = listOf(
                    DataHolder.BadgeV2.Badge(
                        type = "best",
                        detailType = "best_answerer",
                        title = "优秀答主",
                        description = "离线优秀答主",
                        icon = DataHolder.ZH_PLUS_AUTHOR_BADGE_ICON,
                        badgeStatus = "passed",
                    ),
                ),
            ),
        ),
        content = listOf(
            DataHolder.Pin.ContentText(
                content = "这是 PinScreen instrumented test 的离线正文。",
                title = "",
            ),
            DataHolder.Pin.ContentLinkCard(
                dataContentId = "987654321",
                dataContentType = "question",
                url = "https://www.zhihu.com/question/987654321",
            ),
        ),
        contentHtml = "<p>这是 <b>PinScreen</b> instrumented test 的离线正文。</p>",
        likeCount = 9,
        commentCount = 3,
        created = 1_713_456_789L,
        likers = listOf(
            DataHolder.Author(
                avatarUrl = "",
                gender = 0,
                headline = "",
                id = "pin-liker-id",
                isAdvertiser = false,
                isOrg = false,
                name = "离线点赞者",
                type = "people",
                url = "https://www.zhihu.com/people/pin-liker-token",
                urlToken = "pin-liker-token",
                userType = "people",
            ),
        ),
        topics = listOf(
            DataHolder.Topic(
                id = "topic-1",
                type = "topic",
                url = "https://www.zhihu.com/topic/topic-1",
                name = "离线话题一",
            ),
        ),
    )

    private fun seededPollPinContent(): DataHolder.Pin =
        seededPinContent().let { pin ->
            pin.copy(
                content = pin.content + DataHolder.Pin.ContentPoll(
                    duration = 0,
                    pollId = 2051253919255360130L,
                ),
                bottomPoll = DataHolder.Pin.BottomPoll(
                    voting = DataHolder.Pin.Poll(
                        id = "poll-101",
                        title = "知乎++好用吗",
                        maxSelections = 1,
                        type = "single",
                        endAt = -1,
                        options = listOf(
                            DataHolder.Pin.PollOption(
                                id = "option-a",
                                title = "五颗星",
                                votingCount = 0,
                            ),
                            DataHolder.Pin.PollOption(
                                id = "option-b",
                                title = "四颗星",
                                votingCount = 0,
                            ),
                        ),
                    ),
                ),
            )
        }
}
