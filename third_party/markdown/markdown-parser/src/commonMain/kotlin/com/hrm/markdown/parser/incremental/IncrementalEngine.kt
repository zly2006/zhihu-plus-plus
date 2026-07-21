package com.hrm.markdown.parser.incremental

import com.hrm.markdown.parser.LineRange
import com.hrm.markdown.parser.SourcePosition
import com.hrm.markdown.parser.SourceRange
import com.hrm.markdown.parser.ast.*
import com.hrm.markdown.parser.block.BlockParser
import com.hrm.markdown.parser.block.postprocessors.PostProcessorRegistry
import com.hrm.markdown.parser.block.starters.BlockStarterRegistry
import com.hrm.markdown.parser.core.SourceText
import com.hrm.markdown.parser.flavour.ExtendedFlavour
import com.hrm.markdown.parser.flavour.FlavourCache
import com.hrm.markdown.parser.flavour.MarkdownFlavour
import com.hrm.markdown.parser.inline.InlineParser
import com.hrm.markdown.parser.lint.LintingPostProcessor
import com.hrm.markdown.parser.log.HLog
import com.hrm.markdown.parser.streaming.InlineAutoCloser

/**
 * 核心增量解析引擎。
 *
 * 统一处理全量解析、流式追加和编辑三种场景：
 * - **全量解析**：直接调用 BlockParser 解析全部文本
 * - **流式追加**：利用脏区域追踪，只重解析尾部变化区域
 * - **编辑**：利用脏区域追踪和节点复用，精准重解析编辑影响区域
 *
 * ## 关键组件
 * - [DirtyRegionTracker]：计算脏区域
 * - [NodeReuser]：判断和复用未变化的旧节点
 * - [PostProcessorRegistry]：后处理管线
 * - [MarkdownFlavour]：方言配置，控制支持的语法特性
 *
 * @param flavour Markdown 方言，控制支持的语法特性
 * @param postProcessors 后处理器注册表，若为 null 则从 flavour 获取
 */
class IncrementalEngine(
    private val flavour: MarkdownFlavour = ExtendedFlavour,
    private val customEmojiMap: Map<String, String> = emptyMap(),
    private val enableAsciiEmoticons: Boolean = false,
    postProcessors: PostProcessorRegistry? = null,
    private val lintingProcessor: LintingPostProcessor? = null,
    /**
     * 流式 append 的合并阈值（字符数）。0 表示关闭（默认行为：每次 append 立即增量解析）。
     *
     * 启用后，[append] 会把不含换行符的小 chunk 缓冲到 [fullText] 中但不立即触发块解析，
     * 直到出现以下任一情况才真正调用 [doIncrementalAppend]：
     *   - 本次 chunk 含 `\n`
     *   - 自上次解析以来累积未解析字符数 >= 该阈值
     *   - [endStream] / [currentText] 等需要"实时一致"的访问点
     *
     * 收益：把 LLM token 级 chunk（avg ~8 字符）压成行级 chunk，可减少 60-80% 的流式总耗时（streaming tax）。
     * 代价：未跨 `\n` 时，AST 上"正在写的最后一行"会延迟更新，最大延迟 = `appendCoalesceThreshold` 字符。
     *
     * 推荐值：32-64（基本无可见延迟，且消除大部分 dirty region 重复启动开销）。
     */
    private val appendCoalesceThreshold: Int = 0,
) {
    companion object {
        private const val TAG = "IncrementalEngine"
    }

    /** 使用 FlavourCache 缓存方言配置，避免重复初始化。 */
    private val flavourCache = FlavourCache.of(flavour)

    private val postProcessors: PostProcessorRegistry = postProcessors
        ?: flavourCache.newPostProcessorRegistry().apply {
            lintingProcessor?.let { register(it) }
        }

    /**
     * 获取缓存的 BlockStarter 注册表。
     * 利用 [FlavourCache] 避免每次解析都重新创建和排序。
     */
    private fun buildRegistry(source: SourceText): BlockStarterRegistry {
        return flavourCache.blockStarterRegistry
    }
    // ────── 状态 ──────
    private val fullText = StringBuilder()
    private var _document: Document = Document()
    private var _sourceText: SourceText = SourceText.of("")
    private var stableBlockCount: Int = 0
    private var stableEndLine: Int = 0
    private var lastParsedLength: Int = 0
    private var _isStreaming: Boolean = false
    /** 自上次 doIncrementalAppend 以来积压的、尚未触发解析的字符数。 */
    private var pendingAppendChars: Int = 0

    private val dirtyTracker = DirtyRegionTracker()
    private val nodeReuser = NodeReuser()

    // ────── 公开属性 ──────
    val document: Document get() = _document
    val sourceText: SourceText get() = _sourceText
    val isStreaming: Boolean get() = _isStreaming

    // ────── 全量解析 ──────

    /**
     * 对给定输入执行完整解析。
     */
    fun fullParse(input: String): Document {
        HLog.d(TAG) { "fullParse input=${input.length} chars" }
        fullText.clear()
        fullText.append(SourceText.normalize(input))
        _isStreaming = false
        stableBlockCount = 0
        stableEndLine = 0
        lastParsedLength = 0
        return doFullParse()
    }

    // ────── 流式 API ──────

    fun beginStream() {
        HLog.d(TAG, "beginStream")
        fullText.clear()
        _document = Document()
        _sourceText = SourceText.of("")
        stableBlockCount = 0
        stableEndLine = 0
        lastParsedLength = 0
        pendingAppendChars = 0
        _isStreaming = true
    }

    fun append(chunk: String): Document {
        if (chunk.isEmpty()) return _document
        val normalized = SourceText.normalize(chunk)
        fullText.append(normalized)
        if (appendCoalesceThreshold > 0) {
            pendingAppendChars += normalized.length
            val containsNewline = normalized.indexOf('\n') >= 0
            if (!containsNewline && pendingAppendChars < appendCoalesceThreshold) {
                // Skip incremental parse; fullText is already updated for currentText()/sourceText accessors via flush.
                return _document
            }
        }
        pendingAppendChars = 0
        return doIncrementalAppend()
    }

    fun endStream(): Document {
        HLog.d(TAG) { "endStream, totalLength=${fullText.length}" }
        _isStreaming = false
        pendingAppendChars = 0
        return doFullParse()
    }

    fun abort(): Document {
        HLog.w(TAG, "abort")
        _isStreaming = false
        pendingAppendChars = 0
        return _document
    }

    fun currentText(): String {
        // If there are pending un-parsed chars, callers reading sourceText still see them via fullText/_sourceText
        // because fullText is always kept up-to-date; but the AST may lag. Returning fullText is correct.
        return fullText.toString()
    }

    // ────── 编辑 API ──────

    /**
     * 应用编辑操作并增量更新 AST。
     *
     * @param edit 编辑操作
     * @return 更新后的 Document
     */
    fun applyEdit(edit: EditOperation): Document {
        HLog.d(TAG) { "applyEdit: $edit" }
        val oldSource = _sourceText
        val oldText = fullText.toString()

        // 应用编辑到文本（统一规范化插入文本，使 fullText 与 _sourceText.content 始终一致）
        val newSource: SourceText = when (edit) {
            is EditOperation.Insert -> {
                val ins = SourceText.normalize(edit.text)
                fullText.insert(edit.offset, ins)
                SourceText.applyEditFast(oldSource, edit.offset, 0, ins)
            }
            is EditOperation.Delete -> {
                fullText.deleteRange(edit.offset, edit.offset + edit.length)
                SourceText.applyEditFast(oldSource, edit.offset, edit.length, "")
            }
            is EditOperation.Replace -> {
                val ins = SourceText.normalize(edit.newText)
                fullText.replaceRange(edit.offset, edit.offset + edit.length, ins)
                SourceText.applyEditFast(oldSource, edit.offset, edit.length, ins)
            }
            is EditOperation.Append -> {
                fullText.append(SourceText.normalize(edit.text))
                // 对于 Append，委托给流式增量逻辑
                return doIncrementalAppend()
            }
        }
        _sourceText = newSource

        if (newSource.lineCount == 0) {
            _document = Document()
            return _document
        }

        // 计算脏区域
        val oldChildren = _document.children.toList()
        val dirtyRange = dirtyTracker.computeDirtyRange(edit, oldSource, newSource, oldChildren)

        // 找出脏区域之前可复用的节点
        val reusablePrefixCount = nodeReuser.findReusablePrefixCount(oldChildren, dirtyRange)

        // 解析脏区域（BlockParser 只做块结构 + 行内解析，后处理由 Engine 统一控制）
        val parser = BlockParser(
            source = newSource,
            registry = buildRegistry(newSource),
            inlineParserFactory = { doc ->
                doc.linkDefinitions.putAll(_document.linkDefinitions)
                InlineParser(doc, customEmojiMap, enableAsciiEmoticons, flavour.enableGfmAutolinks, flavour.enableExtendedInline, flavour.enableEmphasisCoalescing, flavour.enableStrikethrough)
            }
        )

        val newBlocks = if (dirtyRange.startLine < dirtyRange.endLine.coerceAtMost(newSource.lineCount)) {
            parser.parseLines(dirtyRange.startLine, dirtyRange.endLine.coerceAtMost(newSource.lineCount))
        } else {
            emptyList()
        }

        // 计算行数偏移
        val oldLineCount = oldSource.lineCount
        val newLineCount = newSource.lineCount
        val linesDelta = newLineCount - oldLineCount

        // 找出脏区域之后可复用的节点（需要根据旧坐标计算）
        val oldDirtyEndLine = dirtyRange.endLine - linesDelta
        val dirtyRangeOld = LineRange(dirtyRange.startLine, oldDirtyEndLine.coerceAtLeast(dirtyRange.startLine))
        val reusableSuffix = nodeReuser.findReusableSuffix(oldChildren, dirtyRangeOld, linesDelta, newSource)

        // 构建新文档
        val newDoc = Document()
        newDoc.linkDefinitions.putAll(_document.linkDefinitions)
        newDoc.footnoteDefinitions.putAll(_document.footnoteDefinitions)
        newDoc.abbreviationDefinitions.putAll(_document.abbreviationDefinitions)

        // 添加可复用的前缀
        for (i in 0 until reusablePrefixCount) {
            val child = oldChildren[i]
            child.parent = null
            newDoc.appendChild(child)
        }

        // 添加新解析的块
        for (block in newBlocks) {
            newDoc.appendChild(block)
        }

        // 添加可复用的后缀
        for (block in reusableSuffix) {
            block.parent = null
            newDoc.appendChild(block)
        }

        // 更新文档元数据
        setDocumentRanges(newDoc, newSource)

        // 后处理
        postProcessors.processAll(newDoc)

        // 将 Linting 诊断结果附加到 Document
        lintingProcessor?.let { newDoc.diagnostics = it.result }

        _document = newDoc
        lastParsedLength = newSource.length
        return _document
    }

    // ────── 内部实现 ──────

    /**
     * 全量解析（流结束或非流式模式）。
     */
    private fun doFullParse(): Document {
        val text = fullText.toString()
        _sourceText = SourceText.of(text)
        val parser = BlockParser(
            source = _sourceText,
            registry = buildRegistry(_sourceText),
            inlineParserFactory = { doc -> InlineParser(doc, customEmojiMap, enableAsciiEmoticons, flavour.enableGfmAutolinks, flavour.enableExtendedInline, flavour.enableEmphasisCoalescing, flavour.enableStrikethrough) }
        )
        _document = parser.parse()

        // 后处理统一由 Engine 控制
        postProcessors.processAll(_document)

        // 将 Linting 诊断结果附加到 Document
        lintingProcessor?.let { _document.diagnostics = it.result }

        stableBlockCount = _document.children.size
        stableEndLine = _sourceText.lineCount
        lastParsedLength = text.length
        HLog.d(TAG) { "doFullParse done: ${_document.children.size} blocks, ${_sourceText.lineCount} lines" }
        return _document
    }

    /**
     * append-only 增量解析（流式场景）。
     */
    private fun doIncrementalAppend(): Document {
        val text = fullText.toString()
        val newSource = SourceText.of(text)
        _sourceText = newSource

        if (newSource.lineCount == 0) {
            _document = Document()
            return _document
        }

        if (text.length == lastParsedLength) {
            return _document
        }
        lastParsedLength = text.length

        val oldChildren = _document.children
        // 使用脏区域追踪器计算安全重解析起点
        val lastStableBlock = if (stableBlockCount > 0) oldChildren.getOrNull(stableBlockCount - 1) else null
        val safeStableEndLine = if (lastStableBlock != null && !isSelfDelimitedBlock(lastStableBlock)) {
            lastStableBlock.lineRange.startLine
        } else {
            stableEndLine
        }
        val reparseStart = if (lastStableBlock != null && isSelfDelimitedBlock(lastStableBlock)) {
            safeStableEndLine.coerceAtMost(newSource.lineCount)
        } else {
            dirtyTracker.computeAppendDirtyRange(safeStableEndLine, newSource).startLine
        }
        HLog.v(TAG) {
            "doIncrementalAppend: reparseStart=$reparseStart, lines=${newSource.lineCount}, " +
                "stableEndLine=$stableEndLine, safeStableEndLine=$safeStableEndLine"
        }

        // 解析脏区域（BlockParser 只做块结构 + 行内解析，后处理由 Engine 统一控制）
        val parser = BlockParser(
            source = newSource,
            registry = buildRegistry(newSource),
            inlineParserFactory = { doc ->
                doc.linkDefinitions.putAll(_document.linkDefinitions)
                InlineParser(doc, customEmojiMap, enableAsciiEmoticons, flavour.enableGfmAutolinks, flavour.enableExtendedInline, flavour.enableEmphasisCoalescing, flavour.enableStrikethrough)
            }
        )

        val tailBlocks = if (reparseStart < newSource.lineCount) {
            parser.parseLines(reparseStart, newSource.lineCount)
        } else {
            emptyList()
        }

        // 分类稳定和不稳定块
        val (nowStable, stillOpen) = classifyTailBlocks(tailBlocks, newSource)
        HLog.v(TAG) { "tailBlocks=${tailBlocks.size}, stable=${nowStable.size}, open=${stillOpen.size}" }

        // 对仍在构建中的块做 auto-close 修复
        val displayBlocks = if (_isStreaming && stillOpen.isNotEmpty()) {
            autoCloseBlocks(stillOpen, newSource)
        } else {
            stillOpen
        }

        // 构建新文档
        val newDoc = Document()
        newDoc.linkDefinitions.putAll(_document.linkDefinitions)
        newDoc.footnoteDefinitions.putAll(_document.footnoteDefinitions)
        newDoc.abbreviationDefinitions.putAll(_document.abbreviationDefinitions)

        // 复用 reparseStart 之前的旧块
        val reusableCount = oldChildren.count { child ->
            child.lineRange.endLine <= reparseStart
        }
        for (i in 0 until reusableCount) {
            val child = oldChildren[i]
            child.parent = null
            newDoc.appendChild(child)
        }

        val reusedStableBlocks = reuseStreamingBlockInstances(nowStable, oldChildren)
        for (block in reusedStableBlocks) {
            newDoc.appendChild(block)
        }

        val reusedDisplayBlocks = reuseStreamingBlockInstances(displayBlocks, oldChildren)
        for (block in reusedDisplayBlocks) {
            newDoc.appendChild(block)
        }

        val newStableCount = reusableCount + reusedStableBlocks.size
        stableBlockCount = newStableCount
        stableEndLine = if (reusedStableBlocks.isNotEmpty()) {
            reusedStableBlocks.last().lineRange.endLine
        } else if (reusableCount > 0) {
            oldChildren[reusableCount - 1].lineRange.endLine
        } else {
            0
        }

        setDocumentRanges(newDoc, newSource)

        for (block in tailBlocks) {
            collectLinkDefinitions(block, newDoc)
        }

        // 后处理
        postProcessors.processAll(newDoc)

        _document = newDoc
        HLog.v(TAG) { "doIncrementalAppend done: reused=$reusableCount, stable=${reusedStableBlocks.size}, open=${displayBlocks.size}, total=${newDoc.children.size}" }
        return _document
    }

    /**
     * 对仍在构建中的 displayBlocks，尝试在旧文档的 children 中查找
     * 同 startLine + 同类型的旧节点实例。若找到，将新解析的属性写入旧实例并返回旧实例，
     * 使 Compose 的 === 引用比较判断为同一对象，跳过不必要的重组。
     *
     * 对内部渲染器带状态的块做复用，避免外层替换节点导致 Compose 子树被卸载重建。
     */
    private fun reuseStreamingBlockInstances(
        displayBlocks: List<Node>,
        oldChildren: List<Node>,
    ): List<Node> {
        if (displayBlocks.isEmpty() || oldChildren.isEmpty()) return displayBlocks

        return displayBlocks.map { newBlock ->
            when (newBlock) {
                is FencedCodeBlock -> reuseFencedCodeBlockInstance(newBlock, oldChildren)
                is MathBlock -> reuseMathBlockInstance(newBlock, oldChildren)
                else -> newBlock
            }
        }
    }

    private fun reuseFencedCodeBlockInstance(
        newBlock: FencedCodeBlock,
        oldChildren: List<Node>,
    ): Node {
        val oldBlock = oldChildren.find { old ->
            old is FencedCodeBlock &&
                    old.lineRange.startLine == newBlock.lineRange.startLine
        } as? FencedCodeBlock ?: return newBlock

        oldBlock.literal = newBlock.literal
        oldBlock.lineRange = newBlock.lineRange
        oldBlock.sourceRange = newBlock.sourceRange
        oldBlock.contentHash = newBlock.contentHash
        oldBlock.info = newBlock.info
        oldBlock.language = newBlock.language
        oldBlock.fenceChar = newBlock.fenceChar
        oldBlock.fenceLength = newBlock.fenceLength
        oldBlock.fenceIndent = newBlock.fenceIndent
        oldBlock.attributes = newBlock.attributes
        oldBlock.highlightLines = newBlock.highlightLines
        oldBlock.showLineNumbers = newBlock.showLineNumbers
        oldBlock.startLineNumber = newBlock.startLineNumber
        oldBlock.parent = null
        return oldBlock
    }

    private fun reuseMathBlockInstance(
        newBlock: MathBlock,
        oldChildren: List<Node>,
    ): Node {
        val oldBlock = oldChildren.find { old ->
            old is MathBlock &&
                    old.lineRange.startLine == newBlock.lineRange.startLine
        } as? MathBlock ?: return newBlock

        oldBlock.literal = newBlock.literal
        oldBlock.lineRange = newBlock.lineRange
        oldBlock.sourceRange = newBlock.sourceRange
        oldBlock.contentHash = newBlock.contentHash
        oldBlock.parent = null
        return oldBlock
    }

    // ────── 块稳定性分类 ──────

    private fun classifyTailBlocks(
        blocks: List<Node>,
        source: SourceText
    ): Pair<List<Node>, List<Node>> {
        if (blocks.isEmpty()) return Pair(emptyList(), emptyList())
        if (!_isStreaming) return Pair(blocks, emptyList())

        val stable = mutableListOf<Node>()
        val open = mutableListOf<Node>()

        for (i in blocks.indices) {
            if (i == blocks.size - 1) {
                val lastBlock = blocks[i]
                if (isBlockFullyClosed(lastBlock, source)) {
                    stable.add(lastBlock)
                } else {
                    open.add(lastBlock)
                }
            } else {
                val block = blocks[i]
                val nextBlock = blocks[i + 1]
                val gapStart = block.lineRange.endLine
                val gapEnd = nextBlock.lineRange.startLine
                val hasBlankSeparator = (gapStart < gapEnd) && hasBlankLineInRange(source, gapStart, gapEnd)
                if (hasBlankSeparator || isSelfDelimitedBoundary(block, nextBlock)) {
                    stable.add(block)
                } else {
                    for (j in i until blocks.size) {
                        open.add(blocks[j])
                    }
                    break
                }
            }
        }
        return Pair(stable, open)
    }

    private fun isSelfDelimitedBoundary(block: Node, nextBlock: Node): Boolean {
        if (block.lineRange.endLine > nextBlock.lineRange.startLine) return false
        return isSelfDelimitedBlock(block)
    }

    private fun isSelfDelimitedBlock(block: Node): Boolean {
        return when (block) {
            is FencedCodeBlock,
            is IndentedCodeBlock,
            is HtmlBlock,
            is MathBlock,
            is DiagramBlock,
            is FrontMatter,
            is ThematicBreak -> true
            else -> false
        }
    }

    private fun isBlockFullyClosed(block: Node, source: SourceText): Boolean {
        if (block is ListBlock) return false
        val endLine = block.lineRange.endLine
        if (endLine >= source.lineCount) return false
        return hasBlankLineInRange(source, endLine, source.lineCount)
    }

    private fun hasBlankLineInRange(source: SourceText, startLine: Int, endLine: Int): Boolean {
        for (line in startLine until endLine.coerceAtMost(source.lineCount)) {
            if (source.lineContent(line).isBlank()) return true
        }
        return false
    }

    // ────── 块级自动关闭 ──────

    private fun autoCloseBlocks(blocks: List<Node>, source: SourceText): List<Node> {
        return blocks.map { block -> autoCloseBlock(block, source) }
    }

    private fun autoCloseBlock(block: Node, source: SourceText): Node {
        return when (block) {
            is FencedCodeBlock, is MathBlock, is FrontMatter, is HtmlBlock, is DiagramBlock -> block
            is Paragraph -> {
                autoCloseInlineContent(block, source)
                block
            }
            is Heading -> {
                autoCloseInlineContent(block, source)
                block
            }
            is SetextHeading -> {
                if (shouldDowngradeTransientSetextHeading(block, source)) {
                    createDisplayParagraphFromSetextHeading(block, source)
                } else {
                    autoCloseInlineContent(block, source)
                    block
                }
            }
            is BlockQuote -> {
                val children = block.children.toList()
                if (children.isNotEmpty()) {
                    val lastChild = children.last()
                    val repaired = autoCloseBlock(lastChild, source)
                    if (repaired !== lastChild) {
                        block.replaceChild(lastChild, repaired)
                    }
                }
                block
            }
            is ListBlock, is ListItem, is CustomContainer -> {
                val children = block.children.toList()
                if (children.isNotEmpty()) {
                    val lastChild = children.last()
                    val repaired = autoCloseBlock(lastChild, source)
                    if (repaired !== lastChild) {
                        block.replaceChild(lastChild, repaired)
                    }
                }
                block
            }
            is Table -> {
                autoCloseTableCells(block, source)
                block
            }
            else -> block
        }
    }

    private fun shouldDowngradeTransientSetextHeading(
        block: SetextHeading,
        source: SourceText,
    ): Boolean {
        if (!_isStreaming) return false
        if (block.lineRange.endLine != source.lineCount) return false
        val underlineLine = source.lineContent(source.lineCount - 1).trim()
        if (underlineLine.isEmpty()) return false
        return underlineLine.all { it == '-' || it == '=' }
    }

    private fun createDisplayParagraphFromSetextHeading(
        heading: SetextHeading,
        source: SourceText,
    ): Paragraph {
        val paragraph = Paragraph()
        val content = heading.rawContent ?: buildString {
            val contentEnd = (heading.lineRange.endLine - 1).coerceAtLeast(heading.lineRange.startLine)
            for (line in heading.lineRange.startLine until contentEnd) {
                if (line > heading.lineRange.startLine) append('\n')
                append(source.lineContent(line).trimStart().trimEnd())
            }
        }
        paragraph.rawContent = content
        paragraph.lineRange = LineRange(
            heading.lineRange.startLine,
            (heading.lineRange.endLine - 1).coerceAtLeast(heading.lineRange.startLine + 1)
        )
        paragraph.sourceRange = heading.sourceRange
        if (content.isNotEmpty()) {
            val tempDoc = Document()
            tempDoc.linkDefinitions.putAll(_document.linkDefinitions)
            val inlineParser = InlineParser(
                tempDoc,
                customEmojiMap,
                enableAsciiEmoticons,
                flavour.enableGfmAutolinks,
                flavour.enableExtendedInline,
                flavour.enableEmphasisCoalescing,
                flavour.enableStrikethrough
            )
            inlineParser.parseInlines(content, paragraph)
        }
        return paragraph
    }

    private fun autoCloseInlineContent(node: ContainerNode, source: SourceText) {
        val inlineText = currentInlineSource(node, source)
        if (inlineText.isEmpty()) return

        val repairSuffix = InlineAutoCloser.buildRepairSuffix(inlineText)
        val repairedContent = inlineText + repairSuffix
        val tempDoc = Document()
        tempDoc.linkDefinitions.putAll(_document.linkDefinitions)
        val inlineParser = InlineParser(tempDoc, customEmojiMap, enableAsciiEmoticons, flavour.enableGfmAutolinks, flavour.enableExtendedInline, flavour.enableEmphasisCoalescing, flavour.enableStrikethrough)
        node.clearChildren()
        inlineParser.parseInlines(repairedContent, node)
    }

    private fun currentInlineSource(node: ContainerNode, source: SourceText): String {
        val raw = when (node) {
            is Paragraph -> node.rawContent
            is Heading -> node.rawContent
            is SetextHeading -> node.rawContent
            is TableCell -> node.rawContent
            else -> null
        }
        if (!raw.isNullOrEmpty()) return raw

        if (node.lineRange.lineCount > 0 && node.lineRange.endLine <= source.lineCount) {
            return buildString {
                for (line in node.lineRange.startLine until node.lineRange.endLine) {
                    if (line > node.lineRange.startLine) append('\n')
                    append(source.lineContent(line).trimStart().trimEnd())
                }
            }
        }

        return extractInlineText(node)
    }

    private fun autoCloseTableCells(table: Table, source: SourceText) {
        for (child in table.children) {
            if (child is ContainerNode) {
                for (row in child.children) {
                    if (row is TableRow) {
                        val cells = row.children.toList()
                        if (cells.isNotEmpty()) {
                            val lastCell = cells.last()
                            if (lastCell is TableCell) {
                                autoCloseInlineContent(lastCell, source)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun extractInlineText(node: ContainerNode): String {
        val sb = StringBuilder()
        for (child in node.children) {
            appendNodeText(child, sb)
        }
        return sb.toString()
    }

    private fun appendNodeText(node: Node, sb: StringBuilder) {
        when (node) {
            is Text -> sb.append(node.literal)
            is InlineCode -> sb.append("`").append(node.literal).append("`")
            is InlineMath -> sb.append("$").append(node.literal).append("$")
            is Emphasis -> {
                val d = node.delimiter
                sb.append(d)
                for (child in node.children) appendNodeText(child, sb)
                sb.append(d)
            }
            is StrongEmphasis -> {
                val d = node.delimiter.toString().repeat(2)
                sb.append(d)
                for (child in node.children) appendNodeText(child, sb)
                sb.append(d)
            }
            is Strikethrough -> {
                sb.append("~~")
                for (child in node.children) appendNodeText(child, sb)
                sb.append("~~")
            }
            is Highlight -> {
                sb.append("==")
                for (child in node.children) appendNodeText(child, sb)
                sb.append("==")
            }
            is Link -> {
                sb.append("[")
                for (child in node.children) appendNodeText(child, sb)
                sb.append("](").append(node.destination)
                if (node.title != null) sb.append(" \"").append(node.title).append("\"")
                sb.append(")")
            }
            is Image -> {
                sb.append("![")
                for (child in node.children) appendNodeText(child, sb)
                sb.append("](").append(node.destination)
                // 重建 =WxH 尺寸后缀
                if (node.imageWidth != null || node.imageHeight != null) {
                    sb.append(" =")
                    sb.append(node.imageWidth?.toString() ?: "")
                    sb.append("x")
                    sb.append(node.imageHeight?.toString() ?: "")
                }
                if (node.title != null) sb.append(" \"").append(node.title).append("\"")
                sb.append(")")
                // 重建属性块
                if (node.attributes.isNotEmpty()) {
                    sb.append("{")
                    for ((key, value) in node.attributes) {
                        when (key) {
                            "class" -> value.split(" ").filter { it.isNotEmpty() }.forEach { sb.append(".").append(it).append(" ") }
                            "id" -> sb.append("#").append(value).append(" ")
                            else -> if (value.isNotEmpty()) sb.append(key).append("=").append(value).append(" ") else sb.append(key).append(" ")
                        }
                    }
                    sb.trimEnd()
                    sb.append("}")
                }
            }
            is EscapedChar -> sb.append("\\").append(node.literal)
            is SoftLineBreak -> sb.append("\n")
            is HardLineBreak -> sb.append("\n")
            is HtmlEntity -> sb.append(node.literal)
            is Autolink -> sb.append("<").append(node.destination).append(">")
            is ContainerNode -> {
                for (child in node.children) appendNodeText(child, sb)
            }
            is LeafNode -> sb.append(node.literal)
        }
    }

    // ────── 工具方法 ──────

    private fun setDocumentRanges(doc: Document, source: SourceText) {
        doc.lineRange = LineRange(0, source.lineCount)
        if (source.lineCount > 0) {
            doc.sourceRange = SourceRange(
                SourcePosition(0, 0, 0),
                SourcePosition(
                    source.lineCount - 1,
                    source.lineContent(source.lineCount - 1).length,
                    source.length
                )
            )
        }
    }

    private fun collectLinkDefinitions(node: Node, doc: Document) {
        when (node) {
            is LinkReferenceDefinition -> {
                val label = node.label.lowercase().trim()
                if (label.isNotEmpty() && !doc.linkDefinitions.containsKey(label)) {
                    doc.linkDefinitions[label] = node
                }
            }
            is ContainerNode -> {
                for (child in node.children) {
                    collectLinkDefinitions(child, doc)
                }
            }
            else -> {}
        }
    }
}
