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

import androidx.compose.ui.text.font.FontFamily
import com.hrm.latex.renderer.model.LatexFontFamilies
import org.jetbrains.compose.resources.FontResource

/**
 * Font source used by the fork. No font is bundled in the renderer: hosts may pass a downloaded
 * OpenType font or a complete family set, while [Default] falls back to the system font.
 */
sealed class MathFont {
    data object Default : MathFont()

    /** Compatibility name for the metrics-based fallback used before a custom font is ready. */
    data object KaTeXTTF : MathFont()

    class OTF : MathFont {
        val fontBytes: ByteArray?
        val fontFamily: FontFamily?
        val fontResource: FontResource?

        constructor(fontResource: FontResource) {
            fontBytes = null
            fontFamily = null
            this.fontResource = fontResource
        }

        constructor(fontBytes: ByteArray, fontFamily: FontFamily) {
            this.fontBytes = fontBytes
            this.fontFamily = fontFamily
            fontResource = null
        }
    }

    data class TTF(val fontFamilies: LatexFontFamilies) : MathFont()

    internal fun fontFamiliesOrNull(): LatexFontFamilies? = when (this) {
        Default, KaTeXTTF -> null
        is TTF -> fontFamilies
        is OTF -> {
            val family = fontFamily
            val bytes = fontBytes
            if (family == null || bytes == null) {
                null
            } else {
                LatexFontFamilies(
                    main = family,
                    math = family,
                    ams = family,
                    sansSerif = family,
                    monospace = family,
                    caligraphic = family,
                    fraktur = family,
                    script = family,
                    size1 = family,
                    size2 = family,
                    size3 = family,
                    size4 = family,
                    mainBytes = bytes,
                    mathBytes = bytes,
                    amsBytes = bytes,
                    size1Bytes = bytes,
                    size2Bytes = bytes,
                    size3Bytes = bytes,
                    size4Bytes = bytes,
                )
            }
        }
    }
}
