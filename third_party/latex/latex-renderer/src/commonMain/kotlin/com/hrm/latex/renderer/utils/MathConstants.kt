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

/**
 * 渲染引擎的所有排版常量集中定义处。
 *
 * 设计理由：
 * - 所有魔法数字收拢到此一处，消除散落在各个 Measurer 中的硬编码比例值。
 * - 命名遵循 TeX 排版术语（sigma, xi, mu），便于与 TeXbook 参数对照。
 * - 值均为 **fontSize 的比例系数**，乘以 fontSizePx 得到像素值。
 *
 * 参考：TeXbook Appendix G 与 KaTeX font metrics
 */
internal object MathConstants {

    // ═══════════════════════════════════════════════════════════════════════
    // 1. MathStyle 缩放系数 (TeXbook §702)
    // ═══════════════════════════════════════════════════════════════════════

    /** SCRIPT 样式相对于 TEXT/DISPLAY 的字号缩放 (σ₅) */
    const val SCRIPT_SCALE = 0.7f

    /** SCRIPT_SCRIPT 样式的字号缩放 (σ₆) */
    const val SCRIPT_SCRIPT_SCALE = 0.5f

    // ═══════════════════════════════════════════════════════════════════════
    // 2. 分数 (Fraction) 排版参数
    // ═══════════════════════════════════════════════════════════════════════

    /** 分数线粗细 / fontSize (ξ₈, default rule thickness) */
    const val FRACTION_RULE_THICKNESS = 0.04f

    /** 分数子式（分子/分母）字号缩小因子 */
    const val FRACTION_CHILD_SCALE = 0.9f

    /** 分数线与分子/分母之间的间距 / fontSize */
    const val FRACTION_GAP = 0.15f

    /** 分数线两端水平内缩 / fontSize */
    const val FRACTION_RULE_INSET = 0.075f

    // ═══════════════════════════════════════════════════════════════════════
    // 3. 根号 (Radical) 排版参数
    // ═══════════════════════════════════════════════════════════════════════

    /** 根号钩子宽度 / fontSize */
    const val RADICAL_HOOK_WIDTH = 0.3f

    /** 根号指数水平偏移系数（相对于 hookWidth） */
    const val RADICAL_INDEX_OFFSET = 0.5f

    /** 根号内容上方到横线的间距倍数（相对于 ruleThickness） */
    const val RADICAL_TOP_GAP_MULTIPLIER = 2.0f

    // ═══════════════════════════════════════════════════════════════════════
    // 4. 上下标 (Script) 排版参数
    // ═══════════════════════════════════════════════════════════════════════

    /** 上标向上偏移 / fontSize (σ₁₃, superscript shift-up) */
    const val SUPERSCRIPT_SHIFT = 0.363f

    /** 下标向下偏移 / fontSize (σ₁₆, subscript shift-down) */
    const val SUBSCRIPT_SHIFT = 0.15f

    /** 上下标之间的最小间距 / fontSize */
    const val SCRIPT_MIN_GAP = 0.16f

    /** 上下标与基础符号之间的水平间距 (dp) */
    const val SCRIPT_KERN_DP = 1.0f

    // ═══════════════════════════════════════════════════════════════════════
    // 5. 大型运算符 (Big Operator) 排版参数
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * 混合拉伸中 fontSize 均匀放大的分摊比例（0~1）。
     * 0.5 表示总拉伸的平方根分配给 fontSize，平方根分配给 Canvas scale。
     * 值越大 fontSize 分摊越多（笔画越粗但比例越自然），
     * 值越小 Canvas scale 分摊越多（笔画越细但可能有轻微压缩感）。
     */
    const val INTEGRAL_FONT_SCALE_RATIO = 0.5f

    /** 上下限字号缩放因子 */
    const val BIG_OP_LIMIT_SCALE = 0.7f

    /** 积分符号视觉核心宽度 / fontSize */
    const val INTEGRAL_VISUAL_WIDTH = 0.22f

    /** 积分高度暗示的过大化系数 */
    const val INTEGRAL_HEIGHT_HINT_OVERSHOOT = 1.05f

    /** 积分符号最小垂直拉伸倍数（保证无右侧内容时仍有合理高度） */
    const val INTEGRAL_MIN_VERTICAL_SCALE = 1.5f

    /** 运算符宽度溢出保护系数 */
    const val BIG_OP_WIDTH_OVERFLOW_FACTOR = 1.1f

    // ═══════════════════════════════════════════════════════════════════════
    // 6. 定界符 (Delimiter) 排版参数
    // ═══════════════════════════════════════════════════════════════════════

    /** 定界符上下 padding / fontSize */
    const val DELIMITER_PADDING = 0.15f

    // ═══════════════════════════════════════════════════════════════════════
    // 7. 二项式 (Binomial) 排版参数
    // ═══════════════════════════════════════════════════════════════════════

    /** 二项式子式缩放因子 */
    const val BINOMIAL_CHILD_SCALE = 0.9f

    /** 二项式上下间距 / fontSize */
    const val BINOMIAL_GAP = 0.2f

    // ═══════════════════════════════════════════════════════════════════════
    // 8. 矩阵 (Matrix) 排版参数
    // ═══════════════════════════════════════════════════════════════════════

    /** 矩阵列间距 / fontSize */
    const val MATRIX_COLUMN_SPACING = 0.5f

    /** 矩阵行间距 / fontSize */
    const val MATRIX_ROW_SPACING = 0.2f

    /** Multline 行间距 / fontSize */
    const val MULTLINE_ROW_SPACING = 0.3f

    // ═══════════════════════════════════════════════════════════════════════
    // 9. 装饰符号 (Accent) 排版参数
    // ═══════════════════════════════════════════════════════════════════════

    /** 宽装饰线条/箭头高度 / fontSize */
    const val WIDE_ACCENT_ARROW_HEIGHT = 0.18f

    /** 宽装饰默认高度 / fontSize */
    const val WIDE_ACCENT_DEFAULT_HEIGHT = 0.3f

    /** 宽装饰箭头间距 / fontSize */
    const val WIDE_ACCENT_ARROW_GAP = 0.02f

    /** 宽装饰默认间距 / fontSize */
    const val WIDE_ACCENT_DEFAULT_GAP = 0.08f

    // ═══════════════════════════════════════════════════════════════════════
    // 10. Stack (overset/underset) 排版参数
    // ═══════════════════════════════════════════════════════════════════════

    /** Stack 上下内容字号缩放因子 */
    const val STACK_SCRIPT_SCALE = 0.7f

    /** Stack 标签与基础盒子之间的最小垂直间距 / fontSize。 */
    const val STACK_VERTICAL_GAP = 0.08f

    // ═══════════════════════════════════════════════════════════════════════
    // 11. 可扩展箭头 (ExtensibleArrow) 排版参数
    // ═══════════════════════════════════════════════════════════════════════

    /** 箭头最小长度 (dp) */
    const val EXTENSIBLE_ARROW_MIN_LENGTH_DP = 30f

    /** 箭头水平 padding (dp) */
    const val EXTENSIBLE_ARROW_PADDING_DP = 4f

    /** 箭头头部大小 (dp) */
    const val EXTENSIBLE_ARROW_HEAD_SIZE_DP = 5f

    /** 箭头线条粗细 (dp) */
    const val EXTENSIBLE_ARROW_STROKE_DP = 1.5f

    /** 箭头与上下标注之间的间距 / fontSize（KaTeX xArrow: 2mu = 0.111em） */
    const val EXTENSIBLE_ARROW_TEXT_GAP = 0.111f

    // ═══════════════════════════════════════════════════════════════════════
    // 12. Boxed 排版参数
    // ═══════════════════════════════════════════════════════════════════════

    /** Boxed 内边距 / fontSize */
    const val BOXED_PADDING = 0.15f

    /** Boxed 边框线宽 (dp) */
    const val BOXED_BORDER_WIDTH_DP = 1f

    // ═══════════════════════════════════════════════════════════════════════
    // 13. 外层 Canvas 内边距
    // ═══════════════════════════════════════════════════════════════════════

    /** Canvas 水平内边距 / fontSize */
    const val CANVAS_HORIZONTAL_PADDING = 0.15f

    /** Canvas 垂直内边距 / fontSize */
    const val CANVAS_VERTICAL_PADDING = 0.10f

    // ═══════════════════════════════════════════════════════════════════════
    // 14. 行排版参数
    // ═══════════════════════════════════════════════════════════════════════

    /** 行间距 / fontSize */
    const val LINE_SPACING = 0.25f

    /** 数学轴高度相对于字体大小的回退比例（当动态计算失败时使用） */
    const val MATH_AXIS_HEIGHT_RATIO = 0.25f

    /** Operator（sin/cos 等）右侧间距 / fontSize */
    const val OPERATOR_RIGHT_GAP = 0.166f

    // ═══════════════════════════════════════════════════════════════════════
    // TeX 标准间距（mu 单位，1em = 18mu）
    // ═══════════════════════════════════════════════════════════════════════

    /** 细间距 \, = 3mu ≈ 0.167em */
    const val THIN_SPACE = 3f / 18f

    /** 中间距 \: = 4mu ≈ 0.222em */
    const val MEDIUM_SPACE = 4f / 18f

    /** 粗间距 \; = 5mu ≈ 0.278em */
    const val THICK_SPACE = 5f / 18f

    /** 四倍间距 \quad = 18mu = 1em */
    const val QUAD_SPACE = 1f

    // ═══════════════════════════════════════════════════════════════════════
    // 15. 斜体悬伸补偿
    // ═══════════════════════════════════════════════════════════════════════

    /** 大写字母右侧斜体悬伸 / fontSizePx */
    const val ITALIC_RIGHT_OVERHANG_UPPER = 0.15f

    /** 小写字母右侧斜体悬伸 / fontSizePx */
    const val ITALIC_RIGHT_OVERHANG_LOWER = 0.12f

    /** 其他字符右侧斜体悬伸 / fontSizePx */
    const val ITALIC_RIGHT_OVERHANG_OTHER = 0.08f

    /** 特定首字符左侧斜体悬伸 / fontSizePx */
    const val ITALIC_LEFT_OVERHANG = 0.05f

    // ═══════════════════════════════════════════════════════════════════════
    // 16. 大型运算符 FontWeight 补偿
    // ═══════════════════════════════════════════════════════════════════════

    /** 符号运算符正常 fontWeight */
    const val BIG_OP_SYMBOL_BASE_WEIGHT = 400

    /** 符号运算符最小 fontWeight */
    const val BIG_OP_SYMBOL_MIN_WEIGHT = 100

    /** 命名运算符最小 fontWeight */
    const val BIG_OP_NAMED_MIN_WEIGHT = 300

    /** 垂直拉伸后的额外 weight 减少量（最大值） */
    const val BIG_OP_VERTICAL_SCALE_WEIGHT_REDUCTION = 200
}
