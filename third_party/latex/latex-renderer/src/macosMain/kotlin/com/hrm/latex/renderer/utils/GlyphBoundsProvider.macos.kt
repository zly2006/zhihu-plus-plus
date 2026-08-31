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

package com.hrm.latex.renderer.utils

import org.jetbrains.skia.Data
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr

actual fun measureGlyphBounds(
    text: String,
    fontSizePx: Float,
    fontBytes: ByteArray,
    fontWeightValue: Int,
): GlyphBounds? {
    if (text.isEmpty()) return null
    return try {
        val typeface = FontMgr.default.makeFromData(Data.makeFromBytes(fontBytes)) ?: return null
        val font = Font(typeface, fontSizePx)
        val glyphIds = font.getStringGlyphs(text)
        if (glyphIds.isEmpty()) return null
        val glyphBounds = font.getBounds(glyphIds)
        if (glyphBounds.isEmpty()) return null

        var minTop = Float.MAX_VALUE
        var maxBottom = Float.MIN_VALUE
        glyphBounds.forEach { bounds ->
            if (bounds.width > 0 || bounds.height > 0) {
                minTop = minOf(minTop, bounds.top)
                maxBottom = maxOf(maxBottom, bounds.bottom)
            }
        }
        if (minTop == Float.MAX_VALUE) return null
        GlyphBounds(
            ascentPx = (-minTop).coerceAtLeast(0f),
            descentPx = maxBottom.coerceAtLeast(0f),
            inkWidth = font.measureTextWidth(text),
        )
    } catch (_: Exception) {
        null
    }
}
