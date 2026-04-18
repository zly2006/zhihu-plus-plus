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
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.subscreens.OpenSourceLicensesScreen
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.util.strippedLicenseContent
import com.mikepenz.aboutlibraries.util.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpenSourceLicensesScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
    }

    @Test
    fun opensLicenseDialogAfterDeterministicScrollAndHandlesBackNavigation() {
        // Expected behavior:
        // 1. The screen must render the generated open-source library list backed by aboutlibraries.json.
        // 2. The test must reach a nontrivial target row using index-based LazyColumn scrolling instead of
        //    pixel swipes, because index scrolling is deterministic and resilient to device timing variance.
        // 3. Clicking a generated library with embedded license text must open the in-app license dialog,
        //    and tapping the dialog confirmation button must close it again.
        // 4. The top app bar back button must call navigator.onNavigateBack exactly once after the dialog flow.
        val generatedLibraries = loadGeneratedLibraries(composeRule.activity)
        assertFalse("Expected generated open-source libraries to exist", generatedLibraries.isEmpty())

        val targetLibrary = generatedLibraries.findDeterministicDialogTarget()
        val targetListIndex = targetLibrary.index + manualLibraryHeaderCount()
        val dialogSnippet = targetLibrary.value.firstLicenseBodySnippet()
        val topAnchorText = topAnchorText(generatedLibraries)

        val recordingNavigator = composeRule.setScreenContent {
            OpenSourceLicensesScreen()
        }

        val licensesList = composeRule.onNode(hasScrollToIndexAction())

        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                licensesList.performScrollToIndex(targetListIndex)
                composeRule.onAllNodesWithText(targetLibrary.value.name).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }

        composeRule.onNodeWithText(targetLibrary.value.name).assertIsDisplayed().performClick()
        composeRule.onNodeWithText(dialogSnippet, substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("OK").assertHasClickAction().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("OK").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithText("OK").assertDoesNotExist()

        licensesList.performScrollToIndex(0)
        composeRule.onNodeWithText(topAnchorText).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("返回").assertHasClickAction().performClick()

        composeRule.runOnIdle {
            assertEquals("Back navigation should be recorded exactly once", 1, recordingNavigator.backCount)
        }
    }

    private fun loadGeneratedLibraries(context: Context): List<Library> =
        Libs
            .Builder()
            .withContext(context)
            .build()
            .libraries

    private fun List<Library>.findDeterministicDialogTarget(): IndexedValue<Library> =
        withIndex().lastOrNull { (_, library) ->
            library.strippedLicenseContent.isNotBlank()
        } ?: error("Expected at least one library with embedded license text")

    private fun Library.firstLicenseBodySnippet(): String =
        strippedLicenseContent
            .lineSequence()
            .map(String::trim)
            .firstOrNull { it.isNotEmpty() }
            ?.take(48)
            ?: error("Expected a non-empty license body snippet for $name")

    private fun manualLibraryHeaderCount(): Int = if (BuildConfig.IS_LITE) 0 else 1

    private fun topAnchorText(generatedLibraries: List<Library>): String =
        if (BuildConfig.IS_LITE) {
            generatedLibraries.first().name
        } else {
            "Full 版本特有组件"
        }
}
