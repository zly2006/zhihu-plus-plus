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

package com.chloemlla.zhplus

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chloemlla.zhplus.navigation.History
import com.chloemlla.zhplus.shared.data.FeedDisplayItem
import com.chloemlla.zhplus.shared.data.ZhihuJson
import com.chloemlla.zhplus.test.MainActivityComposeRule
import com.chloemlla.zhplus.test.RecordingNavigator
import com.chloemlla.zhplus.test.performVerticalSwipeCycle
import com.chloemlla.zhplus.test.pressSystemBack
import com.chloemlla.zhplus.test.resetAppPreferences
import com.chloemlla.zhplus.test.setScreenContent
import com.chloemlla.zhplus.ui.ONLINE_HISTORY_OVERFLOW_TAG
import com.chloemlla.zhplus.ui.OnlineHistoryScreen
import com.chloemlla.zhplus.viewmodel.PaginationEnvironment
import com.chloemlla.zhplus.viewmodel.feed.OnlineHistoryViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnlineHistoryScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule()

    private val onlineHistoryViewModel: OnlineHistoryViewModel
        get() = ViewModelProvider(composeRule.activity)[OnlineHistoryViewModel::class.java]

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
        clearDisplayItems()
    }

    @After
    fun tearDown() {
        clearDisplayItems()
    }

    @Test
    fun overflowMenuShowsStableActionsAndBackDismissesWithoutNavigation() {
        // Seed deterministic fake rows before composition so OnlineHistoryScreen skips the
        // automatic refresh path. This keeps the test fully local and makes the toolbar/menu
        // assertions independent from login state, network reachability, and remote history data.
        val navigator = setOnlineHistoryScreen()

        composeRule.onNodeWithText("历史记录").assertIsDisplayed()
        composeRule.onNodeWithTag(ONLINE_HISTORY_OVERFLOW_TAG).performClick()

        // The overflow menu is the only stable toolbar entry point on this screen, so it must
        // always expose both supported actions and be dismissible with the system back gesture
        // without emitting any app-level navigation callbacks.
        composeRule.onNodeWithText("查看本地历史记录").assertIsDisplayed()
        composeRule.onNodeWithText("清除历史记录").assertIsDisplayed()
        composeRule.pressSystemBack()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("查看本地历史记录").fetchSemanticsNodes().isEmpty() &&
                composeRule.onAllNodesWithText("清除历史记录").fetchSemanticsNodes().isEmpty()
        }

        composeRule.runOnIdle {
            assertEquals(0, navigator.destinations.size)
            assertEquals(0, navigator.backCount)
        }
    }

    @Test
    fun historyItemOverflowOffersSingleDeleteAction() {
        setOnlineHistoryScreen()

        // The toolbar is the first "更多选项" node; the next one belongs to the first visible
        // history card. Single-record deletion lives in that existing card menu rather than the
        // global toolbar menu.
        composeRule.onAllNodesWithContentDescription("更多选项")[1].performClick()
        composeRule.onNodeWithText("删除该条历史记录").assertIsDisplayed()
    }

    @Test
    fun singleDeletePostsDecodedPairAndRemovesRowAfterSuccess() {
        val client = HttpClient(
            MockEngine { request ->
                assertEquals(HttpMethod.Post, request.method)
                assertEquals("https://api.zhihu.com/read_history/batch_del", request.url.toString())
                assertEquals(
                    Json.parseToJsonElement(
                        """{"pairs":[{"content_token":"123","content_type":"answer"}],"clear":false}""",
                    ),
                    Json.parseToJsonElement((request.body as TextContent).text),
                )
                respond("", HttpStatusCode.OK)
            },
        )
        val response = ZhihuJson.json
            .parseToJsonElement(
                """
                {
                  "data": [
                    {
                      "card_type": "single_card",
                      "data": {
                        "header": {
                          "icon": "https://example.com/icon.png",
                          "title": "待删除的在线历史"
                        },
                        "action": {
                          "type": "router",
                          "url": "zhihu://answers/123"
                        },
                        "extra": {
                          "content_token": "123",
                          "content_type": "answer",
                          "read_time": 1716120000,
                          "question_token": "456"
                        }
                      }
                    }
                  ],
                  "paging": {
                    "is_end": true,
                    "is_start": true,
                    "totals": 1
                  }
                }
                """.trimIndent(),
            ).jsonObject
        val environment = object : PaginationEnvironment {
            override fun httpClient() = client

            override fun authenticatedCookies() = emptyMap<String, String>()

            override suspend fun fetchJson(url: String, include: String): JsonObject = response

            override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit
        }
        val viewModel = OnlineHistoryViewModel()

        composeRule.activity.runOnUiThread {
            viewModel.refresh(environment)
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.displayItems.singleOrNull()?.title == "待删除的在线历史"
        }

        runBlocking {
            viewModel.deleteItem(environment, viewModel.displayItems.single())
        }

        assertEquals(0, viewModel.displayItems.size)
    }

    @Test
    fun overflowMenuNavigatesLocallyAndClearDialogCancelsWithoutMutatingSeededRows() {
        // Populate the activity-scoped ViewModel with stable placeholder rows so the screen renders
        // a real lazy list but never depends on zhihu read-history responses during this test.
        val navigator = setOnlineHistoryScreen()

        // Choosing the local-history action should synchronously record one navigation event to the
        // History destination and close the menu, while keeping the current screen mounted because
        // the test host only records navigation instead of replacing the content tree.
        composeRule.onNodeWithTag(ONLINE_HISTORY_OVERFLOW_TAG).performClick()
        composeRule.onNodeWithText("查看本地历史记录").performClick()
        composeRule.runOnIdle {
            assertEquals(listOf(History), navigator.destinations)
            assertEquals(0, navigator.backCount)
        }

        // Opening the clear-history flow must show the confirmation copy and both dialog buttons,
        // but cancelling via the explicit secondary action should keep the seeded rows intact and
        // must not create any extra navigation event or trigger a destructive clear.
        composeRule.onNodeWithTag(ONLINE_HISTORY_OVERFLOW_TAG).performClick()
        composeRule.onNodeWithText("清除历史记录").performClick()
        composeRule.onNodeWithText("确认清除历史记录").assertIsDisplayed()
        composeRule.onNodeWithText("此操作会清除当前账号的在线和本地的全部历史记录。").assertIsDisplayed()
        composeRule.onNodeWithText("确认").assertIsDisplayed()
        composeRule.onNodeWithText("我再想想").assertIsDisplayed()
        composeRule.onNodeWithText("我再想想").performClick()
        composeRule.onNodeWithText("确认清除历史记录").assertDoesNotExist()
        composeRule.onNodeWithText(seedTitle(1)).assertExists()

        composeRule.runOnIdle {
            assertEquals(listOf(History), navigator.destinations)
            assertEquals(0, navigator.backCount)
        }
    }

    @Test
    fun listSwipeCyclesKeepToolbarMenuAndDialogInteractionsStable() {
        // Use enough fake rows to guarantee a scrollable lazy list. The swipe assertions then
        // exercise gesture handling on the actual list container without ever approaching the end
        // of pagination or invoking the network-backed clear-history confirmation path.
        val navigator = setOnlineHistoryScreen(itemCount = 24)

        composeRule.onNodeWithTag(LIST_TAG).assertExists()
        composeRule.onNodeWithTag(LIST_TAG).performVerticalSwipeCycle()

        // After the gesture cycle, the toolbar should remain interactive, the overflow menu
        // should still open normally, and dismissing the confirmation dialog with system back
        // should restore the untouched list state instead of navigating away or corrupting UI.
        composeRule.onNodeWithText("历史记录").assertIsDisplayed()
        composeRule.onNodeWithTag(ONLINE_HISTORY_OVERFLOW_TAG).performClick()
        composeRule.onNodeWithText("清除历史记录").performClick()
        composeRule.onNodeWithText("确认清除历史记录").assertIsDisplayed()
        composeRule.pressSystemBack()
        composeRule.onNodeWithText("确认清除历史记录").assertDoesNotExist()
        composeRule.onNodeWithTag(LIST_TAG).assertExists()

        composeRule.runOnIdle {
            assertEquals(0, navigator.destinations.size)
            assertEquals(0, navigator.backCount)
        }
    }

    private fun setOnlineHistoryScreen(itemCount: Int = 24): RecordingNavigator {
        seedDisplayItems(itemCount)
        return composeRule.setScreenContent {
            OnlineHistoryScreen()
        }
    }

    private fun seedDisplayItems(itemCount: Int) {
        composeRule.activity.runOnUiThread {
            onlineHistoryViewModel.displayItems.clear()
            onlineHistoryViewModel.displayItems.addAll(
                List(itemCount) { index ->
                    FeedDisplayItem(
                        title = seedTitle(index + 1),
                        summary = "用于 OnlineHistoryScreen 仪器测试的固定摘要 ${index + 1}",
                        details = "固定详情 ${index + 1}",
                        feed = null,
                        authorName = "作者 ${index + 1}",
                    )
                },
            )
        }
        composeRule.waitForIdle()
    }

    private fun clearDisplayItems() {
        composeRule.activity.runOnUiThread {
            onlineHistoryViewModel.displayItems.clear()
        }
        composeRule.waitForIdle()
    }

    private companion object {
        const val LIST_TAG = "online_history_list"

        fun seedTitle(index: Int) = "固定在线历史条目 $index"
    }
}
