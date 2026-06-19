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
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.test.InstrumentedTestEnvironment
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.ZhihuMockApi
import com.github.zly2006.zhihu.test.performHorizontalSwipeCycle
import com.github.zly2006.zhihu.test.performVerticalSwipeCycle
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.PIN_SCREEN_AUTHOR_TAG
import com.github.zly2006.zhihu.ui.PIN_SCREEN_BACK_BUTTON_TAG
import com.github.zly2006.zhihu.ui.PIN_SCREEN_COMMENT_BUTTON_TAG
import com.github.zly2006.zhihu.ui.PIN_SCREEN_ERROR_TAG
import com.github.zly2006.zhihu.ui.PIN_SCREEN_LIKE_BUTTON_TAG
import com.github.zly2006.zhihu.ui.PIN_SCREEN_LINK_CARD_TAG
import com.github.zly2006.zhihu.ui.PIN_SCREEN_LOADING_TAG
import com.github.zly2006.zhihu.ui.PIN_SCREEN_POLL_CARD_TAG
import com.github.zly2006.zhihu.ui.PIN_SCREEN_SCROLL_TAG
import com.github.zly2006.zhihu.ui.PIN_SCREEN_SHARE_BUTTON_TAG
import com.github.zly2006.zhihu.ui.PinLinkCardPreview
import com.github.zly2006.zhihu.ui.PinScreen
import com.github.zly2006.zhihu.ui.PinScreenTestOverrides
import com.github.zly2006.zhihu.ui.pinScreenPollOptionTag
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.encodeToString
import org.junit.After
import org.junit.Assert.assertEquals
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

    @Test
    fun loadingAndErrorStatesRenderDeterministicallyOffline() {
        /*
         * Expected behavior:
         * 1. A pending mocked pin-detail request should render the dedicated loading container.
         * 2. A null mocked response should render the visible error message contract exactly where
         *    PinScreen normally reports load failures.
         * 3. Both states stay offline while still entering the production loadPinDetail path.
         */
        val releasePinDetail = CompletableDeferred<Unit>()
        mockPinDetailBody(
            pinId = 101,
            body = ZhihuJson.json.encodeToString(seededPinContent()),
            beforeRespond = { releasePinDetail.await() },
        )

        try {
            composeRule.setScreenContent {
                PinScreen(
                    pin = Pin(101),
                )
            }
            composeRule.waitUntilTagExists(PIN_SCREEN_LOADING_TAG)
            composeRule.onNodeWithTag(PIN_SCREEN_LOADING_TAG).assertIsDisplayed()
        } finally {
            releasePinDetail.complete(Unit)
        }

        mockPinDetailBody(pinId = 102, body = "null")

        composeRule.setScreenContent {
            PinScreen(
                pin = Pin(102),
            )
        }
        composeRule.waitUntilTagExists(PIN_SCREEN_ERROR_TAG)
        composeRule.onNodeWithTag(PIN_SCREEN_ERROR_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("加载失败: 想法详情为空").assertIsDisplayed()
    }

    @Test
    fun pollOptionsRenderAndDispatchVoteOffline() {
        /*
         * Expected behavior:
         * 1. A mocked pin detail with bottom_poll should render the poll card without using the
         *    real network.
         * 2. The poll should expose each option through a stable test tag so UI automation can tap by
         *    semantic identity instead of coordinates.
         * 3. Tapping an unvoted option must dispatch the exact poll id and option id through the
         *    injected callback, matching the production POST contract.
         */
        var selectedPollId: String? = null
        var selectedOptionId: String? = null
        mockPinDetail(content = seededPollPinContent())

        composeRule.setScreenContent {
            PinScreen(
                pin = Pin(101),
                testOverrides = PinScreenTestOverrides(
                    onPollVote = { pollId, optionId ->
                        selectedPollId = pollId
                        selectedOptionId = optionId
                    },
                ),
            )
        }

        composeRule.waitUntilTagExists(PIN_SCREEN_POLL_CARD_TAG)
        composeRule.onNodeWithTag(PIN_SCREEN_POLL_CARD_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("知乎++好用吗").assertIsDisplayed()
        composeRule.onNodeWithTag(pinScreenPollOptionTag("option-b")).assertIsDisplayed().performClick()

        assertEquals("poll-101", selectedPollId)
        assertEquals("option-b", selectedOptionId)
    }

    @Test
    fun contentActionsNavigationDialogsAndSwipeCyclesStayStableOffline() {
        /*
         * Expected behavior:
         * 1. With a fully mocked pin detail, the author row, link card, like button, comment button,
         *    share button, and back button should all remain interactive without real network access.
         * 2. Author and link-card clicks must navigate to their deterministic Person and Question
         *    destinations, while the back button must increment only the navigator back counter.
         * 3. Like, comment, and share actions should route through injected callbacks and custom
         *    test content, proving PinScreen can be exercised without real bottom sheets or shares.
         * 4. Vertical and horizontal swipe cycles on the main scroll container must preserve all of
         *    the seeded content and action affordances afterward.
         */
        var likeCount = 0
        var shareActionCount = 0
        mockPinDetail(content = seededPinContent())
        val navigator = composeRule.setScreenContent {
            PinScreen(
                pin = Pin(101),
                testOverrides = PinScreenTestOverrides(
                    onLikeClick = { likeCount++ },
                    onShareAction = { showShareDialog ->
                        shareActionCount++
                        showShareDialog()
                    },
                    linkCardPreview = PinLinkCardPreview(
                        title = "离线关联问题",
                        preview = "这是关联内容的固定摘要",
                    ),
                    commentScreenContent = { showComments, onDismiss, _ ->
                        if (showComments) {
                            Text(
                                text = "离线评论面板",
                                modifier = Modifier.clickable { onDismiss() },
                            )
                        }
                    },
                    shareDialogContent = { showDialog, onDismissRequest, _, _ ->
                        if (showDialog) {
                            Text(
                                text = "离线分享面板",
                                modifier = Modifier.clickable { onDismissRequest() },
                            )
                        }
                    },
                ),
            )
        }

        composeRule.waitUntilTagExists(PIN_SCREEN_SCROLL_TAG)
        composeRule.onNodeWithTag(PIN_SCREEN_SCROLL_TAG).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("离线优秀答主").assertIsDisplayed()
        composeRule.onNodeWithText("离线点赞者 等 9 人赞同了该想法").assertIsDisplayed()
        composeRule.onNodeWithTag(PIN_SCREEN_SCROLL_TAG).performVerticalSwipeCycle()
        composeRule.onNodeWithTag(PIN_SCREEN_SCROLL_TAG).performHorizontalSwipeCycle()
        composeRule.onNodeWithTag(PIN_SCREEN_AUTHOR_TAG).assertIsDisplayed().performClick()
        composeRule.onNodeWithTag(PIN_SCREEN_LINK_CARD_TAG).assertIsDisplayed().performClick()
        composeRule.onNodeWithTag(PIN_SCREEN_LIKE_BUTTON_TAG).performClick()

        composeRule.onNodeWithTag(PIN_SCREEN_COMMENT_BUTTON_TAG).performClick()
        composeRule.onNodeWithText("离线评论面板").assertIsDisplayed().performClick()

        composeRule.onNodeWithTag(PIN_SCREEN_SHARE_BUTTON_TAG).performClick()
        composeRule.onNodeWithText("离线分享面板").assertIsDisplayed().performClick()

        composeRule.onNodeWithTag(PIN_SCREEN_BACK_BUTTON_TAG).performClick()

        assertEquals(1, likeCount)
        assertEquals(1, shareActionCount)
        assertEquals(1, navigator.backCount)
        assertEquals(
            listOf(
                Person(
                    id = "pin-author-id",
                    urlToken = "pin-author-token",
                    name = "离线想法作者",
                ),
                Question(questionId = 987654321L),
            ),
            navigator.destinations,
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
