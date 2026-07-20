package com.hrm.markdown.parser.block

import com.hrm.markdown.parser.SourcePosition
import com.hrm.markdown.parser.SourceRange
import com.hrm.markdown.parser.LineRange
import com.hrm.markdown.parser.ast.*
import com.hrm.markdown.parser.block.starters.BlockStarterRegistry
import com.hrm.markdown.parser.block.starters.HeadingStarter
import com.hrm.markdown.parser.core.CharacterUtils
import com.hrm.markdown.parser.core.HtmlEntities
import com.hrm.markdown.parser.core.LineCursor
import com.hrm.markdown.parser.core.SourceText

/**
 * 高性能块级解析器。
 *
 * 实现 CommonMark 两遍块解析算法：
 * 1. 第一遍：逐行处理，构建块结构。
 *    每行测试是否延续已打开的块，或者开启新块。
 * 2. 第二遍：在叶子块（段落、标题）中解析行内内容。
 *
 * 此设计支持高效的增量解析：仅需重新解析脏块，
 * 且行内解析是延迟执行的。
 *
 * 块开启逻辑委托给 [BlockStarters]（默认）或 [BlockStarterRegistry]（插件模式），
 * 表格行解析委托给 [TableParser]。
 *
 * **注意**：`BlockParser` 只负责块结构 + 行内解析，不执行后处理。
 * 后处理（标题ID、HTML过滤、缩写替换等）由调用者通过
 * [com.hrm.markdown.parser.block.postprocessors.PostProcessorRegistry] 统一控制。
 *
 * ## 扩展方式
 * 传入自定义 [BlockStarterRegistry] 可以注册额外的块级语法：
 * ```kotlin
 * val starters = BlockStarterRegistry()
 * starters.registerAll(*defaultStarters)
 * starters.register(MyCustomBlockStarter())
 *
 * val parser = BlockParser(source, registry = starters) { doc -> InlineParser(doc) }
 * ```
 */
class BlockParser(
    private val source: SourceText,
    internal val registry: BlockStarterRegistry? = null,
    private val inlineParserFactory: ((Document) -> InlineParserInterface)? = null
) {
    private val document = Document()
    private val openBlocks = mutableListOf<OpenBlock>()
    private var currentLine = 0

    private val starters = BlockStarters(source)

    /**
     * 行内解析的接口，用于后续注入。
     */
    fun interface InlineParserInterface {
        fun parseInlines(content: String, parent: ContainerNode)
    }

    /**
     * 解析整个源文本并返回文档 AST。
     */
    fun parse(): Document {
        openBlocks.clear()
        openBlocks.add(OpenBlock(document, contentStartLine = 0))
        document.clearChildren()
        document.linkDefinitions.clear()
        document.footnoteDefinitions.clear()
        document.abbreviationDefinitions.clear()

        for (lineIdx in 0 until source.lineCount) {
            currentLine = lineIdx
            processLine(source.lineContent(lineIdx), lineIdx)
        }

        // 关闭所有已打开的块
        while (openBlocks.size > 1) {
            finalizeBlock(openBlocks.last())
            openBlocks.removeAt(openBlocks.size - 1)
        }
        finalizeBlock(openBlocks[0])

        // 设置文档范围
        document.lineRange = LineRange(0, source.lineCount)
        document.sourceRange = SourceRange(
            SourcePosition(0, 0, 0),
            SourcePosition(
                source.lineCount - 1,
                source.lineContent(source.lineCount - 1).length,
                source.length
            )
        )

        // 解析行内内容
        parseInlineContent(document)

        return document
    }

    /**
     * 解析 [startLine, endLine) 范围内的行并返回解析后的块。
     * 由增量解析器用于重新解析脏区域。
     */
    fun parseLines(startLine: Int, endLine: Int): List<Node> {
        openBlocks.clear()
        val tempDoc = Document()
        openBlocks.add(OpenBlock(tempDoc, contentStartLine = startLine))

        for (lineIdx in startLine until endLine) {
            currentLine = lineIdx
            processLine(source.lineContent(lineIdx), lineIdx)
        }

        while (openBlocks.size > 1) {
            finalizeBlock(openBlocks.last())
            openBlocks.removeAt(openBlocks.size - 1)
        }
        finalizeBlock(openBlocks[0])

        parseInlineContent(tempDoc)

        return tempDoc.children.toList()
    }

    private fun processLine(line: String, lineIdx: Int) {
        val cursor = LineCursor(line)

        // 第一阶段：尝试延续现有已打开的块
        var matchedDepth = 1 // Document 始终匹配
        var closedByFenceOrMath = false
        for (i in 1 until openBlocks.size) {
            val ob = openBlocks[i]
            if (continueBlock(ob, cursor)) {
                matchedDepth = i + 1
            } else {
                // 检查块是否被关闭围栏/定界符关闭
                // （围栏代码块、数学块或前置元数据）
                if (ob.node is FencedCodeBlock || ob.node is MathBlock || ob.node is CustomContainer
                    || ob.node is DirectiveBlock
                    || (ob.node is HtmlBlock && ob.htmlType in 1..5)) {
                    closedByFenceOrMath = true
                }
                break
            }
        }

        // lazy continuation: if the deepest open block has a paragraph
        // and unmatched blocks are only containers (block quotes, lists, list items),
        // treat as lazy continuation per commonmark spec
        if (!closedByFenceOrMath && matchedDepth < openBlocks.size && !cursor.restIsBlank()) {
            val deepest = openBlocks.last()
            if (deepest.paragraphContent != null) {
                var onlyContainers = true
                for (i in matchedDepth until openBlocks.size - 1) {
                    val n = openBlocks[i].node
                    if (n !is BlockQuote && n !is ListBlock && n !is ListItem) {
                        onlyContainers = false; break
                    }
                }
                if (onlyContainers && !wouldStartNonLazyBlock(cursor.rest())) {
                    deepest.paragraphContent!!.append('\n').append(cursor.rest())
                    deepest.lastLineIndex = lineIdx
                    return
                }
            }
        }

        // close unmatched blocks
        // track whether a list item with blank lines was closed
        var closedListItemHadBlank = false
        while (openBlocks.size > matchedDepth) {
            val closed = openBlocks.removeAt(openBlocks.size - 1)
            if (closed.node is ListItem && closed.blankLineCount > 0) {
                closedListItemHadBlank = true
            }
            finalizeBlock(closed)
        }

        // if a list item with blank lines was just closed and the remaining tip is
        // a list block, close the list too if the current line can't start a new
        // matching list item (per commonmark spec, insufficient indent after blank
        // line ends the list)
        if (closedListItemHadBlank && !closedByFenceOrMath && !cursor.restIsBlank()) {
            val tip = openBlocks.lastOrNull()
            if (tip != null && tip.node is ListBlock) {
                val snap = cursor.snapshot()
                val newBlock = if (registry != null) {
                    registry.tryStart(cursor, lineIdx, tip)
                } else {
                    starters.tryStartBlock(cursor, lineIdx, tip)
                }
                cursor.restore(snap)
                val startsNewItem = newBlock != null && newBlock.node is ListItem
                if (!startsNewItem) {
                    val closed = openBlocks.removeAt(openBlocks.size - 1)
                    finalizeBlock(closed)
                }
            }
        }

        if (closedByFenceOrMath) return

        // 第三阶段：尝试开启新块
        var lastMatched = openBlocks.last()
        var blockStarted = true
        while (blockStarted) {
            blockStarted = false

            // handle blank lines (but fenced code blocks preserve blank lines as content)
            if (cursor.restIsBlank()) {
                if (lastMatched.isFenced || lastMatched.node is IndentedCodeBlock) {
                    addLineToTip(lastMatched, cursor, lineIdx)
                } else {
                    handleBlankLine(lastMatched, lineIdx)
                    // set blankLineAfterContent on the deepest open ListItem
                    for (i in openBlocks.indices.reversed()) {
                        val ob = openBlocks[i]
                        if (ob.node is ListItem) {
                            val li = ob.node
                            if (li.children.isNotEmpty() || ob.paragraphContent != null) {
                                ob.blankLineAfterContent = true
                            }
                            break
                        }
                    }
                }
                lastMatched.lastLineIndex = lineIdx
                return
            }

            // 仅对容器块和段落尝试开启新块
            if (lastMatched.node !is ContainerNode && lastMatched.node !is Paragraph) break
            if (lastMatched.isFenced) break // 在围栏代码块内部，不开启新块

            // 按优先级顺序尝试各种块开启
            // 优先使用注册制（如果有），否则使用旧的 BlockStarters
            val preStartSnap = cursor.snapshot()
            val newBlock = if (registry != null) {
                registry.tryStart(cursor, lineIdx, lastMatched)
            } else {
                starters.tryStartBlock(cursor, lineIdx, lastMatched)
            }
            if (newBlock != null) {
                val canInterrupt = if (registry != null) {
                    registry.canInterruptParagraph(newBlock)
                } else {
                    starters.canInterruptParagraph(newBlock.node, cursor)
                }
                if (lastMatched.paragraphContent != null && !canInterrupt) {
                    cursor.restore(preStartSnap)
                    break
                }
                if (lastMatched.paragraphContent != null && newBlock.node is ListItem) {
                    val meta = newBlock.listItemMeta
                    if (meta != null) {
                        if (cursor.isAtEnd || cursor.restIsBlank()) {
                            cursor.restore(preStartSnap)
                            break
                        }
                        if (meta.ordered && meta.startNumber != 1) {
                            cursor.restore(preStartSnap)
                            break
                        }
                    }
                }

                // 如果当前是 ListBlock 且新块不是 ListItem，
                // 先关闭列表，使新块成为兄弟节点而非子节点
                if (lastMatched.node is ListBlock && newBlock.node !is ListItem) {
                    finalizeBlock(lastMatched)
                    openBlocks.removeAt(openBlocks.size - 1)
                    lastMatched = openBlocks.last()
                }

                // 关闭当前已打开的段落
                if (lastMatched.paragraphContent != null) {
                    val paragraphNode = lastMatched.node
                    val paragraphParent = paragraphNode.parent as? ContainerNode

                    if (newBlock.node is DefinitionDescription) {
                        // 将段落转换为定义术语，包装在定义列表中
                        val termContent = lastMatched.paragraphContent.toString().trim()
                        finalizeBlock(lastMatched)
                        openBlocks.removeAt(openBlocks.size - 1)

                        val para = paragraphNode as Paragraph

                        // 创建定义术语
                        val term = DefinitionTerm()
                        term.lineRange = para.lineRange

                        // 创建定义列表
                        val defList = DefinitionList()
                        defList.lineRange = LineRange(para.lineRange.startLine, lineIdx + 1)

                        // 将段落替换为定义列表
                        paragraphParent?.replaceChild(para, defList)
                        defList.appendChild(term)

                        // 将术语内容存入 term（后续行内解析会处理）
                        // 从段落的子节点复制到术语
                        for (child in para.children.toList()) {
                            term.appendChild(child)
                        }

                        val defListOb = OpenBlock(defList, contentStartLine = defList.lineRange.startLine, lastLineIndex = lineIdx)
                        openBlocks.add(defListOb)

                        // DefinitionDescription 将作为 defList 的子节点添加
                        defList.appendChild(newBlock.node)
                        openBlocks.add(newBlock)
                        lastMatched = newBlock
                        blockStarted = true
                        continue
                    } else if (newBlock.node is SetextHeading) {
                        val rawContent = lastMatched.paragraphContent.toString()
                        val afterLinkDefs = extractLinkReferenceDefs(rawContent)
                        if (afterLinkDefs.isBlank()) {
                            // All paragraph content was link ref defs; setext underline
                            // is not a heading, treat it as paragraph text
                            paragraphParent?.removeChild(paragraphNode)
                            openBlocks.removeAt(openBlocks.size - 1)
                            lastMatched = openBlocks.last()
                            cursor.restore(preStartSnap)
                            break
                        }
                        // Update content to only include the non-link-ref-def part
                        newBlock.contentLines.clear()
                        newBlock.contentLines.addAll(afterLinkDefs.split('\n'))
                        paragraphParent?.removeChild(paragraphNode)
                    } else if (newBlock.node is Table) {
                        paragraphParent?.removeChild(paragraphNode)
                    } else {
                        finalizeBlock(lastMatched)
                    }
                    openBlocks.removeAt(openBlocks.size - 1)
                }

                // ensure list block exists (after paragraph is closed)
                if (newBlock.node is ListItem) {
                    ensureListBlock(newBlock, lineIdx)
                }

                val parent = if (openBlocks.last().node is ContainerNode) {
                    openBlocks.last().node as ContainerNode
                } else {
                    var idx = openBlocks.size - 1
                    while (idx >= 0 && openBlocks[idx].node !is ContainerNode) idx--
                    if (idx >= 0) openBlocks[idx].node as ContainerNode else document
                }

                parent.appendChild(newBlock.node)
                openBlocks.add(newBlock)
                if (isSelfContainedBlock(newBlock)) {
                    finalizeBlock(newBlock)
                    openBlocks.removeAt(openBlocks.size - 1)
                    appendTrailingParagraph(parent, newBlock, lineIdx)
                    return
                }
                lastMatched = newBlock
                blockStarted = true
            }
        }

        // 第四阶段：将行添加到当前块
        addLineToTip(lastMatched, cursor, lineIdx)
    }

    private fun isSelfContainedBlock(block: OpenBlock): Boolean {
        return block.node is MathBlock && block.node.literal.isNotEmpty()
    }

    private fun appendTrailingParagraph(parent: ContainerNode, block: OpenBlock, lineIdx: Int) {
        val trailing = block.trailingContent?.trimStart()
        if (trailing.isNullOrBlank()) return

        val paragraph = Paragraph()
        paragraph.rawContent = trailing
        paragraph.lineRange = LineRange(lineIdx, lineIdx + 1)
        parent.appendChild(paragraph)
    }

    /**
     * 确保 openBlocks 中存在匹配的 ListBlock。
     * 如果不存在或不匹配，创建新的 ListBlock。
     * 这与原始 tryStartListItem 内部操作 openBlocks 的行为一致。
     */
    private fun ensureListBlock(newBlock: OpenBlock, lineIdx: Int) {
        val listItem = newBlock.node as ListItem
        val meta = newBlock.listItemMeta ?: return

        val parentOb = if (openBlocks.isNotEmpty()) openBlocks.last() else null
        val parentList = parentOb?.node as? ListBlock

        if (parentList == null || !listsMatch(parentList, meta.ordered, meta.bulletChar, meta.delimiter)) {
            // 如果存在不匹配的列表，先关闭它
            if (parentList != null && !listsMatch(parentList, meta.ordered, meta.bulletChar, meta.delimiter)) {
                finalizeBlock(parentOb)
                openBlocks.removeAt(openBlocks.size - 1)
            }

            // 创建新列表
            val list = ListBlock(
                ordered = meta.ordered,
                bulletChar = meta.bulletChar,
                startNumber = meta.startNumber,
                delimiter = meta.delimiter
            )
            list.lineRange = LineRange(lineIdx, lineIdx + 1)

            val container = findNearestContainer()
            container.appendChild(list)
            val listOb = OpenBlock(list, contentStartLine = lineIdx, lastLineIndex = lineIdx)
            openBlocks.add(listOb)
        }
    }

    /**
     * 检查指定的已打开块能否在当前行上继续。
     */
    private fun continueBlock(ob: OpenBlock, cursor: LineCursor): Boolean {
        return when (val node = ob.node) {
            is BlockQuote -> {
                val snap = cursor.snapshot()
                val spaces = cursor.advanceSpaces(3)
                if (!cursor.isAtEnd && cursor.peek() == '>') {
                    cursor.advance() // skip '>'
                    // skip optional space (or one column of a tab)
                    if (!cursor.isAtEnd && (cursor.peek() == ' ' || cursor.peek() == '\t')) {
                        cursor.advanceSpaces(1)
                    }
                    true
                } else {
                    cursor.restore(snap)
                    false
                }
            }
            is ListItem -> {
                if (cursor.restIsBlank()) {
                    ob.blankLineCount++
                    true
                } else {
                    // per commonmark spec, an empty list item (no content on marker line)
                    // followed by a blank line (actual blank source line, not just marker
                    // line with whitespace) cannot contain further blocks.
                    // blankLineCount > 1 means there was at least one actual blank source
                    // line beyond the initial marker line's blank content area.
                    if (ob.blankLineCount > 1 && node.children.isEmpty() && ob.paragraphContent == null) {
                        false
                    } else {
                        val snap = cursor.snapshot()
                        val indent = cursor.advanceSpaces(node.contentIndent)
                        if (indent >= node.contentIndent) {
                            if (ob.blankLineAfterContent) node.containsBlankLine = true
                            ob.blankLineAfterContent = false
                            true
                        } else {
                            cursor.restore(snap)
                            false
                        }
                    }
                }
            }
            is FencedCodeBlock -> {
                ob.isFenced = true
                // 检查关闭围栏
                val snap = cursor.snapshot()
                val indent = cursor.advanceSpaces(3)
                val rest = cursor.rest()
                if (isClosingFence(rest, ob.fenceChar, ob.fenceLength)) {
                    // 标记为已关闭 - 将被最终化
                    ob.isFenced = false
                    ob.lastLineIndex = currentLine
                    return false
                }
                cursor.restore(snap)
                // 移除最多 fenceIndent 个空格
                cursor.advanceSpaces(ob.fenceIndent)
                true
            }
            is IndentedCodeBlock -> {
                if (cursor.restIsBlank()) {
                    cursor.advanceSpaces(4)
                    true
                } else {
                    val snap = cursor.snapshot()
                    val indent = cursor.advanceSpaces(4)
                    if (indent >= 4) {
                        true
                    } else {
                        cursor.restore(snap)
                        false
                    }
                }
            }
            is HtmlBlock -> {
                val line = cursor.rest()
                val isEnd = checkHtmlBlockEnd(line, ob.htmlType)
                if (isEnd && ob.htmlType in 1..5) {
                    // add the closing line before finalizing
                    ob.contentLines.add(line)
                    ob.lastLineIndex = currentLine
                    return false
                }
                true
            }
            is ListBlock -> true // 列表块始终继续
            is Table -> {
                // 表格行必须包含 '|' 才能延续
                val rest = cursor.rest()
                if (rest.isBlank() || !rest.contains('|')) {
                    false
                } else {
                    true
                }
            }
            is Paragraph -> true // 段落通过懒延续单独处理
            is MathBlock -> {
                // 检查关闭 $$
                val rest = cursor.rest().trim()
                if (rest == "$$") {
                    ob.lastLineIndex = currentLine
                    return false
                }
                true
            }
            is FootnoteDefinition -> {
                // 如果行有 4+ 空格缩进则继续
                if (cursor.restIsBlank()) {
                    true
                } else {
                    val snap = cursor.snapshot()
                    val indent = cursor.advanceSpaces(4)
                    if (indent >= 4) {
                        true
                    } else {
                        cursor.restore(snap)
                        false
                    }
                }
            }
            is DefinitionList -> {
                if (cursor.restIsBlank()) {
                    ob.blankLineCount++
                    true
                } else {
                    // 检查当前行是否是定义描述的延续（缩进内容）或新的定义描述开头
                    val snap = cursor.snapshot()
                    val indent = cursor.advanceSpaces(3)
                    val isDefDescStart = !cursor.isAtEnd && cursor.peek() == ':' && run {
                        val snap2 = cursor.snapshot()
                        cursor.advance()
                        val hasSpace = !cursor.isAtEnd && (cursor.peek() == ' ' || cursor.peek() == '\t')
                        cursor.restore(snap2)
                        hasSpace
                    }
                    cursor.restore(snap)
                    if (isDefDescStart) {
                        true
                    } else if (ob.blankLineCount > 0) {
                        // 空行之后的非定义描述行 → 检查是否有缩进（属于子 DefinitionDescription 内容）
                        val snap2 = cursor.snapshot()
                        val indentCheck = cursor.advanceSpaces()
                        cursor.restore(snap2)
                        if (indentCheck >= 2) {
                            true // 缩进内容可能属于 DefinitionDescription 内的块级元素
                        } else {
                            false // 无缩进的非定义描述行 → 结束定义列表
                        }
                    } else {
                        true // 无空行时，非定义描述行可能是新术语（段落 → DefinitionTerm 转换）
                    }
                }
            }
            is DefinitionDescription -> {
                // 类似 ListItem：空行时递增空行计数并继续
                if (cursor.restIsBlank()) {
                    ob.blankLineCount++
                    true
                } else {
                    val snap = cursor.snapshot()
                    val indent = cursor.advanceSpaces()
                    if (indent >= 2) {
                        // 有缩进的内容继续属于该定义描述
                        true
                    } else if (ob.blankLineCount > 0) {
                        // 空行之后无缩进的内容 → 结束定义描述
                        cursor.restore(snap)
                        false
                    } else {
                        cursor.restore(snap)
                        false
                    }
                }
            }
            is CustomContainer -> {
                // 检查关闭围栏 `:::`（至少 3 个冒号，且长度 >= 开启围栏长度）
                val snap = cursor.snapshot()
                cursor.advanceSpaces(3)
                val rest = cursor.rest().trim()
                if (rest.length >= ob.fenceLength && rest.all { it == ':' }) {
                    ob.lastLineIndex = currentLine
                    cursor.restore(snap)
                    return false
                }
                cursor.restore(snap)
                true
            }
            is DirectiveBlock -> {
                // check for closing end tag: {% endtag %}
                val snap = cursor.snapshot()
                cursor.advanceSpaces(3)
                val rest = cursor.rest().trim()
                if (rest.startsWith("{%") && rest.endsWith("%}")) {
                    val inner = rest.removePrefix("{%").removeSuffix("%}").trim()
                    if (inner == "end${node.tagName}") {
                        ob.lastLineIndex = currentLine
                        ob.isFenced = false
                        cursor.restore(snap)
                        return false
                    }
                }
                cursor.restore(snap)
                true
            }
            is TabBlock -> {
                // TabBlock 续行：检查是否有新的 === "Title" 行或缩进内容
                val snap = cursor.snapshot()
                val rest = cursor.rest()
                if (rest.isBlank()) {
                    // 空行在 TabBlock 内是允许的
                    true
                } else if (com.hrm.markdown.parser.block.starters.TabBlockStarter.isTabItemLine(rest)) {
                    // 新的 tab item：添加到当前 TabBlock
                    val title = com.hrm.markdown.parser.block.starters.TabBlockStarter.extractTabTitle(rest)!!
                    val tabItem = TabItem(title = title)
                    tabItem.lineRange = LineRange(currentLine, currentLine + 1)
                    node.appendChild(tabItem)
                    cursor.advance(cursor.remaining) // consume the line
                    true
                } else {
                    val indent = cursor.advanceSpaces()
                    if (indent >= 4) {
                        // 缩进内容属于当前 tab item
                        true
                    } else {
                        // 非缩进内容结束 TabBlock
                        cursor.restore(snap)
                        false
                    }
                }
            }
            is TabItem -> {
                // TabItem 续行：类似 ListItem
                if (cursor.restIsBlank()) {
                    ob.blankLineCount++
                    true
                } else {
                    val snap = cursor.snapshot()
                    val rest = cursor.rest()
                    // 新的 === 行结束当前 TabItem
                    if (com.hrm.markdown.parser.block.starters.TabBlockStarter.isTabItemLine(rest)) {
                        cursor.restore(snap)
                        false
                    } else {
                        val indent = cursor.advanceSpaces(4)
                        if (indent >= 4) {
                            true
                        } else if (ob.blankLineCount > 0) {
                            cursor.restore(snap)
                            false
                        } else {
                            // lazy continuation
                            true
                        }
                    }
                }
            }
            // 单行块，永远不继续
            is Heading -> false
            is SetextHeading -> false
            is ThematicBreak -> false
            is PageBreak -> false
            is BlankLine -> false
            else -> true
        }
    }

    private fun isClosingFence(line: String, fenceChar: Char, openLength: Int): Boolean {
        val trimmed = line.trimEnd()
        if (trimmed.isEmpty()) return false
        if (trimmed[0] != fenceChar) return false
        if (!trimmed.all { it == fenceChar }) return false
        return trimmed.length >= openLength
    }

    private fun wouldStartNonLazyBlock(line: String): Boolean {
        // count leading spaces (0-3 allowed for most block starts; 4+ is indented code
        // which cannot interrupt a paragraph, so not a lazy-breaking block)
        var indent = 0
        var idx = 0
        while (idx < line.length && (line[idx] == ' ' || line[idx] == '\t')) {
            if (line[idx] == '\t') indent += (4 - indent % 4) else indent++
            idx++
        }
        val s = if (idx < line.length) line.substring(idx) else ""
        if (s.isEmpty()) return false
        // 4+ spaces of indent means indented code, which cannot interrupt a paragraph
        if (indent >= 4) return false
        if (s.startsWith('>')) return true
        if (s.startsWith('#') && s.length > 1 && (s[1] == ' ' || s[1] == '\t' || s[1] == '#')) return true
        val tbChar = s[0]
        if ((tbChar == '-' || tbChar == '*' || tbChar == '_') && s.length >= 3) {
            val stripped = s.replace(" ", "").replace("\t", "")
            if (stripped.length >= 3 && stripped.all { it == tbChar }) return true
        }
        if (s.startsWith("```") || s.startsWith("~~~")) return true
        if (s.startsWith('<') && s.length > 1) return true
        // list item markers: bullet (- * +) followed by space/tab/eol
        if ((s[0] == '-' || s[0] == '*' || s[0] == '+') &&
            (s.length == 1 || s[1] == ' ' || s[1] == '\t')) return true
        // ordered list markers: digits followed by . or ) then space/tab/eol
        if (s[0] in '0'..'9') {
            var i = 1
            while (i < s.length && i < 10 && s[i] in '0'..'9') i++
            if (i < s.length && (s[i] == '.' || s[i] == ')')) {
                if (i + 1 >= s.length || s[i + 1] == ' ' || s[i + 1] == '\t') return true
            }
        }
        return false
    }

    private fun checkHtmlBlockEnd(line: String, htmlType: Int): Boolean {
        val lower = line.lowercase()
        return when (htmlType) {
            1 -> lower.contains("</script>") || lower.contains("</pre>") ||
                    lower.contains("</style>") || lower.contains("</textarea>")
            2 -> line.contains("-->")
            3 -> line.contains("?>")
            4 -> line.contains(">")
            5 -> line.contains("]]>")
            6, 7 -> CharacterUtils.isBlank(line)
            else -> false
        }
    }

    private fun listsMatch(list: ListBlock, ordered: Boolean, bulletChar: Char, delimiter: Char): Boolean {
        if (list.ordered != ordered) return false
        if (ordered) return list.delimiter == delimiter
        return list.bulletChar == bulletChar
    }

    // ────── 行处理 ──────

    private fun addLineToTip(tip: OpenBlock, cursor: LineCursor, lineIdx: Int) {
        tip.lastLineIndex = lineIdx
        val lineContent = cursor.rest()

        when (val node = tip.node) {
            is FencedCodeBlock -> {
                // skip the opening fence line itself (content starts on the next line)
                if (lineIdx > tip.contentStartLine) {
                    tip.contentLines.add(lineContent)
                    syncOpenFencedCodeLiteral(node, tip)
                }
            }
            is IndentedCodeBlock -> {
                tip.contentLines.add(lineContent)
            }
            is HtmlBlock -> {
                // first line content is already added by HtmlBlockStarter; use cursor.rest() for subsequent lines
                if (lineIdx > tip.contentStartLine) {
                    tip.contentLines.add(cursor.rest())
                }
                if (checkHtmlBlockEnd(cursor.rest(), tip.htmlType)) {
                    finalizeBlock(tip)
                    openBlocks.removeAt(openBlocks.size - 1)
                }
            }
            is Heading -> {
                // ATX 标题内容已在解析时捕获
            }
            is MathBlock -> {
                val content = if (lineIdx == tip.contentStartLine) lineContent.trimStart() else lineContent
                if (lineIdx == tip.contentStartLine && content.isEmpty()) return
                tip.contentLines.add(content)
            }
            is FrontMatter -> {
                val trimmed = source.lineContent(lineIdx).trim()
                val endMarker = if (node.format == "yaml") "---" else "+++"
                if (trimmed == endMarker && lineIdx > tip.contentStartLine) {
                    // 不添加关闭标记
                    finalizeBlock(tip)
                    openBlocks.removeAt(openBlocks.size - 1)
                } else {
                    tip.contentLines.add(source.lineContent(lineIdx))
                }
            }
            is Table -> {
                // 跳过分隔行（创建表格时分隔行也会被传入 addLineToTip）
                val rawLine = source.lineContent(lineIdx)
                if (TableParser.parseTableDelimiterRow(rawLine.trim()) != null) return

                // 作为表格行解析
                val cells = TableParser.parseTableRow(rawLine)
                val body = node.children.filterIsInstance<TableBody>().firstOrNull() ?: return
                val row = TableRow()
                val alignments = node.columnAlignments
                // 列数以分隔行为准：多余截断，不足补空
                val colCount = alignments.size
                for (i in 0 until colCount) {
                    val cellContent = cells.getOrElse(i) { "" }
                    val cell = TableCell(alignment = alignments[i], isHeader = false)
                    cell.lineRange = LineRange(lineIdx, lineIdx + 1)
                    cell.rawContent = cellContent
                    row.appendChild(cell)
                }
                row.lineRange = LineRange(lineIdx, lineIdx + 1)
                body.appendChild(row)
                body.lineRange = LineRange(body.lineRange.startLine, lineIdx + 1)
                node.lineRange = LineRange(node.lineRange.startLine, lineIdx + 1)
            }
            is ListBlock, is ListItem, is BlockQuote, is Document,
            is DefinitionList, is DefinitionDescription, is FootnoteDefinition, is CustomContainer,
            is DirectiveBlock, is TabBlock, is TabItem -> {
                // 容器块：创建新段落或处理懒延续
                handleContainerLine(tip, cursor, lineIdx)
            }
            is SetextHeading -> {
                // Setext 内容已捕获
            }
            is Paragraph -> {
                // 正常情况下不应到达段落这里
                if (tip.paragraphContent == null) {
                    tip.paragraphContent = StringBuilder(lineContent)
                } else {
                    tip.paragraphContent!!.append('\n').append(lineContent)
                }
            }
            else -> {
                tip.contentLines.add(lineContent)
            }
        }
    }

    private fun handleContainerLine(tip: OpenBlock, cursor: LineCursor, lineIdx: Int) {
        val content = cursor.rest()

        // 检查该容器中是否有正在构建的段落
        if (openBlocks.last() === tip) {
            // 在当前块创建新段落
            val para = Paragraph()
            para.lineRange = LineRange(lineIdx, lineIdx + 1)
            val container = tip.node as ContainerNode
            container.appendChild(para)

            val paraOb = OpenBlock(para, contentStartLine = lineIdx, lastLineIndex = lineIdx)
            paraOb.paragraphContent = StringBuilder(content)
            openBlocks.add(paraOb)
        } else {
            // 添加到已有段落
            val last = openBlocks.last()
            if (last.paragraphContent != null) {
                last.paragraphContent!!.append('\n').append(content)
                last.lastLineIndex = lineIdx
            }
        }
    }

    private fun syncOpenFencedCodeLiteral(
        node: FencedCodeBlock,
        tip: OpenBlock,
    ) {
        if (!tip.isFenced) return
        if (tip.contentLines.isEmpty()) {
            node.literal = ""
            return
        }
        node.literal = tip.contentLines.joinToString("\n")
        if (!node.literal.endsWith('\n')) {
            node.literal += "\n"
        }
    }

    private fun handleBlankLine(tip: OpenBlock, lineIdx: Int) {
        when (tip.node) {
            is IndentedCodeBlock -> {
                tip.contentLines.add("")
            }
            is FencedCodeBlock -> {
                tip.contentLines.add("")
                syncOpenFencedCodeLiteral(tip.node, tip)
            }
            is Paragraph -> {
                // 空行结束段落
                finalizeBlock(tip)
                openBlocks.removeAt(openBlocks.size - 1)
            }
            is HtmlBlock -> {
                if (tip.htmlType == 6 || tip.htmlType == 7) {
                    tip.contentLines.add("")
                    finalizeBlock(tip)
                    openBlocks.removeAt(openBlocks.size - 1)
                } else {
                    tip.contentLines.add("")
                }
            }
            is Table -> {
                // 空行结束表格
                finalizeBlock(tip)
                openBlocks.removeAt(openBlocks.size - 1)
            }
            is ListItem -> {
                tip.blankLineCount++
            }
            is ListBlock -> {
                // 标记空行用于松散列表检测
            }
            is DefinitionDescription -> {
                // 空行不立即结束定义描述，由 continueBlock 通过空行计数和缩进判断
                tip.blankLineCount++
            }
            is DefinitionList -> {
                // 空行不立即结束定义列表，由子节点的终止来决定
            }
            is CustomContainer -> {
                // 空行不终止自定义容器，由关闭围栏 ::: 决定
            }
            is TabBlock -> {
                // 空行不终止 TabBlock
            }
            is TabItem -> {
                tip.blankLineCount++
            }
            is DirectiveBlock -> {
                // blank lines do not close directive blocks, only {% endtag %} does
            }
            else -> {
                // 其他块：空行
            }
        }
    }

    // ────── 块最终化 ──────

    private fun finalizeBlock(ob: OpenBlock) {
        val node = ob.node
        when (node) {
            is Paragraph -> {
                // strip leading spaces from each line per commonmark spec
                val content = ob.paragraphContent?.toString()
                    ?.lines()?.joinToString("\n") { it.trimStart() }
                    ?.trim() ?: ""
                if (content.isEmpty()) {
                    // 移除空段落
                    (node.parent as? ContainerNode)?.removeChild(node)
                    return
                }

                // 检查是否为 TOC 占位符（支持高级配置）
                val tocResult = tryParseTocPlaceholder(content)
                if (tocResult != null) {
                    tocResult.lineRange = LineRange(ob.contentStartLine, ob.lastLineIndex + 1)
                    val parent = node.parent as? ContainerNode
                    parent?.replaceChild(node, tocResult)
                    return
                }

                // 尝试从段落中提取链接引用定义
                var remaining = extractLinkReferenceDefs(content)
                // 尝试提取缩写定义
                remaining = extractAbbreviationDefs(remaining)
                if (remaining.isBlank()) {
                    (node.parent as? ContainerNode)?.removeChild(node)
                } else {
                    ob.contentLines.clear()
                    ob.contentLines.add(remaining)
                    node.rawContent = remaining
                }
                node.lineRange = LineRange(ob.contentStartLine, ob.lastLineIndex + 1)
            }
            is Heading -> {
                node.rawContent = ob.contentLines.joinToString("\n")
                node.lineRange = LineRange(ob.contentStartLine, ob.lastLineIndex + 1)
            }
            is SetextHeading -> {
                // store the parsed content (from paragraph), trimming leading/trailing whitespace
                // per line just like paragraph finalization does
                if (ob.contentLines.isNotEmpty()) {
                    node.rawContent = ob.contentLines.joinToString("\n") { it.trimStart().trimEnd() }
                }
                node.lineRange = LineRange(ob.contentStartLine, ob.lastLineIndex + 1)
            }
            is FencedCodeBlock -> {
                val lines = ob.contentLines
                if (lines.size == 1 && lines[0].isEmpty()) {
                    node.literal = ""
                } else if (lines.isNotEmpty()) {
                    if (ob.isFenced) {
                        // unclosed fence: use joinToString which handles trailing empty line from
                        // source text splitting naturally (no extra trailing newline)
                        node.literal = lines.joinToString("\n")
                        if (!node.literal.endsWith('\n') && lines.isNotEmpty()) {
                            node.literal += "\n"
                        }
                    } else {
                        // properly closed fence: each content line gets its own trailing newline
                        node.literal = lines.joinToString("") { it + "\n" }
                    }
                }
                node.lineRange = LineRange(ob.contentStartLine, ob.lastLineIndex + 1)
            }
            is IndentedCodeBlock -> {
                // 去除尾部空行
                while (ob.contentLines.isNotEmpty() && ob.contentLines.last().isBlank()) {
                    ob.contentLines.removeAt(ob.contentLines.size - 1)
                }
                node.literal = ob.contentLines.joinToString("\n") + "\n"
                node.lineRange = LineRange(ob.contentStartLine, ob.lastLineIndex + 1)
            }
            is HtmlBlock -> {
                node.literal = ob.contentLines.joinToString("\n")
                node.lineRange = LineRange(ob.contentStartLine, ob.lastLineIndex + 1)
            }
            is MathBlock -> {
                if (node.literal.isEmpty()) {
                    node.literal = ob.contentLines.joinToString("\n")
                }
                node.lineRange = LineRange(ob.contentStartLine, ob.lastLineIndex + 1)
            }
            is FrontMatter -> {
                // 检查是否已被正确关闭（addLineToTip 会在遇到闭合标记时 finalizeBlock）
                val closingMarker = if (node.format == "yaml") "---" else "+++"
                val lastContentLine = if (ob.lastLineIndex < source.lineCount) {
                    source.lineContent(ob.lastLineIndex).trim()
                } else ""
                val wasClosed = lastContentLine == closingMarker && ob.lastLineIndex > ob.contentStartLine

                if (!wasClosed) {
                    // 未关闭的 FrontMatter：降级为 ThematicBreak（如果格式为 yaml 即 ---）
                    val parent = node.parent as? ContainerNode ?: return
                    if (node.format == "yaml") {
                        val tb = ThematicBreak(char = '-')
                        tb.lineRange = LineRange(ob.contentStartLine, ob.contentStartLine + 1)
                        parent.replaceChild(node, tb)
                    } else {
                        // TOML 格式 +++ 不是合法的 ThematicBreak，直接移除
                        parent.removeChild(node)
                    }
                    // 剩余内容行会在后续作为独立块被重新处理（因为 FrontMatter 被移除后解析器会继续）
                    return
                }

                if (node.literal.isEmpty()) {
                    // 跳过开头标记行
                    node.literal = ob.contentLines.drop(1).joinToString("\n")
                }
                node.lineRange = LineRange(ob.contentStartLine, ob.lastLineIndex + 1)
            }
            is BlockQuote -> {
                // 检查是否为警告块
                checkAdmonition(node)
                node.lineRange = LineRange(ob.contentStartLine, ob.lastLineIndex + 1)
            }
            is ListBlock -> {
                // compute endLine from children for accurate lineRange
                val childEnd = node.children.maxOfOrNull { it.lineRange.endLine } ?: (ob.lastLineIndex + 1)
                val endLine = maxOf(ob.lastLineIndex + 1, childEnd)
                node.lineRange = LineRange(ob.contentStartLine, endLine)
                // 判断紧凑 vs 松散
                node.tight = isListTight(node)
            }
            is ListItem -> {
                // compute endLine from children for accurate lineRange
                val childEnd = node.children.maxOfOrNull { it.lineRange.endLine } ?: (ob.lastLineIndex + 1)
                val endLine = maxOf(ob.lastLineIndex + 1, childEnd)
                node.lineRange = LineRange(ob.contentStartLine, endLine)
            }
            is FootnoteDefinition -> {
                document.footnoteDefinitions[node.label] = node
                node.lineRange = LineRange(ob.contentStartLine, ob.lastLineIndex + 1)
            }
            is Table -> {
                node.lineRange = LineRange(ob.contentStartLine, ob.lastLineIndex + 1)
            }
            is CustomContainer -> {
                node.lineRange = LineRange(ob.contentStartLine, ob.lastLineIndex + 1)
            }
            is DirectiveBlock -> {
                node.lineRange = LineRange(ob.contentStartLine, ob.lastLineIndex + 1)
            }
            is TabBlock -> {
                node.lineRange = LineRange(ob.contentStartLine, ob.lastLineIndex + 1)
            }
            is TabItem -> {
                node.lineRange = LineRange(ob.contentStartLine, ob.lastLineIndex + 1)
            }
            is Document -> {
                // 无特殊处理
            }
            else -> {
                node.lineRange = LineRange(ob.contentStartLine, ob.lastLineIndex + 1)
            }
        }

        // Tree-sitter 风格：为节点计算内容哈希，用于增量复用
        if (node !is Document && node.lineRange.lineCount > 0) {
            node.contentHash = source.contentHash(node.lineRange)
        }
    }

    private fun checkAdmonition(blockQuote: BlockQuote) {
        val firstChild = blockQuote.children.firstOrNull()
        if (firstChild !is Paragraph) return

        // 从源文本重建段落首行内容
        val lr = firstChild.lineRange
        if (lr.lineCount <= 0) return
        val firstLine = source.lineContent(lr.startLine).trimStart()
        // 去除 > 前缀
        val content = firstLine.let {
            var s = it
            if (s.startsWith('>')) {
                s = s.drop(1)
                if (s.startsWith(' ')) s = s.drop(1)
            }
            s.trimStart()
        }

        // 匹配 [!TYPE] 或 [!TYPE] 后跟标题文本
        val match = ADMONITION_REGEX.find(content) ?: return
        val type = match.groupValues[1]
        val title = match.groupValues[2].trim()

        // 创建 Admonition 节点，替换 BlockQuote
        val admonition = Admonition(type = type, title = title)
        admonition.lineRange = blockQuote.lineRange
        admonition.sourceRange = blockQuote.sourceRange

        // 检查首段是否仅含 [!TYPE] 行
        val remainingContent = if (lr.lineCount > 1) {
            // 首段有多行，将第一行之后的内容保留为新段落
            val newPara = Paragraph()
            newPara.lineRange = LineRange(lr.startLine + 1, lr.endLine)
            admonition.appendChild(newPara)
        } else {
            // 首段只有 [!TYPE] 一行，移除它
        }

        // 将 blockQuote 的其余子节点（跳过首段）移到 admonition
        val remainingChildren = blockQuote.children.drop(1)
        for (child in remainingChildren) {
            admonition.appendChild(child)
        }

        // 在父节点中用 Admonition 替换 BlockQuote
        val parent = blockQuote.parent as? ContainerNode ?: return
        parent.replaceChild(blockQuote, admonition)
    }

    private fun isListTight(list: ListBlock): Boolean {
        val items = list.children.filterIsInstance<ListItem>()
        val lastContentLine = source.lineCount - 1
        fun isBlankContentLine(line: Int): Boolean {
            if (line < 0 || line >= source.lineCount) return false
            if (line == lastContentLine && source.lineContent(line).isEmpty()) return false
            return source.lineContent(line).isBlank()
        }
        for (item in items) {
            if (item.containsBlankLine) return false
            val children = item.children
            if (children.size >= 2) {
                // check for blank lines between consecutive direct children of this item
                for (c in 0 until children.size - 1) {
                    val gapStart = children[c].lineRange.endLine
                    val gapEnd = children[c + 1].lineRange.startLine
                    for (line in gapStart until gapEnd) {
                        if (isBlankContentLine(line)) return false
                    }
                }
            }
            if (children.isNotEmpty()) {
                // check for trailing blank lines within this item (after last child)
                val lastChildEnd = children.last().lineRange.endLine
                val itemEnd = item.lineRange.endLine
                for (line in lastChildEnd until itemEnd) {
                    if (isBlankContentLine(line)) return false
                }
            } else {
                // empty item: check for blank lines within its range
                for (line in item.lineRange.startLine + 1 until item.lineRange.endLine) {
                    if (isBlankContentLine(line)) return false
                }
            }
        }
        // check for blank lines between consecutive list items
        for (i in 0 until items.size - 1) {
            val gapStart = items[i].lineRange.endLine
            val gapEnd = items[i + 1].lineRange.startLine
            for (line in gapStart until gapEnd) {
                if (isBlankContentLine(line)) return false
            }
        }
        return true
    }

    /**
     * 从段落内容中提取链接引用定义。
     * 返回不属于链接引用定义的剩余内容。
     */
    private fun extractLinkReferenceDefs(content: String): String {
        var remaining = content
        while (true) {
            // 先尝试单行匹配
            val match = LINK_REF_DEF_REGEX.find(remaining)
            // 再尝试多行标题匹配（标题在下一行）
            val multiMatch = LINK_REF_DEF_MULTILINE_TITLE_REGEX.find(remaining)

            val effectiveMatch = when {
                match != null && match.range.first == 0 -> match
                multiMatch != null && multiMatch.range.first == 0 -> multiMatch
                else -> break
            }

            val label = CharacterUtils.normalizeLinkLabel(effectiveMatch.groupValues[1])
            val rawDest = effectiveMatch.groupValues[2].ifEmpty { effectiveMatch.groupValues[3] }.let {
                if (it.startsWith('<') && it.endsWith('>')) it.drop(1).dropLast(1) else it
            }
            val destination = CharacterUtils.percentEncodeUrl(
                HtmlEntities.replaceAll(resolveBackslashEscapes(rawDest))
            )
            val rawTitle = effectiveMatch.groupValues[4].ifEmpty {
                effectiveMatch.groupValues[5].ifEmpty {
                    effectiveMatch.groupValues[6].ifEmpty { null }
                }
            }
            val title = rawTitle?.let { HtmlEntities.replaceAll(resolveBackslashEscapes(it)) }

            // blank labels are not valid link reference definitions
            if (label.isEmpty()) break

            if (!document.linkDefinitions.containsKey(label)) {
                val def = LinkReferenceDefinition(
                    label = label,
                    destination = destination,
                    title = title
                )
                document.linkDefinitions[label] = def
                document.appendChild(def)
            }

            remaining = remaining.substring(effectiveMatch.range.last + 1).trimStart('\n')
        }
        return remaining
    }

    /**
     * 从段落内容中提取缩写定义 `*[abbr]: Full Text`。
     * 返回不属于缩写定义的剩余内容。
     */
    private fun extractAbbreviationDefs(content: String): String {
        var remaining = content
        while (true) {
            val match = ABBREVIATION_DEF_REGEX.find(remaining) ?: break
            if (match.range.first != 0) break

            val abbr = match.groupValues[1]
            val fullText = match.groupValues[2].trim()

            if (abbr.isNotEmpty() && !document.abbreviationDefinitions.containsKey(abbr)) {
                val def = AbbreviationDefinition(abbreviation = abbr, fullText = fullText)
                document.abbreviationDefinitions[abbr] = def
                document.appendChild(def)
            }

            remaining = remaining.substring(match.range.last + 1).trimStart('\n')
        }
        return remaining
    }

    private fun resolveBackslashEscapes(s: String): String {
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            if (s[i] == '\\' && i + 1 < s.length && CharacterUtils.isAsciiPunctuation(s[i + 1])) {
                sb.append(s[i + 1])
                i += 2
            } else {
                sb.append(s[i])
                i++
            }
        }
        return sb.toString()
    }

    private fun findNearestContainer(): ContainerNode {
        for (i in openBlocks.indices.reversed()) {
            val node = openBlocks[i].node
            if (node is ContainerNode) return node
        }
        return document
    }

    // ────── 行内解析 ──────

    private fun parseInlineContent(doc: Document) {
        val inlineParser = inlineParserFactory?.invoke(doc) ?: return
        setupLazyInlineParsing(doc, inlineParser)
    }

    /**
     * 为需要行内解析的块级节点设置延迟解析（Lazy Inline Parsing）。
     *
     * 不立即执行行内解析，而是将原始内容和解析器引用存储在节点上，
     * 当首次访问 `children` 时才触发解析。这优化了：
     * - IDE 语法高亮场景：只解析可见块
     * - 长文档预览：分页 + 按需解析行内内容
     */
    private fun setupLazyInlineParsing(node: Node, inlineParser: InlineParserInterface) {
        when (node) {
            is Paragraph -> {
                val content = reconstructContent(node)
                node.clearChildren()
                node.setLazyInlineContent(content) { c, parent ->
                    inlineParser.parseInlines(c, parent)
                }
            }
            is Heading -> {
                val content = reconstructContent(node)
                node.clearChildren()
                node.setLazyInlineContent(content) { c, parent ->
                    inlineParser.parseInlines(c, parent)
                }
            }
            is SetextHeading -> {
                val content = reconstructContent(node)
                node.clearChildren()
                node.setLazyInlineContent(content) { c, parent ->
                    inlineParser.parseInlines(c, parent)
                }
            }
            is TableCell -> {
                val content = reconstructContent(node)
                node.clearChildren()
                node.setLazyInlineContent(content) { c, parent ->
                    inlineParser.parseInlines(c, parent)
                }
            }
            is DefinitionTerm -> {
                val content = reconstructContent(node)
                node.clearChildren()
                node.setLazyInlineContent(content) { c, parent ->
                    inlineParser.parseInlines(c, parent)
                }
            }
            is ContainerNode -> {
                // 对容器节点，递归设置其直接子节点的 lazy 解析
                // 注意：这里直接访问内部子节点列表，不触发子节点的 lazy 解析
                for (child in node.children.toList()) {
                    setupLazyInlineParsing(child, inlineParser)
                }
            }
            else -> {}
        }
    }

    private fun reconstructContent(node: Node): String {
        // Try to reconstruct content from source lines
        val lr = node.lineRange
        if (lr.lineCount <= 0) return ""
        return when (node) {
            is Heading -> {
                if (node.rawContent != null) {
                    return node.rawContent!!.trim()
                }
                val line = source.lineContent(lr.startLine)
                val stripped = line.trimStart()
                val hashes = stripped.takeWhile { it == '#' }
                if (hashes.length in 1..6) {
                    var content = stripped.drop(hashes.length)
                    if (content.startsWith(' ') || content.startsWith('\t')) {
                        content = content.drop(1)
                    }
                    content = content.trimEnd()
                    val customId = HeadingStarter.extractCustomId(content)
                    if (customId != null) {
                        content = content.replace(HeadingStarter.CUSTOM_ID_STRIP_REGEX, "").trimEnd()
                    }
                    if (content.endsWith('#')) {
                        val t = content.trimEnd('#')
                        if (t.isEmpty() || t.endsWith(' ') || t.endsWith('\t')) {
                            content = t.trimEnd()
                        }
                    }
                    content.trimStart()
                } else {
                    line
                }
            }
            is SetextHeading -> {
                // use parsed content if available (strips block-level markers like list prefixes)
                if (node.rawContent != null) {
                    return node.rawContent!!
                }
                buildString {
                    for (i in lr.startLine until lr.endLine - 1) {
                        if (i > lr.startLine) append('\n')
                        append(source.lineContent(i).trimStart().trimEnd())
                    }
                }
            }
            is Paragraph -> {
                // 优先使用解析阶段已捕获的内容（已去除块级标记如列表标记）
                if (node.rawContent != null) {
                    return node.rawContent!!
                }
                buildString {
                    for (i in lr.startLine until lr.endLine) {
                        if (i > lr.startLine) append('\n')
                        append(source.lineContent(i).trimStart())
                    }
                }
            }
            is TableCell -> {
                // 使用解析时存储的单元格内容，而非从源文本行读取
                node.rawContent
            }
            is DefinitionTerm -> {
                buildString {
                    for (i in lr.startLine until lr.endLine) {
                        if (i > lr.startLine) append('\n')
                        append(source.lineContent(i).trimStart())
                    }
                }
            }
            else -> {
                buildString {
                    for (i in lr.startLine until lr.endLine) {
                        if (i > lr.startLine) append('\n')
                        append(source.lineContent(i))
                    }
                }
            }
        }
    }

    /**
     * 尝试将段落内容解析为 TOC 占位符（支持高级配置）。
     *
     * 支持格式：
     * - `[TOC]` 或 `[[toc]]`（基础）
     * - 后跟配置行：`:depth=2-4`、`:exclude=#ignore`、`:order=asc`
     *
     * @return 解析成功返回 [TocPlaceholder]，否则返回 null
     */
    private fun tryParseTocPlaceholder(content: String): TocPlaceholder? {
        val lines = content.lines()
        if (lines.isEmpty()) return null

        val firstLine = lines[0].trim()
        if (firstLine != "[TOC]" && firstLine != "[[toc]]") return null

        val toc = TocPlaceholder()

        // 解析后续配置行
        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            if (!line.startsWith(':')) {
                // 非配置行 → 不是纯 TOC 段落
                return null
            }
            parseTocConfigLine(toc, line)
        }

        return toc
    }

    /**
     * 解析单行 TOC 配置参数。
     */
    private fun parseTocConfigLine(toc: TocPlaceholder, line: String) {
        val content = line.removePrefix(":")
        val eqIndex = content.indexOf('=')
        if (eqIndex < 0) return

        val key = content.substring(0, eqIndex).trim().lowercase()
        val value = content.substring(eqIndex + 1).trim()

        when (key) {
            "depth" -> {
                // 支持 "2-4" 或 "3" 格式
                val dashIndex = value.indexOf('-')
                if (dashIndex >= 0) {
                    toc.minDepth = value.substring(0, dashIndex).trim().toIntOrNull() ?: 1
                    toc.maxDepth = value.substring(dashIndex + 1).trim().toIntOrNull() ?: 6
                } else {
                    val depth = value.toIntOrNull()
                    if (depth != null) {
                        toc.minDepth = 1
                        toc.maxDepth = depth
                    }
                }
            }
            "exclude" -> {
                // 支持逗号分隔的 ID 列表，ID 前可选 #
                toc.excludeIds = value.split(',').map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }
            }
            "order" -> {
                val normalized = value.lowercase()
                if (normalized == "asc" || normalized == "desc") {
                    toc.order = normalized
                }
            }
        }
    }

    companion object {
        // link label: no unescaped [ or ], but allow backslash escapes like \] \[ \\
        private val LINK_LABEL_PATTERN = "((?:[^\\[\\]\\\\]|\\\\.)+)"

        private val LINK_REF_DEF_REGEX = Regex(
            "^\\s{0,3}\\[${LINK_LABEL_PATTERN}\\]:\\s*(?:<([^>]*)>|([^\\s<>][^\\s]*))(?:\\s+(?:\"((?:[^\"]|\\\\\")*)\"|'([^']*)'|\\(([^)]*)\\)))?\\s*$",
            RegexOption.MULTILINE
        )

        /** 支持标题跨行的链接引用定义（标题可在下一行） */
        private val LINK_REF_DEF_MULTILINE_TITLE_REGEX = Regex(
            "^\\s{0,3}\\[${LINK_LABEL_PATTERN}\\]:\\s*(?:<([^>]*)>|([^\\s<>][^\\s]*))\\s*\\n\\s+(?:\"((?:[^\"]|\\\\\")*)\"|'([^']*)'|\\(([^)]*)\\))\\s*$",
            RegexOption.MULTILINE
        )

        /** 缩写定义：*[abbr]: Full Text */
        private val ABBREVIATION_DEF_REGEX = Regex(
            "^\\*\\[([^\\]]+)\\]:\\s*(.+)$",
            RegexOption.MULTILINE
        )

        /** Admonition 类型匹配：[!TYPE] 或 [!TYPE] title */
        private val ADMONITION_REGEX = Regex("^\\[!([A-Z]+)\\]\\s*(.*)")
    }
}
