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

package com.github.zly2006.zhihu.test

import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Navigator
import com.github.zly2006.zhihu.theme.ZhihuTheme
import com.github.zly2006.zhihu.ui.ZhihuMain
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

typealias MainActivityComposeRule =
    AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

class RecordingNavigator {
    private val navigateEvents = CopyOnWriteArrayList<NavDestination>()
    private val backEvents = AtomicInteger(0)

    val destinations: List<NavDestination>
        get() = navigateEvents.toList()

    val backCount: Int
        get() = backEvents.get()

    fun reset() {
        navigateEvents.clear()
        backEvents.set(0)
    }

    fun asNavigator(): Navigator = Navigator(
        onNavigate = { destination -> navigateEvents += destination },
        onNavigateBack = { backEvents.incrementAndGet() },
    )
}

fun MainActivityComposeRule.resetAppPreferences() {
    InstrumentedTestEnvironment.reseed(activity)
    waitForIdle()
}

fun MainActivityComposeRule.setScreenContent(
    recordingNavigator: RecordingNavigator = RecordingNavigator(),
    content: @Composable () -> Unit,
): RecordingNavigator {
    activity.setContent { }
    waitForIdle()
    activity.setContent {
        ZhihuTheme {
            CompositionLocalProvider(LocalNavigator provides recordingNavigator.asNavigator()) {
                content()
            }
        }
    }
    waitForIdle()
    return recordingNavigator
}

fun MainActivityComposeRule.setZhihuMainContent() {
    activity.setContent { }
    waitForIdle()
    activity.setContent {
        ZhihuTheme {
            ZhihuMain(navController = rememberNavController())
        }
    }
    waitForIdle()
}

fun SemanticsNodeInteraction.performVerticalSwipeCycle() {
    performTouchInput { swipeUp() }
    performTouchInput { swipeDown() }
}

fun SemanticsNodeInteraction.performHorizontalSwipeCycle() {
    performTouchInput { swipeLeft() }
    performTouchInput { swipeRight() }
}
