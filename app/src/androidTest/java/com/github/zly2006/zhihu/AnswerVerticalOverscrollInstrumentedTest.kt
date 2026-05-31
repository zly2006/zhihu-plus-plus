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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

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
            nextNavigationCount.get() == 1
        }
    }

    private companion object {
        const val ANSWER_CONTENT_TAG = "answer_vertical_overscroll_content"
        const val ANSWER_BOTTOM_TAG = "answer_vertical_overscroll_bottom"

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
