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

package com.hrm.latex.renderer.export

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.graphics.Canvas
import androidx.graphics.path.PathIterator
import androidx.graphics.path.PathSegment
import kotlin.math.round

internal actual fun renderToSvgPlatform(
    width: Float,
    height: Float,
    textAsPath: Boolean,
    prettyPrint: Boolean,
    draw: (Canvas) -> Unit
): ByteArray? {
    val recorder = AndroidSvgCanvas(
        viewportWidth = width.toInt().coerceAtLeast(1),
        viewportHeight = height.toInt().coerceAtLeast(1),
        textAsPath = textAsPath,
        prettyPrint = prettyPrint
    )
    draw(Canvas(recorder))
    return recorder.finish().encodeToByteArray()
}

/**
 * Android Compose uses the framework text stack rather than Skia's public Canvas API. This
 * Canvas records the exact framework draw calls as SVG elements, so commonMain can still use
 * the same NodeLayout draw lambdas on every target.
 *
 * A backing bitmap is required only to give Android's text layout a valid viewport. No pixels
 * from it are read or embedded in the SVG.
 */
private class AndroidSvgCanvas(
    private val viewportWidth: Int,
    private val viewportHeight: Int,
    private val textAsPath: Boolean,
    private val prettyPrint: Boolean
) : AndroidCanvas(Bitmap.createBitmap(viewportWidth, viewportHeight, Bitmap.Config.ALPHA_8)) {
    private val body = StringBuilder()
    private val matrixValues = FloatArray(9)

    fun finish(): String = buildString {
        append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"")
        append(viewportWidth)
        append("\" height=\"")
        append(viewportHeight)
        append("\" viewBox=\"0 0 ")
        append(viewportWidth)
        append(' ')
        append(viewportHeight)
        append("\">")
        newline(this)
        append(body)
        append("</svg>")
        newline(this)
    }

    override fun drawLine(startX: Float, startY: Float, stopX: Float, stopY: Float, paint: Paint) {
        element("line") {
            attr("x1", number(startX))
            attr("y1", number(startY))
            attr("x2", number(stopX))
            attr("y2", number(stopY))
            paintAttributes(paint, forceStroke = true)
            transformAttribute()
        }
    }

    override fun drawLines(points: FloatArray, offset: Int, count: Int, paint: Paint) {
        var index = offset
        val end = (offset + count).coerceAtMost(points.size)
        while (index + 3 < end) {
            drawLine(points[index], points[index + 1], points[index + 2], points[index + 3], paint)
            index += 4
        }
    }

    override fun drawLines(points: FloatArray, paint: Paint) {
        drawLines(points, 0, points.size, paint)
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        element("rect") {
            attr("x", number(left))
            attr("y", number(top))
            attr("width", number(right - left))
            attr("height", number(bottom - top))
            paintAttributes(paint)
            transformAttribute()
        }
    }

    override fun drawRect(rect: RectF, paint: Paint) {
        drawRect(rect.left, rect.top, rect.right, rect.bottom, paint)
    }

    override fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        rx: Float,
        ry: Float,
        paint: Paint
    ) {
        element("rect") {
            attr("x", number(left))
            attr("y", number(top))
            attr("width", number(right - left))
            attr("height", number(bottom - top))
            attr("rx", number(rx))
            attr("ry", number(ry))
            paintAttributes(paint)
            transformAttribute()
        }
    }

    override fun drawRoundRect(rect: RectF, rx: Float, ry: Float, paint: Paint) {
        drawRoundRect(rect.left, rect.top, rect.right, rect.bottom, rx, ry, paint)
    }

    override fun drawCircle(cx: Float, cy: Float, radius: Float, paint: Paint) {
        element("circle") {
            attr("cx", number(cx))
            attr("cy", number(cy))
            attr("r", number(radius))
            paintAttributes(paint)
            transformAttribute()
        }
    }

    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        element("ellipse") {
            attr("cx", number((left + right) / 2f))
            attr("cy", number((top + bottom) / 2f))
            attr("rx", number((right - left) / 2f))
            attr("ry", number((bottom - top) / 2f))
            paintAttributes(paint)
            transformAttribute()
        }
    }

    override fun drawOval(oval: RectF, paint: Paint) {
        drawOval(oval.left, oval.top, oval.right, oval.bottom, paint)
    }

    override fun drawArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        paint: Paint
    ) {
        val path = Path()
        val oval = RectF(left, top, right, bottom)
        if (useCenter) {
            path.moveTo(oval.centerX(), oval.centerY())
        }
        path.arcTo(oval, startAngle, sweepAngle, false)
        if (useCenter) path.close()
        drawPath(path, paint)
    }

    override fun drawArc(
        oval: RectF,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        paint: Paint
    ) {
        drawArc(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle, useCenter, paint)
    }

    override fun drawPath(path: Path, paint: Paint) {
        val data = pathData(path)
        if (data.isEmpty()) return
        element("path") {
            attr("d", data)
            if (path.fillType == Path.FillType.EVEN_ODD ||
                path.fillType == Path.FillType.INVERSE_EVEN_ODD
            ) {
                attr("fill-rule", "evenodd")
            }
            paintAttributes(paint)
            transformAttribute()
        }
    }

    override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
        drawTextContent(text, x, y, paint, isRtl = false)
    }

    override fun drawText(text: String, start: Int, end: Int, x: Float, y: Float, paint: Paint) {
        drawTextContent(text.substring(start, end), x, y, paint, isRtl = false)
    }

    override fun drawText(
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        y: Float,
        paint: Paint
    ) {
        drawTextContent(text.subSequence(start, end).toString(), x, y, paint, isRtl = false)
    }

    override fun drawText(text: CharArray, index: Int, count: Int, x: Float, y: Float, paint: Paint) {
        drawTextContent(text.concatToString(index, index + count), x, y, paint, isRtl = false)
    }

    override fun drawTextRun(
        text: CharSequence,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        x: Float,
        y: Float,
        isRtl: Boolean,
        paint: Paint
    ) {
        drawTextContent(text.subSequence(start, end).toString(), x, y, paint, isRtl)
    }

    override fun drawTextRun(
        text: CharArray,
        index: Int,
        count: Int,
        contextIndex: Int,
        contextCount: Int,
        x: Float,
        y: Float,
        isRtl: Boolean,
        paint: Paint
    ) {
        drawTextContent(text.concatToString(index, index + count), x, y, paint, isRtl)
    }

    private fun drawTextContent(text: String, x: Float, y: Float, paint: Paint, isRtl: Boolean) {
        if (text.isEmpty()) return
        if (textAsPath) {
            val path = Path()
            paint.getTextPath(text, 0, text.length, x, y, path)
            drawPath(path, paint)
            return
        }

        element("text", escapeXml(text)) {
            attr("x", number(x))
            attr("y", number(y))
            attr("font-size", number(paint.textSize))
            if (paint.textScaleX != 1f) attr("font-stretch", number(paint.textScaleX))
            if (paint.textSkewX != 0f) attr("font-style", "italic")
            if (paint.isFakeBoldText || paint.typeface?.isBold == true) attr("font-weight", "bold")
            when (paint.textAlign) {
                Paint.Align.CENTER -> attr("text-anchor", "middle")
                Paint.Align.RIGHT -> attr("text-anchor", "end")
                else -> Unit
            }
            if (isRtl) {
                attr("direction", "rtl")
                attr("unicode-bidi", "bidi-override")
            }
            paintAttributes(paint)
            transformAttribute()
        }
    }

    private fun pathData(path: Path): String = buildString {
        val iterator = PathIterator(path)
        while (iterator.hasNext()) {
            val segment = iterator.next()
            val points = segment.points
            when (segment.type) {
                PathSegment.Type.Move -> command("M", points[0].x, points[0].y)
                PathSegment.Type.Line -> command("L", points[1].x, points[1].y)
                PathSegment.Type.Quadratic,
                PathSegment.Type.Conic -> command(
                    "Q",
                    points[1].x,
                    points[1].y,
                    points[2].x,
                    points[2].y
                )
                PathSegment.Type.Cubic -> command(
                    "C",
                    points[1].x,
                    points[1].y,
                    points[2].x,
                    points[2].y,
                    points[3].x,
                    points[3].y
                )
                PathSegment.Type.Close -> append("Z ")
                PathSegment.Type.Done -> Unit
            }
        }
    }.trimEnd()

    private fun StringBuilder.command(name: String, vararg values: Float) {
        append(name)
        for (value in values) {
            append(' ')
            append(number(value))
        }
        append(' ')
    }

    private fun StringBuilder.paintAttributes(paint: Paint, forceStroke: Boolean = false) {
        val color = colorHex(paint.color)
        val opacity = paint.alpha / 255f
        val fill = !forceStroke && paint.style != Paint.Style.STROKE
        val stroke = forceStroke || paint.style != Paint.Style.FILL

        attr("fill", if (fill) color else "none")
        if (fill && opacity < 1f) attr("fill-opacity", number(opacity))
        if (stroke) {
            attr("stroke", color)
            attr("stroke-width", number(paint.strokeWidth.coerceAtLeast(0f)))
            if (opacity < 1f) attr("stroke-opacity", number(opacity))
            attr("stroke-linecap", paint.strokeCap.name.lowercase())
            attr("stroke-linejoin", paint.strokeJoin.name.lowercase())
            attr("stroke-miterlimit", number(paint.strokeMiter))
        }
    }

    @Suppress("DEPRECATION")
    private fun StringBuilder.transformAttribute() {
        matrix.getValues(matrixValues)
        if (matrixValues.contentEquals(IDENTITY_MATRIX)) return
        attr(
            "transform",
            "matrix(${number(matrixValues[Matrix.MSCALE_X])} " +
                    "${number(matrixValues[Matrix.MSKEW_Y])} " +
                    "${number(matrixValues[Matrix.MSKEW_X])} " +
                    "${number(matrixValues[Matrix.MSCALE_Y])} " +
                    "${number(matrixValues[Matrix.MTRANS_X])} " +
                    "${number(matrixValues[Matrix.MTRANS_Y])})"
        )
    }

    private fun element(
        name: String,
        content: String? = null,
        attributes: StringBuilder.() -> Unit
    ) {
        body.append(if (prettyPrint) "  " else "")
        body.append('<').append(name)
        body.attributes()
        if (content == null) {
            body.append("/>")
        } else {
            body.append('>').append(content).append("</").append(name).append('>')
        }
        newline(body)
    }

    private fun StringBuilder.attr(name: String, value: String) {
        append(' ').append(name).append("=\"").append(value).append('"')
    }

    private fun newline(builder: StringBuilder) {
        if (prettyPrint) builder.append('\n')
    }

    private fun number(value: Float): String {
        if (!value.isFinite()) return "0"
        val rounded = round(value * 1000f) / 1000f
        return if (rounded == rounded.toInt().toFloat()) rounded.toInt().toString() else rounded.toString()
    }

    private fun colorHex(color: Int): String = buildString(7) {
        append('#')
        append(((color shr 16) and 0xff).toString(16).padStart(2, '0'))
        append(((color shr 8) and 0xff).toString(16).padStart(2, '0'))
        append((color and 0xff).toString(16).padStart(2, '0'))
    }

    private fun escapeXml(value: String): String = buildString(value.length) {
        for (char in value) {
            append(
                when (char) {
                    '&' -> "&amp;"
                    '<' -> "&lt;"
                    '>' -> "&gt;"
                    '"' -> "&quot;"
                    '\'' -> "&apos;"
                    else -> char
                }
            )
        }
    }

    private companion object {
        val IDENTITY_MATRIX = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
    }
}
