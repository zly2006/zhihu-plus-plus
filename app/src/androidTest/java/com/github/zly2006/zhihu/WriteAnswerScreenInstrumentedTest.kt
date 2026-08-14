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

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.navigation.WriteAnswer
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.WRITE_ANSWER_CONTENT_TAG
import com.github.zly2006.zhihu.ui.WRITE_ANSWER_FAB_PREVIEW_TAG
import com.github.zly2006.zhihu.ui.WRITE_ANSWER_FAB_SAVE_TAG
import com.github.zly2006.zhihu.ui.WriteAnswerScreen
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WriteAnswerScreenInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
    }

    @Test
    fun editorActionsFollowVerticalScrollDirection() {
        composeRule.setScreenContent {
            WriteAnswerScreen(
                WriteAnswer(
                    questionId = 648,
                    questionTitle = "离线编辑器滚动测试",
                ),
            )
        }

        val editor = composeRule.onNodeWithTag(WRITE_ANSWER_CONTENT_TAG)
        editor.performTextInput((1..80).joinToString("\n") { "回答正文第 $it 行" })

        composeRule.onNodeWithTag(WRITE_ANSWER_FAB_PREVIEW_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(WRITE_ANSWER_FAB_SAVE_TAG).assertIsDisplayed()

        editor.performTouchInput { swipeDown(durationMillis = 700) }
        editor.performTouchInput { swipeUp(durationMillis = 700) }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(WRITE_ANSWER_FAB_PREVIEW_TAG).fetchSemanticsNodes().isEmpty()
        }

        editor.performTouchInput { swipeDown(durationMillis = 700) }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(WRITE_ANSWER_FAB_PREVIEW_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(WRITE_ANSWER_FAB_PREVIEW_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(WRITE_ANSWER_FAB_SAVE_TAG).assertIsDisplayed()
    }

    @Test
    fun previewRendersMarkdownWithReferenceDefinitions() {
        composeRule.setScreenContent {
            WriteAnswerScreen(
                WriteAnswer(
                    questionId = 648,
                    questionTitle = "回答预览重复状态键测试",
                ),
            )
        }

        composeRule.onNodeWithTag(WRITE_ANSWER_CONTENT_TAG).performTextInput(
            buildString {
                appendLine("[回答预览正文][docs]")
                appendLine()
                repeat(12) { index ->
                    appendLine("[引用 $index][ref-$index]")
                    appendLine("[ref-$index]: https://example.com/preview/$index")
                    appendLine("${'$'}${'$'}x_$index${'$'}${'$'} 同行尾随正文 $index")
                    appendLine()
                }
                appendLine("[docs]: https://example.com/preview")
            },
        )
        composeRule.onNodeWithTag(WRITE_ANSWER_FAB_PREVIEW_TAG).performClick()

        composeRule.onNodeWithText("Markdown").assertIsDisplayed()
        composeRule.onAllNodesWithText("回答预览正文", substring = true).assertCountEquals(2)
    }
}
