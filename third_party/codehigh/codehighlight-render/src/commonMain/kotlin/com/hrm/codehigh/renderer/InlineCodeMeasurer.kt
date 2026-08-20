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

package com.hrm.codehigh.renderer

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

data class InlineCodeSize(
    val width: Float,
    val height: Float,
) {
    fun widthDp(density: Density): Float = with(density) { width.toDp().value }

    fun heightDp(density: Density): Float = with(density) { height.toDp().value }
}

fun measureInlineCodeSize(
    text: String,
    style: InlineCodeStyle,
    density: Density,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    maxWidth: Float = Float.POSITIVE_INFINITY,
): InlineCodeSize {
    val annotatedString = AnnotatedString(text)

    return measureAnnotatedStringSize(
        annotatedString = annotatedString,
        textStyle = style.textStyle,
        density = density,
        maxWidth = maxWidth,
        contentPadding = style.contentPadding,
        borderWidth = style.borderWidth,
        textMeasurer = textMeasurer,
    )
}

internal fun measureAnnotatedStringSize(
    annotatedString: AnnotatedString,
    textStyle: TextStyle,
    density: Density,
    maxWidth: Float,
    contentPadding: PaddingValues,
    borderWidth: Dp = 0.dp,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
): InlineCodeSize {
    with(density) {
        val horizontalPaddingPx =
            contentPadding.calculateLeftPadding(LayoutDirection.Ltr).toPx() +
                    contentPadding.calculateRightPadding(LayoutDirection.Ltr).toPx()
        val verticalPaddingPx =
            contentPadding.calculateTopPadding().toPx() +
                    contentPadding.calculateBottomPadding().toPx()
        val horizontalBorderPx = borderWidth.toPx() * 2f
        val verticalBorderPx = borderWidth.toPx() * 2f
        val horizontalDecorationPx = horizontalPaddingPx + horizontalBorderPx
        val verticalDecorationPx = verticalPaddingPx + verticalBorderPx

        val maxWidthWithoutDecoration = maxWidth - horizontalDecorationPx

        val constraints =
            if (maxWidthWithoutDecoration.isFinite() && maxWidthWithoutDecoration > 0) {
                androidx.compose.ui.unit.Constraints(
                    maxWidth = maxWidthWithoutDecoration.toInt(),
                )
            } else {
                androidx.compose.ui.unit.Constraints()
            }

        val layoutResult = textMeasurer.measure(
            text = annotatedString,
            style = textStyle,
            overflow = TextOverflow.Clip,
            softWrap = false,
            constraints = constraints,
        )

        return InlineCodeSize(
            width = layoutResult.size.width + horizontalDecorationPx,
            height = layoutResult.size.height + verticalDecorationPx,
        )
    }
}
