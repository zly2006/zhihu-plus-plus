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

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Search
import com.github.zly2006.zhihu.test.ZhihuMockApi
import com.github.zly2006.zhihu.test.performHorizontalSwipeCycle
import com.github.zly2006.zhihu.test.performVerticalSwipeCycle
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.SearchScreen
import io.ktor.http.HttpMethod
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchScreenInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
    }

    @Test
    fun searchBoxEditingClearImeAndBackAreDeterministic() {
        // This test disables hot-search entirely so the screen stays offline and deterministic.
        // Expected behavior:
        // 1. The search field starts empty and shows its placeholder.
        // 2. Text input and replacement update the editable value exactly.
        // 3. Triggering the IME search action navigates with the final query instead of touching the network.
        // 4. The clear button resets the field back to the placeholder state.
        // 5. Pressing back only records a back event and does not create any extra navigation entries.
        composeRule.activity
            .getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("showSearchHotSearch", false)
            .commit()

        val recordingNavigator = composeRule.setScreenContent {
            SearchScreen(
                innerPadding = PaddingValues(),
                search = Search(),
            )
        }

        val searchInput = composeRule.onNodeWithTag("search_input")
        searchInput.assertIsDisplayed()
        composeRule.onNodeWithText("搜索内容").assertIsDisplayed()

        // Typing should produce an exact editable value and reveal the explicit clear affordance.
        searchInput.performTextInput("compose")
        searchInput.assertTextEquals("compose")
        composeRule.onNodeWithTag("search_clear_button").assertIsDisplayed()

        // Replacing the text should fully overwrite the current query, and IME search should navigate with it.
        searchInput.performTextReplacement("jetpack compose")
        searchInput.assertTextEquals("jetpack compose")
        searchInput.performImeAction()
        composeRule.waitForIdle()
        assertEquals(listOf(Search(query = "jetpack compose")), recordingNavigator.destinations)

        // Clearing should return the field to an empty state and restore the placeholder.
        composeRule.onNodeWithTag("search_clear_button").performClick()
        composeRule.onAllNodesWithTag("search_clear_button").assertCountEquals(0)
        composeRule.onNodeWithText("搜索内容").assertIsDisplayed()

        // Back should only increment the recorded back count and leave prior navigation untouched.
        composeRule.onNodeWithTag("search_back_button").performClick()
        composeRule.waitForIdle()
        assertEquals(1, recordingNavigator.backCount)
        assertEquals(listOf(Search(query = "jetpack compose")), recordingNavigator.destinations)
    }

    @Test
    fun mockedHotSearchMenuActionsAndSwipesStayStableAcrossRealFetchCalls() {
        // This test uses the real SearchScreen fetch path but replaces Zhihu's endpoint at the HTTP
        // layer with a Ktor MockEngine response.
        // Expected behavior:
        // 1. The mocked hot-search list renders after the screen performs its real fetchHotSearch()
        //    call through AccountData.fetchGet().
        // 2. Pressing refresh performs a second mocked HTTP request and keeps the rendered list stable.
        // 3. Opening the overflow menu exposes the settings action and navigates to the expected destination.
        // 4. Vertical and horizontal swipe cycles leave the mocked content intact instead of breaking layout state.
        ZhihuMockApi.mockJson(
            method = HttpMethod.Get,
            url = "https://www.zhihu.com/api/v4/search/hot_search",
            body =
                """
                {
                  "hot_search_queries": [
                    {"query": "mock alpha"},
                    {"query": "mock beta"},
                    {"query": "mock gamma"}
                  ]
                }
                """.trimIndent(),
        )
        val recordingNavigator = composeRule.setScreenContent {
            SearchScreen(
                innerPadding = PaddingValues(),
                search = Search(),
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            ZhihuMockApi.requestCount(
                method = HttpMethod.Get,
                urlSubstring = "/api/v4/search/hot_search",
            ) == 1
        }
        composeRule.onNodeWithTag("search_hot_list").assertIsDisplayed()
        composeRule.onNodeWithText("mock alpha").assertIsDisplayed()
        composeRule.onNodeWithText("mock gamma").assertIsDisplayed()

        // Refresh should trigger the real mocked HTTP fetch again, so the list must remain visible after the click.
        composeRule.onNodeWithTag("search_hot_refresh_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            ZhihuMockApi.requestCount(
                method = HttpMethod.Get,
                urlSubstring = "/api/v4/search/hot_search",
            ) == 2
        }
        composeRule.onNodeWithText("mock alpha").assertIsDisplayed()
        assertEquals(0, recordingNavigator.destinations.size)

        // The overflow menu should reveal the stable settings action and navigate to appearance settings.
        composeRule.onNodeWithTag("search_hot_more_button").performClick()
        composeRule.onNodeWithText("关闭热搜显示").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(
            listOf(Account.AppearanceSettings("showSearchHotSearch")),
            recordingNavigator.destinations,
        )

        // Swipe cycles should not disturb the injected offline list or create any extra navigation side effects.
        composeRule.onNodeWithTag("search_hot_list").performVerticalSwipeCycle()
        composeRule.onNodeWithTag("search_hot_list").performHorizontalSwipeCycle()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("mock alpha").assertIsDisplayed()
        composeRule.onNodeWithText("mock gamma").assertIsDisplayed()
        assertEquals(
            2,
            ZhihuMockApi.requestCount(
                method = HttpMethod.Get,
                urlSubstring = "/api/v4/search/hot_search",
            ),
        )
        assertEquals(
            listOf(Account.AppearanceSettings("showSearchHotSearch")),
            recordingNavigator.destinations,
        )
        assertEquals(0, recordingNavigator.backCount)
    }
}
