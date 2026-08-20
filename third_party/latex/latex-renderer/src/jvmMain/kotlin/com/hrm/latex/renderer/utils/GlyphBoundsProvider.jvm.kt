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

/**
 * JVM Desktop 平台：使用 Skia (org.jetbrains.skia) API 获取精确墨水边界。
 *
 * Compose for Desktop 底层使用 Skia 渲染引擎，可以直接访问 Skia API。
 * FontMgr.makeFromData() 从字节数据创建 Typeface，
 * Font.getBounds() 获取 glyph 的精确 bounding box。
 */
actual fun measureGlyphBounds(
    text: String,
    fontSizePx: Float,
    fontBytes: ByteArray,
    fontWeightValue: Int
): GlyphBounds? {
    if (text.isEmpty()) return null

    return try {
        val data = Data.makeFromBytes(fontBytes)
        val typeface = FontMgr.default.makeFromData(data) ?: return null
        val font = Font(typeface, fontSizePx)

        // 获取字符串对应的 glyph IDs
        val glyphIds = font.getStringGlyphs(text)
        if (glyphIds.isEmpty()) return null

        // 获取每个 glyph 的精确 bounding box
        val glyphBoundsArray = font.getBounds(glyphIds)
        if (glyphBoundsArray.isEmpty()) return null

        // 合并所有 glyph 的 bounds（处理多字符文本）
        // bounds 坐标系：原点在 glyph 的基线左端
        // top 为负值（baseline 以上），bottom 为正值（baseline 以下）
        var minTop = Float.MAX_VALUE
        var maxBottom = Float.MIN_VALUE

        for (rect in glyphBoundsArray) {
            if (rect.width > 0 || rect.height > 0) {
                minTop = minOf(minTop, rect.top)
                maxBottom = maxOf(maxBottom, rect.bottom)
            }
        }

        if (minTop == Float.MAX_VALUE) return null

        val ascentPx = (-minTop).coerceAtLeast(0f)
        val descentPx = maxBottom.coerceAtLeast(0f)
        val inkWidth = font.measureTextWidth(text)

        GlyphBounds(ascentPx, descentPx, inkWidth)
    } catch (e: Exception) {
        null
    }
}
