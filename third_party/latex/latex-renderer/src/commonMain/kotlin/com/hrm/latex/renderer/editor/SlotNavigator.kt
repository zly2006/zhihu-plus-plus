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

import com.hrm.latex.parser.SourceMapper
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.model.SourceRange

/**
 * Slot 导航器
 *
 * 在复合结构（分数、上下标、根号等）的各个输入槽位之间导航。
 * 典型使用场景：插入 `\frac{}{}` 模板后，用 Enter 键依次跳转：
 *   分子 → 分母 → 分数之后
 *
 * 每个复合结构定义有序的 slot 列表，Enter 键跳转到当前 slot 的下一个。
 * 如果已在最后一个 slot，则跳转到整个复合结构之后。
 *
 * 注意：编辑器功能目前处于实验阶段（Experimental），API 可能在后续版本中变更。
 */
object SlotNavigator {

    /**
     * 计算下一个 slot 的光标偏移量
     *
     * @param cursorOffset 当前光标偏移量
     * @param document 已解析的 AST 根节点
     * @return 下一个 slot 的偏移量，如果当前位置不在任何复合结构内则返回 null
     */
    fun nextSlotOffset(cursorOffset: Int, document: LatexNode): Int? {
        val path = SourceMapper.nodePathAt(document, cursorOffset)
        if (path.isEmpty()) return null

        // 从最内层向外找第一个复合结构节点
        for (i in path.lastIndex downTo 0) {
            val node = path[i]
            val slots = orderedSlots(node) ?: continue
            if (slots.isEmpty()) continue

            val nodeRange = node.sourceRange ?: continue

            // 确定当前 slot 索引
            val currentSlotIndex = findCurrentSlot(cursorOffset, slots)

            return if (currentSlotIndex >= 0 && currentSlotIndex < slots.lastIndex) {
                // 跳到下一个 slot 的内容起始位置
                slotContentStart(slots[currentSlotIndex + 1])
            } else {
                // 已在最后一个 slot 或无法确定 → 跳到复合结构之后
                nodeRange.end
            }
        }

        return null
    }

    /**
     * 获取复合结构的有序 slot 列表。
     * 每个 slot 是一个子节点（通常是 Group），按用户输入顺序排列。
     *
     * @return slot 节点列表，如果不是复合结构则返回 null
     */
    internal fun orderedSlots(node: LatexNode): List<LatexNode>? = when (node) {
        is LatexNode.Fraction -> listOf(node.numerator, node.denominator)
        is LatexNode.Superscript -> listOf(node.exponent)
        is LatexNode.Subscript -> listOf(node.index)
        is LatexNode.Root -> listOfNotNull(node.index, node.content)
        is LatexNode.BigOperator -> listOfNotNull(node.subscript, node.superscript)
        is LatexNode.Binomial -> listOf(node.top, node.bottom)
        is LatexNode.Accent -> listOf(node.content)
        is LatexNode.Stack -> {
            val above = node.above
            val below = node.below
            listOfNotNull(above, node.base, below)
        }
        is LatexNode.ExtensibleArrow -> listOfNotNull(node.content, node.below)
        else -> null
    }

    /**
     * 找到 cursorOffset 当前所在的 slot 索引
     *
     * @return slot 索引（0-based），如果不在任何 slot 内返回 -1
     */
    private fun findCurrentSlot(cursorOffset: Int, slots: List<LatexNode>): Int {
        for (i in slots.indices) {
            val range = slots[i].sourceRange ?: continue
            // 使用闭区间 [start, end] 判断：
            // cursorOffset == range.end 时仍认为在该 slot 内（光标在 Group 闭合 } 的位置）
            if (cursorOffset in range.start..range.end) {
                return i
            }
        }
        return -1
    }

    /**
     * 获取 slot 内容的起始偏移量。
     *
     * 如果 slot 是一个 Group 节点（有花括号），则光标应放在 `{` 之后：
     * 即 `sourceRange.start + 1`
     *
     * 如果 slot 不是 Group（例如裸表达式），则放在 sourceRange.start
     */
    private fun slotContentStart(slot: LatexNode): Int {
        val range = slot.sourceRange ?: return 0
        return if (slot is LatexNode.Group) {
            // Group 的 sourceRange 包含花括号 { ... }
            // 光标应在 { 之后
            range.start + 1
        } else {
            range.start
        }
    }
}
