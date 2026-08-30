/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.github.zly2006.zhihu

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.data.ZHIHU_ME_URL
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.ZhihuMockApi
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.HOME_NOTIFICATION_BADGE_TAG
import com.github.zly2006.zhihu.ui.HomeScreen
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import io.ktor.http.HttpMethod
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeNotificationPixelInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
        ZhihuMockApi.mockJson(
            method = HttpMethod.Get,
            url = ZHIHU_ME_URL,
            body =
                """
                {
                  "defaultNotificationsCount": 99,
                  "followNotificationsCount": 99,
                  "voteThankNotificationsCount": 99
                }
                """.trimIndent(),
        )
        composeRule.activity.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).edit(commit = true) {
            putBoolean("duo3_home_account", false)
            putBoolean("showRefreshFab", false)
        }
        composeRule.setScreenContent {
            HomeScreen(
                scrollToTopTrigger = 0,
                innerPadding = androidx.compose.foundation.layout
                    .PaddingValues(),
            )
        }
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/696
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/709
     * Target state: an unread notification badge is fully visible with rounded corners.
     * Verifies the badge's rendered pixels, so restoring the clipped IconButton fails.
     */
    @Test
    fun unreadNotificationBadgeIsRenderedWithoutParentClipping() {
        composeRule.waitUntil("Expected unread count request", timeoutMillis = 5_000) {
            ZhihuMockApi.requestCount(HttpMethod.Get, ZHIHU_ME_URL) > 0
        }
        composeRule.waitForIdle()

        val image = composeRule
            .onNodeWithTag(HOME_NOTIFICATION_BADGE_TAG)
            .assertIsDisplayed()
            .captureToImage()
        val pixels = image.toPixelMap()
        val foreground = { x: Int, y: Int -> isBadgeForeground(pixels[x, y]) }
        val centerX = pixels.width / 2
        val centerY = pixels.height / 2

        assertTrue("badge must have a visible fill", foreground(centerX, centerY))
        assertTrue("badge top edge must be rendered", foreground(centerX, 0))
        assertTrue("badge bottom edge must be rendered", foreground(centerX, pixels.height - 1))
        assertTrue("badge left edge must be rendered", foreground(0, centerY))
        assertTrue("badge right edge must be rendered", foreground(pixels.width - 1, centerY))
        assertTrue("badge top-left corner must remain rounded", !foreground(0, 0))
        assertTrue("badge top-right corner must remain rounded", !foreground(pixels.width - 1, 0))
    }

    private fun isBadgeForeground(color: Color): Boolean =
        color.alpha > 0.5f &&
            color.red > color.green + 0.12f &&
            color.red > color.blue + 0.12f
}
