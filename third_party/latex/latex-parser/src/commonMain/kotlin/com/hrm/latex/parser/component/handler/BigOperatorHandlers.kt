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

/**
 * 大型运算符命令：\sum, \prod, \int, \lim 等
 */
internal fun CommandRegistry.installBigOperatorHandlers() {
    val displayLimitOperators = setOf(
        "sum", "prod", "bigcup", "bigcap", "bigvee", "bigwedge",
        "coprod", "bigoplus", "bigotimes", "bigsqcup", "bigodot", "biguplus",
        "lim", "max", "min", "sup", "inf", "limsup", "liminf", "det", "gcd"
    )
    val bigOpHandler = CommandHandler { cmdName, ctx, stream ->
        val (sub, sup, limitsMode) = ParseUtils.parseScriptsAndLimits(ctx, stream)
        LatexNode.BigOperator(
            operator = cmdName,
            subscript = sub,
            superscript = sup,
            limitsMode = limitsMode,
            limitsInDisplay = cmdName in displayLimitOperators
        )
    }

    register(
        "sum", "prod", "int", "oint", "iint", "iiint",
        "bigcup", "bigcap", "bigvee", "bigwedge",
        "coprod", "bigoplus", "bigotimes", "bigsqcup", "bigodot", "biguplus",
        "lim", "max", "min", "sup", "inf", "limsup", "liminf",
        "det", "gcd", "deg", "dim", "ker", "arg", "hom",
        handler = bigOpHandler
    )
}
