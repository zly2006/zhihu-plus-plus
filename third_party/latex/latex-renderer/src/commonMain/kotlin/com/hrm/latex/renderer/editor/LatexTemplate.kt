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

package com.hrm.latex.renderer.editor

/**
 * 模板展开结果
 *
 * @property text 展开后的 LaTeX 文本
 * @property cursorDelta 光标相对于插入点起始位置的偏移量
 */
data class TemplateExpansion(
    val text: String,
    val cursorDelta: Int
)

/**
 * LaTeX 结构模板枚举
 *
 * 提供常用 LaTeX 结构的快速插入，光标自动定位到第一个占位符位置。
 *
 * 注意：编辑器功能目前处于实验阶段（Experimental），API 可能在后续版本中变更。
 *
 * 使用方式：
 * ```kotlin
 * editorState.insertTemplate(LatexTemplate.FRACTION)
 * // 插入 "\frac{}{}"，光标定位在第一个 {} 内
 * ```
 */
enum class LatexTemplate(
    val displayName: String,
    val icon: String,
    private val template: String,
    private val cursorOffset: Int
) {
    /** 分数 \frac{分子}{分母} — 光标在分子位置 */
    FRACTION(
        displayName = "分数",
        icon = "½",
        template = "\\frac{}{}",
        cursorOffset = 6  // \frac{|} 的位置
    ),

    /** 平方根 \sqrt{内容} — 光标在内容位置 */
    SQRT(
        displayName = "平方根",
        icon = "√",
        template = "\\sqrt{}",
        cursorOffset = 6  // \sqrt{|}
    ),

    /** n 次根 \sqrt[n]{内容} — 光标在 n 位置 */
    NTHROOT(
        displayName = "n次根",
        icon = "∛",
        template = "\\sqrt[]{}",
        cursorOffset = 6  // \sqrt[|]{}
    ),

    /** 上标 ^{指数} — 光标在指数位置 */
    SUPERSCRIPT(
        displayName = "上标",
        icon = "x²",
        template = "^{}",
        cursorOffset = 2  // ^{|}
    ),

    /** 下标 _{下标} — 光标在下标位置 */
    SUBSCRIPT(
        displayName = "下标",
        icon = "x₂",
        template = "_{}",
        cursorOffset = 2  // _{|}
    ),

    /** 求和 \sum_{下限}^{上限} — 光标在下限位置 */
    SUM(
        displayName = "求和",
        icon = "∑",
        template = "\\sum_{}^{}",
        cursorOffset = 6  // \sum_{|}^{}
    ),

    /** 积分 \int_{下限}^{上限} — 光标在下限位置 */
    INTEGRAL(
        displayName = "积分",
        icon = "∫",
        template = "\\int_{}^{}",
        cursorOffset = 6  // \int_{|}^{}
    ),

    /** 极限 \lim_{变量} — 光标在变量位置 */
    LIMIT(
        displayName = "极限",
        icon = "lim",
        template = "\\lim_{}",
        cursorOffset = 6  // \lim_{|}
    ),

    /** 矩阵 \begin{pmatrix}...\end{pmatrix} — 光标在第一个元素位置 */
    MATRIX(
        displayName = "矩阵",
        icon = "[ ]",
        template = "\\begin{pmatrix}  \\\\  \\end{pmatrix}",
        cursorOffset = 16  // \begin{pmatrix}|
    ),

    /** 自动定界符 \left( \right) — 光标在内容位置 */
    PARENTHESES(
        displayName = "括号",
        icon = "()",
        template = "\\left( \\right)",
        cursorOffset = 7  // \left( |
    ),

    /** 上划线 \overline{} — 光标在内容位置 */
    OVERLINE(
        displayName = "上划线",
        icon = "x̄",
        template = "\\overline{}",
        cursorOffset = 10  // \overline{|}
    ),

    /** 向量 \vec{} — 光标在内容位置 */
    VEC(
        displayName = "向量",
        icon = "→",
        template = "\\vec{}",
        cursorOffset = 5  // \vec{|}
    ),

    /** 帽子 \hat{} — 光标在内容位置 */
    HAT(
        displayName = "帽子",
        icon = "â",
        template = "\\hat{}",
        cursorOffset = 5  // \hat{|}
    );

    /**
     * 展开模板
     */
    fun expand(): TemplateExpansion = TemplateExpansion(
        text = template,
        cursorDelta = cursorOffset
    )
}
