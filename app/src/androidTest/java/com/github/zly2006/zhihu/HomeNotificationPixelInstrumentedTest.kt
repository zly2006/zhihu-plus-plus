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
import android.graphics.Bitmap
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.zly2006.zhihu.data.ZHIHU_ME_URL
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.ZhihuMockApi
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.HOME_NOTIFICATION_BUTTON_CONTENT_TAG
import com.github.zly2006.zhihu.ui.HomeScreen
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import io.ktor.http.HttpMethod
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt
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
                innerPadding = PaddingValues(),
            )
        }
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/696
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/709
     */
    @Test
    fun homeNotificationButtonContentBoxKeepsRoundedBackgroundWithUnreadBadge() {
        composeRule.waitUntil("Expected unread count request", timeoutMillis = 5_000) {
            ZhihuMockApi.requestCount(HttpMethod.Get, ZHIHU_ME_URL) > 0
        }
        composeRule.waitForIdle()

        val contentBounds = composeRule
            .onNodeWithTag(HOME_NOTIFICATION_BUTTON_CONTENT_TAG, useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
        val rootImage = composeRule.onRoot().captureToImage()
        val screenshot = File(
            requireNotNull(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)),
            "home-notification-button-content.png",
        )
        FileOutputStream(screenshot).use { stream ->
            rootImage.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
        }

        val pixels = rootImage.toPixelMap()
        val left = contentBounds.left.roundToInt().coerceIn(0, pixels.width - 1)
        val top = contentBounds.top.roundToInt().coerceIn(0, pixels.height - 1)
        val right = ceil(contentBounds.right).toInt().coerceIn(left + 1, pixels.width) - 1
        val bottom = ceil(contentBounds.bottom).toInt().coerceIn(top + 1, pixels.height) - 1
        val backgroundX = (left - 12).coerceAtLeast(0)
        val backgroundY = ((top + bottom) / 2).coerceIn(0, pixels.height - 1)
        val background = pixels[backgroundX, backgroundY]

        listOf(
            left + 1 to top + 1,
            right - 1 to top + 1,
            left + 1 to bottom - 1,
            right - 1 to bottom - 1,
        ).forEach { (x, y) ->
            val corner = pixels[x.coerceIn(0, pixels.width - 1), y.coerceIn(0, pixels.height - 1)]
            assertTrue(
                "Notification icon content box corners must stay on the app-bar background; corner=$corner background=$background screenshot=${screenshot.absolutePath}",
                isCloseTo(corner, background),
            )
        }

        val foregroundPixels = (top + 2..bottom - 2).sumOf { y ->
            (left + 2..right - 2).count { x ->
                !isCloseTo(pixels[x, y], background)
            }
        }
        assertTrue(
            "Notification icon content box must contain visible foreground pixels for the icon/badge; found $foregroundPixels screenshot=${screenshot.absolutePath}",
            foregroundPixels >= 80,
        )
    }

    private fun isCloseTo(
        first: androidx.compose.ui.graphics.Color,
        second: androidx.compose.ui.graphics.Color,
    ): Boolean =
        abs(first.red - second.red) < 0.08f &&
            abs(first.green - second.green) < 0.08f &&
            abs(first.blue - second.blue) < 0.08f &&
            abs(first.alpha - second.alpha) < 0.08f
}
