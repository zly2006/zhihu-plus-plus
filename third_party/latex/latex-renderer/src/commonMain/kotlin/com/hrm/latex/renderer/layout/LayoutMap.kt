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

package com.hrm.latex.renderer.layout

import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.model.SourceRange

/**
 * 节点布局条目：记录单个 AST 节点在画布中的相对位置和尺寸
 *
 * 坐标是相对于所属 LayoutMap 根节点的内容区原点 (0,0)，
 * 实际画布坐标需加上外层 padding。
 *
 * @property node 关联的 AST 节点
 * @property relX 相对于根节点内容区的 X 偏移
 * @property relY 相对于根节点内容区的 Y 偏移
 * @property width 节点的墨水边界宽度
 * @property height 节点的墨水边界高度
 * @property baseline 节点基线距顶部的距离
 */
class NodeLayoutEntry(
    val node: LatexNode,
    val relX: Float,
    val relY: Float,
    val width: Float,
    val height: Float,
    val baseline: Float
)

/**
 * 布局映射表：记录 AST 节点到画布坐标的映射关系
 *
 * 在 measureGroup 测量阶段作为可选的 sidecar 采集子节点位置信息，
 * 为编辑器集成提供 hit-testing 和光标定位能力。
 *
 * **零开销设计**：当不需要编辑器功能时传 null，measureGroup 不做任何额外工作。
 *
 * 典型使用流程：
 * ```
 * val layoutMap = LayoutMap()
 * val layout = measureGroup(nodes, context, measurer, density, layoutMap)
 * // layoutMap 现在包含所有 Document.children 的位置信息
 * val entry = layoutMap.entryAt(cursorOffset)  // 光标定位
 * val hit = layoutMap.hitTest(px, py)          // 点击定位
 * ```
 */
class LayoutMap {
    private val entries = mutableListOf<NodeLayoutEntry>()

    /** 添加一个节点布局条目 */
    fun add(entry: NodeLayoutEntry) {
        entries.add(entry)
    }

    /** 添加节点布局条目（便捷方法） */
    fun add(
        node: LatexNode, relX: Float, relY: Float,
        width: Float, height: Float, baseline: Float
    ) {
        entries.add(NodeLayoutEntry(node, relX, relY, width, height, baseline))
    }

    /** 获取所有条目（不可变视图） */
    fun entries(): List<NodeLayoutEntry> = entries.toList()

    /** 条目数量 */
    val size: Int get() = entries.size

    /**
     * Hit-test：找到画布坐标 (px, py) 处最深的（面积最小的）节点
     *
     * @param px 相对于内容区原点的 X 坐标
     * @param py 相对于内容区原点的 Y 坐标
     * @return 匹配的布局条目，如果坐标不在任何节点内则返回 null
     */
    fun hitTest(px: Float, py: Float): NodeLayoutEntry? {
        var best: NodeLayoutEntry? = null
        var bestArea = Float.MAX_VALUE
        for (entry in entries) {
            if (px >= entry.relX && px <= entry.relX + entry.width &&
                py >= entry.relY && py <= entry.relY + entry.height
            ) {
                val area = entry.width * entry.height
                if (area < bestArea) {
                    bestArea = area
                    best = entry
                }
            }
        }
        return best
    }

    /**
     * 根据源码偏移量查找对应的布局条目
     *
     * @param sourceOffset 原始 LaTeX 字符串中的字符偏移
     * @return 包含该偏移量的最深节点的布局条目，如果没有匹配则返回 null
     */
    fun entryAt(sourceOffset: Int): NodeLayoutEntry? {
        var best: NodeLayoutEntry? = null
        var bestSize = Int.MAX_VALUE
        for (entry in entries) {
            val range = entry.node.sourceRange ?: continue
            if (range.contains(sourceOffset)) {
                if (range.length < bestSize) {
                    bestSize = range.length
                    best = entry
                }
            }
        }
        return best
    }

    /**
     * 查找指定 SourceRange 范围内的所有条目
     */
    fun entriesInRange(range: SourceRange): List<NodeLayoutEntry> {
        return entries.filter { entry ->
            val nodeRange = entry.node.sourceRange ?: return@filter false
            nodeRange.start < range.end && nodeRange.end > range.start
        }
    }

    /** 清空所有条目 */
    fun clear() {
        entries.clear()
    }
}
