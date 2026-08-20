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

package com.hrm.latex.parser.component.handler

import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.tokenizer.LatexToken

/**
 * 可扩展箭头 & 堆叠命令：\xrightarrow, \overset, \underset, \stackrel
 */
internal fun CommandRegistry.installArrowAndStackHandlers() {
    // 可扩展箭头
    val arrowMapping = mapOf(
        "xrightarrow" to LatexNode.ExtensibleArrow.Direction.RIGHT,
        "xleftarrow" to LatexNode.ExtensibleArrow.Direction.LEFT,
        "xleftrightarrow" to LatexNode.ExtensibleArrow.Direction.BOTH,
        "xhookrightarrow" to LatexNode.ExtensibleArrow.Direction.HOOK_RIGHT,
        "xhookleftarrow" to LatexNode.ExtensibleArrow.Direction.HOOK_LEFT,
        "xRightarrow" to LatexNode.ExtensibleArrow.Direction.RIGHT_DOUBLE,
        "xLeftarrow" to LatexNode.ExtensibleArrow.Direction.LEFT_DOUBLE,
        "xLeftrightarrow" to LatexNode.ExtensibleArrow.Direction.BOTH_DOUBLE,
        "xmapsto" to LatexNode.ExtensibleArrow.Direction.MAPSTO,
        "xlongequal" to LatexNode.ExtensibleArrow.Direction.EQUAL,
    )

    for ((cmd, direction) in arrowMapping) {
        register(cmd) { _, ctx, stream ->
            // 可选参数（下方文字）
            val below = if (stream.peek() is LatexToken.LeftBracket) {
                stream.advance() // 消费 [
                val nodes = ParseUtils.parseUntil(ctx, stream) { it is LatexToken.RightBracket }
                stream.advance() // 消费 ]
                if (nodes.isEmpty()) null else LatexNode.Group(nodes)
            } else {
                null
            }

            // 必选参数（上方文字）
            val above = ctx.parseArgument() ?: LatexNode.Text("")

            LatexNode.ExtensibleArrow(above, below, direction)
        }
    }

    // 堆叠：\overset, \stackrel
    register("overset", "stackrel") { _, ctx, _ ->
        val firstArg = ctx.parseArgument() ?: LatexNode.Text("")
        val base = ctx.parseArgument() ?: LatexNode.Text("")
        LatexNode.Stack(base = base, above = firstArg, below = null)
    }

    // 堆叠：\underset
    register("underset") { _, ctx, _ ->
        val firstArg = ctx.parseArgument() ?: LatexNode.Text("")
        val base = ctx.parseArgument() ?: LatexNode.Text("")
        LatexNode.Stack(base = base, above = null, below = firstArg)
    }
}
