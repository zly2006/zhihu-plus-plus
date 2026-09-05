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

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.data.RecommendationMode
import com.github.zly2006.zhihu.data.ZhihuJson
import com.github.zly2006.zhihu.data.toFeedDisplayItemNavDestinationJson
import com.github.zly2006.zhihu.navigation.Search
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.ZhihuMockApi
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.HOME_FEED_LIST_TAG
import com.github.zly2006.zhihu.ui.HomeScreen
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.ZHIHU_PLUS_AUTHOR_PINS_URL
import com.github.zly2006.zhihu.ui.homeAuthorPollAnnouncementTag
import com.github.zly2006.zhihu.ui.homePinAnnouncementReadKey
import com.github.zly2006.zhihu.updater.UpdateManager
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedViewModel
import io.ktor.http.HttpMethod
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeAnnouncementRegressionInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.setScreenContent {}
        composeRule.resetAppPreferences()
        composeRule.activity.runOnUiThread {
            UpdateManager.updateState.value = UpdateManager.UpdateState.NoUpdate
            ViewModelProvider(composeRule.activity)[HomeFeedViewModel::class.java].apply {
                allData.clear()
                debugData.clear()
                displayItems.clear()
                addDisplayItems(
                    List(8) { index ->
                        FeedDisplayItem(
                            title = "离线条目 ${index.toString().padStart(2, '0')}",
                            summary = "这是第 ${index + 1} 条用于首页公告 instrument test 的离线摘要。",
                            details = "离线验证 · 固定假数据",
                            feed = null,
                            navDestinationJson =
                                Search(query = "fixture-$index").toFeedDisplayItemNavDestinationJson(),
                        )
                    },
                )
            }
        }
        composeRule.waitForIdle()
    }

    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/476
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/479
     * Multi-item read-state correction: https://github.com/zly2006/zhihu-plus-plus/commit/d4b935329cb316a0b3793e807ca23cf4fb448f7c
     */
    @Test
    fun authorAnnouncements_keepUnreadItemsWhenOneIsDismissed() {
        val firstPinId = 2064846692340470567L
        val secondPinId = 2064847476998279874L
        val firstPin = authorTopicPin(firstPinId, "第一条开发动态")
        val secondPin = authorTopicPin(secondPinId, "第二条开发动态")
        mockAuthorAnnouncements(firstPin, secondPin)
        composeRule.activity.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).edit(commit = true) {
            putBoolean("duo3_home_account", false)
            putBoolean("showRefreshFab", false)
            putBoolean("loginForRecommendation", true)
            putBoolean("survey_feedback_done", true)
            putString("recommendationMode", RecommendationMode.WEB.key)
        }
        composeRule.setScreenContent {
            HomeScreen(
                scrollToTopTrigger = 0,
                innerPadding = PaddingValues(),
            )
        }
        composeRule.waitForIdle()

        composeRule.waitUntilRequestCount(1)
        composeRule.waitUntilHomeFeedTagExists(homeAuthorPollAnnouncementTag(firstPinId))
        composeRule.waitUntilHomeFeedTagExists(homeAuthorPollAnnouncementTag(secondPinId))
        composeRule
            .onNode(hasText("关闭") and hasAnyAncestor(hasTestTag(homeAuthorPollAnnouncementTag(firstPinId))))
            .performClick()
        composeRule.waitUntil {
            composeRule.activity
                .getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .getBoolean(homePinAnnouncementReadKey(firstPinId), false)
        }
        composeRule.onNodeWithTag(homeAuthorPollAnnouncementTag(firstPinId)).assertDoesNotExist()
        composeRule.onNodeWithTag(homeAuthorPollAnnouncementTag(secondPinId)).assertExists()
        assertEquals(
            false,
            composeRule.activity
                .getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .getBoolean(homePinAnnouncementReadKey(secondPinId), false),
        )

        val thirdPinId = 2064848000000000000L
        mockAuthorAnnouncements(
            authorTopicPin(thirdPinId, "关于来自123duo3的UI修改<br><p>详细投票说明</p>"),
            secondPin,
            firstPin,
        )
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        composeRule.waitUntilRequestCount(2)
        composeRule.waitUntilHomeFeedTagExists(homeAuthorPollAnnouncementTag(secondPinId))
        composeRule.waitUntilHomeFeedTagExists(homeAuthorPollAnnouncementTag(thirdPinId))
        composeRule.onNodeWithTag(homeAuthorPollAnnouncementTag(firstPinId)).assertDoesNotExist()
        composeRule.onNodeWithText("关于来自123duo3的UI修改").assertExists()
        composeRule.onNodeWithText("详细投票说明").assertDoesNotExist()
    }

    private fun MainActivityComposeRule.waitUntilHomeFeedTagExists(tag: String) {
        waitUntil("Expected tag $tag in home feed", timeoutMillis = 5_000) {
            runCatching {
                onNodeWithTag(HOME_FEED_LIST_TAG).performScrollToNode(hasTestTag(tag))
                true
            }.getOrDefault(false)
        }
    }

    private fun MainActivityComposeRule.waitUntilRequestCount(count: Int) {
        waitUntil("Expected $count author announcement requests", timeoutMillis = 5_000) {
            ZhihuMockApi.requestCount(HttpMethod.Get, ZHIHU_PLUS_AUTHOR_PINS_URL) == count
        }
    }

    private fun authorTopicPin(
        pinId: Long,
        title: String,
    ) = DataHolder.Pin(
        id = pinId.toString(),
        url = "https://www.zhihu.com/pin/$pinId",
        author = DataHolder.Author(
            avatarUrl = "",
            gender = 0,
            headline = "",
            id = "zhihu-plus-author",
            isAdvertiser = false,
            isOrg = false,
            name = "知乎++作者",
            type = "people",
            url = "https://www.zhihu.com/people/scanmenge",
            urlToken = "scanmenge",
            userType = "people",
        ),
        excerptTitle = title,
        created = System.currentTimeMillis() / 1_000,
        topics = listOf(
            DataHolder.Topic(
                id = "2064846813258109867",
                type = "topic",
                url = "zhihu://topic/2064846813258109867/pin20",
                name = "zhihuplusplus",
            ),
        ),
    )

    private fun mockAuthorAnnouncements(vararg pins: DataHolder.Pin) {
        ZhihuMockApi.mockJson(
            method = HttpMethod.Get,
            url = ZHIHU_PLUS_AUTHOR_PINS_URL,
            body = """{"data":${ZhihuJson.json.encodeToString(pins.toList())}}""",
        )
    }
}
