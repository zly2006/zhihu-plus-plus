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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.test.MainActivityComposeRule
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
import com.github.zly2006.zhihu.ui.PIN_SCREEN_SCROLL_TAG
import com.github.zly2006.zhihu.ui.PIN_SCREEN_SHARE_BUTTON_TAG
import com.github.zly2006.zhihu.ui.PinLinkCardPreview
import com.github.zly2006.zhihu.ui.PinScreen
import com.github.zly2006.zhihu.ui.PinScreenTestOverrides
import com.github.zly2006.zhihu.ui.PinScreenUiState
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
    }

    @Test
    fun loadingAndErrorStatesRenderDeterministicallyOffline() {
        /*
         * Expected behavior:
         * 1. A loading-only injected state should render the dedicated loading container instead of
         *    trying to fetch pin details from the network.
         * 2. An error-only injected state should render the visible error message contract exactly
         *    where PinScreen normally reports load failures.
         * 3. Switching between those injected states inside tests must remain completely offline and
         *    deterministic because the screen never instantiates the production PinViewModel path.
         */
        composeRule.setScreenContent {
            PinScreen(
                innerPadding = PaddingValues(),
                pin = Pin(101),
                testOverrides = PinScreenTestOverrides(
                    state = PinScreenUiState(isLoading = true),
                ),
            )
        }
        composeRule.onNodeWithTag(PIN_SCREEN_LOADING_TAG).assertIsDisplayed()

        composeRule.setScreenContent {
            PinScreen(
                innerPadding = PaddingValues(),
                pin = Pin(101),
                testOverrides = PinScreenTestOverrides(
                    state = PinScreenUiState(errorMessage = "离线错误"),
                ),
            )
        }
        composeRule.onNodeWithTag(PIN_SCREEN_ERROR_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("加载失败: 离线错误").assertIsDisplayed()
    }

    @Test
    fun contentActionsNavigationDialogsAndSwipeCyclesStayStableOffline() {
        /*
         * Expected behavior:
         * 1. With a fully seeded local pin, the author row, link card, like button, comment button,
         *    share button, and back button should all remain interactive without network access.
         * 2. Author and link-card clicks must navigate to their deterministic Person and Question
         *    destinations, while the back button must increment only the navigator back counter.
         * 3. Like, comment, and share actions should route through injected callbacks and custom
         *    test content, proving PinScreen can be exercised without real bottom sheets or shares.
         * 4. Vertical and horizontal swipe cycles on the main scroll container must preserve all of
         *    the seeded content and action affordances afterward.
         */
        var likeCount = 0
        var shareActionCount = 0
        val navigator = composeRule.setScreenContent {
            PinScreen(
                innerPadding = PaddingValues(),
                pin = Pin(101),
                testOverrides = PinScreenTestOverrides(
                    state = PinScreenUiState(
                        pinContent = seededPinContent(),
                        isLiked = false,
                        likeCount = 9,
                    ),
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

        composeRule.onNodeWithTag(PIN_SCREEN_SCROLL_TAG).assertIsDisplayed()
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
        topics = listOf(
            DataHolder.Topic(
                id = "topic-1",
                type = "topic",
                url = "https://www.zhihu.com/topic/topic-1",
                name = "离线话题一",
            ),
        ),
    )
}
