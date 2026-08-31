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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.applyMathStyle
import com.hrm.latex.renderer.utils.DelimiterRenderer
import com.hrm.latex.renderer.utils.LayoutUtils
import com.hrm.latex.renderer.utils.MathConstants
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass

/**
 * 矩阵与数组测量器
 */
internal class MatrixMeasurer : NodeMeasurer {

    override val handledNodeTypes: Set<KClass<out LatexNode>> = setOf(
        LatexNode.Matrix::class,
        LatexNode.Array::class,
        LatexNode.Cases::class,
        LatexNode.Aligned::class,
        LatexNode.Split::class,
        LatexNode.Multline::class,
        LatexNode.Eqnarray::class,
        LatexNode.Subequations::class,
        LatexNode.Tabular::class
    )

    enum class ColumnAlignment { LEFT, CENTER, RIGHT }

    /**
     * 将 MatrixType 映射到对应的 Unicode 括号字符
     */
    private fun getDelimiterChar(type: LatexNode.Matrix.MatrixType, isLeft: Boolean): String {
        return when (type) {
            LatexNode.Matrix.MatrixType.PAREN -> if (isLeft) "(" else ")"
            LatexNode.Matrix.MatrixType.BRACKET -> if (isLeft) "[" else "]"
            LatexNode.Matrix.MatrixType.BRACE -> if (isLeft) "{" else "}"
            LatexNode.Matrix.MatrixType.VBAR -> "|"
            LatexNode.Matrix.MatrixType.DOUBLE_VBAR -> "‖"
            LatexNode.Matrix.MatrixType.PLAIN -> ""
        }
    }

    override fun measure(
        node: LatexNode,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureNode: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        return when (node) {
            is LatexNode.Matrix -> measureMatrix(node, context, measurer, density, measureNode)
            is LatexNode.Array -> {
                val (alignments, _) = parseAlignments(node.alignment)
                measureMatrixLike(
                    node.rows,
                    context,
                    measurer,
                    density,
                    measureNode,
                    alignments = alignments
                )
            }

            is LatexNode.Tabular -> measureTabular(node, context, measurer, density, measureNode)
            is LatexNode.Cases -> measureCases(node, context, measurer, density, measureNode)
            is LatexNode.Aligned, is LatexNode.Split -> {
                val rows =
                    if (node is LatexNode.Aligned) node.rows else (node as LatexNode.Split).rows
                val alignments =
                    List(10) { if (it % 2 == 0) ColumnAlignment.RIGHT else ColumnAlignment.LEFT }
                measureMatrixLike(
                    rows,
                    context,
                    measurer,
                    density,
                    measureNode,
                    alignments = alignments
                )
            }

            is LatexNode.Eqnarray -> {
                val alignments =
                    listOf(ColumnAlignment.RIGHT, ColumnAlignment.CENTER, ColumnAlignment.LEFT)
                measureMatrixLike(
                    node.rows,
                    context,
                    measurer,
                    density,
                    measureNode,
                    alignments = alignments
                )
            }

            is LatexNode.Multline -> measureMultline(node, context, density, measureNode)
            is LatexNode.Subequations -> measureGroup(node.content, context)
            else -> throw IllegalArgumentException("Unsupported node type: ${node::class.simpleName}")
        }
    }

    private fun measureMatrix(
        node: LatexNode.Matrix,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureNode: (LatexNode, RenderContext) -> NodeLayout
    ): NodeLayout {
        val alignments = node.alignment?.let { spec ->
            val parsed = parseAlignments(spec).first
            if (parsed.size == 1) {
                List(node.rows.maxOfOrNull { it.size } ?: 0) { parsed.single() }
            } else parsed
        }
        val contentLayout = measureMatrixLike(
            node.rows, context, measurer, density, measureNode, alignments = alignments
        )
        val bracketType = node.type
        if (bracketType == LatexNode.Matrix.MatrixType.PLAIN) return contentLayout

        val axisHeight = LayoutUtils.getAxisHeight(density, context, measurer)
        val delimiterHeight = delimiterTargetHeight(contentLayout, context, density, axisHeight)

        // 使用字体渲染括号（而不是 Path）
        val leftChar = getDelimiterChar(bracketType, isLeft = true)
        val rightChar = getDelimiterChar(bracketType, isLeft = false)

        val leftLayout = if (leftChar.isNotEmpty()) {
            centerOnAxis(
                DelimiterRenderer.measureScaled(leftChar, context, measurer, delimiterHeight, density),
                axisHeight
            )
        } else null

        val rightLayout = if (rightChar.isNotEmpty()) {
            centerOnAxis(
                DelimiterRenderer.measureScaled(rightChar, context, measurer, delimiterHeight, density),
                axisHeight
            )
        } else null

        val leftW = leftLayout?.width ?: 0f
        val rightW = rightLayout?.width ?: 0f

        val width = leftW + contentLayout.width + rightW
        val baseline = maxOf(
            contentLayout.baseline,
            leftLayout?.baseline ?: 0f,
            rightLayout?.baseline ?: 0f
        )
        val descent = maxOf(
            contentLayout.height - contentLayout.baseline,
            leftLayout?.let { it.height - it.baseline } ?: 0f,
            rightLayout?.let { it.height - it.baseline } ?: 0f
        )
        val height = baseline + descent

        return NodeLayout(width, height, baseline) { x, y ->
            var curX = x

            if (leftLayout != null) {
                leftLayout.draw(this, curX, y + baseline - leftLayout.baseline)
                curX += leftLayout.width
            }

            contentLayout.draw(this, curX, y + baseline - contentLayout.baseline)
            curX += contentLayout.width

            // 绘制右侧括号
            if (rightLayout != null) {
                rightLayout.draw(this, curX, y + baseline - rightLayout.baseline)
            }
        }
    }

    /**
     * 通用网格测量逻辑
     */
    private fun measureMatrixLike(
        rows: List<List<LatexNode>>,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureNode: (LatexNode, RenderContext) -> NodeLayout,
        alignments: List<ColumnAlignment>? = null,
        colSpacingRatio: Float = MathConstants.MATRIX_COLUMN_SPACING,
        rowSpacingRatio: Float = MathConstants.MATRIX_ROW_SPACING,
        isBaselineFirstRow: Boolean = false
    ): NodeLayout {
        val measuredRows = rows.map { row -> row.map { measureNode(it, context) } }
        val colCount = measuredRows.maxOfOrNull { it.size } ?: 0
        val rowCount = measuredRows.size
        if (rowCount == 0) return NodeLayout(0f, 0f, 0f) { _, _ -> }

        val colWidths = FloatArray(colCount)
        val rowHeights = FloatArray(rowCount)
        val rowBaselines = FloatArray(rowCount)

        for (c in 0 until colCount) {
            colWidths[c] = measuredRows.maxOfOrNull { if (c < it.size) it[c].width else 0f } ?: 0f
        }

        for (r in 0 until rowCount) {
            var maxAscent = 0f
            var maxDescent = 0f
            measuredRows[r].forEach { cell ->
                maxAscent = max(maxAscent, cell.baseline)
                maxDescent = max(maxDescent, cell.height - cell.baseline)
            }
            rowHeights[r] = maxAscent + maxDescent
            rowBaselines[r] = maxAscent
        }

        val colSpacing = with(density) { (context.fontSize * colSpacingRatio).toPx() }
        val rowSpacing = with(density) { (context.fontSize * rowSpacingRatio).toPx() }

        val totalWidth = colWidths.sum() + colSpacing * max(0, colCount - 1)
        val totalHeight = rowHeights.sum() + rowSpacing * max(0, rowCount - 1)

        val baseline = if (isBaselineFirstRow) {
            rowBaselines[0]
        } else {
            val axisHeight = LayoutUtils.getAxisHeight(density, context, measurer)
            totalHeight / 2 + axisHeight
        }

        return NodeLayout(totalWidth, totalHeight, baseline) { x, y ->
            var currentY = y
            for (r in 0 until rowCount) {
                var currentX = x
                val rowBaseY = currentY + rowBaselines[r]
                for (c in 0 until measuredRows[r].size) {
                    val cell = measuredRows[r][c]
                    val alignment = alignments?.getOrNull(c) ?: ColumnAlignment.CENTER
                    val cellX = when (alignment) {
                        ColumnAlignment.LEFT -> currentX
                        ColumnAlignment.CENTER -> currentX + (colWidths[c] - cell.width) / 2
                        ColumnAlignment.RIGHT -> currentX + colWidths[c] - cell.width
                    }
                    cell.draw(this, cellX, rowBaseY - cell.baseline)
                    currentX += colWidths[c] + colSpacing
                }
                currentY += rowHeights[r] + rowSpacing
            }
        }
    }

    private fun measureCases(
        node: LatexNode.Cases,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureNode: (LatexNode, RenderContext) -> NodeLayout
    ): NodeLayout {
        // dcases 使用 displaystyle 测量内容
        val contentContext = if (node.style == LatexNode.Cases.CasesStyle.DISPLAY) {
            context.applyMathStyle(LatexNode.MathStyle.MathStyleType.DISPLAY)
        } else {
            context
        }

        val rows = node.cases.map { listOf(it.second, LatexNode.Text(" if "), it.first) }
        val matrixLayout = measureMatrixLike(
            rows, contentContext, measurer, density, measureNode,
            alignments = listOf(ColumnAlignment.LEFT, ColumnAlignment.CENTER, ColumnAlignment.LEFT)
        )

        val axisHeight = LayoutUtils.getAxisHeight(density, context, measurer)
        val delimiterHeight = delimiterTargetHeight(matrixLayout, context, density, axisHeight)

        // rcases 使用右花括号，其他使用左花括号
        val isRight = node.style == LatexNode.Cases.CasesStyle.RIGHT
        val braceChar = getDelimiterChar(LatexNode.Matrix.MatrixType.BRACE, isLeft = !isRight)
        val braceLayout = centerOnAxis(
            DelimiterRenderer.measureScaled(braceChar, context, measurer, delimiterHeight, density),
            axisHeight
        )

        val width = braceLayout.width + matrixLayout.width
        val baseline = max(matrixLayout.baseline, braceLayout.baseline)
        val descent = max(
            matrixLayout.height - matrixLayout.baseline,
            braceLayout.height - braceLayout.baseline
        )
        val height = baseline + descent

        return NodeLayout(width, height, baseline) { x, y ->
            if (isRight) {
                matrixLayout.draw(this, x, y + baseline - matrixLayout.baseline)
                braceLayout.draw(this, x + matrixLayout.width, y + baseline - braceLayout.baseline)
            } else {
                braceLayout.draw(this, x, y + baseline - braceLayout.baseline)
                matrixLayout.draw(this, x + braceLayout.width, y + baseline - matrixLayout.baseline)
            }
        }
    }

    private fun delimiterTargetHeight(
        content: NodeLayout,
        context: RenderContext,
        density: Density,
        axisHeight: Float
    ): Float {
        val fontSizePx = with(density) { context.fontSize.toPx() }
        val maxDistance = max(
            content.baseline - axisHeight,
            content.height - content.baseline + axisHeight
        )
        return max(2f * maxDistance * 0.901f, 2f * maxDistance - fontSizePx * 0.5f)
    }

    private fun centerOnAxis(layout: NodeLayout, axisHeight: Float): NodeLayout = NodeLayout(
        layout.width,
        layout.height,
        layout.height / 2f + axisHeight,
        draw = layout.draw
    )

    /**
     * 解析 tabular/array 对齐字符串
     * @return Pair: 列对齐列表 + 竖线位置集合（列索引，0=最左侧，colCount=最右侧）
     */
    private fun parseAlignments(alignment: String): Pair<List<ColumnAlignment>, Set<Int>> {
        val aligns = mutableListOf<ColumnAlignment>()
        val vLines = mutableSetOf<Int>()
        for (ch in alignment) {
            when (ch) {
                'l' -> aligns.add(ColumnAlignment.LEFT)
                'c' -> aligns.add(ColumnAlignment.CENTER)
                'r' -> aligns.add(ColumnAlignment.RIGHT)
                '|' -> vLines.add(aligns.size)
            }
        }
        return aligns to vLines
    }

    private fun measureTabular(
        node: LatexNode.Tabular,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureNode: (LatexNode, RenderContext) -> NodeLayout
    ): NodeLayout {
        val (alignments, vLinePositions) = parseAlignments(node.alignment)

        // 分离数据行和 hline/cline 标记
        data class HLineInfo(val rowIndex: Int, val startCol: Int, val endCol: Int)

        val dataRows = mutableListOf<List<LatexNode>>()
        val hLines = mutableListOf<HLineInfo>()

        for (row in node.rows) {
            if (row.size == 1) {
                val singleNode = row[0]
                when (singleNode) {
                    is LatexNode.HLine -> {
                        hLines.add(HLineInfo(dataRows.size, 1, Int.MAX_VALUE))
                        continue
                    }
                    is LatexNode.CLine -> {
                        hLines.add(HLineInfo(dataRows.size, singleNode.startCol, singleNode.endCol))
                        continue
                    }
                    else -> {}
                }
            }
            dataRows.add(row)
        }

        if (dataRows.isEmpty()) return NodeLayout(0f, 0f, 0f) { _, _ -> }

        val colSpacing = with(density) { (context.fontSize * MathConstants.MATRIX_COLUMN_SPACING).toPx() }
        val rowSpacing = with(density) { (context.fontSize * MathConstants.MATRIX_ROW_SPACING).toPx() }
        val lineStrokeWidth = with(density) { 1f.dp.toPx() }
        val vLinePadding = with(density) { 2f.dp.toPx() }

        // 处理 multicolumn：先测量所有单元格
        // 每行可能有不同数量的逻辑单元格（因为 multicolumn 合并了多列）
        data class CellInfo(
            val layout: NodeLayout,
            val colSpan: Int,
            val alignment: ColumnAlignment,
            val mcVLines: Set<Int> = emptySet(), // multicolumn 自身的竖线位置（parseAlignments 返回的索引）
            val mcAlignCount: Int = 1 // multicolumn 对齐列数（用于判断右边界竖线）
        )

        val measuredRows = dataRows.map { row ->
            row.map { cell ->
                if (cell is LatexNode.Multicolumn) {
                    val contentLayout = measureNode(LatexNode.Group(cell.content), context)
                    val (cellAligns, cellVLines) = parseAlignments(cell.alignment)
                    val align = cellAligns.firstOrNull() ?: ColumnAlignment.CENTER
                    CellInfo(contentLayout, cell.columnCount, align, cellVLines, cellAligns.size)
                } else {
                    CellInfo(measureNode(cell, context), 1, ColumnAlignment.CENTER)
                }
            }
        }

        // 为每行构建「该行有效的竖线位置集合」
        // 对于普通行，使用全局 vLinePositions
        // 对于含 multicolumn 的行，multicolumn 跨越的内部列边界竖线被抑制，
        // 改用 multicolumn 自身 alignment 中声明的竖线
        val rowVLines = measuredRows.map { row ->
            val hasMulticolumn = row.any { it.colSpan > 1 }
            if (!hasMulticolumn) {
                vLinePositions
            } else {
                val effectiveVLines = mutableSetOf<Int>()
                var colIdx = 0
                for (cell in row) {
                    if (cell.colSpan > 1) {
                        // multicolumn: 左边界竖线
                        if (cell.mcVLines.contains(0)) effectiveVLines.add(colIdx)
                        // multicolumn: 右边界竖线
                        if (cell.mcVLines.contains(cell.mcAlignCount)) {
                            effectiveVLines.add(colIdx + cell.colSpan)
                        }
                    } else {
                        // 普通单元格：保留该列两侧的全局竖线
                        if (vLinePositions.contains(colIdx)) effectiveVLines.add(colIdx)
                        if (vLinePositions.contains(colIdx + 1)) effectiveVLines.add(colIdx + 1)
                    }
                    colIdx += cell.colSpan
                }
                effectiveVLines
            }
        }

        // 确定列数
        val colCount = max(
            alignments.size,
            measuredRows.maxOfOrNull { row ->
                row.sumOf { it.colSpan }
            } ?: 0
        )
        val rowCount = measuredRows.size

        // 计算列宽（仅基于 colSpan=1 的单元格）
        val colWidths = FloatArray(colCount)
        for (r in 0 until rowCount) {
            var colIdx = 0
            for (cell in measuredRows[r]) {
                if (cell.colSpan == 1 && colIdx < colCount) {
                    colWidths[colIdx] = max(colWidths[colIdx], cell.layout.width)
                }
                colIdx += cell.colSpan
            }
        }

        // 检查 multicolumn 单元格是否需要扩展列宽
        for (r in 0 until rowCount) {
            var colIdx = 0
            for (cell in measuredRows[r]) {
                if (cell.colSpan > 1) {
                    val endCol = min(colIdx + cell.colSpan, colCount)
                    val spanWidth = (colIdx until endCol).sumOf { colWidths[it].toDouble() }.toFloat() +
                        colSpacing * max(0, endCol - colIdx - 1)
                    if (cell.layout.width > spanWidth) {
                        val extra = cell.layout.width - spanWidth
                        val perCol = extra / (endCol - colIdx)
                        for (c in colIdx until endCol) {
                            colWidths[c] += perCol
                        }
                    }
                }
                colIdx += cell.colSpan
            }
        }

        // 计算行高和基线
        val rowHeights = FloatArray(rowCount)
        val rowBaselines = FloatArray(rowCount)
        for (r in 0 until rowCount) {
            var maxAscent = 0f
            var maxDescent = 0f
            measuredRows[r].forEach { cell ->
                maxAscent = max(maxAscent, cell.layout.baseline)
                maxDescent = max(maxDescent, cell.layout.height - cell.layout.baseline)
            }
            rowHeights[r] = maxAscent + maxDescent
            rowBaselines[r] = maxAscent
        }

        // 计算列的 x 偏移量（考虑竖线占用的额外空间）
        val colXOffsets = FloatArray(colCount)
        var xAcc = if (vLinePositions.contains(0)) vLinePadding + lineStrokeWidth + vLinePadding else 0f
        for (c in 0 until colCount) {
            colXOffsets[c] = xAcc
            xAcc += colWidths[c]
            if (c < colCount - 1) {
                xAcc += colSpacing
                if (vLinePositions.contains(c + 1)) {
                    xAcc += vLinePadding + lineStrokeWidth + vLinePadding
                }
            }
        }
        if (vLinePositions.contains(colCount)) {
            xAcc += vLinePadding + lineStrokeWidth + vLinePadding
        }
        val totalWidth = xAcc

        // 计算行的 y 偏移量（考虑 hline 间距）
        val rowYOffsets = FloatArray(rowCount)
        var yAcc = 0f
        // hline before row 0
        val hlinesAtRow0 = hLines.count { it.rowIndex == 0 }
        if (hlinesAtRow0 > 0) {
            yAcc += lineStrokeWidth + rowSpacing * 0.5f
        }
        for (r in 0 until rowCount) {
            rowYOffsets[r] = yAcc
            yAcc += rowHeights[r]
            if (r < rowCount - 1) {
                yAcc += rowSpacing
                val hlinesAtNextRow = hLines.count { it.rowIndex == r + 1 }
                if (hlinesAtNextRow > 0) {
                    yAcc += lineStrokeWidth + rowSpacing * 0.5f
                }
            }
        }
        // hline after last row
        val hlinesAtEnd = hLines.count { it.rowIndex == rowCount }
        if (hlinesAtEnd > 0) {
            yAcc += rowSpacing * 0.5f + lineStrokeWidth
        }
        val totalHeight = yAcc

        val baseline = if (rowCount > 0) {
            rowYOffsets[0] + rowBaselines[0]
        } else 0f

        return NodeLayout(totalWidth, totalHeight, baseline) { x, y ->
            // 绘制单元格内容
            for (r in 0 until rowCount) {
                val rowBaseY = y + rowYOffsets[r] + rowBaselines[r]
                var colIdx = 0
                for (cell in measuredRows[r]) {
                    if (colIdx >= colCount) break
                    val endCol = min(colIdx + cell.colSpan, colCount)
                    val cellLeft = colXOffsets[colIdx]
                    val cellRight = colXOffsets[endCol - 1] + colWidths[endCol - 1]
                    val cellWidth = cellRight - cellLeft
                    val align = if (cell.colSpan > 1) cell.alignment
                        else alignments.getOrNull(colIdx) ?: ColumnAlignment.CENTER
                    val cellX = when (align) {
                        ColumnAlignment.LEFT -> x + cellLeft
                        ColumnAlignment.CENTER -> x + cellLeft + (cellWidth - cell.layout.width) / 2
                        ColumnAlignment.RIGHT -> x + cellLeft + cellWidth - cell.layout.width
                    }
                    cell.layout.draw(this, cellX, rowBaseY - cell.layout.baseline)
                    colIdx += cell.colSpan
                }
            }

            // 辅助函数：根据竖线位置计算 x 坐标
            fun vLineX(vPos: Int): Float {
                return if (vPos == 0) {
                    x + vLinePadding + lineStrokeWidth / 2f
                } else if (vPos >= colCount) {
                    x + totalWidth - vLinePadding - lineStrokeWidth / 2f
                } else {
                    val prevRight = colXOffsets[vPos - 1] + colWidths[vPos - 1]
                    val nextLeft = colXOffsets[vPos]
                    x + (prevRight + nextLeft) / 2f
                }
            }

            // 逐行分段绘制竖线
            for (r in 0 until rowCount) {
                val segTop = y + rowYOffsets[r]
                val segBottom = segTop + rowHeights[r]
                // 向上延伸到与上方 hline 或表格顶部衔接
                val extTop = if (r == 0) y else {
                    val prevBottom = rowYOffsets[r - 1] + rowHeights[r - 1]
                    y + (prevBottom + rowYOffsets[r]) / 2f
                }
                // 向下延伸到与下方 hline 或表格底部衔接
                val extBottom = if (r == rowCount - 1) y + totalHeight else {
                    val nextTop = rowYOffsets[r + 1]
                    y + (rowYOffsets[r] + rowHeights[r] + nextTop) / 2f
                }
                for (vPos in rowVLines[r]) {
                    val vx = vLineX(vPos)
                    drawLine(
                        color = context.color,
                        start = Offset(vx, extTop),
                        end = Offset(vx, extBottom),
                        strokeWidth = lineStrokeWidth
                    )
                }
            }

            // 绘制水平线
            for (hl in hLines) {
                val hy = if (hl.rowIndex <= 0) {
                    y + lineStrokeWidth / 2f
                } else if (hl.rowIndex >= rowCount) {
                    y + totalHeight - lineStrokeWidth / 2f
                } else {
                    val prevBottom = rowYOffsets[hl.rowIndex - 1] + rowHeights[hl.rowIndex - 1]
                    val nextTop = rowYOffsets[hl.rowIndex]
                    y + (prevBottom + nextTop) / 2f
                }

                val isFullWidth = hl.startCol == 1 && hl.endCol == Int.MAX_VALUE
                if (isFullWidth) {
                    // \hline: 覆盖完整表宽（包括竖线 padding 区域）
                    drawLine(
                        color = context.color,
                        start = Offset(x, hy),
                        end = Offset(x + totalWidth, hy),
                        strokeWidth = lineStrokeWidth
                    )
                } else {
                    // \cline: 仅覆盖指定列范围
                    val startCol = max(hl.startCol - 1, 0)
                    val endCol = min(hl.endCol, colCount) - 1
                    if (startCol > endCol) continue
                    val hStartX = x + colXOffsets[startCol]
                    val hEndX = x + colXOffsets[endCol] + colWidths[endCol]
                    drawLine(
                        color = context.color,
                        start = Offset(hStartX, hy),
                        end = Offset(hEndX, hy),
                        strokeWidth = lineStrokeWidth
                    )
                }
            }
        }
    }

    private fun measureMultline(
        node: LatexNode.Multline,
        context: RenderContext,
        density: Density,
        measureNode: (LatexNode, RenderContext) -> NodeLayout
    ): NodeLayout {
        val lineLayouts = node.lines.map { measureNode(it, context) }
        if (lineLayouts.isEmpty()) return NodeLayout(0f, 0f, 0f) { _, _ -> }

        val maxWidth = lineLayouts.maxOf { it.width }
        val rowSpacing =
            with(density) { (context.fontSize * MathConstants.MULTLINE_ROW_SPACING).toPx() }
        val totalHeight = lineLayouts.sumOf { it.height.toDouble() }
            .toFloat() + rowSpacing * (lineLayouts.size - 1)
        val baseline = lineLayouts.first().baseline

        return NodeLayout(maxWidth, totalHeight, baseline) { x, y ->
            var currentY = y
            lineLayouts.forEachIndexed { i, layout ->
                val offsetX = when {
                    i == 0 -> 0f
                    i == lineLayouts.lastIndex -> maxWidth - layout.width
                    else -> (maxWidth - layout.width) / 2
                }
                layout.draw(this, x + offsetX, currentY)
                currentY += layout.height + rowSpacing
            }
        }
    }
}
