package com.hrm.markdown.parser.block.postprocessors

import com.hrm.markdown.parser.ast.*

/**
 * 后处理：将 `:::columns` / `:::column{width=...}` 自定义容器转换为
 * [ColumnsLayout] / [ColumnItem] 专用 AST 节点。
 *
 * 转换规则：
 * - `CustomContainer(type="columns")` → [ColumnsLayout]
 * - 其内部的 `CustomContainer(type="column")` → [ColumnItem]
 * - `column` 容器的 CSS 类中 `width=...` 提取为列宽属性
 *
 * 不匹配的 CustomContainer 保持原样。
 */
class ColumnsLayoutProcessor : PostProcessor {
    override val priority: Int = 350

    override fun process(document: Document) {
        processRecursive(document)
    }

    private fun processRecursive(node: Node) {
        if (node is ContainerNode) {
            val children = node.children.toList()
            for (child in children) {
                if (child is CustomContainer && child.type.equals("columns", ignoreCase = true)) {
                    val columnsLayout = convertToColumnsLayout(child)
                    node.replaceChild(child, columnsLayout)
                    // 递归处理 ColumnsLayout 内部的子节点
                    processRecursive(columnsLayout)
                } else {
                    processRecursive(child)
                }
            }
        }
    }

    private fun convertToColumnsLayout(container: CustomContainer): ColumnsLayout {
        val layout = ColumnsLayout()
        layout.lineRange = container.lineRange
        layout.sourceRange = container.sourceRange
        layout.contentHash = container.contentHash

        // 由于块解析器的嵌套机制，`:::column` 块会形成嵌套结构而非兄弟关系。
        // 例如：:::columns → column1 → (content, column2 → (content, column3 → ...))
        // 需要递归展开为扁平的列结构。
        val columns = mutableListOf<ColumnItem>()
        flattenColumns(container.children.toList(), columns)

        for (col in columns) {
            layout.appendChild(col)
        }

        return layout
    }

    /**
     * 递归展开嵌套的 column 容器为扁平列表。
     *
     * 块解析器将连续的 `:::column` 解析为嵌套结构：
     * ```
     * columns
     *   column1
     *     paragraph("左列")
     *     column2          ← 嵌套在 column1 内
     *       paragraph("右列")
     * ```
     * 本方法将其展开为：
     * ```
     * [ColumnItem("左列"), ColumnItem("右列")]
     * ```
     */
    private fun flattenColumns(children: List<Node>, result: MutableList<ColumnItem>) {
        // 收集当前层级的非 column 内容
        val contentBefore = mutableListOf<Node>()
        var foundColumn = false
        var currentColumnContainer: CustomContainer? = null

        for (child in children) {
            if (child is CustomContainer && child.type.equals("column", ignoreCase = true)) {
                if (!foundColumn && contentBefore.isNotEmpty()) {
                    // 在第一个 column 之前有内容 → 创建隐式列
                    val implicitCol = ColumnItem()
                    implicitCol.lineRange = contentBefore.first().lineRange
                    for (c in contentBefore) implicitCol.appendChild(c)
                    result.add(implicitCol)
                    contentBefore.clear()
                }
                foundColumn = true

                // 处理之前的 column 容器（如果有的话）
                if (currentColumnContainer != null) {
                    // 这不应该在同一层级发生，但以防万一
                }

                // 当前 column 容器的直接内容（非 column 子节点）作为该列内容
                val colItem = ColumnItem(width = extractWidth(child))
                colItem.lineRange = child.lineRange
                colItem.sourceRange = child.sourceRange

                val colChildren = child.children.toList()
                val directContent = mutableListOf<Node>()
                var nestedColumnFound = false

                for (colChild in colChildren) {
                    if (colChild is CustomContainer && colChild.type.equals("column", ignoreCase = true)) {
                        nestedColumnFound = true
                        // 当前列的内容到此为止
                        for (dc in directContent) colItem.appendChild(dc)
                        result.add(colItem)
                        // 递归展开剩余的嵌套 column
                        flattenColumns(colChildren.dropWhile { it !== colChild }, result)
                        return
                    } else {
                        directContent.add(colChild)
                    }
                }

                if (!nestedColumnFound) {
                    for (dc in directContent) colItem.appendChild(dc)
                    result.add(colItem)
                }
            } else {
                contentBefore.add(child)
            }
        }

        // 如果没找到任何 column，把所有内容放到隐式列中
        if (!foundColumn && contentBefore.isNotEmpty()) {
            val implicitCol = ColumnItem()
            implicitCol.lineRange = contentBefore.first().lineRange
            for (c in contentBefore) implicitCol.appendChild(c)
            result.add(implicitCol)
        }
    }

    /**
     * 从 CustomContainer 的属性中提取列宽。
     *
     * 支持以下格式：
     * - `:::column{width=50%}` — 从 CSS 类/属性中提取
     * - `:::column{.w-50}` — 预定义的宽度类
     */
    private fun extractWidth(container: CustomContainer): String {
        // 优先从 title 中解析 width=... （因为 CustomContainerStarter 把未匹配的属性放在 title）
        val titleWidthMatch = WIDTH_FROM_TITLE_REGEX.find(container.title)
        if (titleWidthMatch != null) {
            return titleWidthMatch.groupValues[1]
        }

        // 从 CSS 类中查找宽度类
        for (cls in container.cssClasses) {
            val widthMatch = WIDTH_CLASS_REGEX.find(cls)
            if (widthMatch != null) {
                return widthMatch.groupValues[1] + "%"
            }
        }

        return ""
    }

    companion object {
        /** 匹配 width=50% 或 width="50%" 格式 */
        private val WIDTH_FROM_TITLE_REGEX = Regex("""width\s*=\s*"?([^"\s}]+)"?""", RegexOption.IGNORE_CASE)

        /** 匹配预定义宽度类如 w-50 → 50% */
        private val WIDTH_CLASS_REGEX = Regex("""^w-(\d+)$""")
    }
}
