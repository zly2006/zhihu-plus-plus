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

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.zly2006.zhihu.shared.theme.ThemeMode
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.theme.AndroidThemeSettings
import com.github.zly2006.zhihu.theme.ZhihuTheme
import com.github.zly2006.zhihu.ui.subscreens.IdentityManagementRuntime
import com.github.zly2006.zhihu.ui.subscreens.rememberIdentityManagementRuntime
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import androidx.compose.ui.graphics.Color as ComposeColor

@RunWith(AndroidJUnit4::class)
class SystemBarsInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
        AndroidThemeSettings.setThemeMode(composeRule.activity, ThemeMode.DARK)
    }

    @Test
    fun identityChangeRestartKeepsDarkThemeStatusBarEdgeToEdge() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val originalActivity = composeRule.activity
        val runtime = AtomicReference<IdentityManagementRuntime>()
        instrumentation.runOnMainSync {
            originalActivity.setContent {
                ZhihuTheme {
                    val identityRuntime = rememberIdentityManagementRuntime()
                    runtime.set(identityRuntime)
                    SolidThemeSurface()
                }
            }
        }
        composeRule.waitForIdle()
        assertNotNull("Identity management runtime was not composed", runtime.get())
        assertStatusBarColor(originalActivity, "before identity restart")

        val relaunchedActivity = AtomicReference<MainActivity>()
        val resumedLatch = CountDownLatch(1)
        val callbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                if (activity is MainActivity && activity !== originalActivity) {
                    relaunchedActivity.set(activity)
                    resumedLatch.countDown()
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

            override fun onActivityStarted(activity: Activity) = Unit

            override fun onActivityPaused(activity: Activity) = Unit

            override fun onActivityStopped(activity: Activity) = Unit

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

            override fun onActivityDestroyed(activity: Activity) = Unit
        }
        originalActivity.application.registerActivityLifecycleCallbacks(callbacks)
        try {
            instrumentation.runOnMainSync {
                runtime.get().reloadApplication()
            }
            assertTrue(
                "Identity restart did not launch a fresh MainActivity",
                resumedLatch.await(10, TimeUnit.SECONDS),
            )
        } finally {
            originalActivity.application.unregisterActivityLifecycleCallbacks(callbacks)
        }

        val activity = checkNotNull(relaunchedActivity.get())
        instrumentation.runOnMainSync {
            activity.setContent {
                ZhihuTheme {
                    SolidThemeSurface()
                }
            }
        }
        instrumentation.waitForIdleSync()

        val deadline = SystemClock.uptimeMillis() + 5_000
        while (
            sampleStatusBarColor(activity) != SOLID_SURFACE_COLOR &&
            SystemClock.uptimeMillis() < deadline
        ) {
            instrumentation.waitForIdleSync()
        }
        assertStatusBarColor(activity, "after identity restart")

        val screenshot = instrumentation.uiAutomation.takeScreenshot()
        FileOutputStream(activity.cacheDir.resolve("system-bars-after-identity-restart.png")).use {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun assertStatusBarColor(
        activity: MainActivity,
        stage: String,
    ) {
        val statusBarColor = sampleStatusBarColor(activity)
        assertTrue(
            "$stage: expected status bar ${SOLID_SURFACE_COLOR.toUInt().toString(16)}, " +
                "but was ${statusBarColor.toUInt().toString(16)}",
            statusBarColor == SOLID_SURFACE_COLOR,
        )
    }

    private fun sampleStatusBarColor(activity: MainActivity): Int {
        val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val statusBarHeight = ViewCompat
            .getRootWindowInsets(activity.window.decorView)
            ?.getInsets(WindowInsetsCompat.Type.statusBars())
            ?.top
            ?: error("Status bar insets unavailable")
        val x = screenshot.width / 2
        return screenshot.getPixel(x, statusBarHeight / 2)
    }
}

@Composable
private fun SolidThemeSurface() {
    Box(
        Modifier
            .fillMaxSize()
            .background(ComposeColor(SOLID_SURFACE_COLOR)),
    )
}

private const val SOLID_SURFACE_COLOR = 0xFF121212.toInt()
