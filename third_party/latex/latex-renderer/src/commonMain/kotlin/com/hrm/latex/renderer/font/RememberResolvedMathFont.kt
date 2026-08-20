/*
 * Copyright (c) 2026 huarangmeng
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.hrm.latex.renderer.font

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import com.hrm.latex.base.log.HLog
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.getFontResourceBytes
import org.jetbrains.compose.resources.getSystemResourceEnvironment

private const val TAG = "Latex-font"

/** Resolves only caller-provided resources. The fork deliberately has no bundled default font. */
@Composable
internal fun rememberResolvedMathFont(mathFont: MathFont): MathFont = when (mathFont) {
    MathFont.Default -> MathFont.KaTeXTTF
    is MathFont.OTF -> mathFont.fontResource?.let { rememberCustomOtfAsync(it) } ?: mathFont
    else -> mathFont
}

@Composable
private fun rememberCustomOtfAsync(fontResource: FontResource): MathFont {
    val fontFamily = FontFamily(Font(fontResource))
    var resolved by remember(fontResource) { mutableStateOf<MathFont>(MathFont.KaTeXTTF) }

    LaunchedEffect(fontResource) {
        try {
            val bytes = getFontResourceBytes(getSystemResourceEnvironment(), fontResource)
            if (bytes.isNotEmpty()) {
                resolved = MathFont.OTF(bytes, fontFamily)
            } else {
                HLog.e(TAG, "Custom OTF font bytes empty, using system fallback")
            }
        } catch (e: Exception) {
            HLog.e(TAG, "Custom OTF font load failed, using system fallback", e)
        }
    }

    return resolved
}
