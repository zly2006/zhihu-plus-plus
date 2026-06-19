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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.test.InstrumentedTestEnvironment
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.ZhihuMockApi
import com.github.zly2006.zhihu.test.performHorizontalSwipeCycle
import com.github.zly2006.zhihu.test.performVerticalSwipeCycle
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.DailyScreen
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailyScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule()

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
    fun latestAndLoadMoreRenderThroughMockHttp() {
        // This test keeps DailyScreen offline by mocking the HTTP layer while still exercising
        // DailyViewModel.loadLatest() and loadMore().
        // Expected behavior:
        // 1. The latest-story endpoint populates the first daily section through the production
        //    ViewModel fetch path.
        // 2. Scrolling near the tail requests the previous section through the real load-more path.
        // 3. Swipe cycles keep the loaded list, toolbar, and stable row tags intact.
        mockLatest(date = "20260418", storyIds = 1..12)
        mockBefore(apiDate = "20260418", responseDate = "20260417", storyIds = 13..24)

        composeRule.setScreenContent {
            DailyScreen()
        }

        composeRule.waitUntilTagExists(LIST_TAG)
        composeRule.onNodeWithTag(TITLE_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("知乎日报").assertIsDisplayed()
        composeRule.onNodeWithTag(CURRENT_DATE_TAG).assertTextEquals("2026年04月18日")
        composeRule.onNodeWithText("固定日报问题 1").assertIsDisplayed()
        composeRule.onNodeWithTag(SECTION_TAG_PREFIX + "20260418").assertIsDisplayed()

        val dailyList = composeRule.onNodeWithTag(LIST_TAG)
        dailyList.performScrollToNode(hasTestTag(storyTag(12)))
        composeRule.waitUntilTagExists(storyTag(13))
        composeRule.onNodeWithText("固定日报问题 13").assertIsDisplayed()

        dailyList.performVerticalSwipeCycle()
        dailyList.performHorizontalSwipeCycle()
        composeRule.waitForIdle()

        dailyList.performScrollToNode(hasTestTag(storyTag(13)))
        composeRule.onNodeWithText("固定日报问题 13").assertIsDisplayed()
        dailyList.performScrollToNode(hasTestTag(SECTION_TAG_PREFIX + "20260417"))
        composeRule.onNodeWithTag(SECTION_TAG_PREFIX + "20260417").assertIsDisplayed()
        composeRule.onNodeWithTag(DATE_PICKER_BUTTON_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(CURRENT_DATE_TAG).assertIsDisplayed()
    }

    @Test
    fun pendingLatestRenderLoadingToolbarAndDatePickerThroughMockHttp() {
        val releaseLatest = CompletableDeferred<Unit>()
        ZhihuMockApi.mockJson(
            method = HttpMethod.Get,
            url = DAILY_LATEST_URL,
            body = dailyStoriesResponse(date = "20260418", storyIds = 1..12),
            beforeRespond = { releaseLatest.await() },
        )
        ZhihuMockApi.mockJsonPrefix(
            method = HttpMethod.Get,
            urlPrefix = DAILY_BEFORE_PREFIX,
            body = dailyStoriesResponse(date = "20260416", storyIds = 31..54),
        )

        try {
            composeRule.setScreenContent {
                DailyScreen()
            }

            composeRule.waitUntilTagExists(LOADING_TAG)
            composeRule.onNodeWithTag(TITLE_TAG).assertIsDisplayed()
            composeRule.onNodeWithText("知乎日报").assertIsDisplayed()
            composeRule.onNodeWithText("正在加载...").assertIsDisplayed()
            composeRule.onNodeWithTag(DATE_PICKER_BUTTON_TAG).assertIsDisplayed().performClick()
            composeRule.onNodeWithText("确认").assertIsDisplayed().performClick()
            composeRule.waitUntil(timeoutMillis = 5_000) {
                ZhihuMockApi.requestCount(method = HttpMethod.Get, urlSubstring = "/api/4/stories/before/") >= 1
            }
            composeRule.waitUntilTagExists(storyTag(31))

            composeRule.onNodeWithText("固定日报问题 31").assertIsDisplayed()
            composeRule.onNodeWithTag(CURRENT_DATE_TAG).assertTextEquals("2026年04月16日")
        } finally {
            releaseLatest.complete(Unit)
        }
    }

    @Test
    fun datePickerReloadsThroughMockHttp() {
        // The date picker should call DailyViewModel.loadDate() instead of using a UI-only test
        // callback, so this route accepts any computed before/{date} request.
        mockLatest(date = "20260418", storyIds = 1..12)
        ZhihuMockApi.mockJsonPrefix(
            method = HttpMethod.Get,
            urlPrefix = DAILY_BEFORE_PREFIX,
            body = dailyStoriesResponse(date = "20260416", storyIds = 31..54),
        )

        composeRule.setScreenContent {
            DailyScreen()
        }

        composeRule.waitUntilTagExists(storyTag(1))
        composeRule.onNodeWithTag(DATE_PICKER_BUTTON_TAG).assertIsDisplayed().performClick()
        composeRule.onNodeWithText("确认").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            ZhihuMockApi.requestCount(method = HttpMethod.Get, urlSubstring = "/api/4/stories/before/") >= 1
        }
        composeRule.waitUntilTagExists(storyTag(31))

        composeRule.onNodeWithText("固定日报问题 31").assertIsDisplayed()
        composeRule.onNodeWithTag(CURRENT_DATE_TAG).assertTextEquals("2026年04月16日")
    }

    @Test
    fun errorAndEmptyStatesRenderFromViewModelResults() {
        // Error and empty rendering are still deterministic, but the states now come from the
        // production ViewModel's HTTP result handling instead of direct UI snapshot injection.
        mockLatestBody("""{"date":"20260418"}""")
        composeRule.setScreenContent {
            DailyScreen()
        }
        composeRule.waitUntilTagExists(ERROR_TAG)
        composeRule.onNodeWithText("加载失败:", substring = true).assertIsDisplayed()

        mockLatest(date = "20260418", storyIds = emptyList())
        composeRule.setScreenContent {
            DailyScreen()
        }
        composeRule.waitUntilTagExists(EMPTY_TAG)
        composeRule.onNodeWithTag(EMPTY_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("暂无内容").assertIsDisplayed()
    }

    private fun mockLatest(date: String, storyIds: Iterable<Int>) {
        mockLatestBody(dailyStoriesResponse(date, storyIds))
    }

    private fun mockLatestBody(body: String) {
        ZhihuMockApi.mockJson(
            method = HttpMethod.Get,
            url = DAILY_LATEST_URL,
            body = body,
        )
    }

    private fun mockBefore(apiDate: String, responseDate: String, storyIds: Iterable<Int>) {
        ZhihuMockApi.mockJson(
            method = HttpMethod.Get,
            url = "$DAILY_BEFORE_PREFIX$apiDate",
            body = dailyStoriesResponse(responseDate, storyIds),
        )
    }

    private fun dailyStoriesResponse(date: String, storyIds: Iterable<Int>): String {
        val stories = storyIds.joinToString(",") { index ->
            """
            {
              "id": $index,
              "title": "固定日报问题 $index",
              "url": "https://example.com/daily/$index",
              "hint": "固定日报摘要 $index",
              "images": [],
              "type": 0
            }
            """.trimIndent()
        }
        return """
            {
              "date": "$date",
              "stories": [$stories]
            }
            """.trimIndent()
    }

    private fun MainActivityComposeRule.waitUntilTagExists(tag: String) {
        waitUntil("Expected node with tag $tag", timeoutMillis = 5_000) {
            onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val DAILY_LATEST_URL = "https://news-at.zhihu.com/api/4/stories/latest"
        const val DAILY_BEFORE_PREFIX = "https://news-at.zhihu.com/api/4/stories/before/"
        const val TITLE_TAG = "daily_screen_title"
        const val CURRENT_DATE_TAG = "daily_screen_current_date"
        const val DATE_PICKER_BUTTON_TAG = "daily_screen_date_picker_button"
        const val ERROR_TAG = "daily_screen_error"
        const val EMPTY_TAG = "daily_screen_empty"
        const val LOADING_TAG = "daily_screen_loading"
        const val LIST_TAG = "daily_screen_list"
        const val SECTION_TAG_PREFIX = "daily_screen_section_"

        fun storyTag(storyId: Int) = "daily_screen_story_$storyId"
    }
}
