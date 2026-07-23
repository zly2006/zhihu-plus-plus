package com.hrm.markdown.parser.ast

import com.hrm.markdown.parser.SourceRange
import com.hrm.markdown.parser.LineRange

/**
 * 所有 AST 节点的基类。
 * 节点通过父子关系形成树结构。
 *
 * Tree-sitter 风格增量解析支持：
 * - 每个节点携带 [contentHash]，用于快速判断内容是否改变。
 * - 增量解析时，若旧节点与新源文本对应区域的哈希一致，
 *   则直接复用旧子树，无需重新解析。
 */
sealed class Node {
    var sourceRange: SourceRange = SourceRange.EMPTY
    var lineRange: LineRange = LineRange(0, 0)
    var parent: Node? = null

    /**
     * 内容哈希：基于该节点所覆盖的源文本行内容计算。
     * 用于增量解析时快速判断节点是否可复用。
     */
    var contentHash: Long = 0L

    /**
     * 用于 Compose `key()` 的稳定身份标识。
     * 仅基于起始行号，确保：
     * - 同一位置的块始终被 Compose 视为同一组件实例 → 复用组件、只更新内容
     * - 不同位置的块 key 不同 → 正确分辨不同组件
     *
     * 注意：不包含 contentHash，因为流式输入时内容每个 token 都在变，
     * 如果 key 跟着变，Compose 会销毁旧组件再创建新组件，导致"闪缩"和状态丢失。
     */
    open val stableKey: Int
        get() = lineRange.startLine

    /**
     * 接受访问者进行树遍历。
     */
    abstract fun <R> accept(visitor: NodeVisitor<R>): R
}

/**
 * 容器节点，可以包含子节点。
 *
 * 支持按需内联解析（Lazy Inline Parsing）：
 * 块级解析完成后，行内内容可以延迟到首次访问 [children] 时才解析。
 * 这优化了 IDE 语法高亮场景（只解析可见块）和长文档预览（分页 + 按需解析）。
 *
 * 通过 [setLazyInlineContent] 设置延迟解析内容，首次访问 [children] 时自动触发。
 */
sealed class ContainerNode : Node() {
    private val _children: MutableList<Node> = mutableListOf()

    /**
     * 延迟行内解析的原始内容。设置后，首次访问 [children] 时自动触发行内解析。
     */
    private var _lazyInlineContent: String? = null

    /**
     * 行内解析器引用。与 [_lazyInlineContent] 配合使用。
     */
    private var _lazyInlineParser: ((String, ContainerNode) -> Unit)? = null

    /**
     * 是否已完成延迟行内解析。
     */
    private var _inlineParsed: Boolean = false

    /**
     * 子节点列表。
     *
     * 若设置了延迟行内解析内容（通过 [setLazyInlineContent]），
     * 首次访问时会自动触发行内解析。
     */
    val children: List<Node>
        get() {
            ensureInlineParsed()
            return _children
        }

    /**
     * 设置延迟行内解析内容。
     *
     * 调用后，行内内容不会立即解析，而是在首次访问 [children] 时触发。
     * 适用于 Paragraph、Heading、SetextHeading、TableCell、DefinitionTerm 等
     * 需要行内解析的块级节点。
     *
     * @param content 待解析的行内原始文本
     * @param parser 行内解析函数，接收 (content, parent) 并将解析结果追加到 parent
     */
    fun setLazyInlineContent(content: String, parser: (String, ContainerNode) -> Unit) {
        _lazyInlineContent = content
        _lazyInlineParser = parser
        _inlineParsed = false
    }

    /**
     * 行内内容是否已解析。
     */
    val isInlineParsed: Boolean get() = _inlineParsed

    /**
     * 确保行内内容已被解析。若有延迟内容则立即执行解析。
     */
    private fun ensureInlineParsed() {
        if (!_inlineParsed && _lazyInlineContent != null) {
            _inlineParsed = true
            val content = _lazyInlineContent!!
            val parser = _lazyInlineParser!!
            _lazyInlineContent = null
            _lazyInlineParser = null
            if (content.isNotEmpty()) {
                parser(content, this)
            }
        }
    }

    fun appendChild(child: Node) {
        child.parent = this
        _children.add(child)
    }

    fun insertChild(index: Int, child: Node) {
        child.parent = this
        _children.add(index, child)
    }

    fun removeChild(child: Node): Boolean {
        child.parent = null
        return _children.remove(child)
    }

    fun removeChildAt(index: Int): Node {
        val child = _children.removeAt(index)
        child.parent = null
        return child
    }

    fun replaceChild(old: Node, new: Node) {
        val index = _children.indexOf(old)
        if (index >= 0) {
            old.parent = null
            new.parent = this
            _children[index] = new
        }
    }

    fun replaceChildren(startIndex: Int, endIndex: Int, newChildren: List<Node>) {
        for (i in startIndex until endIndex) {
            _children[i].parent = null
        }
        val removed = endIndex - startIndex
        for (i in 0 until removed) {
            _children.removeAt(startIndex)
        }
        newChildren.forEachIndexed { i, child ->
            child.parent = this
            _children.add(startIndex + i, child)
        }
    }

    fun clearChildren() {
        _children.forEach { it.parent = null }
        _children.clear()
        // 同时清除 lazy 状态，避免 clear 后重新触发旧的 lazy 解析
        _lazyInlineContent = null
        _lazyInlineParser = null
        _inlineParsed = false
    }

    fun childCount(): Int {
        ensureInlineParsed()
        return _children.size
    }
}

/**
 * 叶子节点，不能包含子节点，仅包含文本内容。
 */
sealed class LeafNode : Node() {
    abstract val literal: String
}
