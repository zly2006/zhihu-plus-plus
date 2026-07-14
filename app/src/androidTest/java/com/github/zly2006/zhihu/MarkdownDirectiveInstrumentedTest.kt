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
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.navigation.Video
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarkdownDirectiveInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
    }

    @Test
    fun segmentedTextAndVideoDirectivesRenderAndKeepVideoNavigation() {
        val navigator = composeRule.setScreenContent {
            RenderMarkdown(
                html = TEST_HTML,
                enableScroll = false,
            )
        }

        composeRule.onNodeWithText("段评高亮文本，普通文本。").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("播放视频")
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(Video(VIDEO_ID)), navigator.destinations)
        }
    }

    private companion object {
        const val VIDEO_ID = 2029631316597973958L
        const val TEST_HTML = """
            <p data-pid="seg-1"><span class="highlight-wrap other has-comments"
                data-highlight-id="abc"
                data-highlight-like-count="5"
                data-highlight-comment-count="1"
                data-highlight-my-comment-count="0"
                data-highlight-is-like="true"
                data-highlight-is-span="false"
                data-highlight-content-id="42"
                data-highlight-content-type="answer"
                data-highlight-pid="seg-1"
                data-highlight-start-offset="0"
                data-highlight-end-offset="6">段评高亮文本</span>，普通文本。</p>
            <a class="video-box" data-lens-id="2029631316597973958"></a>
        """
    }
}
