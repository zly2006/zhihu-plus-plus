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

import androidx.compose.ui.graphics.Canvas

/**
 * 在平台原生的 SVG 画布上执行现有 Compose 绘制命令并返回 UTF-8 文档。
 *
 * 该边界让测量、节点布局和绘制保持在 commonMain；平台层只负责把 Canvas 命令编码成 SVG。
 */
internal fun renderToSvg(
    width: Float,
    height: Float,
    textAsPath: Boolean,
    prettyPrint: Boolean,
    draw: (Canvas) -> Unit
): ByteArray? {
    val bytes = renderToSvgPlatform(width, height, textAsPath, prettyPrint, draw) ?: return null
    val svg = bytes.decodeToString()
    val rootStart = svg.indexOf("<svg")
    val rootEnd = if (rootStart >= 0) svg.indexOf('>', rootStart) else -1
    if (rootEnd < 0 || "viewBox=" in svg.substring(rootStart, rootEnd)) return bytes

    // Skia currently emits width/height but not viewBox on some targets. Normalize the public
    // result so responsive Web embedding and physical-size overrides behave consistently.
    val viewBox = " viewBox=\"0 0 ${svgNumber(width)} ${svgNumber(height)}\""
    return (svg.substring(0, rootEnd) + viewBox + svg.substring(rootEnd)).encodeToByteArray()
}

private fun svgNumber(value: Float): String =
    if (value == value.toInt().toFloat()) value.toInt().toString() else value.toString()

internal expect fun renderToSvgPlatform(
    width: Float,
    height: Float,
    textAsPath: Boolean,
    prettyPrint: Boolean,
    draw: (Canvas) -> Unit
): ByteArray?
