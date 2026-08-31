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


package com.hrm.latex.parser

import com.hrm.latex.base.log.HLog
import com.hrm.latex.parser.incremental.IncrementalTokenizer
import com.hrm.latex.parser.incremental.TextEdit
import com.hrm.latex.parser.incremental.TreeReuser
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.model.SourceRange
import com.hrm.latex.parser.tokenizer.LatexToken

/**
 * 增量 LaTeX 解析器（tree-sitter 风格）
 *
 * 核心设计参考 tree-sitter 的增量解析三层策略：
 *
 * ### 第 1 层：增量分词（Token 复用）
 * 编辑发生时，通过 [IncrementalTokenizer] 只重新分词编辑影响的区域，
 * 前后未变更的 token 直接复用（偏移平移），避免全文重新分词。
 *
 * ### 第 2 层：AST 子树复用
 * 通过 [TreeReuser] 将旧 AST 的顶层子节点分为 prefix/dirty/suffix 三段：
 * - prefix：编辑区域前，直接保留
 * - dirty：与编辑区域重叠，使用新 token 重新解析
 * - suffix：编辑区域后，偏移平移后复用
 *
 * ### 第 3 层：容错解析
 * 对于不完整输入（如流式打字场景），使用智能截断而非暴力回退：
 * 在完整解析失败时，按结构边界（`{}`、`$`、`\[...\]`、`\begin..\end`）截断到最近的安全点。
 *
 * ### 使用场景
 * - 实时输入 / 打字效果
 * - 流式传输的 LaTeX 内容
 * - 实时预览编辑器
 *
 * ### 示例
 * ```kotlin
 * val parser = IncrementalLatexParser()
 * parser.append("\\int_{-\\")
 * val result1 = parser.getCurrentDocument()
 * parser.append("infty}^{\\infty}")
 * val result2 = parser.getCurrentDocument()
 * ```
 *
 * ### 性能对比（vs 旧方案）
 * | 操作 | 旧方案 | 新方案 |
 * |------|--------|--------|
 * | 追加 1 字符 | 全文 tokenize + 全文 parse | 增量 tokenize + 局部 parse + 子树复用 |
 * | 中间插入 | 全文 tokenize + 全文 parse | 增量 tokenize + 局部 parse + 前缀/后缀复用 |
 * | 解析失败 | 逐字符回退（最坏 O(n²)） | 结构边界截断（O(n)） |
 */
class IncrementalLatexParser {

    /** 增量分词器（维护 token 缓存） */
    private val tokenizer = IncrementalTokenizer()

    /** 基础解析器（用于解析 token 列表） */
    private val baseParser = LatexParser()

    /** 缓存的解析结果 */
    private var cachedDocument: LatexNode.Document? = null

    /** 最后一次成功解析的位置 */
    private var lastSuccessfulPosition = 0

    companion object {
        private const val TAG = "IncrementalLatexParser"
    }

    /**
     * 追加新的 LaTeX 内容（快速路径）
     *
     * 使用 TextEdit.fromAppend() 直接构造编辑描述，跳过 O(n) 的 diff 比较。
     * 文本拼接直接在 IncrementalTokenizer 的 StringBuilder 上完成，避免 O(n) 字符串拷贝。
     *
     * @param text 新增的文本内容
     */
    fun append(text: String) {
        if (text.isEmpty()) return
        val oldLength = tokenizer.getTextLength()
        // 直接在 tokenizer 的 StringBuilder 上追加，避免 toString() + 拼接
        val newLength = tokenizer.appendText(text)
        val newText = tokenizer.getCurrentText()
        val edit = TextEdit.fromAppend(oldLength, newLength)
        HLog.d(TAG) { "追加内容: '$text', 新长度: $newLength" }
        applyEdit(oldLength, newText, edit)
    }

    /**
     * 完全替换输入内容
     * @param newText 新的完整文本
     */
    fun setInput(newText: String) {
        val oldText = tokenizer.getCurrentText()
        if (oldText == newText) return
        HLog.d(TAG) { "替换内容, 新长度: ${newText.length}" }
        val edit = TextEdit.diff(oldText, newText)
        applyEdit(oldText.length, newText, edit)
    }

    /**
     * 应用编辑操作（核心增量解析入口）
     *
     * @param oldTextLength 编辑前的文本长度
     * @param newText 编辑后的完整文本
     * @param edit 预计算的编辑差异（由调用方提供，避免重复 diff）
     */
    private fun applyEdit(oldTextLength: Int, newText: String, edit: TextEdit) {
        if (newText.isEmpty()) {
            clear()
            return
        }

        HLog.d(TAG) {
            "编辑差异: start=${edit.startOffset}, " +
                    "oldEnd=${edit.oldEndOffset}, newEnd=${edit.newEndOffset}, delta=${edit.delta}"
        }

        // 第 1 层：增量分词（总是执行，即使后续走全量解析也需要更新 token 缓存）
        val isFirstParse = oldTextLength == 0
        if (isFirstParse) {
            tokenizer.tokenize(newText)
        } else {
            tokenizer.update(newText, edit)
        }

        // 第 2 层：决策 — 增量解析 vs 全量解析
        val oldDoc = cachedDocument

        // 追加场景快速路径：上次解析成功 + 纯追加 → 尾部增量 AST 构建
        val canAppendIncremental = !isFirstParse
                && oldDoc != null
                && oldDoc.children.isNotEmpty()
                && lastSuccessfulPosition == oldTextLength  // 上次完整解析成功
                && edit.isInsertion                          // 纯追加/插入
                && edit.startOffset == oldTextLength         // 追加在末尾（非中间插入）

        // 中间替换/删除场景：使用 TreeReuser 三段划分
        val canIncrementalParse = !isFirstParse
                && oldDoc != null
                && oldDoc.children.isNotEmpty()
                && lastSuccessfulPosition == oldTextLength  // 上次完整解析成功
                && edit.startOffset > 0                       // 编辑不在最开头
                && !edit.isInsertion                          // 非纯插入（中间替换/删除场景）

        if (canAppendIncremental) {
            val result = appendIncrementalParse(newText, oldDoc)
            if (result != null) {
                cachedDocument = result
                return
            }
            // 追加增量失败（结构不完整等），走普通分支
        }

        if (canIncrementalParse) {
            cachedDocument = incrementalParse(newText, oldDoc, edit)
        } else {
            // 检查输入是否结构完整（花括号、数学模式、环境是否都已闭合）
            val isStructurallyComplete = isInputComplete(newText)

            if (isStructurallyComplete) {
                // 结构完整 → 优先使用缓存的 token 列表解析（避免二次分词）
                val fullResult = tryParseFromTokens() ?: tryParse(newText)
                if (fullResult != null) {
                    cachedDocument = fullResult
                    lastSuccessfulPosition = newText.length
                    HLog.d(TAG) { "全量解析成功" }
                } else {
                    cachedDocument = truncatedParse(newText)
                }
            } else if (oldDoc != null && oldDoc.children.isNotEmpty()) {
                // 结构不完整但有旧缓存 → 保留旧缓存，不展示异常中间态
                HLog.d(TAG) { "输入结构不完整, 保留上次成功结果, 等待输入补全" }
            } else {
                // 无旧缓存可用 → 容错截断解析（首次输入场景）
                cachedDocument = truncatedParse(newText)
            }
        }
    }

    /**
     * 追加场景的增量 AST 构建
     *
     * 核心思路：旧 AST 的前缀节点完全保留，只重新解析最后一个受影响的节点
     * + 新追加的部分。对于逐字输入场景，前缀可能是整个旧 AST 除最后一个节点。
     *
     * 追加的文本可能与前一个 token 合并（如 "\fr" + "a" → "\fra"），
     * 因此需要找到最后一个 sourceRange 触及追加点的旧节点，从该节点开始
     * 的文本区域 + 新追加文本一起重新解析。
     *
     * @return 成功则返回新 Document，失败（结构不完整等）返回 null
     */
    private fun appendIncrementalParse(
        newText: String,
        oldDoc: LatexNode.Document
    ): LatexNode.Document? {
        val oldChildren = oldDoc.children

        // 找到最后一个需要重新解析的节点索引
        // 追加点 = lastSuccessfulPosition（旧文本末尾）
        val appendPoint = lastSuccessfulPosition
        var reparseFromIdx = oldChildren.size  // 默认：无节点需要重解析

        for (i in oldChildren.indices.reversed()) {
            val range = oldChildren[i].sourceRange
            if (range != null && range.end >= appendPoint) {
                // 该节点触及或超过追加点，需要重新解析
                reparseFromIdx = i
            } else {
                // 该节点完全在追加点之前，安全
                break
            }
        }

        // 前缀节点：完全在追加点之前的节点
        val prefixNodes = if (reparseFromIdx > 0) {
            oldChildren.subList(0, reparseFromIdx)
        } else {
            emptyList()
        }

        // 确定需要重新解析的文本范围
        val reparseStart = if (reparseFromIdx < oldChildren.size) {
            oldChildren[reparseFromIdx].sourceRange?.start ?: appendPoint
        } else {
            appendPoint
        }

        val reparseText = if (reparseStart < newText.length) {
            newText.substring(reparseStart, newText.length)
        } else {
            return null // 没有需要解析的文本
        }

        // 检查需要重解析的部分是否结构完整
        if (!isTextStructurallyComplete(reparseText)) {
            // 结构不完整 → 保留旧缓存（返回 null 让调用方决策）
            HLog.d(TAG) { "追加增量: 尾部结构不完整, 保留旧缓存" }
            return null
        }

        // 解析尾部文本
        val tailDoc = tryParse(reparseText)
        if (tailDoc == null) {
            HLog.d(TAG) { "追加增量: 尾部解析失败" }
            return null
        }

        // 调整 sourceRange 到全局坐标
        val tailChildren = if (reparseStart > 0) {
            adjustSourceRanges(tailDoc.children, reparseStart)
        } else {
            tailDoc.children
        }

        // 拼接前缀 + 新尾部
        val newChildren = ArrayList<LatexNode>(prefixNodes.size + tailChildren.size).apply {
            addAll(prefixNodes)
            addAll(tailChildren)
        }

        lastSuccessfulPosition = newText.length
        HLog.d(TAG) {
            "追加增量成功: 复用 ${prefixNodes.size} 个前缀节点, " +
                    "重解析 ${tailChildren.size} 个尾部节点 (从位置 $reparseStart)"
        }
        return LatexNode.Document(
            newChildren,
            sourceRange = SourceRange(0, newText.length)
        )
    }

    /**
     * 检查文本片段是否结构完整（不依赖 tokenizer 缓存）
     * 用于追加增量解析中对尾部片段的快速检查
     */
    private fun isTextStructurallyComplete(text: String): Boolean {
        if (text.isEmpty()) return true
        var braceDepth = 0
        var mathMode = false
        var displayMath = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                ch == '\\' -> { i += 2; continue }
                ch == '{' -> braceDepth++
                ch == '}' -> braceDepth = maxOf(0, braceDepth - 1)
                ch == '$' -> {
                    if (i + 1 < text.length && text[i + 1] == '$') {
                        displayMath = !displayMath
                        i += 2; continue
                    } else {
                        mathMode = !mathMode
                    }
                }
            }
            i++
        }
        if (braceDepth > 0 || mathMode || displayMath) return false
        // 检查是否以反斜杠结尾
        var j = text.length - 1
        while (j >= 0 && text[j].isWhitespace()) j--
        if (j >= 0 && text[j] == '\\') return false
        return true
    }

    /**
     * 增量解析：复用旧 AST 子树 + 只重新解析脏区域
     *
     * 适用场景：上次完整解析成功，且本次编辑发生在文本中间（非纯追加）。
     * 通过 [TreeReuser] 将旧 AST 的顶层子节点分为三段（prefix/dirty/suffix），
     * 只重新解析 dirty 区域，前缀和后缀直接复用。
     */
    private fun incrementalParse(
        newText: String,
        oldDoc: LatexNode.Document,
        edit: TextEdit
    ): LatexNode.Document {

        val oldChildren = oldDoc.children

        // 对旧 AST 进行三段划分
        val partition = TreeReuser.partition(oldChildren, edit)
        HLog.d(TAG) {
            "子树划分: prefix=${partition.prefix.size}, " +
                    "dirty=${partition.dirty.size}, suffix=${partition.suffix.size}"
        }

        // 确定脏区域在新文本中的范围
        val dirtyStart = if (partition.prefix.isNotEmpty()) {
            partition.prefix.last().sourceRange?.end ?: 0
        } else {
            0
        }

        val dirtyEnd = if (partition.suffix.isNotEmpty()) {
            val firstSuffixOldStart = partition.suffix.first().sourceRange?.start ?: newText.length
            firstSuffixOldStart + edit.delta
        } else {
            newText.length
        }

        // 重新解析脏区域
        val dirtyText = if (dirtyStart < dirtyEnd && dirtyEnd <= newText.length) {
            newText.substring(dirtyStart, dirtyEnd)
        } else {
            ""
        }

        val newDirtyChildren = if (dirtyText.isNotEmpty()) {
            parseSafe(dirtyText, dirtyStart)
        } else {
            emptyList()
        }

        // 后缀节点偏移平移
        val shiftedSuffix = TreeReuser.shiftNodes(partition.suffix, edit.delta)

        // 拼接三段
        val newChildren = mutableListOf<LatexNode>().apply {
            addAll(partition.prefix)
            addAll(newDirtyChildren)
            addAll(shiftedSuffix)
        }

        lastSuccessfulPosition = newText.length
        return LatexNode.Document(
            newChildren,
            sourceRange = SourceRange(0, newText.length)
        )
    }

    /**
     * 安全解析子区域，解析失败时进行容错截断
     *
     * @param text 待解析的文本片段
     * @param globalOffset 该片段在完整文本中的起始偏移（用于 sourceRange 调整）
     * @return 解析出的节点列表
     */
    private fun parseSafe(text: String, globalOffset: Int): List<LatexNode> {
        // 先尝试完整解析
        val doc = tryParse(text)
        if (doc != null) {
            return adjustSourceRanges(doc.children, globalOffset)
        }

        // 完整解析失败 → 结构边界截断
        val truncated = truncateToSafeBoundary(text)
        if (truncated.isNotEmpty() && truncated != text) {
            val truncDoc = tryParse(truncated)
            if (truncDoc != null) {
                return adjustSourceRanges(truncDoc.children, globalOffset)
            }
        }

        // 截断后仍失败 → 返回原始文本节点
        HLog.w(TAG) { "脏区域解析完全失败, 返回原始文本, 长度=${text.length}" }
        return if (text.isNotBlank()) {
            listOf(
                LatexNode.Text(
                    text,
                    sourceRange = SourceRange(globalOffset, globalOffset + text.length)
                )
            )
        } else {
            emptyList()
        }
    }

    /**
     * 尝试解析，失败返回 null（不抛异常）
     */
    private fun tryParse(text: String): LatexNode.Document? {
        return try {
            baseParser.parse(text)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 使用已缓存的 token 列表解析，避免二次分词
     */
    private fun tryParseFromTokens(): LatexNode.Document? {
        return try {
            val tokens = tokenizer.getCurrentTokens()
            val text = tokenizer.getCurrentText()
            if (tokens.isNotEmpty() && text.isNotEmpty()) {
                baseParser.parse(tokens, text.length)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 将子区域解析结果的 sourceRange 调整到全局坐标
     *
     * 子区域解析时 sourceRange 基于子串的局部坐标（从 0 开始），
     * 需要加上 globalOffset 转换为完整文本中的全局坐标。
     */
    private fun adjustSourceRanges(nodes: List<LatexNode>, globalOffset: Int): List<LatexNode> {
        if (globalOffset == 0) return nodes
        return nodes.map { node -> shiftNodeRange(node, globalOffset) }
    }

    /**
     * 递归平移节点及其子节点的 sourceRange
     */
    private fun shiftNodeRange(node: LatexNode, offset: Int): LatexNode {
        val oldRange = node.sourceRange
        val shiftedNode = if (oldRange != null) {
            node.withSourceRange(SourceRange(oldRange.start + offset, oldRange.end + offset))
        } else {
            node
        }

        val children = node.children()
        if (children.isEmpty()) return shiftedNode

        val shiftedChildren = children.map { child -> shiftNodeRange(child, offset) }
        return shiftedNode.withChildren(shiftedChildren)
    }

    /**
     * 全量解析（首次且无旧缓存时使用）
     *
     * 注意：此方法仅在没有旧缓存可回退时调用。
     * 有旧缓存时，解析失败会保留旧缓存，不走截断逻辑。
     */
    private fun fullParse(text: String): LatexNode.Document {
        // 先尝试完整解析
        val doc = tryParse(text)
        if (doc != null) {
            lastSuccessfulPosition = text.length
            HLog.d(TAG) { "全量解析成功" }
            return doc
        }

        // 完整解析失败 → 结构边界截断（仅首次无缓存时使用）
        HLog.d(TAG) { "全量解析失败, 尝试结构边界截断" }
        return truncatedParse(text)
    }

    /**
     * 容错截断解析
     *
     * 与旧方案的逐字符回退不同，新方案按结构边界截断：
     * 找到最后一个"安全点"（结构闭合的位置），只解析到安全点。
     *
     * 安全点的定义：
     * 1. 最后一个闭合的 `}` 后
     * 2. 最后一个闭合的 `$`/`$$` 后
     * 3. 最后一个 `\end{...}` 后
     * 4. 最后一个完整命令（无未闭合 `{`）后
     */
    private fun truncatedParse(text: String): LatexNode.Document {
        val truncated = truncateToSafeBoundary(text)
        if (truncated.isNotEmpty()) {
            val doc = tryParse(truncated)
            if (doc != null) {
                lastSuccessfulPosition = truncated.length
                HLog.d(TAG) { "截断解析成功, 截断位置: ${truncated.length}/${text.length}" }
                return doc
            }
        }

        // 逐步缩短到半长度尝试
        var length = text.length * 3 / 4
        while (length > 0) {
            val sub = truncateToSafeBoundary(text.substring(0, length))
            if (sub.isNotEmpty()) {
                val doc = tryParse(sub)
                if (doc != null) {
                    lastSuccessfulPosition = sub.length
                    HLog.d(TAG) { "渐进截断解析成功, 截断位置: ${sub.length}/${text.length}" }
                    return doc
                }
            }
            length = length * 3 / 4
        }

        HLog.w(TAG) { "所有解析策略失败, 返回空文档" }
        lastSuccessfulPosition = 0
        return LatexNode.Document(emptyList())
    }

    /**
     * 检查输入的结构是否完整（利用已缓存的 token 列表，避免逐字符扫描）
     *
     * 遍历 token 列表，检查花括号、数学模式（`$`/`$$`/`\[...\]`）、环境是否都已正确闭合。
     * 如果存在未闭合的结构，说明输入不完整（如流式输入的中间状态），
     * 此时不应更新 AST，避免显示异常的中间态。
     *
     * 性能优势（vs 旧方案）：
     * - 旧方案逐字符扫描 O(n) 其中 n = 文本长度
     * - 新方案遍历 token 列表 O(m) 其中 m = token 数量，通常 m << n
     * - 且 token 列表已由增量分词器维护，无额外分配
     */
    private fun isInputComplete(text: String): Boolean {
        if (text.isEmpty()) return true

        val tokens = tokenizer.getCurrentTokens()

        var braceDepth = 0
        var mathMode = false
        var displayMath = false
        var envDepth = 0
        var lastToken: LatexToken? = null

        for (token in tokens) {
            when (token) {
                is LatexToken.LeftBrace -> braceDepth++
                is LatexToken.RightBrace -> braceDepth = maxOf(0, braceDepth - 1)
                is LatexToken.MathShift -> {
                    if (token.count == 2) {
                        displayMath = !displayMath
                    } else {
                        mathMode = !mathMode
                    }
                }
                is LatexToken.BeginEnvironment -> envDepth++
                is LatexToken.EndEnvironment -> envDepth = maxOf(0, envDepth - 1)
                is LatexToken.EOF -> break
                else -> { /* no-op */ }
            }
            lastToken = token
        }

        // 结构不完整
        if (braceDepth > 0 || mathMode || displayMath || envDepth > 0) return false

        // 检查是否以反斜杠结尾（未完成的命令）
        // 最后一个有意义的 token 如果是单个 "\" 命令，说明命令未完成
        // 使用反向字符扫描替代 trimEnd() 字符串分配
        var i = text.length - 1
        while (i >= 0 && text[i].isWhitespace()) i--
        if (i >= 0 && text[i] == '\\') return false

        return true
    }

    /**
     * 截断文本到最近的结构安全边界
     *
     * 扫描文本，跟踪花括号深度、数学模式状态、环境嵌套，
     * 返回最后一个所有结构都已闭合的位置。
     */
    private fun truncateToSafeBoundary(text: String): String {
        if (text.isEmpty()) return ""

        var braceDepth = 0
        var mathMode = false      // 在 $ 内
        var displayMath = false   // 在 $$ 内
        var lastSafePos = 0
        var i = 0

        while (i < text.length) {
            val ch = text[i]
            when {
                ch == '\\' -> {
                    if (i + 1 < text.length && (text[i + 1] == '[' || text[i + 1] == ']')) {
                        displayMath = !displayMath
                        i += 2
                        if (!displayMath && braceDepth == 0 && !mathMode) {
                            lastSafePos = i
                        }
                        continue
                    }
                    // 跳过普通转义字符
                    i += 2
                    continue
                }

                ch == '{' -> braceDepth++
                ch == '}' -> {
                    braceDepth = maxOf(0, braceDepth - 1)
                    if (braceDepth == 0 && !mathMode && !displayMath) {
                        lastSafePos = i + 1
                    }
                }

                ch == '$' -> {
                    if (i + 1 < text.length && text[i + 1] == '$') {
                        displayMath = !displayMath
                        i += 2
                        if (!displayMath && braceDepth == 0) {
                            lastSafePos = i
                        }
                        continue
                    } else {
                        mathMode = !mathMode
                        if (!mathMode && braceDepth == 0 && !displayMath) {
                            lastSafePos = i + 1
                        }
                    }
                }
            }
            // 在所有结构闭合时更新安全位置
            if (braceDepth == 0 && !mathMode && !displayMath) {
                // 空格/换行是天然的安全点
                if (ch == ' ' || ch == '\n' || ch == '\t') {
                    lastSafePos = i + 1
                }
            }
            i++
        }

        // 如果整个文本结构都闭合了，返回完整文本
        if (braceDepth == 0 && !mathMode && !displayMath) {
            return text
        }

        return if (lastSafePos > 0) text.substring(0, lastSafePos) else ""
    }

    /**
     * 清空所有内容和状态
     */
    fun clear() {
        tokenizer.tokenize("") // 清空 tokenizer 缓存
        lastSuccessfulPosition = 0
        cachedDocument = null
        HLog.d(TAG) { "清空解析器状态" }
    }

    /**
     * 获取当前可解析的文档
     *
     * 优先返回缓存结果。如果无缓存，尝试完整解析当前输入；
     * 若完整解析失败则走截断容错（仅首次调用且无历史缓存时）。
     */
    fun getCurrentDocument(): LatexNode.Document {
        if (cachedDocument == null) {
            val text = tokenizer.getCurrentText()
            if (text.isNotEmpty()) {
                cachedDocument = fullParse(text)
            }
        }
        return cachedDocument ?: LatexNode.Document(emptyList())
    }

    /**
     * 获取当前缓冲区的完整内容
     */
    fun getCurrentInput(): String = tokenizer.getCurrentText()

    /**
     * 获取解析进度（已成功解析的字符数 / 总字符数）
     */
    fun getProgress(): Float {
        val total = tokenizer.getCurrentText().length
        return if (total > 0) {
            lastSuccessfulPosition.toFloat() / total
        } else {
            1f
        }
    }

    /**
     * 获取未解析的部分（可用于调试或显示）
     */
    fun getUnparsedContent(): String {
        val total = tokenizer.getCurrentText()
        return if (lastSuccessfulPosition < total.length) {
            total.substring(lastSuccessfulPosition)
        } else {
            ""
        }
    }
}
