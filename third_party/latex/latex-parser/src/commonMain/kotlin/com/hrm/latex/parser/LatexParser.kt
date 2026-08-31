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
import com.hrm.latex.parser.component.ChemicalParser
import com.hrm.latex.parser.component.CommandParser
import com.hrm.latex.parser.component.CustomCommand
import com.hrm.latex.parser.component.CustomEnvironment
import com.hrm.latex.parser.component.EnvironmentParser
import com.hrm.latex.parser.component.LatexParserContext
import com.hrm.latex.parser.component.LatexTokenStream
import com.hrm.latex.parser.component.handler.ParseUtils
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.model.SourceRange
import com.hrm.latex.parser.tokenizer.LatexToken
import com.hrm.latex.parser.tokenizer.LatexTokenizer

/**
 * LaTeX 语法解析器
 *
 * 组件化架构:
 * - [LatexTokenStream]: Token 流管理
 * - [EnvironmentParser]: 环境解析
 * - [CommandParser]: 命令解析
 */
class LatexParser {

    companion object {
        private const val TAG = "LatexParser"
    }

    /**
     * 解析 LaTeX 字符串
     */
    fun parse(input: String): LatexNode.Document {
        HLog.d(TAG) { "开始解析 LaTeX: $input" }
        val tokens = LatexTokenizer(input).tokenize()
        return ParseSession(tokens, input.length).parse()
    }

    /**
     * 解析 LaTeX 字符串，返回包含 AST 和结构化诊断信息的 ParseResult
     *
     * 相比 [parse] 方法，此方法额外返回解析过程中收集的所有诊断信息，
     * 支持诊断面板 API、错误过滤和分类查询。
     */
    fun parseWithDiagnostics(input: String): ParseResult {
        HLog.d(TAG) { "开始解析 LaTeX (带诊断): $input" }
        val tokens = LatexTokenizer(input).tokenize()
        val session = ParseSession(tokens, input.length)
        val document = session.parse()
        return ParseResult(document, session.diagnostics.toList())
    }

    /**
     * 从 token 列表直接解析（供增量解析器使用，避免二次分词）
     */
    fun parse(tokens: List<LatexToken>, inputLength: Int): LatexNode.Document {
        HLog.d(TAG) { "从 token 列表解析, token 数: ${tokens.size}" }
        return ParseSession(tokens, inputLength).parse()
    }

    class ParseException(message: String) : Exception(message)
}

/**
 * 解析会话：封装单次解析的所有可变状态。
 *
 * 每次 [LatexParser.parse] 创建一个新的 ParseSession，
 * 解析完成后即被丢弃，不存在跨调用的状态污染。
 */
internal class ParseSession(
    tokens: List<LatexToken>,
    private val inputLength: Int
) : LatexParserContext {

    companion object {
        private const val TAG = "ParseSession"
    }

    override val tokenStream = LatexTokenStream(tokens)
    override val customCommands: MutableMap<String, CustomCommand> = mutableMapOf()
    override val customEnvironments: MutableMap<String, CustomEnvironment> = mutableMapOf()
    override val diagnostics: MutableList<ParseDiagnostic> = mutableListOf()

    private val environmentParser = EnvironmentParser(this)
    private val chemicalParser = ChemicalParser(this)
    private val commandParser = CommandParser(this, chemicalParser)

    fun parse(): LatexNode.Document {
        val children = parseMathList { false }

        val document = LatexNode.Document(
            children,
            sourceRange = SourceRange(0, inputLength)
        )
        HLog.d(TAG) { "解析成功，生成 ${children.size} 个节点, 诊断: ${diagnostics.size} 条" }
        return document
    }

    override fun parseExpression(): LatexNode? {
        val startOffset = tokenStream.currentSourceOffset()
        var node = parseFactor() ?: return null

        while (true) {
            val token = tokenStream.peek()
            if (token is LatexToken.Prime) {
                val primes = mutableListOf<LatexNode>()
                while (tokenStream.peek() is LatexToken.Prime) {
                    val prime = tokenStream.advance() as LatexToken.Prime
                    primes.add(LatexNode.Symbol("prime", "′", sourceRange = prime.range))
                }
                if (node.hasSuperscript()) {
                    reportDoubleSuperscript(token.range)
                } else {
                    node = LatexNode.Superscript(
                        node,
                        LatexNode.Group(primes),
                        sourceRange = tokenStream.rangeFrom(startOffset)
                    )
                }
            } else if (token is LatexToken.Superscript) {
                tokenStream.advance()
                val exponent = parseScriptContent()
                node =
                    if (node.hasPrimeOnlySuperscript()) {
                        node.appendToPrimeSuperscript(exponent, tokenStream.rangeFrom(startOffset))
                    } else if (node.hasSuperscript()) {
                        reportDoubleSuperscript(token.range)
                        node
                    } else {
                        LatexNode.Superscript(
                            node,
                            exponent,
                            sourceRange = tokenStream.rangeFrom(startOffset)
                        )
                    }
            } else if (token is LatexToken.Subscript) {
                tokenStream.advance()
                val index = parseScriptContent()
                node = LatexNode.Subscript(
                    node, index,
                    sourceRange = tokenStream.rangeFrom(startOffset)
                )
            } else {
                break
            }
        }
        return node
    }

    private fun LatexNode.hasSuperscript(): Boolean =
        when (this) {
            is LatexNode.Superscript -> true
            is LatexNode.Subscript -> base.hasSuperscript()
            else -> false
        }

    private fun LatexNode.hasPrimeOnlySuperscript(): Boolean =
        this is LatexNode.Superscript && exponent.isPrimeOnly()

    private fun LatexNode.isPrimeOnly(): Boolean =
        this is LatexNode.Group &&
            children.isNotEmpty() &&
            children.all { child -> child is LatexNode.Symbol && child.symbol == "prime" }

    private fun LatexNode.appendToPrimeSuperscript(
        exponent: LatexNode,
        range: SourceRange
    ): LatexNode =
        (this as LatexNode.Superscript).copy(
            exponent = LatexNode.Group((this.exponent as LatexNode.Group).children + exponent),
            sourceRange = range
        )

    private fun reportDoubleSuperscript(range: SourceRange) {
        diagnostics.add(
            ParseDiagnostic(
                range = range,
                message = "Double superscript",
                severity = ParseDiagnostic.Severity.ERROR,
                category = ParseDiagnostic.Category.INVALID_ARGUMENT
            )
        )
    }

    override fun parseFactor(): LatexNode? {
        when (val token = tokenStream.peek()) {
            is LatexToken.Text -> {
                tokenStream.advance()
                return LatexNode.Text(token.content, sourceRange = token.range)
            }

            is LatexToken.Command -> {
                val cmdStart = token.range.start
                tokenStream.advance()
                val result = commandParser.parseCommand(token.name)
                return if (result?.sourceRange == null && result != null) {
                    result.withSourceRange(tokenStream.rangeFrom(cmdStart))
                } else {
                    result
                }
            }

            is LatexToken.BeginEnvironment -> {
                return environmentParser.parseEnvironment()
            }

            is LatexToken.LeftBrace -> {
                return parseGroup()
            }

            is LatexToken.Superscript, is LatexToken.Subscript -> {
                tokenStream.advance()
                return null
            }

            is LatexToken.Whitespace -> {
                tokenStream.advance()
                return LatexNode.Space(LatexNode.Space.SpaceType.NORMAL, sourceRange = token.range)
            }

            is LatexToken.NewLine -> {
                tokenStream.advance()
                return LatexNode.NewLine(sourceRange = token.range)
            }

            is LatexToken.LeftBracket -> {
                tokenStream.advance()
                return LatexNode.Text("[", sourceRange = token.range)
            }

            is LatexToken.RightBracket -> {
                tokenStream.advance()
                return LatexNode.Text("]", sourceRange = token.range)
            }

            is LatexToken.Ampersand -> {
                tokenStream.advance()
                return LatexNode.Text("&", sourceRange = token.range)
            }

            is LatexToken.Prime -> {
                tokenStream.advance()
                diagnostics.add(
                    ParseDiagnostic(
                        range = token.range,
                        message = "Prime requires a base",
                        severity = ParseDiagnostic.Severity.WARNING,
                        category = ParseDiagnostic.Category.UNEXPECTED_TOKEN
                    )
                )
                return LatexNode.Symbol("prime", "′", sourceRange = token.range)
            }

            is LatexToken.MathShift -> {
                return parseMathMode(token)
            }

            is LatexToken.EOF -> return null
            else -> {
            // P1: 记录诊断而非静默丢弃
                val range = token?.range
                if (range != null) {
                    diagnostics.add(
                        ParseDiagnostic(
                            range = range,
                            message = "Unexpected token: $token",
                            severity = ParseDiagnostic.Severity.WARNING,
                            category = ParseDiagnostic.Category.UNEXPECTED_TOKEN
                        )
                    )
                }
                tokenStream.advance()
                return null
            }
        }
    }

    override fun parseGroup(): LatexNode.Group {
        val startOffset = tokenStream.currentSourceOffset()
        tokenStream.expect("{")
        val children = parseMathList { it is LatexToken.RightBrace }

        if (!tokenStream.isEOF()) {
            tokenStream.expect("}")
        }
        return LatexNode.Group(children, sourceRange = tokenStream.rangeFrom(startOffset))
    }

    private fun parseMathList(isTerminator: (LatexToken?) -> Boolean): List<LatexNode> {
        val listStart = tokenStream.currentSourceOffset()
        val children = mutableListOf<LatexNode>()
        var infixNumerator: List<LatexNode>? = null
        var infixRange: SourceRange? = null
        var infixCommand: String? = null
        var aboveDimension: String? = null

        while (!tokenStream.isEOF() && !isTerminator(tokenStream.peek())) {
            val token = tokenStream.peek()
            if (token is LatexToken.Command &&
                token.name in setOf("over", "atop", "choose", "above") &&
                infixNumerator == null
            ) {
                tokenStream.advance()
                infixNumerator = normalizeStyleDeclarations(children.toList())
                infixRange = token.range
                infixCommand = token.name
                if (token.name == "above") {
                    aboveDimension = ParseUtils.parseDimension(this, tokenStream)
                }
                children.clear()
                continue
            }
            val node = parseExpression()
            if (node != null) {
                children.add(node)
            }
        }

        val normalizedChildren = normalizeStyleDeclarations(children)
        return infixNumerator?.let { numerator ->
            val separatorRange = requireNotNull(infixRange)
            val listEnd = tokenStream.previousEndOffset()
            val top = LatexNode.Group(
                numerator,
                sourceRange = SourceRange(listStart, separatorRange.start)
            )
            val bottom = LatexNode.Group(
                normalizedChildren,
                sourceRange = SourceRange(separatorRange.end, listEnd)
            )
            val combinedRange = SourceRange(listStart, listEnd)
            val combined = when (infixCommand) {
                "choose" -> LatexNode.Binomial(
                    top = top,
                    bottom = bottom,
                    sourceRange = combinedRange
                )
                "atop" -> LatexNode.Fraction(
                    numerator = top,
                    denominator = bottom,
                    style = LatexNode.Fraction.FractionStyle.RULELESS,
                    sourceRange = combinedRange
                )
                "above" -> {
                    val zeroRule = aboveDimension.orEmpty().matches(
                        Regex("""[+-]?0+(?:\.0+)?(?:[A-Za-z]+)?""")
                    )
                    LatexNode.Fraction(
                        numerator = top,
                        denominator = bottom,
                        style = if (zeroRule) {
                            LatexNode.Fraction.FractionStyle.RULELESS
                        } else {
                            LatexNode.Fraction.FractionStyle.NORMAL
                        },
                        sourceRange = combinedRange
                    )
                }
                else -> LatexNode.Fraction(
                    numerator = top,
                    denominator = bottom,
                    style = LatexNode.Fraction.FractionStyle.NORMAL,
                    sourceRange = combinedRange
                )
            }
            listOf(combined)
        } ?: normalizedChildren
    }

    override fun parseArgument(): LatexNode? {
        while (tokenStream.peek() is LatexToken.Whitespace) {
            tokenStream.advance()
        }
        return when (tokenStream.peek()) {
            is LatexToken.LeftBrace -> parseGroup()
            is LatexToken.Text -> {
                val token = tokenStream.consumeTextAtom() ?: return null
                LatexNode.Text(token.content, sourceRange = token.range)
            }
            else -> parseFactor()
        }
    }

    private fun parseMathMode(openToken: LatexToken.MathShift): LatexNode {
        val startOffset = openToken.range.start
        val count = openToken.count
        tokenStream.advance()

        val children = parseMathList { token ->
            token is LatexToken.MathShift && token.count == count
        }
        val closingToken = tokenStream.peek()
        if (closingToken is LatexToken.MathShift && closingToken.count == count) {
            tokenStream.advance()
        }

        val range = tokenStream.rangeFrom(startOffset)
        return if (count == 2) {
            LatexNode.DisplayMath(children, sourceRange = range)
        } else {
            LatexNode.InlineMath(children, sourceRange = range)
        }
    }

    private fun parseScriptContent(): LatexNode {
        return parseArgument() ?: LatexNode.Text("")
    }

    override fun normalizeStyleDeclarations(nodes: List<LatexNode>): List<LatexNode> {
        if (nodes.none { it.isStyleDeclaration() }) return nodes
        val result = mutableListOf<LatexNode>()
        val activeDeclarations = mutableListOf<LatexNode>()
        val segment = mutableListOf<LatexNode>()

        fun flushSegment() {
            if (segment.isEmpty()) return
            if (activeDeclarations.isEmpty()) {
                result.addAll(segment)
            } else {
                result.add(wrapWithDeclarations(segment.toList(), activeDeclarations))
            }
            segment.clear()
        }

        var index = 0
        while (index < nodes.size) {
            val node = nodes[index]
            if (node.isStyleDeclaration()) {
                flushSegment()
                updateActiveDeclaration(activeDeclarations, node)
                index++
                while (index < nodes.size && nodes[index] is LatexNode.Space) {
                    index++
                }
            } else {
                segment.add(node)
                index++
            }
        }
        flushSegment()
        return result
    }

    private fun LatexNode.isStyleDeclaration(): Boolean = when (this) {
        is LatexNode.Style -> content.isEmpty()
        is LatexNode.MathStyle -> content.isEmpty()
        is LatexNode.FontSize -> content.isEmpty()
        else -> false
    }

    private fun updateActiveDeclaration(activeDeclarations: MutableList<LatexNode>, declaration: LatexNode) {
        val existingIndex = activeDeclarations.indexOfLast { existing ->
            (existing is LatexNode.Style && declaration is LatexNode.Style) ||
                (existing is LatexNode.MathStyle && declaration is LatexNode.MathStyle) ||
                (existing is LatexNode.FontSize && declaration is LatexNode.FontSize)
        }
        if (existingIndex >= 0) {
            activeDeclarations[existingIndex] = declaration
        } else {
            activeDeclarations.add(declaration)
        }
    }

    private fun wrapWithDeclarations(content: List<LatexNode>, declarations: List<LatexNode>): LatexNode {
        var node: LatexNode = if (content.size == 1) content[0] else LatexNode.Group(content, mergeRange(content))
        for (declaration in declarations.asReversed()) {
            node = when (declaration) {
                is LatexNode.Style -> declaration.copy(
                    content = listOf(node),
                    sourceRange = declaration.sourceRange.mergeWith(node.sourceRange)
                )
                is LatexNode.MathStyle -> declaration.copy(
                    content = listOf(node),
                    sourceRange = declaration.sourceRange.mergeWith(node.sourceRange)
                )
                is LatexNode.FontSize -> declaration.copy(
                    content = listOf(node),
                    sourceRange = declaration.sourceRange.mergeWith(node.sourceRange)
                )
                else -> node
            }
        }
        return node
    }

    private fun mergeRange(nodes: List<LatexNode>): SourceRange? {
        return nodes.mapNotNull { it.sourceRange }.reduceOrNull { acc, range -> acc.merge(range) }
    }

    private fun SourceRange?.mergeWith(other: SourceRange?): SourceRange? = when {
        this != null && other != null -> merge(other)
        this != null -> this
        else -> other
    }
}
