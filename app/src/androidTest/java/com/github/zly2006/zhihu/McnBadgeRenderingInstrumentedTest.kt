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

import androidx.compose.foundation.layout.Row
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.OfficialBadge
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.components.AuthorBadge
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.McnBadge
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class McnBadgeRenderingInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
    }

    @Test
    fun feedCardRendersMcnBadgeFromDisplayItem() {
        composeRule.setScreenContent {
            FeedCard(
                item = FeedDisplayItem(
                    title = "离线标题",
                    summary = "离线摘要",
                    details = "离线详情",
                    feed = null,
                    avatarSrc = "https://pic.example/avatar.png",
                    authorName = "离线作者",
                    authorMcnCompany = "杭州含章文化传播有限公司",
                ),
                onClick = {},
            )
        }

        composeRule
            .onNodeWithContentDescription("MCN机构：杭州含章文化传播有限公司")
            .assertIsDisplayed()
    }

    @Test
    fun mcnBadgeDoesNotReplaceOfficialBadge() {
        composeRule.setScreenContent {
            Row {
                AuthorBadge(
                    badge = OfficialBadge(
                        title = "优秀答主",
                        description = "官方认证",
                        iconUrl = "https://pic.example/official.png",
                    ),
                    compact = true,
                )
                McnBadge(mcnCompany = "杭州含章文化传播有限公司")
            }
        }

        composeRule.onNodeWithContentDescription("官方认证").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("MCN机构：杭州含章文化传播有限公司")
            .assertIsDisplayed()
    }
}
