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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.test.performVerticalSwipeCycle
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.theme.ThemeMode
import com.github.zly2006.zhihu.ui.subscreens.ColorSchemeScreen
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

private val TestPrimaryColor = Color(0xFF3F51B5)
private val TestLightBackground = Color(0xFFF6F1FF)
private val TestDarkBackground = Color(0xFF10141C)

@RunWith(AndroidJUnit4::class)
class ColorSchemeScreenInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
        composeRule.activity.runOnUiThread {
            ThemeManager.initialize(composeRule.activity)
            ThemeManager.setUseDynamicColor(composeRule.activity, false)
            ThemeManager.setCustomColor(composeRule.activity, TestPrimaryColor)
            ThemeManager.setBackgroundColor(composeRule.activity, TestLightBackground, isDark = false)
            ThemeManager.setBackgroundColor(composeRule.activity, TestDarkBackground, isDark = true)
            ThemeManager.setThemeMode(composeRule.activity, ThemeMode.LIGHT)
        }
        composeRule.waitForIdle()
    }

    @Test
    fun colorSchemeScreen_scroll_click_and_theme_switch_stay_deterministic() {
        val observedBackgroundArgb = AtomicInteger(Int.MIN_VALUE)
        val navigator = composeRule.setScreenContent {
            val observedBackground = MaterialTheme.colorScheme.background
            SideEffect {
                observedBackgroundArgb.set(observedBackground.toArgb())
            }
            ColorSchemeScreen(innerPadding = PaddingValues())
        }
        val scrollContainer = composeRule.onNode(hasScrollAction())

        // The screen should start in the configured light theme, show the header and top groups,
        // and keep the bottom-most "Scrim" section offscreen until an explicit scroll happens.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            observedBackgroundArgb.get() == TestLightBackground.toArgb()
        }
        composeRule.onNodeWithText("Color Scheme").assertIsDisplayed()
        composeRule.onNodeWithText("Primary").assertIsDisplayed()
        composeRule.onNodeWithText("Scrim").assertIsNotDisplayed()

        // A short swipe cycle should exercise the shared touch helper without changing the logical
        // starting state of the screen: the top section stays visible and the bottom section does not
        // accidentally appear just because of a small gesture.
        scrollContainer.performVerticalSwipeCycle()
        composeRule.onNodeWithText("Primary").assertIsDisplayed()
        composeRule.onNodeWithText("Scrim").assertIsNotDisplayed()

        // A semantics-driven scroll should deterministically reach the bottom of the page, making the
        // test independent of screen size and avoiding coordinate-based flakiness.
        scrollContainer.performScrollToNode(hasText("Scrim"))
        composeRule.onNodeWithText("Scrim").assertIsDisplayed()

        // Switching to dark mode should update the composed color scheme in place while preserving the
        // current scroll position, so the bottom section remains visible after recomposition.
        composeRule.activity.runOnUiThread {
            ThemeManager.setThemeMode(composeRule.activity, ThemeMode.DARK)
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            observedBackgroundArgb.get() == TestDarkBackground.toArgb()
        }
        composeRule.onNodeWithText("Scrim").assertIsDisplayed()

        // Switching back to light mode should restore the original configured background color without
        // resetting the page state, proving that repeated theme transitions are stable and reversible.
        composeRule.activity.runOnUiThread {
            ThemeManager.setThemeMode(composeRule.activity, ThemeMode.LIGHT)
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            observedBackgroundArgb.get() == TestLightBackground.toArgb()
        }
        composeRule.onNodeWithText("Scrim").assertIsDisplayed()

        // Tapping the top app bar navigation icon should delegate exactly one back event to the shared
        // recording navigator, confirming that click handling still works after scrolling and recomposition.
        composeRule.onNodeWithContentDescription("返回").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(1, navigator.backCount)
    }
}
