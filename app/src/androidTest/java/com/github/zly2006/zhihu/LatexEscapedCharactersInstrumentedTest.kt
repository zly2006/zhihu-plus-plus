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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.zly2006.zhihu.test.setScreenContent
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.model.LatexConfig
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class LatexEscapedCharactersInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun rendersCommonEscapedCharactersAndSpacingCommands() {
        val samples = listOf(
            "转义字符" to "\\{ \\} \\$ \\% \\# \\& \\_ \\| \\backslash",
            "控制空格" to "a\\ b\\,c\\:d\\>e\\;f\\quad g\\qquad h",
            "命名空格" to "a\\space b\\thinspace c\\medspace d\\thickspace e",
            "半方与自定义空格" to "a\\enspace b\\enskip c\\hspace{1em}d",
            "负空格" to "a\\!b\\negthinspace c\\negmedspace d\\negthickspace e",
            "双反斜杠换行" to "a\\\\b",
        )

        composeRule.setScreenContent {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("latex_compatibility_screenshot"),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "LaTeX 常用转义与空格命令",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "io.github.zly2006 · 0.0.1-alpha3",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    samples.forEach { (label, latex) ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = latex,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Latex(
                                    latex = latex,
                                    modifier = Modifier.fillMaxWidth(),
                                    config = LatexConfig(fontSize = 22.sp),
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }

        captureScreenshot(
            tag = "latex_compatibility_screenshot",
            filename = "latex-escaped-characters.png",
        )
    }

    @Test
    fun rendersAdjacentUnbracedScriptsAsSeparateTerms() {
        val latex = "a_ib_jx^{i+j}"

        composeRule.setScreenContent {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("latex_unbraced_scripts_screenshot"),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Text(
                        text = "无花括号上下标回归验证",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "io.github.zly2006 · 0.0.1-alpha3",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(text = "输入", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = latex,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = "a、b、x 应为三个独立底数",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                    ) {
                        Latex(
                            latex = latex,
                            modifier = Modifier.fillMaxWidth(),
                            config = LatexConfig(fontSize = 40.sp),
                        )
                    }
                }
            }
        }

        captureScreenshot(
            tag = "latex_unbraced_scripts_screenshot",
            filename = "latex-unbraced-scripts.png",
        )
    }

    private fun captureScreenshot(tag: String, filename: String) {
        val screenshotNode = composeRule
            .onNodeWithTag(tag)
            .assertIsDisplayed()
        composeRule.waitForIdle()

        val outputDir = InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .getExternalFilesDir(null)
        val screenshot = File(outputDir, filename)
        val bitmap = screenshotNode.captureToImage().asAndroidBitmap()
        FileOutputStream(screenshot).use { stream ->
            bitmap.compress(
                android.graphics.Bitmap.CompressFormat.PNG,
                100,
                stream,
            )
        }

        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        assertTrue(pixels.any { it != pixels.first() })
        assertTrue(screenshot.exists())
        assertTrue(screenshot.length() > 0)
    }
}
