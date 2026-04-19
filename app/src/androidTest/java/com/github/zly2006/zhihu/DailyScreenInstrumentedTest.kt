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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.data.DailyStory
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.performHorizontalSwipeCycle
import com.github.zly2006.zhihu.test.performVerticalSwipeCycle
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.DailyScreen
import com.github.zly2006.zhihu.ui.DailyScreenUiState
import com.github.zly2006.zhihu.ui.DailySection
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class DailyScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule()

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
    }

    @Test
    fun loadingStateStillShowsTitleAndDatePickerEntryOffline() {
        // This screen test must stay completely offline, so it injects a loading-only UI snapshot
        // instead of allowing DailyViewModel to fetch Zhihu Daily content.
        // Expected behavior:
        // 1. The toolbar title remains visible even while the body is still loading.
        // 2. The date picker entry point is present and clickable in loading state.
        // 3. Confirming the dialog routes through the test seam and returns a yyyyMMdd string
        //    rather than touching the real daily API.
        // 4. The loading copy remains visible after the dialog is dismissed.
        val selectedDate = AtomicReference<String?>(null)
        composeRule.setScreenContent {
            DailyScreen(
                innerPadding = PaddingValues(),
                testState = DailyScreenUiState(isLoading = true),
                onTestDateSelected = selectedDate::set,
            )
        }

        composeRule.onNodeWithTag(TITLE_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("知乎日报").assertIsDisplayed()
        composeRule.onNodeWithTag(DATE_PICKER_BUTTON_TAG).assertIsDisplayed().performClick()
        composeRule.onNodeWithText("确认").assertIsDisplayed().performClick()

        composeRule.waitForIdle()
        assertTrue(selectedDate.get()?.matches(Regex("\\d{8}")) == true)
        composeRule.onNodeWithTag(LOADING_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("正在加载...").assertIsDisplayed()
    }

    @Test
    fun errorAndEmptyStatesRenderDeterministicallyOffline() {
        // Error and empty rendering are the two most important non-happy-path fallbacks here.
        // Both are injected directly so the assertions stay deterministic and never depend on
        // flaky networking, account state, or the current day on Zhihu servers.
        composeRule.setScreenContent {
            DailyScreen(
                innerPadding = PaddingValues(),
                testState = DailyScreenUiState(
                    isLoading = false,
                    error = "离线错误：日报接口不可用",
                ),
            )
        }

        composeRule.onNodeWithTag(ERROR_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("离线错误：日报接口不可用").assertIsDisplayed()

        composeRule.setScreenContent {
            DailyScreen(
                innerPadding = PaddingValues(),
                testState = DailyScreenUiState(isLoading = false),
            )
        }

        composeRule.onNodeWithTag(EMPTY_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("暂无内容").assertIsDisplayed()
    }

    @Test
    fun fixedSectionsRenderAndSwipeCyclesStayStableOffline() {
        // This injects two deterministic sections with enough rows to make the LazyColumn scroll.
        // Expected behavior:
        // 1. The first visible section date is reflected in the toolbar subtitle.
        // 2. Fixed rows render from injected data instead of the network.
        // 3. Scrolling to the second section works deterministically.
        // 4. Vertical and horizontal swipe cycles do not break the list, toolbar, or row semantics.
        val uiState = DailyScreenUiState(
            sections = listOf(
                seededSection(date = "20260418", range = 1..10),
                seededSection(date = "20260417", range = 11..20),
            ),
            isLoading = false,
        )
        composeRule.setScreenContent {
            DailyScreen(
                innerPadding = PaddingValues(),
                testState = uiState,
            )
        }

        composeRule.onNodeWithTag(LIST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(CURRENT_DATE_TAG).assertTextEquals("2026年04月18日")
        composeRule.onNodeWithText("固定日报问题 1").assertIsDisplayed()
        composeRule.onNodeWithTag(SECTION_TAG_PREFIX + "20260418").assertIsDisplayed()

        val dailyList = composeRule.onNodeWithTag(LIST_TAG)
        dailyList.performScrollToNode(hasTestTag(storyTag(12)))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("固定日报问题 12").assertIsDisplayed()

        dailyList.performVerticalSwipeCycle()
        dailyList.performHorizontalSwipeCycle()
        composeRule.waitForIdle()

        dailyList.performScrollToNode(hasTestTag(storyTag(12)))
        composeRule.onNodeWithText("固定日报问题 12").assertIsDisplayed()
        dailyList.performScrollToNode(hasTestTag(SECTION_TAG_PREFIX + "20260417"))
        composeRule.onNodeWithTag(SECTION_TAG_PREFIX + "20260417").assertIsDisplayed()
        composeRule.onNodeWithTag(DATE_PICKER_BUTTON_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(CURRENT_DATE_TAG).assertIsDisplayed()
    }

    private fun seededSection(
        date: String,
        range: IntRange,
    ): DailySection = DailySection(
        date = date,
        stories = range.map { index -> seededStory(index) },
    )

    private fun seededStory(index: Int): DailyStory = DailyStory(
        id = index.toLong(),
        title = "固定日报问题 $index",
        url = "https://example.com/daily/$index",
        hint = "固定日报摘要 $index",
        images = emptyList(),
        type = 0,
    )

    private companion object {
        const val TITLE_TAG = "daily_screen_title"
        const val CURRENT_DATE_TAG = "daily_screen_current_date"
        const val DATE_PICKER_BUTTON_TAG = "daily_screen_date_picker_button"
        const val LOADING_TAG = "daily_screen_loading"
        const val ERROR_TAG = "daily_screen_error"
        const val EMPTY_TAG = "daily_screen_empty"
        const val LIST_TAG = "daily_screen_list"
        const val SECTION_TAG_PREFIX = "daily_screen_section_"

        fun storyTag(storyId: Int) = "daily_screen_story_$storyId"
    }
}
