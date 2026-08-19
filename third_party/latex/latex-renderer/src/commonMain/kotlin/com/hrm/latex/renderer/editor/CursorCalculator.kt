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

import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.model.SourceRange
import com.hrm.latex.renderer.layout.LayoutMap
import com.hrm.latex.renderer.layout.NodeLayoutEntry

/**
 * 光标像素位置
 *
 * @property x 相对于内容区原点的 X 坐标
 * @property y 相对于内容区原点的 Y 坐标（光标顶部）
 * @property height 光标高度
 */
data class CursorPosition(
    val x: Float,
    val y: Float,
    val height: Float
)

/**
 * 光标位置计算器
 *
 * 将源码偏移量（cursorOffset）转换为画布上的像素坐标，
 * 基于 LayoutMap 中记录的节点位置信息。
 *
 * 算法：
 * 1. 在 LayoutMap 中查找包含 cursorOffset 的最深节点
 * 2. 对叶子节点：通过字符比例插值计算光标在该节点内的精确 X 位置
 * 3. 对复合节点（Fraction/Script/Root 等）：定位到子结构的 sourceRange，
 *    使用子结构在复合节点内的视觉区域做更精确的定位
 *
 * 注意：编辑器功能目前处于实验阶段（Experimental），API 可能在后续版本中变更。
 */
object CursorCalculator {

    /**
     * 闭区间匹配：cursorOffset in [start, end]
     *
     * 与 SourceRange.contains()（半开区间 [start, end)）不同，
     * 此方法允许光标出现在子结构末尾（offset == range.end），
     * 因为那代表 `}` 之前的合法编辑位置。
     */
    private fun SourceRange.containsInclusive(offset: Int): Boolean =
        offset in start..end

    /**
     * 判断节点是否为复合节点（包含子结构的节点）
     */
    private fun isCompoundNode(node: LatexNode): Boolean = when (node) {
        is LatexNode.Fraction, is LatexNode.Superscript, is LatexNode.Subscript,
        is LatexNode.Root, is LatexNode.BigOperator, is LatexNode.Accent,
        is LatexNode.Binomial, is LatexNode.Stack, is LatexNode.Delimited -> true
        else -> false
    }

    /**
     * 获取节点的**内容** sourceRange，去除 Group 的花括号。
     *
     * 问题场景：`x^{2}+y^{2}` 中，exponent 是 Group(sourceRange=(2,5))，
     * 包含了 `{` 和 `}`。如果直接用 Group 的 range，光标在 exponent 末尾时
     * offset=5 会落在第二个 Superscript 的 start，导致光标跳到错误位置。
     *
     * 解决方案：对 Group 节点，使用其内部 children 的合并范围（不含花括号）。
     * 如果 Group 为空，则缩小范围到花括号内部（start+1, end-1）。
     */
    private fun innerContentRange(node: LatexNode): SourceRange? {
        if (node is LatexNode.Group) {
            val groupRange = node.sourceRange ?: return null
            if (node.children.isNotEmpty()) {
                val first = node.children.first().sourceRange ?: return null
                val last = node.children.last().sourceRange ?: return null
                return SourceRange(first.start, last.end)
            }
            // 空 Group: "{}" → 光标在 { 和 } 之间
            return if (groupRange.length >= 2) {
                SourceRange(groupRange.start + 1, groupRange.end - 1)
            } else {
                groupRange
            }
        }
        return node.sourceRange
    }

    /**
     * 计算光标的像素位置
     *
     * @param cursorOffset 光标在源码中的字符偏移量
     * @param layoutMap 布局映射表
     * @param horizontalPadding 外层水平内边距
     * @param verticalPadding 外层垂直内边距
     * @return 光标像素位置，如果无法定位则返回 null
     */
    fun calculate(
        cursorOffset: Int,
        layoutMap: LayoutMap,
        horizontalPadding: Float,
        verticalPadding: Float
    ): CursorPosition? {
        if (layoutMap.size == 0) return null

        // 策略 1：复合节点子结构末尾（最高优先级）
        //
        // 当 cursorOffset 恰好落在复合节点某个子结构的 end 位置时，
        // 半开区间 entryAt() 可能匹配到下一个节点（其 start == cursorOffset），
        // 导致光标定位错误。因此优先使用闭区间查找复合节点。
        //
        // 例如 `x^2+y^2=z^2`：
        //   Superscript#1 range=(0,3), exponent innerRange=(2,3)
        //   Text("+y") range=(3,5)
        //   cursorOffset=3 → entryAt() 会命中 Text("+y")，但实际应在 exponent 末尾
        val compoundEntry = findCompoundEntryContaining(cursorOffset, layoutMap)
        if (compoundEntry != null) {
            val subPos = resolveCompoundCursorPosition(
                compoundEntry, cursorOffset, horizontalPadding, verticalPadding
            )
            if (subPos != null) return subPos
        }

        // 策略 2：精确查找 — 光标在某个节点的范围内 [start, end)
        val entry = layoutMap.entryAt(cursorOffset)
        if (entry != null) {
            val range = entry.node.sourceRange ?: return fallbackPosition(
                entry, horizontalPadding, verticalPadding
            )

            // 对复合节点做子结构定位
            val subPos = resolveCompoundCursorPosition(
                entry, cursorOffset, horizontalPadding, verticalPadding
            )
            if (subPos != null) return subPos

            // 叶子节点：字符比例插值
            val ratio = if (range.length > 0) {
                (cursorOffset - range.start).toFloat() / range.length
            } else {
                0f
            }

            val x = entry.relX + entry.width * ratio + horizontalPadding
            val y = entry.relY + verticalPadding
            return CursorPosition(x, y, entry.height)
        }

        // 策略 3：右边缘匹配 — cursorOffset 恰好等于某个顶层节点的 sourceRange.end
        val rightEdgeEntry = findRightEdgeEntry(cursorOffset, layoutMap)
        if (rightEdgeEntry != null) {
            // 先检查这是否是一个复合节点且 cursorOffset 落在某个子结构的末尾
            val subPos = resolveCompoundCursorPosition(
                rightEdgeEntry, cursorOffset, horizontalPadding, verticalPadding
            )
            if (subPos != null) return subPos

            // 非复合节点或 cursorOffset 在整个节点末尾 → 光标在节点右侧
            return CursorPosition(
                x = rightEdgeEntry.relX + rightEdgeEntry.width + horizontalPadding,
                y = rightEdgeEntry.relY + verticalPadding,
                height = rightEdgeEntry.height
            )
        }

        // 策略 4：边界查找 — 光标在节点之间或末尾
        return findNearestPosition(cursorOffset, layoutMap, horizontalPadding, verticalPadding)
    }

    /**
     * Hit-test：将画布坐标转换为源码偏移量
     *
     * @param px 画布 X 坐标（包含 padding）
     * @param py 画布 Y 坐标（包含 padding）
     * @param layoutMap 布局映射表
     * @param horizontalPadding 外层水平内边距
     * @param verticalPadding 外层垂直内边距
     * @param textLength 文本总长度（用于边界处理）
     * @return 源码偏移量
     */
    fun hitTestToOffset(
        px: Float,
        py: Float,
        layoutMap: LayoutMap,
        horizontalPadding: Float,
        verticalPadding: Float,
        textLength: Int
    ): Int {
        val contentX = px - horizontalPadding
        val contentY = py - verticalPadding

        val entry = layoutMap.hitTest(contentX, contentY)
        if (entry == null) {
            // 点击不在任何节点的矩形内 → 找最近的节点
            return hitTestNearestEntry(contentX, contentY, layoutMap, textLength)
        }

        val range = entry.node.sourceRange ?: return textLength

        // 对复合节点做子结构 hit-test
        val subOffset = resolveCompoundHitTest(
            entry, contentX, contentY, textLength
        )
        if (subOffset != null) return subOffset

        // 叶子节点：X 方向字符比例插值
        val ratio = if (entry.width > 0f) {
            ((contentX - entry.relX) / entry.width).coerceIn(0f, 1f)
        } else {
            0f
        }
        val offset = range.start + (ratio * range.length).toInt()
        return offset.coerceIn(0, textLength)
    }

    /**
     * 当点击不在任何节点的矩形内时，找到水平方向最近的节点并计算偏移。
     * 如果点击在某节点的右侧，返回该节点的 sourceRange.end。
     * 如果点击在某节点的左侧，返回该节点的 sourceRange.start。
     */
    private fun hitTestNearestEntry(
        contentX: Float,
        contentY: Float,
        layoutMap: LayoutMap,
        textLength: Int
    ): Int {
        val entries = layoutMap.entries()
        if (entries.isEmpty()) return textLength

        var bestEntry: NodeLayoutEntry? = null
        var bestDist = Float.MAX_VALUE

        for (entry in entries) {
            // 计算到 entry 中心的水平距离（优先水平接近）
            val centerX = entry.relX + entry.width / 2f
            val centerY = entry.relY + entry.height / 2f
            val dx = contentX - centerX
            val dy = contentY - centerY
            // 水平距离权重更高，因为同一行的节点水平排列
            val dist = dx * dx + dy * dy * 4f
            if (dist < bestDist) {
                bestDist = dist
                bestEntry = entry
            }
        }

        val entry = bestEntry ?: return textLength
        val range = entry.node.sourceRange ?: return textLength

        // 判断点击在节点的左侧还是右侧
        val entryCenter = entry.relX + entry.width / 2f
        return if (contentX >= entryCenter) {
            // 点击在节点右半部或右侧 → 节点末尾
            range.end.coerceAtMost(textLength)
        } else {
            // 点击在节点左半部或左侧 → 节点开头
            range.start.coerceIn(0, textLength)
        }
    }

    // ========== 复合节点子结构解析 ==========

    /**
     * 对复合节点（Fraction/Script/Root/BigOperator/Accent 等）的 hit-test，
     * 根据点击位置判断命中的是哪个子结构，返回子结构内的精确偏移量。
     *
     * 关键改进：使用子结构的视觉区域来精确判断命中区域，
     * 并支持定位到子结构的 end 位置（即最后一个字符之后）。
     */
    private fun resolveCompoundHitTest(
        entry: NodeLayoutEntry,
        contentX: Float,
        contentY: Float,
        textLength: Int
    ): Int? {
        val node = entry.node
        if (!isCompoundNode(node)) return null

        val localY = contentY - entry.relY
        val localX = contentX - entry.relX

        // 获取所有子结构的视觉区域
        val regions = collectAllChildVisualRegions(entry)
        if (regions.isEmpty()) return null

        // 找到命中的视觉区域（优先精确命中，否则找最近的）
        val hitRegion = regions.firstOrNull { region ->
            localX >= region.xStart && localX <= region.xEnd &&
                localY >= region.yStart && localY <= region.yEnd
        } ?: findNearestRegion(regions, localX, localY)
        ?: return null

        // 在子结构视觉区域内做字符比例插值
        return offsetInChildVisualRegion(hitRegion, localX, textLength)
    }

    /**
     * 在子结构视觉区域内，根据 localX 位置计算源码偏移量。
     * 支持定位到 childRange.end（最后一个字符之后）。
     */
    private fun offsetInChildVisualRegion(
        region: ChildVisualRegion,
        localX: Float,
        textLength: Int
    ): Int {
        val regionWidth = region.xEnd - region.xStart
        val ratio = if (regionWidth > 0f) {
            ((localX - region.xStart) / regionWidth).coerceIn(0f, 1f)
        } else {
            0f
        }
        val charPos = ratio * region.childRange.length
        val offset = region.childRange.start + (charPos + 0.5f).toInt().coerceAtMost(region.childRange.length)
        return offset.coerceIn(0, textLength)
    }

    /**
     * 找到距离 (localX, localY) 最近的子结构视觉区域
     */
    private fun findNearestRegion(
        regions: List<ChildVisualRegion>,
        localX: Float,
        localY: Float
    ): ChildVisualRegion? {
        var best: ChildVisualRegion? = null
        var bestDist = Float.MAX_VALUE
        for (region in regions) {
            val cx = (region.xStart + region.xEnd) / 2f
            val cy = (region.yStart + region.yEnd) / 2f
            val dx = localX - cx
            val dy = localY - cy
            val dist = dx * dx + dy * dy
            if (dist < bestDist) {
                bestDist = dist
                best = region
            }
        }
        return best
    }

    /**
     * 子结构在复合节点内的视觉区域
     *
     * @property childRange 子结构的 sourceRange
     * @property xStart 子结构左边缘相对于节点左侧的 X 偏移
     * @property xEnd 子结构右边缘相对于节点左侧的 X 偏移
     * @property yStart 子结构顶部相对于节点顶部的 Y 偏移
     * @property yEnd 子结构底部相对于节点顶部的 Y 偏移
     */
    private data class ChildVisualRegion(
        val childRange: SourceRange,
        val xStart: Float,
        val xEnd: Float,
        val yStart: Float,
        val yEnd: Float
    )

    /**
     * 收集复合节点所有子结构的视觉区域列表。
     * 用于 hit-test 时精确判断点击了哪个子结构。
     */
    private fun collectAllChildVisualRegions(entry: NodeLayoutEntry): List<ChildVisualRegion> {
        val node = entry.node
        val w = entry.width
        val h = entry.height
        val midY = h / 2f

        return when (node) {
            is LatexNode.Fraction -> {
                val regions = mutableListOf<ChildVisualRegion>()
                innerContentRange(node.numerator)?.let {
                    regions.add(ChildVisualRegion(it, 0f, w, 0f, midY))
                }
                innerContentRange(node.denominator)?.let {
                    regions.add(ChildVisualRegion(it, 0f, w, midY, h))
                }
                regions
            }

            is LatexNode.Superscript -> {
                val baseRange = innerContentRange(node.base)
                val expRange = innerContentRange(node.exponent)
                val baseLen = baseRange?.length ?: 1
                val expLen = expRange?.length ?: 1
                val estimatedBaseRatio = baseLen.toFloat() / (baseLen + expLen * 0.7f)
                val baseEndX = w * estimatedBaseRatio.coerceIn(0.2f, 0.9f)

                val regions = mutableListOf<ChildVisualRegion>()
                baseRange?.let {
                    regions.add(ChildVisualRegion(it, 0f, baseEndX, 0f, h))
                }
                expRange?.let {
                    regions.add(ChildVisualRegion(it, baseEndX, w, 0f, h * 0.6f))
                }
                regions
            }

            is LatexNode.Subscript -> {
                val baseRange = innerContentRange(node.base)
                val idxRange = innerContentRange(node.index)
                val baseLen = baseRange?.length ?: 1
                val idxLen = idxRange?.length ?: 1
                val estimatedBaseRatio = baseLen.toFloat() / (baseLen + idxLen * 0.7f)
                val baseEndX = w * estimatedBaseRatio.coerceIn(0.2f, 0.9f)

                val regions = mutableListOf<ChildVisualRegion>()
                baseRange?.let {
                    regions.add(ChildVisualRegion(it, 0f, baseEndX, 0f, h))
                }
                idxRange?.let {
                    regions.add(ChildVisualRegion(it, baseEndX, w, h * 0.4f, h))
                }
                regions
            }

            is LatexNode.Root -> {
                val hookRatio = 0.3f
                val contentStartX = w * hookRatio

                val regions = mutableListOf<ChildVisualRegion>()
                node.index?.let { innerContentRange(it) }?.let {
                    regions.add(ChildVisualRegion(it, 0f, contentStartX, 0f, midY))
                }
                innerContentRange(node.content)?.let {
                    regions.add(ChildVisualRegion(it, contentStartX, w, 0f, h))
                }
                regions
            }

            is LatexNode.BigOperator -> {
                val regions = mutableListOf<ChildVisualRegion>()
                node.superscript?.let { innerContentRange(it) }?.let {
                    regions.add(ChildVisualRegion(it, 0f, w, 0f, h * 0.3f))
                }
                node.subscript?.let { innerContentRange(it) }?.let {
                    regions.add(ChildVisualRegion(it, 0f, w, h * 0.7f, h))
                }
                regions
            }

            is LatexNode.Accent -> {
                val regions = mutableListOf<ChildVisualRegion>()
                innerContentRange(node.content)?.let {
                    regions.add(ChildVisualRegion(it, 0f, w, 0f, h))
                }
                regions
            }

            is LatexNode.Binomial -> {
                val regions = mutableListOf<ChildVisualRegion>()
                innerContentRange(node.top)?.let {
                    regions.add(ChildVisualRegion(it, 0f, w, 0f, midY))
                }
                innerContentRange(node.bottom)?.let {
                    regions.add(ChildVisualRegion(it, 0f, w, midY, h))
                }
                regions
            }

            is LatexNode.Stack -> {
                val regions = mutableListOf<ChildVisualRegion>()
                node.above?.let { innerContentRange(it) }?.let {
                    regions.add(ChildVisualRegion(it, 0f, w, 0f, h * 0.33f))
                }
                innerContentRange(node.base)?.let {
                    regions.add(ChildVisualRegion(it, 0f, w, h * 0.33f, h * 0.66f))
                }
                node.below?.let { innerContentRange(it) }?.let {
                    regions.add(ChildVisualRegion(it, 0f, w, h * 0.66f, h))
                }
                regions
            }

            is LatexNode.Delimited -> {
                if (node.content.isNotEmpty()) {
                    val firstChild = node.content.first()
                    val lastChild = node.content.last()
                    val contentStart = innerContentRange(firstChild)?.start
                    val contentEnd = innerContentRange(lastChild)?.end
                    if (contentStart != null && contentEnd != null) {
                        listOf(ChildVisualRegion(
                            SourceRange(contentStart, contentEnd),
                            0f, w, 0f, h
                        ))
                    } else emptyList()
                } else emptyList()
            }

            else -> emptyList()
        }
    }

    /**
     * 对复合节点的光标定位：当 cursorOffset 落在复合节点的 sourceRange 内时，
     * 根据光标在子结构中的**精确视觉区域**返回像素坐标。
     *
     * 使用 containsInclusive（闭区间 [start, end]）匹配子结构，
     * 确保光标在子结构末尾（offset == childRange.end）时也能正确定位。
     */
    private fun resolveCompoundCursorPosition(
        entry: NodeLayoutEntry,
        cursorOffset: Int,
        horizontalPadding: Float,
        verticalPadding: Float
    ): CursorPosition? {
        val region = findChildVisualRegion(entry, cursorOffset) ?: return null

        // 在子结构区域内做字符比例插值
        // 使用闭区间比例：ratio 范围 [0, 1]，其中 1.0 表示子结构末尾
        val ratio = if (region.childRange.length > 0) {
            ((cursorOffset - region.childRange.start).toFloat() / region.childRange.length)
                .coerceIn(0f, 1f)
        } else {
            0f
        }

        val x = entry.relX + region.xStart + (region.xEnd - region.xStart) * ratio + horizontalPadding
        val y = entry.relY + region.yStart + verticalPadding
        val cursorHeight = region.yEnd - region.yStart

        return CursorPosition(x, y, cursorHeight)
    }

    /**
     * 根据复合节点类型和 cursorOffset，确定所在子结构的精确视觉区域。
     *
     * 关键改进：使用 containsInclusive（闭区间 [start, end]）匹配子结构，
     * 这样当 cursorOffset == childRange.end 时（即子结构最后一个字符之后），
     * 仍能匹配到正确的子结构，光标定位到子结构视觉区域的右边缘。
     */
    private fun findChildVisualRegion(
        entry: NodeLayoutEntry,
        cursorOffset: Int
    ): ChildVisualRegion? {
        val node = entry.node
        val w = entry.width
        val h = entry.height
        val midY = h / 2f

        return when (node) {
            is LatexNode.Fraction -> {
                val numRange = innerContentRange(node.numerator)
                val denRange = innerContentRange(node.denominator)
                when {
                    numRange != null && numRange.containsInclusive(cursorOffset) ->
                        ChildVisualRegion(numRange, 0f, w, 0f, midY)
                    denRange != null && denRange.containsInclusive(cursorOffset) ->
                        ChildVisualRegion(denRange, 0f, w, midY, h)
                    else -> null
                }
            }

            is LatexNode.Superscript -> {
                val baseRange = innerContentRange(node.base)
                val expRange = innerContentRange(node.exponent)
                val baseLen = baseRange?.length ?: 1
                val expLen = expRange?.length ?: 1
                val estimatedBaseRatio = baseLen.toFloat() / (baseLen + expLen * 0.7f)
                val baseEndX = w * estimatedBaseRatio.coerceIn(0.2f, 0.9f)

                when {
                    expRange != null && expRange.containsInclusive(cursorOffset) ->
                        ChildVisualRegion(expRange, baseEndX, w, 0f, h * 0.6f)
                    baseRange != null && baseRange.containsInclusive(cursorOffset) ->
                        ChildVisualRegion(baseRange, 0f, baseEndX, 0f, h)
                    else -> null
                }
            }

            is LatexNode.Subscript -> {
                val baseRange = innerContentRange(node.base)
                val idxRange = innerContentRange(node.index)
                val baseLen = baseRange?.length ?: 1
                val idxLen = idxRange?.length ?: 1
                val estimatedBaseRatio = baseLen.toFloat() / (baseLen + idxLen * 0.7f)
                val baseEndX = w * estimatedBaseRatio.coerceIn(0.2f, 0.9f)

                when {
                    idxRange != null && idxRange.containsInclusive(cursorOffset) ->
                        ChildVisualRegion(idxRange, baseEndX, w, h * 0.4f, h)
                    baseRange != null && baseRange.containsInclusive(cursorOffset) ->
                        ChildVisualRegion(baseRange, 0f, baseEndX, 0f, h)
                    else -> null
                }
            }

            is LatexNode.Root -> {
                val contentRange = innerContentRange(node.content)
                val indexRange = node.index?.let { innerContentRange(it) }
                val hookRatio = 0.3f
                val contentStartX = w * hookRatio

                when {
                    indexRange != null && indexRange.containsInclusive(cursorOffset) ->
                        ChildVisualRegion(indexRange, 0f, contentStartX, 0f, midY)
                    contentRange != null && contentRange.containsInclusive(cursorOffset) ->
                        ChildVisualRegion(contentRange, contentStartX, w, 0f, h)
                    else -> null
                }
            }

            is LatexNode.Accent -> {
                val contentRange = innerContentRange(node.content)
                when {
                    contentRange != null && contentRange.containsInclusive(cursorOffset) ->
                        ChildVisualRegion(contentRange, 0f, w, 0f, h)
                    else -> null
                }
            }

            is LatexNode.Binomial -> {
                val topRange = innerContentRange(node.top)
                val bottomRange = innerContentRange(node.bottom)
                when {
                    topRange != null && topRange.containsInclusive(cursorOffset) ->
                        ChildVisualRegion(topRange, 0f, w, 0f, midY)
                    bottomRange != null && bottomRange.containsInclusive(cursorOffset) ->
                        ChildVisualRegion(bottomRange, 0f, w, midY, h)
                    else -> null
                }
            }

            is LatexNode.BigOperator -> {
                val supRange = node.superscript?.let { innerContentRange(it) }
                val subRange = node.subscript?.let { innerContentRange(it) }
                when {
                    supRange != null && supRange.containsInclusive(cursorOffset) ->
                        ChildVisualRegion(supRange, 0f, w, 0f, h * 0.3f)
                    subRange != null && subRange.containsInclusive(cursorOffset) ->
                        ChildVisualRegion(subRange, 0f, w, h * 0.7f, h)
                    else -> null
                }
            }

            is LatexNode.Stack -> {
                val aboveRange = node.above?.let { innerContentRange(it) }
                val baseRange = innerContentRange(node.base)
                val belowRange = node.below?.let { innerContentRange(it) }
                when {
                    aboveRange != null && aboveRange.containsInclusive(cursorOffset) ->
                        ChildVisualRegion(aboveRange, 0f, w, 0f, h * 0.33f)
                    belowRange != null && belowRange.containsInclusive(cursorOffset) ->
                        ChildVisualRegion(belowRange, 0f, w, h * 0.66f, h)
                    baseRange != null && baseRange.containsInclusive(cursorOffset) ->
                        ChildVisualRegion(baseRange, 0f, w, h * 0.33f, h * 0.66f)
                    else -> null
                }
            }

            else -> null
        }
    }

    // ========== 边界查找 ==========

    private fun fallbackPosition(
        entry: NodeLayoutEntry,
        horizontalPadding: Float,
        verticalPadding: Float
    ): CursorPosition {
        return CursorPosition(
            x = entry.relX + horizontalPadding,
            y = entry.relY + verticalPadding,
            height = entry.height
        )
    }

    /**
     * 查找包含 cursorOffset 的最小复合节点（使用闭区间 [start, end]）。
     *
     * 当 cursorOffset == 子结构的 end 时，半开区间 entryAt() 无法命中，
     * 但闭区间可以命中父级复合节点，从而通过 resolveCompoundCursorPosition
     * 将光标定位到子结构末尾。
     *
     * 仅返回复合节点（Fraction/Script/Root 等），排除叶子节点。
     */
    private fun findCompoundEntryContaining(
        cursorOffset: Int,
        layoutMap: LayoutMap
    ): NodeLayoutEntry? {
        var best: NodeLayoutEntry? = null
        var bestSize = Int.MAX_VALUE

        for (entry in layoutMap.entries()) {
            val range = entry.node.sourceRange ?: continue
            if (!isCompoundNode(entry.node)) continue
            if (range.containsInclusive(cursorOffset)) {
                if (range.length < bestSize) {
                    bestSize = range.length
                    best = entry
                }
            }
        }
        return best
    }

    /**
     * 右边缘匹配：查找 sourceRange.end == cursorOffset 的条目
     *
     * 当有多个候选时，选择 relX + width 最大的（视觉上最靠右的），
     * 这样光标会出现在最后一个以该 offset 结束的节点右侧。
     */
    private fun findRightEdgeEntry(
        cursorOffset: Int,
        layoutMap: LayoutMap
    ): NodeLayoutEntry? {
        var best: NodeLayoutEntry? = null
        var bestRightEdge = Float.MIN_VALUE

        for (entry in layoutMap.entries()) {
            val range = entry.node.sourceRange ?: continue
            if (range.end == cursorOffset) {
                val rightEdge = entry.relX + entry.width
                if (rightEdge > bestRightEdge) {
                    bestRightEdge = rightEdge
                    best = entry
                }
            }
        }
        return best
    }

    /**
     * 查找最近的节点位置（光标在节点间或末尾时使用）
     *
     * 策略：
     * 1. 优先找 sourceRange.end <= cursorOffset 的节点（光标在其右侧）
     *    选择 end 最大的（即源码中最靠近光标的前一个节点）
     * 2. 如果找不到，找 sourceRange.start > cursorOffset 的节点（光标在其左侧）
     *    选择 start 最小的（即源码中最靠近光标的后一个节点）
     */
    private fun findNearestPosition(
        cursorOffset: Int,
        layoutMap: LayoutMap,
        horizontalPadding: Float,
        verticalPadding: Float
    ): CursorPosition? {
        val entries = layoutMap.entries()
        if (entries.isEmpty()) return null

        var leftEntry: NodeLayoutEntry? = null
        var leftMaxEnd = Int.MIN_VALUE

        var rightEntry: NodeLayoutEntry? = null
        var rightMinStart = Int.MAX_VALUE

        for (entry in entries) {
            val range = entry.node.sourceRange ?: continue
            if (range.end <= cursorOffset) {
                if (range.end > leftMaxEnd) {
                    leftMaxEnd = range.end
                    leftEntry = entry
                }
            }
            if (range.start > cursorOffset) {
                if (range.start < rightMinStart) {
                    rightMinStart = range.start
                    rightEntry = entry
                }
            }
        }

        if (leftEntry != null) {
            return CursorPosition(
                x = leftEntry.relX + leftEntry.width + horizontalPadding,
                y = leftEntry.relY + verticalPadding,
                height = leftEntry.height
            )
        }

        if (rightEntry != null) {
            return CursorPosition(
                x = rightEntry.relX + horizontalPadding,
                y = rightEntry.relY + verticalPadding,
                height = rightEntry.height
            )
        }

        val fallback = entries.firstOrNull() ?: return null
        return CursorPosition(
            x = fallback.relX + horizontalPadding,
            y = fallback.relY + verticalPadding,
            height = fallback.height
        )
    }
}
