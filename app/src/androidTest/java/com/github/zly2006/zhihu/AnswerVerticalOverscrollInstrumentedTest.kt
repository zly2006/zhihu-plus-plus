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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.components.AnswerVerticalOverscroll
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.absoluteValue

@RunWith(AndroidJUnit4::class)
class AnswerVerticalOverscrollInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
    }

    @Test
    fun bottomOverscrollSwipeUpTriggersNextAnswerNavigation() {
        val nextNavigationCount = AtomicInteger(0)
        val latestOverscrollOffset = AtomicReference(0f)

        composeRule.setScreenContent {
            val scrollState = rememberScrollState()
            AnswerVerticalOverscroll(
                previousAnswer = null,
                nextAnswer = NEXT_ANSWER,
                onNavigatePrevious = {},
                onNavigateNext = { nextNavigationCount.incrementAndGet() },
                isAtTop = { scrollState.value == 0 },
                isAtBottom = { scrollState.value >= scrollState.maxValue },
                scrollState = scrollState,
                onOverscrollOffsetChange = latestOverscrollOffset::set,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .testTag(ANSWER_CONTENT_TAG),
                ) {
                    Text("离线回答正文顶部")
                    Spacer(Modifier.height(1800.dp))
                    Text(
                        text = "离线回答正文底部",
                        modifier = Modifier.testTag(ANSWER_BOTTOM_TAG),
                    )
                }
            }
        }

        composeRule
            .onNodeWithTag(ANSWER_BOTTOM_TAG)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(ANSWER_CONTENT_TAG).performTouchInput {
            swipeUp(durationMillis = 700)
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            nextNavigationCount.get() == 1 && latestOverscrollOffset.get() < 0f
        }
        assertTrue(latestOverscrollOffset.get() < 0f)
    }

    @Test
    fun playerOverlayTracksOverscrollAndReturnsAfterSubthresholdPull() {
        val nextNavigationCount = AtomicInteger(0)
        val latestOverscrollOffset = AtomicReference(0f)
        val minimumOverscrollOffset = AtomicReference(0f)

        composeRule.setScreenContent {
            val scrollState = rememberScrollState()
            var playerOffset by remember { mutableFloatStateOf(0f) }

            Box(modifier = Modifier.fillMaxSize()) {
                AnswerVerticalOverscroll(
                    previousAnswer = null,
                    nextAnswer = NEXT_ANSWER,
                    onNavigatePrevious = {},
                    onNavigateNext = { nextNavigationCount.incrementAndGet() },
                    isAtTop = { scrollState.value == 0 },
                    isAtBottom = { scrollState.value >= scrollState.maxValue },
                    scrollState = scrollState,
                    onOverscrollOffsetChange = { offset ->
                        playerOffset = offset
                        latestOverscrollOffset.set(offset)
                        if (offset < minimumOverscrollOffset.get()) {
                            minimumOverscrollOffset.set(offset)
                        }
                    },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .testTag(GEOMETRY_CONTENT_TAG),
                    ) {
                        Text("离线几何测试正文顶部")
                        Spacer(Modifier.height(1800.dp))
                        Text(
                            text = "离线几何测试正文底部",
                            modifier = Modifier.testTag(GEOMETRY_BOTTOM_TAG),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(width = 180.dp, height = 56.dp)
                        .graphicsLayer { translationY = playerOffset }
                        .testTag(FAKE_PLAYER_TAG),
                )
            }
        }

        composeRule
            .onNodeWithTag(GEOMETRY_BOTTOM_TAG)
            .performScrollTo()
            .assertIsDisplayed()
        val initialPlayerTop = composeRule
            .onNodeWithTag(FAKE_PLAYER_TAG)
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot
            .top
        val subthresholdDragPx =
            SUBTHRESHOLD_DRAG_DP * composeRule.activity.resources.displayMetrics.density

        composeRule.onNodeWithTag(GEOMETRY_CONTENT_TAG).performTouchInput {
            swipe(
                start = center,
                end = Offset(center.x, center.y - subthresholdDragPx),
                durationMillis = 300,
            )
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            minimumOverscrollOffset.get() < 0f &&
                latestOverscrollOffset.get().absoluteValue <= POSITION_TOLERANCE_PX
        }
        composeRule.waitForIdle()
        assertEquals(0, nextNavigationCount.get())
        assertEquals(
            initialPlayerTop,
            composeRule
                .onNodeWithTag(FAKE_PLAYER_TAG)
                .fetchSemanticsNode()
                .boundsInRoot.top,
            POSITION_TOLERANCE_PX,
        )

        composeRule.onNodeWithTag(GEOMETRY_CONTENT_TAG).performTouchInput {
            swipeUp(durationMillis = 700)
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            nextNavigationCount.get() == 1 && latestOverscrollOffset.get() < 0f
        }
        composeRule.waitForIdle()

        val finalOffset = latestOverscrollOffset.get()
        val finalPlayerTop = composeRule
            .onNodeWithTag(FAKE_PLAYER_TAG)
            .fetchSemanticsNode()
            .boundsInRoot
            .top
        val playerTopDelta = finalPlayerTop - initialPlayerTop
        assertTrue(playerTopDelta < 0f)
        assertEquals(finalOffset, playerTopDelta, POSITION_TOLERANCE_PX)
    }

    private companion object {
        const val ANSWER_CONTENT_TAG = "answer_vertical_overscroll_content"
        const val ANSWER_BOTTOM_TAG = "answer_vertical_overscroll_bottom"
        const val GEOMETRY_CONTENT_TAG = "answer_vertical_overscroll_geometry_content"
        const val GEOMETRY_BOTTOM_TAG = "answer_vertical_overscroll_geometry_bottom"
        const val FAKE_PLAYER_TAG = "answer_vertical_overscroll_fake_player"
        const val POSITION_TOLERANCE_PX = 1f
        const val SUBTHRESHOLD_DRAG_DP = 40f

        val NEXT_ANSWER = ArticleViewModel.CachedAnswerContent(
            article = Article(
                type = ArticleType.Answer,
                id = 778L,
                title = "下一个离线回答",
            ),
            title = "下一个离线回答",
            authorName = "离线作者",
            authorBio = "离线签名",
            authorAvatarUrl = "",
            content = "下一个离线回答正文",
            voteUpCount = 1,
            commentCount = 2,
        )
    }
}
