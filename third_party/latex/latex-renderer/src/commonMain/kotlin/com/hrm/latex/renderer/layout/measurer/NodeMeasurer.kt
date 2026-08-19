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


package com.hrm.latex.renderer.layout.measurer

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import kotlin.reflect.KClass

/**
 * 节点测量器接口
 *
 * 定义了测量特定类型 LaTeX 节点的通用契约。
 * 所有的具体测量器（如 [TextContentMeasurer], [FractionMeasurer], [ScriptMeasurer] 等）都实现此接口。
 *
 * 每个测量器通过 [handledNodeTypes] 声明自己能处理的节点类型，
 * [MeasurerRegistry] 根据此信息自动分发节点到对应的测量器。
 * 新增节点类型时只需编写新的测量器并注册，无需修改分发逻辑。
 */
internal interface NodeMeasurer {

    /**
     * 此测量器能处理的节点类型集合。
     *
     * [MeasurerRegistry] 根据此属性自动建立节点类型 → 测量器的映射。
     * 一个测量器可以处理多种节点类型（如 [TextContentMeasurer] 处理 Text/Symbol/Operator 等）。
     */
    val handledNodeTypes: Set<KClass<out LatexNode>>

    /**
     * 测量节点并返回布局结果
     *
     * @param node 要测量的节点
     * @param context 渲染上下文（包含当前字号、颜色等状态）
     * @param measurer Compose 的文本测量器
     * @param density 屏幕密度，用于 dp/sp 转 px
     * @param measureNode 回调函数，用于递归测量通用节点（当节点包含子节点时使用）
     * @param measureGroup 回调函数，用于递归测量节点组（当处理节点列表时使用）
     * @return [NodeLayout] 包含尺寸、基线和绘制逻辑的布局对象
     */
    fun measure(
        node: LatexNode,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureNode: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout
}
