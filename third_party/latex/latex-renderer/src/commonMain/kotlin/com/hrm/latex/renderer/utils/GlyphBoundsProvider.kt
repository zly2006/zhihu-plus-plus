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

/**
 * 字形精确墨水边界。
 *
 * 由平台原生 API 测量得到，表示字形实际绘制像素的精确矩形区域。
 * 所有值均为像素 (px) 单位。
 *
 * 坐标系以 baseline 为 y=0 原点：
 * - ascentPx > 0 表示 baseline 以上的墨水高度
 * - descentPx > 0 表示 baseline 以下的墨水高度
 * - inkHeight = ascentPx + descentPx
 *
 * @property ascentPx baseline 以上的墨水高度（正值，向上为正）
 * @property descentPx baseline 以下的墨水高度（正值，向下为正）
 * @property inkWidth 墨水区域宽度
 * @property inkHeight 墨水区域总高度 = ascentPx + descentPx
 */
class GlyphBounds(
    val ascentPx: Float,
    val descentPx: Float,
    val inkWidth: Float
) {
    val inkHeight: Float get() = ascentPx + descentPx
}

/**
 * 使用平台原生 API 测量字形的精确墨水边界。
 *
 * 各平台实现：
 * - Android: android.graphics.Paint.getTextBounds() + Typeface.createFromAsset()
 * - JVM: org.jetbrains.skia.Font.measureText() + Typeface.makeFromData()
 * - iOS: org.jetbrains.skia.Font (Compose for iOS 底层也是 Skia)
 * - JS/WASM: org.jetbrains.skia.Font (Compose for Web 底层也是 Skia)
 *
 * @param text 要测量的文本（通常是单个字符或运算符符号）
 * @param fontSizePx 字号大小，像素单位
 * @param fontBytes KaTeX TTF 字体文件的字节数据，用于创建原生字体对象
 * @param fontWeightValue 字重值 (100-900)，默认 400 (Normal)
 * @return 精确的墨水边界，如果测量失败返回 null
 */
expect fun measureGlyphBounds(
    text: String,
    fontSizePx: Float,
    fontBytes: ByteArray,
    fontWeightValue: Int = 400
): GlyphBounds?
