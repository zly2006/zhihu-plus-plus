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

/**
 * KaTeX v0.16.11 `Math-Italic` per-glyph metrics.
 *
 * Values are em units from KaTeX's generated `src/fontMetricsData.js`. Keeping
 * the italic correction beside the TTF provider makes measurement and drawing
 * use the same font data instead of category-wide visual guesses.
 */
internal object KaTeXFontMetrics {
    /** KaTeX `Main-Regular` glyph depth/height in em units. */
    data class VerticalMetrics(
        val depth: Float,
        val height: Float
    )

    /**
     * Metrics used by KaTeX's non-stretchy math accents.
     *
     * The values come from the `Main-Regular` entries in KaTeX's generated
     * `src/fontMetricsData.js`. U+20DB is an extension supported by this
     * renderer; it follows the vertical metrics of the KaTeX dot accents.
     */
    fun mainAccentVerticalMetrics(codePoint: Int): VerticalMetrics = when (codePoint) {
        94 -> VerticalMetrics(depth = 0f, height = 0.69444f) // ^
        126 -> VerticalMetrics(depth = 0.35f, height = 0.31786f) // ~
        168 -> VerticalMetrics(depth = 0f, height = 0.66786f) // ¨
        711 -> VerticalMetrics(depth = 0f, height = 0.62847f) // ˇ
        713 -> VerticalMetrics(depth = 0f, height = 0.56778f) // ˉ
        714, 715, 728, 730 -> VerticalMetrics(depth = 0f, height = 0.69444f)
        729 -> VerticalMetrics(depth = 0f, height = 0.66786f) // ˙
        8403 -> VerticalMetrics(depth = 0f, height = 0.66786f) // U+20DB
        else -> VerticalMetrics(depth = 0f, height = 0.69444f)
    }

    fun italicCorrection(codePoint: Int): Float = when (codePoint) {
        66 -> 0.05017f; 67 -> 0.07153f; 68 -> 0.02778f; 69 -> 0.05764f
        70 -> 0.13889f; 72 -> 0.08125f; 73 -> 0.07847f; 74 -> 0.09618f
        75 -> 0.07153f; 77, 78 -> 0.10903f; 79 -> 0.02778f
        80, 84 -> 0.13889f; 82 -> 0.00773f; 83 -> 0.05764f
        85 -> 0.10903f; 86, 89 -> 0.22222f; 87 -> 0.13889f
        88 -> 0.07847f; 90 -> 0.07153f; 102 -> 0.10764f
        103, 113, 118, 121 -> 0.03588f; 106 -> 0.05724f
        107 -> 0.03148f; 108 -> 0.01968f; 114 -> 0.02778f
        119 -> 0.02691f; 122 -> 0.04398f; 915 -> 0.13889f
        920 -> 0.02778f; 926 -> 0.07569f; 928 -> 0.08125f
        931 -> 0.05764f; 933 -> 0.13889f; 936 -> 0.11f
        937 -> 0.05017f; 945 -> 0.0037f; 946 -> 0.05278f
        947 -> 0.05556f; 948 -> 0.03785f; 950 -> 0.07378f
        951, 960, 963, 965, 968, 969 -> 0.03588f
        952, 982 -> 0.02778f; 957 -> 0.06366f; 958 -> 0.04601f
        962 -> 0.07986f; 964 -> 0.1132f
        else -> 0f
    }

    /** KaTeX Size1/Size2 integral glyph italic correction in em units. */
    fun integralItalicCorrection(displayStyle: Boolean): Float =
        if (displayStyle) 0.44445f else 0.19445f

    /** KaTeX `Math-Italic` accent skew in em units. */
    fun skew(codePoint: Int): Float = when (codePoint) {
        65 -> 0.13889f
        66, 67, 69, 70, 71, 77, 78, 79, 80, 81, 82, 83, 84, 88, 90,
        915, 920, 926, 931, 934, 937, 946, 949, 950, 952, 961, 966,
        977, 981, 1009, 57911, 108, 112, 113, 116, 119, 962 -> 0.08334f
        68, 72, 75, 99, 101, 111, 114, 115, 121, 122, 928, 933, 936,
        948, 951, 953, 959, 967, 1013 -> 0.05556f
        73, 958, 968 -> 0.11111f
        74, 100, 102, 916, 923 -> 0.16667f
        76, 85, 103, 117, 118, 120, 945, 956, 957, 964, 965, 57649 -> 0.02778f
        else -> 0f
    }
}
