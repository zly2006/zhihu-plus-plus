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
import kotlin.math.max
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
            body = """
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
                innerPadding = androidx.compose.foundation.layout.PaddingValues(),
            )
        }
    }

    /** The badge must be a complete rounded rectangle, not a parent-clipped fragment. */
    @Test
    fun unreadNotificationBadgeIsRenderedAsRoundedRectangle() {
        composeRule.waitUntil("Expected unread count request", timeoutMillis = 5_000) {
            ZhihuMockApi.requestCount(HttpMethod.Get, ZHIHU_ME_URL) > 0
        }
        composeRule.waitForIdle()

        val image = composeRule
            .onNodeWithTag(HOME_NOTIFICATION_BADGE_TAG, useUnmergedTree = true)
            .captureToImage()
        val pixels = image.toPixelMap()
        assertTrue("badge image is too small: ${pixels.width}x${pixels.height}", pixels.width >= 8 && pixels.height >= 8)

        val mask = Array(pixels.height) { y ->
            BooleanArray(pixels.width) { x -> isBadgeForeground(pixels[x, y]) }
        }
        val rows = (0 until pixels.height).filter { y -> mask[y].any { it } }
        val columns = (0 until pixels.width).filter { x -> (0 until pixels.height).any { y -> mask[y][x] } }
        assertTrue("badge has no rendered foreground", rows.isNotEmpty() && columns.isNotEmpty())

        val left = columns.first()
        val right = columns.last()
        val top = rows.first()
        val bottom = rows.last()
        val centerX = (left + right) / 2
        val centerY = (top + bottom) / 2

        // All four outer corners must be outside the fill, while every edge midpoint is filled.
        listOf(left to top, right to top, left to bottom, right to bottom).forEach { (x, y) ->
            assertTrue("badge corner ($x,$y) is filled; shape is clipped or square", !mask[y][x])
        }
        listOf(
            centerX to top,
            centerX to bottom,
            left to centerY,
            right to centerY,
        ).forEach { (x, y) ->
            assertTrue("badge edge midpoint ($x,$y) is empty; rounded rectangle is incomplete", mask[y][x])
        }

        val horizontalSpan = max(
            (left..right).count { x -> mask[centerY][x] },
            (top..bottom).count { y -> mask[y][centerX] },
        )
        val cornerSpan = max(
            (left..right).count { x -> mask[top][x] },
            (left..right).count { x -> mask[bottom][x] },
        )
        assertTrue(
            "badge has no rounded-corner taper: centerSpan=$horizontalSpan edgeSpan=$cornerSpan",
            horizontalSpan > cornerSpan,
        )
    }

    private fun isBadgeForeground(color: Color): Boolean =
        color.alpha > 0.5f &&
            color.red > color.green + 0.12f &&
            color.red > color.blue + 0.12f
}
