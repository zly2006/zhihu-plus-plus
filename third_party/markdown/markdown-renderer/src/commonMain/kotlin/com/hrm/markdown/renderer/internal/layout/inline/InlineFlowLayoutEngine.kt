package com.hrm.markdown.renderer.internal.layout.inline

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlin.math.ceil

private val WHITESPACE_BOUNDARY_REGEX = Regex("(?<=\\s)|(?=\\s)")

internal fun computeInlineFlowLayout(
    input: InlineFlowInput,
    style: TextStyle,
    density: Density,
    textMeasurer: TextMeasurer,
    maxWidthPx: Float,
    maxLines: Int,
): InlineFlowLayout {
    if (maxLines <= 0 || maxWidthPx <= 0f) {
        return InlineFlowLayout(
            widthPx = maxWidthPx.coerceAtLeast(0f),
            heightPx = 0f,
            lines = emptyList(),
        )
    }

    val textStyle = textMeasurementStyle(style)
    val baseLineHeightPx = baseLineHeightPx(style, density)
    val baseMetrics = textMeasurer.measure(
        text = "Ag",
        style = textStyle,
        constraints = Constraints(maxWidth = Int.MAX_VALUE),
        maxLines = 1,
        softWrap = false,
    )
    val baseTextHeightPx = baseMetrics.size.height.toFloat()
    val baseTextBaselinePx = baseMetrics.firstBaseline

    data class MeasuredText(val widthPx: Float, val heightPx: Float, val baselinePx: Float)

    fun measureText(a: AnnotatedString): MeasuredText {
        if (a.isEmpty()) return MeasuredText(0f, 0f, 0f)
        val res = textMeasurer.measure(
            text = a,
            style = textStyle,
            constraints = Constraints(maxWidth = Int.MAX_VALUE),
            maxLines = 1,
            softWrap = false,
        )
        return MeasuredText(
            widthPx = res.size.width.toFloat(),
            heightPx = res.size.height.toFloat(),
            baselinePx = res.firstBaseline,
        )
    }

    fun inlineSizePx(placeholder: InlinePlaceholderLayoutSpec): Pair<Float, Float> {
        return placeholderSizePx(placeholder)
    }

    val lines = ArrayList<InlineFlowLine>()
    var currentItems = ArrayList<LineItem>()
    var currentWidth = 0f
    var maxItemHeight = 0f
    var lineCount = 0

    fun appendTextItem(
        text: AnnotatedString,
        measured: MeasuredText,
        widthPx: Float = measured.widthPx,
    ) {
        currentItems.add(
            LineItem.TextItem(
                text = text,
                widthPx = widthPx.coerceAtLeast(0f),
                heightPx = measured.heightPx,
                baselinePx = measured.baselinePx,
            )
        )
        currentWidth += widthPx.coerceAtLeast(0f)
        maxItemHeight = maxOf(maxItemHeight, measured.heightPx)
    }

    fun flushLine(force: Boolean = false) {
        if (!force && currentItems.isEmpty()) return
        val lineHeightPx = maxOf(baseLineHeightPx, maxItemHeight)
        val firstTextBaselinePx = currentItems.firstNotNullOfOrNull { item ->
            (item as? LineItem.TextItem)?.let { textItem ->
                ((lineHeightPx - textItem.heightPx) / 2f).coerceAtLeast(0f) + textItem.baselinePx
            }
        }
        lines += InlineFlowLine(
            textStyle = textStyle,
            lineWidthPx = currentWidth,
            lineHeightPx = lineHeightPx,
            baselinePx = firstTextBaselinePx
                ?: (((lineHeightPx - baseTextHeightPx) / 2f).coerceAtLeast(0f) + baseTextBaselinePx),
            textAlign = style.textAlign,
            items = currentItems.toList(),
        )
        currentItems = ArrayList()
        currentWidth = 0f
        maxItemHeight = 0f
        lineCount++
    }

    for (token in input.segments) {
        if (lineCount >= maxLines) break
        when (token) {
            InlineFlowSegment.Newline -> flushLine(force = true)
            is InlineFlowSegment.InlineRun -> {
                val (w, h) = inlineSizePx(token.placeholder)
                if (w <= 0f || h <= 0f) continue
                if (currentWidth > 0f && currentWidth + w > maxWidthPx) {
                    flushLine(force = true)
                    if (lineCount >= maxLines) break
                }
                currentItems.add(
                    LineItem.InlineItem(
                        id = token.id,
                        widthPx = w,
                        heightPx = h,
                        alternateText = token.placeholder.alternateText,
                    )
                )
                currentWidth += w
                maxItemHeight = maxOf(maxItemHeight, h)
            }

            is InlineFlowSegment.TextRun -> {
                var remaining = token.annotated
                while (remaining.isNotEmpty() && lineCount < maxLines) {
                    val measured = measureText(remaining)
                    val w = measured.widthPx
                    val used = currentWidth
                    val available = (maxWidthPx - used).coerceAtLeast(0f)
                    if (w <= available) {
                        appendTextItem(remaining, measured)
                        break
                    }

                    if (available <= 0f && currentItems.isNotEmpty()) {
                        flushLine(force = true)
                        continue
                    }

                    val fit = splitTextToFit(
                        text = remaining,
                        style = textStyle,
                        textMeasurer = textMeasurer,
                        maxWidthPx = available,
                    )
                    if (fit.fit.isNotEmpty()) {
                        val adjustedFit = shrinkTextToWidth(
                            text = fit.fit,
                            style = textStyle,
                            textMeasurer = textMeasurer,
                            maxWidthPx = available,
                        )
                        if (adjustedFit.isEmpty()) {
                            if (currentItems.isNotEmpty()) {
                                flushLine(force = true)
                                continue
                            }
                        } else {
                            val fitMeasured = measureText(adjustedFit)
                            appendTextItem(
                                text = adjustedFit,
                                measured = fitMeasured,
                            )
                            flushLine(force = true)
                            remaining = remaining
                                .subSequence(adjustedFit.length, remaining.length)
                                .trimLeadingSpaces()
                            continue
                        }
                    }

                    if (currentItems.isNotEmpty()) {
                        flushLine(force = true)
                        continue
                    }

                    val cut = firstBreakIndex(remaining.text)
                    val emergencyFit = remaining.subSequence(0, cut)
                    val emergencyMeasured = measureText(emergencyFit)
                    appendTextItem(
                        text = emergencyFit,
                        measured = emergencyMeasured,
                    )
                    flushLine(force = true)
                    remaining = remaining.subSequence(cut, remaining.length)
                }
            }
        }
    }
    if (lineCount < maxLines) flushLine(force = false)

    val totalHeightPx = lines.sumOf { it.lineHeightPx.toDouble() }.toFloat()
    val firstBaselinePx = lines.firstOrNull()?.baselinePx
    val lastBaselinePx = if (lines.isEmpty()) null else {
        var y = 0f
        for (i in 0 until lines.lastIndex) {
            y += lines[i].lineHeightPx
        }
        y + lines.last().baselinePx
    }

    return InlineFlowLayout(
        widthPx = maxWidthPx,
        heightPx = totalHeightPx,
        firstBaselinePx = firstBaselinePx,
        lastBaselinePx = lastBaselinePx,
        lines = lines,
    )
}

private data class SplitResult(val fit: AnnotatedString, val rest: AnnotatedString)

private fun shrinkTextToWidth(
    text: AnnotatedString,
    style: TextStyle,
    textMeasurer: TextMeasurer,
    maxWidthPx: Float,
): AnnotatedString {
    if (text.isEmpty() || maxWidthPx <= 0f) return AnnotatedString("")
    val fullWidth = textMeasurer.measure(
        text = text,
        style = style,
        constraints = Constraints(maxWidth = Int.MAX_VALUE),
        maxLines = 1,
        softWrap = false,
    ).size.width.toFloat()
    if (fullWidth <= maxWidthPx) return text

    var lo = 1
    var hi = text.length
    var best = 0
    while (lo <= hi) {
        val mid = safeBreakIndex(text.text, (lo + hi) / 2)
        if (mid <= 0) {
            hi = -1
            continue
        }
        val sub = text.subSequence(0, mid)
        val width = textMeasurer.measure(
            text = sub,
            style = style,
            constraints = Constraints(maxWidth = Int.MAX_VALUE),
            maxLines = 1,
            softWrap = false,
        ).size.width.toFloat()
        if (width <= maxWidthPx) {
            best = mid
            lo = mid + 1
        } else {
            hi = mid - 1
        }
    }
    if (best <= 0) return AnnotatedString("")
    return text.subSequence(0, safeBreakIndex(text.text, best))
}

private fun splitTextToFit(
    text: AnnotatedString,
    style: TextStyle,
    textMeasurer: TextMeasurer,
    maxWidthPx: Float,
): SplitResult {
    if (text.isEmpty() || maxWidthPx <= 0f) return SplitResult(AnnotatedString(""), text)
    val softWrapCut = firstSoftWrapLineEnd(
        text = text,
        style = style,
        textMeasurer = textMeasurer,
        maxWidthPx = maxWidthPx,
    )
    if (softWrapCut != null) {
        return SplitResult(
            fit = text.subSequence(0, softWrapCut),
            rest = text.subSequence(softWrapCut, text.length),
        )
    }

    val full = text.text
    var lo = 0
    var hi = full.length
    var best = 0
    while (lo <= hi) {
        val mid = (lo + hi) / 2
        val sub = text.subSequence(0, mid)
        val w = textMeasurer.measure(
            text = sub,
            style = style,
            constraints = Constraints(maxWidth = Int.MAX_VALUE),
            maxLines = 1,
            softWrap = false,
        ).size.width.toFloat()
        if (w <= maxWidthPx) {
            best = mid
            lo = mid + 1
        } else {
            hi = mid - 1
        }
    }
    if (best <= 0) return SplitResult(AnnotatedString(""), text)

    val region = full.substring(0, best)
    val lastSpace = region.indexOfLast { it.isWhitespace() }
    val cut = if (lastSpace >= 1) lastSpace + 1 else best
    return SplitResult(
        fit = text.subSequence(0, cut),
        rest = text.subSequence(cut, full.length),
    )
}

private fun firstSoftWrapLineEnd(
    text: AnnotatedString,
    style: TextStyle,
    textMeasurer: TextMeasurer,
    maxWidthPx: Float,
): Int? {
    val layout = textMeasurer.measure(
        text = text,
        style = style,
        constraints = Constraints(maxWidth = ceil(maxWidthPx).toInt().coerceAtLeast(1)),
        maxLines = 1,
        softWrap = true,
    )
    if (!layout.didOverflowWidth && !layout.hasVisualOverflow) return null

    val lineEnd = layout.getLineEnd(lineIndex = 0, visibleEnd = false)
    return lineEnd.takeIf { it in 1 until text.length }
}

private fun firstBreakIndex(text: String): Int {
    if (text.length >= 2 && text[0].isHighSurrogate() && text[1].isLowSurrogate()) {
        return 2
    }
    return 1.coerceAtMost(text.length)
}

private fun safeBreakIndex(text: String, index: Int): Int {
    val bounded = index.coerceIn(0, text.length)
    if (bounded in 1 until text.length &&
        text[bounded - 1].isHighSurrogate() &&
        text[bounded].isLowSurrogate()
    ) {
        return bounded - 1
    }
    return bounded
}

private fun AnnotatedString.trimLeadingSpaces(): AnnotatedString {
    val s = text
    var i = 0
    while (i < s.length && s[i].isWhitespace() && s[i] != '\n') i++
    return if (i == 0) this else subSequence(i, s.length)
}

internal fun textMeasurementStyle(style: TextStyle): TextStyle {
    return if (style.lineHeight.value.isNaN()) style else style.copy(lineHeight = TextUnit.Unspecified)
}

internal fun baseLineHeightPx(style: TextStyle, density: Density): Float = with(density) {
    val lh = style.lineHeight.value.takeUnless { it.isNaN() }
        ?: style.fontSize.value.takeUnless { it.isNaN() }?.times(1.5f)
        ?: 0f
    lh.sp.toPx()
}.coerceAtLeast(0f)

internal fun placeholderSizePx(
    placeholder: InlinePlaceholderLayoutSpec,
): Pair<Float, Float> {
    return placeholder.widthPx to placeholder.heightPx
}

internal fun computeMaxIntrinsicWidthPx(
    input: InlineFlowInput,
    style: TextStyle,
    textMeasurer: TextMeasurer,
): Int {
    val textStyle = textMeasurementStyle(style)
    var lineWidth = 0f
    var maxLineWidth = 0f
    for (token in input.segments) {
        when (token) {
            InlineFlowSegment.Newline -> {
                maxLineWidth = maxOf(maxLineWidth, lineWidth)
                lineWidth = 0f
            }

            is InlineFlowSegment.InlineRun -> {
                lineWidth += placeholderSizePx(token.placeholder).first
            }

            is InlineFlowSegment.TextRun -> {
                if (token.annotated.isEmpty()) continue
                val width = textMeasurer.measure(
                    text = token.annotated,
                    style = textStyle,
                    constraints = Constraints(maxWidth = Int.MAX_VALUE),
                    maxLines = 1,
                    softWrap = false,
                ).size.width.toFloat()
                lineWidth += width
            }
        }
    }
    maxLineWidth = maxOf(maxLineWidth, lineWidth)
    return ceil(maxLineWidth).toInt()
}

internal fun computeMinIntrinsicWidthPx(
    input: InlineFlowInput,
    style: TextStyle,
    textMeasurer: TextMeasurer,
): Int {
    val textStyle = textMeasurementStyle(style)
    var maxPieceWidth = 0f
    for (token in input.segments) {
        when (token) {
            InlineFlowSegment.Newline -> Unit
            is InlineFlowSegment.InlineRun -> {
                maxPieceWidth =
                    maxOf(maxPieceWidth, placeholderSizePx(token.placeholder).first)
            }

            is InlineFlowSegment.TextRun -> {
                val pieces = token.annotated.text.split(WHITESPACE_BOUNDARY_REGEX)
                var cursor = 0
                for (piece in pieces) {
                    if (piece.isEmpty()) continue
                    val end = cursor + piece.length
                    val sub = token.annotated.subSequence(cursor, end)
                    val width = textMeasurer.measure(
                        text = sub,
                        style = textStyle,
                        constraints = Constraints(maxWidth = Int.MAX_VALUE),
                        maxLines = 1,
                        softWrap = false,
                    ).size.width.toFloat()
                    maxPieceWidth = maxOf(maxPieceWidth, width)
                    cursor = end
                }
            }
        }
    }
    return ceil(maxPieceWidth).toInt()
}
