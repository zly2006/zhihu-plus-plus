/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.editor

import io.ktor.http.encodeURLParameter
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.LeafASTNode
import org.intellij.markdown.ast.acceptChildren
import org.intellij.markdown.ast.getParentOfType
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.html.GeneratingProvider
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.html.SimpleTagProvider
import org.intellij.markdown.parser.LinkMap
import org.intellij.markdown.parser.MarkdownParser

// 透明节点渲染器：不输出任何标签，只递归渲染其子节点。
private class TransparentNodeProvider : GeneratingProvider {
    override fun processNode(visitor: HtmlGenerator.HtmlGeneratingVisitor, text: String, node: ASTNode) {
        node.acceptChildren(visitor)
    }
}

// 用于把内容包成 <p><strong>...</strong></p>
// 因为知乎没有三级以上标题，所以这些标题需要降级为加粗段落。
private class StrongParagraphProvider : GeneratingProvider {
    override fun processNode(visitor: HtmlGenerator.HtmlGeneratingVisitor, text: String, node: ASTNode) {
        visitor.consumeTagOpen(node, "p")
        visitor.consumeTagOpen(node, "strong")
        node.acceptChildren(visitor)
        visitor.consumeTagClose("strong")
        visitor.consumeTagClose("p")
    }
}

// 把代码块输出成知乎偏好的 <pre lang="lang">...</pre>
// EXAMPLE:
// ```python
// print("hello")
// ```
// <pre lang="python">
// print("hello")
// </pre>
private class PreCodeFenceProvider : GeneratingProvider {
    override fun processNode(visitor: HtmlGenerator.HtmlGeneratingVisitor, text: String, node: ASTNode) {
        val indentBefore = node.getTextInNode(text).commonPrefixWith(" ".repeat(10)).length
        var childrenToConsider = node.children
        if (childrenToConsider.isNotEmpty() && childrenToConsider.last().type == MarkdownTokenTypes.CODE_FENCE_END) {
            childrenToConsider = childrenToConsider.subList(0, childrenToConsider.size - 1)
        }

        var state = 0
        var lang: String? = null

        for (child in childrenToConsider) {
            if (state == 0 && child.type == MarkdownTokenTypes.FENCE_LANG) {
                lang =
                    HtmlGenerator
                        .leafText(text, child)
                        .toString()
                        .trim()
                        .split(' ')[0]
                        .ifBlank { null }
            }
            if (state == 0 && child.type == MarkdownTokenTypes.EOL) {
                visitor.consumeTagOpen(node, "pre", lang?.let { "lang=\"$it\"" })
                state = 1
                continue
            }
            if (state == 1 && child.type in listOf(MarkdownTokenTypes.CODE_FENCE_CONTENT, MarkdownTokenTypes.EOL)) {
                visitor.consumeHtml(HtmlGenerator.trimIndents(HtmlGenerator.leafText(text, child, false), indentBefore))
            }
        }

        if (state == 0) {
            visitor.consumeTagOpen(node, "pre", lang?.let { "lang=\"$it\"" })
        }
        visitor.consumeTagClose("pre")
    }
}

// 从数学节点的原始文本中提取 TeX 内容：去掉 $ 或 $$ 包裹并 trim。
private fun extractMathTex(text: String, node: ASTNode, delimiterSize: Int): String {
    val raw = node.getTextInNode(text).toString()
    if (raw.length < delimiterSize * 2) return ""
    return raw.drop(delimiterSize).dropLast(delimiterSize).trim()
}

// 将 TeX 进行 URL 编码，用于拼接知乎公式图片链接的 tex 参数。
private fun encodeZhihuEquationTex(tex: String): String =
    tex.encodeURLParameter(spaceToPlus = false)

// HTML attribute 转义
private fun escapeHtmlAttribute(value: String): String =
    value
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

// 行内数学公式渲染器
// EXAMPLE:
// $1/2$
// <img eeimg="1" src="//www.zhihu.com/equation?tex=1%2F2" alt="1/2" />
private class ZhihuInlineMathProvider : GeneratingProvider {
    override fun processNode(visitor: HtmlGenerator.HtmlGeneratingVisitor, text: String, node: ASTNode) {
        val tex = extractMathTex(text, node, delimiterSize = 1)
        val alt = escapeHtmlAttribute(tex.replace(Regex("[\n\r]+"), " "))
        val encoded = encodeZhihuEquationTex(tex)
        visitor.consumeTagOpen(
            node,
            "img",
            "eeimg=\"1\"",
            "src=\"//www.zhihu.com/equation?tex=$encoded\"",
            "alt=\"$alt\"",
            autoClose = true,
        )
    }
}

// 行间数学公式渲染器
// EXAMPLE:
// $$
// a^2+b^2=c^2
// $$
// <img eeimg="2" src="//www.zhihu.com/equation?tex=a%5E2%2Bb%5E2%3Dc%5E2" alt="a^2+b^2=c^2" />
private class ZhihuBlockMathProvider : GeneratingProvider {
    override fun processNode(visitor: HtmlGenerator.HtmlGeneratingVisitor, text: String, node: ASTNode) {
        val tex = extractMathTex(text, node, delimiterSize = 2)
        val alt = escapeHtmlAttribute(tex.replace(Regex("[\n\r]+"), " "))
        val encoded = encodeZhihuEquationTex(tex)
        visitor.consumeTagOpen(node, "p")
        visitor.consumeTagOpen(
            node,
            "img",
            "eeimg=\"2\"",
            "src=\"//www.zhihu.com/equation?tex=$encoded\"",
            "alt=\"$alt\"",
            autoClose = true,
        )
        visitor.consumeTagClose("p")
    }
}

// 表格渲染器：输出知乎需要的 table 属性，并且把 HEADER/ROW 都放在同一个 <tbody> 下。
// EXAMPLE:
// <table data-draft-node="block" data-draft-type="table" data-size="normal"><tbody>
// <tr><th>水果</th><th>英文</th></tr>
// <tr><td>苹果</td><td>apple</td></tr>
// </tbody></table>
private class ZhihuTableProvider : GeneratingProvider {
    override fun processNode(visitor: HtmlGenerator.HtmlGeneratingVisitor, text: String, node: ASTNode) {
        visitor.consumeTagOpen(
            node,
            "table",
            "data-draft-node=\"block\"",
            "data-draft-type=\"table\"",
            "data-size=\"normal\"",
        )
        visitor.consumeTagOpen(node, "tbody")

        for (child in node.children) {
            when (child.type) {
                GFMElementTypes.HEADER -> renderHeaderRow(visitor, text, child)
                GFMElementTypes.ROW -> visitor.visitNode(child)
            }
        }

        visitor.consumeTagClose("tbody")
        visitor.consumeTagClose("table")
    }

    private fun renderHeaderRow(visitor: HtmlGenerator.HtmlGeneratingVisitor, text: String, headerNode: ASTNode) {
        val cells = collectNodesOfType(headerNode, GFMTokenTypes.CELL)
        if (cells.isEmpty()) return
        visitor.consumeTagOpen(headerNode, "tr")
        for (cell in cells) {
            renderCell(visitor, text, cell, "th")
        }
        visitor.consumeTagClose("tr")
    }

    private fun renderCell(visitor: HtmlGenerator.HtmlGeneratingVisitor, text: String, cellNode: ASTNode, cellTag: String) {
        visitor.consumeTagOpen(cellNode, cellTag)

        if (cellNode.children.isEmpty()) {
            visitor.consumeHtml(HtmlGenerator.leafText(text, cellNode).toString().trim())
        } else {
            val renderChildren = cellNode.children.filter { it.type != GFMTokenTypes.TABLE_SEPARATOR }
            var start = 0
            var end = renderChildren.size - 1
            while (start <= end && isBlankLeaf(text, renderChildren[start])) start += 1
            while (start <= end && isBlankLeaf(text, renderChildren[end])) end -= 1
            for (i in start..end) {
                val cellChild = renderChildren[i]
                if (cellChild is LeafASTNode) visitor.visitLeaf(cellChild) else visitor.visitNode(cellChild)
            }
        }

        visitor.consumeTagClose(cellTag)
    }

    private fun isBlankLeaf(text: String, node: ASTNode): Boolean {
        if (node !is LeafASTNode) return false
        return HtmlGenerator.leafText(text, node).toString().isBlank()
    }

    private fun collectNodesOfType(root: ASTNode, type: IElementType): List<ASTNode> {
        if (root.children.isEmpty()) return emptyList()
        val result = ArrayList<ASTNode>()
        val stack = ArrayDeque<ASTNode>()
        stack.add(root)
        while (stack.isNotEmpty()) {
            val n = stack.removeLast()
            if (n.type == type) result.add(n)
            for (i in n.children.size - 1 downTo 0) stack.add(n.children[i])
        }
        return result
    }
}

// HEADER 容器渲染器：TABLE 渲染器会自行处理表头行，这里保持透明以避免输出多余文本。
private class ZhihuTableHeaderProvider : GeneratingProvider {
    override fun processNode(visitor: HtmlGenerator.HtmlGeneratingVisitor, text: String, node: ASTNode) {
    }
}

// ROW 渲染器：生成 <tr>，若该 ROW 在 HEADER 下则输出 <th> 单元格，否则输出 <td>。
private class ZhihuTableRowProvider : GeneratingProvider {
    override fun processNode(visitor: HtmlGenerator.HtmlGeneratingVisitor, text: String, node: ASTNode) {
        val isHeaderRow = node.getParentOfType(GFMElementTypes.HEADER) != null
        val cellTag = if (isHeaderRow) "th" else "td"

        visitor.consumeTagOpen(node, "tr")

        for (child in node.children) {
            if (child.type != GFMTokenTypes.CELL) continue

            visitor.consumeTagOpen(child, cellTag)

            if (child.children.isEmpty()) {
                visitor.consumeHtml(HtmlGenerator.leafText(text, child).toString().trim())
            } else {
                val renderChildren = child.children.filter { it.type != GFMTokenTypes.TABLE_SEPARATOR }
                var start = 0
                var end = renderChildren.size - 1
                while (start <= end && isBlankLeaf(text, renderChildren[start])) start += 1
                while (start <= end && isBlankLeaf(text, renderChildren[end])) end -= 1
                for (i in start..end) {
                    val cellChild = renderChildren[i]
                    if (cellChild is LeafASTNode) visitor.visitLeaf(cellChild) else visitor.visitNode(cellChild)
                }
            }

            visitor.consumeTagClose(cellTag)
        }

        visitor.consumeTagClose("tr")
    }

    private fun isBlankLeaf(text: String, node: ASTNode): Boolean {
        if (node !is LeafASTNode) return false
        return HtmlGenerator.leafText(text, node).toString().isBlank()
    }
}

// 将 Markdown AST 节点类型映射到 1~6 层级数字
// 非标题节点返回 null
private fun headingLevelOf(type: IElementType): Int? =
    when (type) {
        MarkdownElementTypes.ATX_1 -> 1
        MarkdownElementTypes.ATX_2 -> 2
        MarkdownElementTypes.ATX_3 -> 3
        MarkdownElementTypes.ATX_4 -> 4
        MarkdownElementTypes.ATX_5 -> 5
        MarkdownElementTypes.ATX_6 -> 6
        MarkdownElementTypes.SETEXT_1 -> 1
        MarkdownElementTypes.SETEXT_2 -> 2
        else -> null
    }

// 扫描整棵 AST，收集本文实际出现过的标题层级集合
private fun collectHeadingLevels(root: ASTNode): Set<Int> {
    val result = LinkedHashSet<Int>()
    val stack = ArrayDeque<ASTNode>()
    stack.add(root)
    while (stack.isNotEmpty()) {
        val n = stack.removeLast()
        val level = headingLevelOf(n.type)
        if (level != null) result.add(level)
        for (i in n.children.size - 1 downTo 0) stack.add(n.children[i])
    }
    return result
}

// 按标题层级做归一化：
// - 最高级统一输出为 <h2>
// - 次高级统一输出为 <h3>
// - 更低级标题统一输出为 <p><strong>...</strong></p>
//
// 说明：
// - 之所以放在 parse 之后，是因为需要先拿到整篇文章的 AST 才能知道出现过哪些层级
// - 这里通过覆写 providers 来改变标题节点的渲染方式，不影响其他节点渲染
private fun applyHeadingNormalization(
    providers: MutableMap<IElementType, GeneratingProvider>,
    root: ASTNode,
) {
    val usedLevels = collectHeadingLevels(root).toList().sorted()
    if (usedLevels.isEmpty()) return

    val top = usedLevels.first()
    val second = usedLevels.drop(1).firstOrNull()

    fun providerForLevel(level: Int): GeneratingProvider =
        when {
            level == top -> SimpleTagProvider("h2")
            second != null && level == second -> SimpleTagProvider("h3")
            else -> StrongParagraphProvider()
        }

    // 对所有可能的标题类型统一覆写 provider。没有出现在本文中的层级即使被覆写也不会生效。
    val headingTypesAndLevels: List<Pair<IElementType, Int>> =
        listOf(
            MarkdownElementTypes.ATX_1 to 1,
            MarkdownElementTypes.ATX_2 to 2,
            MarkdownElementTypes.ATX_3 to 3,
            MarkdownElementTypes.ATX_4 to 4,
            MarkdownElementTypes.ATX_5 to 5,
            MarkdownElementTypes.ATX_6 to 6,
            MarkdownElementTypes.SETEXT_1 to 1,
            MarkdownElementTypes.SETEXT_2 to 2,
        )

    for ((type, level) in headingTypesAndLevels) {
        providers[type] = providerForLevel(level)
    }
}

@Suppress("UNUSED_PARAMETER")
suspend fun compileMdToZhihuHtml(
    markdown: String,
    publisher: ZhihuAnswerPublisher,
): String {
    val flavour: MarkdownFlavourDescriptor =
        GFMFlavourDescriptor(
            useSafeLinks = true,
            absolutizeAnchorLinks = false,
            makeHttpsAutoLinks = false,
        )
    val root = MarkdownParser(flavour).buildMarkdownTreeFromString(markdown)
    val providers =
        flavour
            .createHtmlGeneratingProviders(LinkMap.buildLinkMap(root, markdown), null)
            .toMutableMap()

    // 默认 CommonMark 会输出 <body>...</body>，这里改成只输出子节点 HTML。
    providers[MarkdownElementTypes.MARKDOWN_FILE] = TransparentNodeProvider()

    // 代码块 provider 覆写
    providers[MarkdownElementTypes.CODE_FENCE] = PreCodeFenceProvider()

    // 行内数学公式和行间数学公式 provider 覆写
    providers[GFMElementTypes.INLINE_MATH] = ZhihuInlineMathProvider()
    providers[GFMElementTypes.BLOCK_MATH] = ZhihuBlockMathProvider()

    // 表格 provider 覆写
    providers[GFMElementTypes.TABLE] = ZhihuTableProvider()
    providers[GFMElementTypes.HEADER] = ZhihuTableHeaderProvider()
    providers[GFMElementTypes.ROW] = ZhihuTableRowProvider()

    applyHeadingNormalization(providers, root)
    val html = HtmlGenerator(markdown, root, providers).generateHtml()

    return html.trimEnd()
}
