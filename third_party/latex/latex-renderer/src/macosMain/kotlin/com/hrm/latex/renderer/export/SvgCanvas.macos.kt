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
import androidx.compose.ui.graphics.asComposeCanvas
import org.jetbrains.skia.DynamicMemoryWStream
import org.jetbrains.skia.Rect
import org.jetbrains.skia.svg.SVGCanvas

internal actual fun renderToSvgPlatform(
    width: Float,
    height: Float,
    textAsPath: Boolean,
    prettyPrint: Boolean,
    draw: (Canvas) -> Unit,
): ByteArray? {
    val stream = DynamicMemoryWStream()
    return try {
        val svgCanvas = SVGCanvas.make(
            bounds = Rect.makeWH(width, height),
            out = stream,
            convertTextToPaths = textAsPath,
            prettyXML = prettyPrint,
        )
        try {
            draw(svgCanvas.asComposeCanvas())
        } finally {
            svgCanvas.close()
        }
        ByteArray(stream.bytesWritten()).let { bytes ->
            if (stream.read(bytes, 0, bytes.size)) bytes else null
        }
    } finally {
        stream.close()
    }
}
