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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.hrm.latex.base.log.HLog
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.model.LatexNode.Space.SpaceType
import com.hrm.latex.renderer.model.RenderContext

const val TAG = "LatexUtils"

/**
 * 分割多行内容
 *
 * 性能优化：先检测是否存在 NewLine 节点，无则直接返回单行包装，避免分配列表。
 */
fun splitLines(nodes: List<LatexNode>): List<List<LatexNode>> {
    // 短路：无 NewLine 节点时直接返回，避免分配 mutableList
    if (nodes.none { it is LatexNode.NewLine }) {
        return listOf(nodes)
    }

    val result = mutableListOf<MutableList<LatexNode>>()
    var current = mutableListOf<LatexNode>()
    nodes.forEach { node ->
        if (node is LatexNode.NewLine) {
            result.add(current)
            current = mutableListOf()
        } else {
            current.add(node)
        }
    }
    if (current.isNotEmpty() || result.isEmpty()) result.add(current)
    return result
}

/**
 * 计算行间距
 */
internal fun lineSpacingPx(context: RenderContext, density: Density): Float =
    with(density) { (context.fontSize * MathConstants.LINE_SPACING).toPx() }

/**
 * 计算空白宽度
 */
internal fun spaceWidthPx(context: RenderContext, type: SpaceType, density: Density): Float {
    val factor = when (type) {
        SpaceType.THIN -> 0.166f
        SpaceType.MEDIUM -> 0.222f
        SpaceType.THICK -> 0.277f
        SpaceType.QUAD -> 1f
        SpaceType.QQUAD -> 2f
        SpaceType.NORMAL -> 0.25f
        SpaceType.NEGATIVE_THIN -> -0.166f
    }
    return with(density) { (context.fontSize * factor).toPx() }
}

/**
 * 解析尺寸字符串 (如 "1cm", "10pt", "-5mm")
 */
internal fun parseDimension(dimension: String, context: RenderContext, density: Density): Float {
    val dim = dimension.trim()
    if (dim.isEmpty()) return 0f

    // 正则提取数值和单位
    // 简单解析，不支持复杂表达式
    var numEnd = 0
    while (numEnd < dim.length && (dim[numEnd].isDigit() || dim[numEnd] == '.' || dim[numEnd] == '-')) {
        numEnd++
    }

    val value = dim.take(numEnd).toFloatOrNull() ?: 0f
    val unit = dim.substring(numEnd).trim().lowercase()

    return with(density) {
        when (unit) {
            "pt" -> value.dp.toPx() // CSS pt ~= Android dp (approx) or use standard conversion
            "px" -> value
            "mm" -> (value * 3.78f).dp.toPx() // 1mm ~= 3.78px (at 96dpi) -> map to dp
            "cm" -> (value * 37.8f).dp.toPx()
            "in" -> (value * 96f).dp.toPx()
            "em" -> context.fontSize.toPx() * value
            "ex" -> context.fontSize.toPx() * 0.5f * value
            "mu" -> context.fontSize.toPx() * value / 18f
            "bp" -> (value * 1.00375f).dp.toPx()
            "pc" -> (value * 12f).dp.toPx()
            "dd" -> (value * 1.07f).dp.toPx()
            "cc" -> (value * 12.84f).dp.toPx()
            "sp" -> (value / 65536f).dp.toPx()
            else -> value.dp.toPx() // default to dp
        }
    }
}

/**
 * 大型运算符符号映射
 */
fun mapBigOp(op: String): String {
    return when (val name = op.trim()) {
        "sum" -> "∑"
        "prod" -> "∏"
        "coprod" -> "∐"
        "int" -> "∫"
        "oint" -> "∮"
        "iint" -> "∬"
        "iiint" -> "∭"
        "bigcap" -> "⋂"
        "bigcup" -> "⋃"
        "bigsqcup" -> "⨆"
        "bigvee" -> "⋁"
        "bigwedge" -> "⋀"
        "bigoplus" -> "⨁"
        "bigotimes" -> "⨂"
        "bigodot" -> "⨀"
        "biguplus" -> "⨄"
        else -> name
    }
}

/**
 * 解析颜色字符串
 */
fun parseColor(color: String): Color? {
    val trimmed = color.trim().removePrefix("#").lowercase()
    if (trimmed.isEmpty()) return null

    // 优先匹配颜色名称（避免 6 字符颜色名如 yellow/orange/purple 被误入十六进制分支）
    val namedColor = when (trimmed) {
        "red" -> Color.Red
        "blue" -> Color.Blue
        "green" -> Color.Green
        "black" -> Color.Black
        "white" -> Color.White
        "gray", "grey" -> Color.Gray
        "yellow" -> Color.Yellow
        "cyan" -> Color.Cyan
        "magenta" -> Color.Magenta
        "orange" -> Color(0xFFFFA500.toInt())
        "purple" -> Color(0xFF800080.toInt())
        "brown" -> Color(0xFFA52A2A.toInt())
        "pink" -> Color(0xFFFFC0CB.toInt())
        "lime" -> Color(0xFF00FF00.toInt())
        "navy" -> Color(0xFF000080.toInt())
        "teal" -> Color(0xFF008080.toInt())
        "violet" -> Color(0xFFEE82EE.toInt())
        else -> null
    }
    if (namedColor != null) return namedColor

    // 再尝试十六进制解析
    return try {
        when (trimmed.length) {
            6 -> {
                // RGB 格式: RRGGBB
                val rgb = trimmed.toLongOrNull(16) ?: return null
                if (rgb > 0xFFFFFF) return null
                // 将 RGB 转换为 ARGB (添加 alpha = FF)
                val argb = (0xFF000000 or rgb).toInt()
                Color(argb)
            }

            8 -> {
                // ARGB 格式: AARRGGBB
                val argb = trimmed.toLongOrNull(16) ?: return null
                if (argb > 0xFFFFFFFF) return null
                Color(argb.toInt())
            }

            else -> {
                HLog.e(TAG, "⚠️ Unknown color: '$color' (trimmed: '$trimmed')")
                null
            }
        }
    } catch (e: Exception) {
        HLog.e(TAG, "❌ Error parsing color '$color': ${e.message}", e)
        null
    }
}

/**
 * 居中显示的符号集合（编译时常量，避免每次调用重新创建）
 */
private val CENTERED_SYMBOLS = setOf(
    // 箭头
    "rightarrow", "leftarrow", "leftrightarrow",
    "Rightarrow", "Leftarrow", "Leftrightarrow",
    "longrightarrow", "longleftarrow", "longleftrightarrow",
    "Longleftarrow", "Longrightarrow", "Longleftrightarrow",
    "uparrow", "downarrow", "updownarrow",
    "Uparrow", "Downarrow", "Updownarrow",
    "mapsto", "to", "longmapsto",
    "implies", "iff",
    "nearrow", "searrow", "nwarrow", "swarrow",
    // 等号和关系符号
    "equals", "neq", "approx", "equiv", "sim", "simeq", "cong",
    "leq", "geq", "ll", "gg", "le", "ge",
    "subset", "supset", "subseteq", "supseteq",
    "prec", "succ", "preceq", "succeq",
    "in", "ni", "notin",
    "propto", "varpropto", "perp", "parallel",
    "vdash", "dashv",
    // 二元运算符
    "plus", "minus", "times", "div", "cdot",
    "pm", "mp", "ast", "star", "circ",
    "oplus", "ominus", "otimes", "oslash",
    "cup", "cap", "setminus", "wedge", "vee",
    "land", "lor"
)

/**
 * 判断符号是否应该垂直居中
 *
 * 箭头、等号、加减号等二元运算符应该居中显示
 */
fun isCenteredSymbol(symbol: String): Boolean {
    return symbol in CENTERED_SYMBOLS
}
