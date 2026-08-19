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


package com.hrm.latex.parser.component

import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.tokenizer.LatexToken

/**
 * 解析 \ce{...} 化学公式
 */
internal class ChemicalParser(private val context: LatexParserContext) {
    private val tokenStream get() = context.tokenStream

    companion object {
        private const val TAG = "ChemicalParser"
    }

    fun parseChemicalArgument(): LatexNode {
        // \ce 命令的参数通常用 {} 包裹
        if (tokenStream.peek() is LatexToken.LeftBrace) {
            tokenStream.advance() // eat {
            val content = parseChemicalContent(stopAtRightBrace = true)
            if (tokenStream.peek() is LatexToken.RightBrace) {
                tokenStream.advance() // eat }
            }
            return LatexNode.Group(content)
        } else {
            // 如果没有 {}，则只解析下一个 token (不太常见，但兼容标准 LaTeX 命令行为)
            // 但 \ce 通常必须带 {} 因为内部语法复杂。
            // 简单处理：如果只是单个 token，尝试解析它
            val content = parseChemicalContent(stopAtRightBrace = false, singleToken = true)
            return LatexNode.Group(content)
        }
    }

    private fun parseChemicalContent(
        stopAtRightBrace: Boolean,
        singleToken: Boolean = false
    ): List<LatexNode> {
        val nodes = mutableListOf<LatexNode>()
        // 跟踪上一个添加的节点，用于附着上下标
        // 注意：由于 nodes 列表可变，我们可以直接操作 nodes.last()
        // 状态标记
        var expectCoefficient = true // 是否期望系数（行首或空格后）

        while (!tokenStream.isEOF()) {
            val token = tokenStream.peek()

            if (stopAtRightBrace && token is LatexToken.RightBrace) {
                break
            }

            when (token) {
                is LatexToken.Text -> {
                    // 普通 tokenizer 会把 + - = < > 等运算符拆成独立 Text token。
                    // mhchem 需要把连续片段重新拼成一个词法单元，才能识别
                    // Fe3+、=>、<=> 等跨 token 结构。
                    parseChemicalText(consumeChemicalTextRun(), nodes, expectCoefficient)
                    expectCoefficient = false
                }

                is LatexToken.Whitespace -> {
                    tokenStream.advance()
                    nodes.add(LatexNode.Space(LatexNode.Space.SpaceType.NORMAL))
                    expectCoefficient = true
                }

                is LatexToken.Subscript -> {
                    // 显式下标 _
                    tokenStream.advance()
                    val index = parseChemicalScriptContent()
                    attachSubscript(nodes, index)
                    expectCoefficient = false
                }

                is LatexToken.Superscript -> {
                    // 显式上标 ^
                    tokenStream.advance()

                    // 特殊情况：单独的 ^ 表示气体逸出（向上箭头）
                    // 判断方法：后面是空格、右括号、或 EOF
                    val nextToken = tokenStream.peek()
                    if (nextToken is LatexToken.Whitespace ||
                        nextToken is LatexToken.RightBrace ||
                        tokenStream.isEOF()
                    ) {

                        // 优化间距：如果前面有普通空格，减小它
                        shrinkLastNormalSpace(nodes)
                        nodes.add(LatexNode.Symbol("uparrow", "↑"))
                        expectCoefficient = true
                    } else {
                        // 普通上标
                        val exponent = parseChemicalScriptContent()
                        attachSuperscript(nodes, exponent)
                        expectCoefficient = false
                    }
                }

                is LatexToken.Command -> {
                    // 内部命令，如 \frac，转交给 context 处理
                    // 注意：不要在这里 advance，让 parseExpression 去消费
                    val node = context.parseExpression()
                    if (node != null) nodes.add(node)
                    expectCoefficient = false
                }

                else -> {
                    // 其他 token，如 Group ({), Bracket ([) 等
                    if (token is LatexToken.LeftBrace) {
                        // 递归解析 Group
                        val group = context.parseGroup()
                        nodes.add(group)
                        expectCoefficient = false
                    } else if (token is LatexToken.LeftBracket) {
                        val direction = (nodes.lastOrNull() as? LatexNode.Symbol)?.let(::arrowDirection)
                        if (direction != null) {
                            nodes.removeAt(nodes.lastIndex)
                            val above = parseChemicalAnnotation()
                            val below = if (tokenStream.peek() is LatexToken.LeftBracket) {
                                parseChemicalAnnotation()
                            } else null
                            nodes.add(LatexNode.ExtensibleArrow(above, below, direction))
                        } else {
                            nodes.add(LatexNode.Text("["))
                            tokenStream.advance()
                        }
                        expectCoefficient = false
                    } else if (token is LatexToken.RightBracket) {
                        nodes.add(LatexNode.Text("]"))
                        tokenStream.advance()
                        expectCoefficient = false
                    } else {
                        // Fallback
                        val node = context.parseExpression()
                        if (node != null) nodes.add(node)
                        expectCoefficient = false
                    }
                }
            }

            if (singleToken) break
        }
        return nodes
    }

    // 特殊处理 Command 的分支逻辑
    // 上面的 when 分支有点乱，特别是 Command 和 else。
    // 我们可以简化：优先处理 Text/Space/Sub/Super，其他交给 context.parseExpression

    private fun parseChemicalScriptContent(): LatexNode {
        // 化学脚本内容也可能包含化学语法？mhchem 中 ^{2+} 是合法的。
        return when (tokenStream.peek()) {
            is LatexToken.LeftBrace -> context.parseGroup()
            is LatexToken.Text -> {
                val first = tokenStream.advance() as LatexToken.Text
                val content = buildString {
                    append(first.content)
                    // 无花括号电荷（如 ^2-）在 mhchem 中是一个整体；只追加
                    // 紧随其后的符号，避免吞掉后续元素或表达式。
                    if (first.content.all { it.isDigit() }) {
                        while (true) {
                            val next = tokenStream.peek() as? LatexToken.Text ?: break
                            if (next.content.any { it != '+' && it != '-' }) break
                            append(next.content)
                            tokenStream.advance()
                        }
                    }
                }
                LatexNode.Text(content)
            }
            else -> context.parseExpression() ?: LatexNode.Text("")
        }
    }

    private fun consumeChemicalTextRun(): String = buildString {
        while (true) {
            val token = tokenStream.peek() as? LatexToken.Text ?: break
            append(token.content)
            tokenStream.advance()
        }
    }

    private fun parseChemicalText(text: String, nodes: MutableList<LatexNode>, isStart: Boolean) {
        var i = 0
        var expectCoef = isStart

        while (i < text.length) {
            val char = text[i]

            // 1. 检查箭头
            // 1.1 可逆箭头 <-> 或 <=>
            if (char == '<') {
                if (i + 2 < text.length) {
                    val twoChars = text.substring(i + 1, i + 3)
                    if (twoChars == "->") {
                        nodes.add(LatexNode.Symbol("leftrightarrow", "↔"))
                        i += 3
                        expectCoef = true
                        continue
                    } else if (twoChars == "=>") {
                        nodes.add(LatexNode.Symbol("Leftrightarrow", "⇔"))
                        i += 3
                        expectCoef = true
                        continue
                    }
                }
                // 如果不是可逆箭头，继续处理 <-(左箭头)
                if (i + 1 < text.length && text[i + 1] == '-') {
                    nodes.add(LatexNode.Symbol("leftarrow", "←"))
                    i += 2
                    expectCoef = true
                    continue
                }
                // 否则作为普通字符处理（不太常见）
            }

            // 1.2 单向箭头 ->
            if (char == '-' && i + 1 < text.length && text[i + 1] == '>') {
                nodes.add(LatexNode.Symbol("rightarrow", "→"))
                i += 2
                expectCoef = true
                continue
            }

            // 1.3 双线箭头 =>
            if (char == '=' && i + 1 < text.length && text[i + 1] == '>') {
                nodes.add(LatexNode.Symbol("Rightarrow", "⇒"))
                i += 2
                expectCoef = true
                continue
            }

            // 1.4 结晶水连接符 * 或 .
            if (char == '*' || char == '.') {
                nodes.add(LatexNode.Symbol("cdot", "·"))
                i++
                expectCoef = true
                continue
            }

            // 1.5 化学键。箭头已在前面的分支优先识别。
            if (char == '#' || char == '=' || char == '~' ||
                (char == '-' && i + 1 < text.length && (text[i + 1].isLetter() || text[i + 1] == '('))
            ) {
                val (name, symbol) = when (char) {
                    '#' -> "triplebond" to "≡"
                    '=' -> "doublebond" to "="
                    '~' -> "aromaticbond" to "∼"
                    else -> "singlebond" to "−"
                }
                nodes.add(LatexNode.Symbol(name, symbol))
                i++
                expectCoef = true
                continue
            }

            // 2. 数字处理
            if (char.isDigit()) {
                // 提取连续数字
                val start = i
                while (i < text.length && text[i].isDigit()) {
                    i++
                }
                val numStr = text.substring(start, i)

                if (expectCoef) {
                    // 系数：作为普通文本添加
                    nodes.add(LatexNode.Text(numStr))
                    expectCoef = false
                } else {
                    // 下标：检查是否要处理为电荷（如 Fe3+）
                    // mhchem 规则：数字后跟 +/- 是上标电荷
                    var isCharge = false
                    if (i < text.length) {
                        val nextChar = text[i]
                        if (nextChar == '+' || nextChar == '-') {
                            isCharge = true
                        }
                    }

                    if (isCharge) {
                        // 将数字作为上标的一部分，留给 +/- 处理逻辑，或者预读？
                        // 简单起见，我们在这里吞掉后面的 +/-
                        val chargeStart = i
                        while (i < text.length && (text[i] == '+' || text[i] == '-')) {
                            i++
                        }
                        val chargeStr = numStr + text.substring(chargeStart, i)
                        attachSuperscript(nodes, LatexNode.Text(chargeStr))
                    } else {
                        // 普通下标
                        attachSubscript(nodes, LatexNode.Text(numStr))
                    }
                }
                continue
            }

            // 3. 电荷 +/- (孤立的，如 Na+)
            if (char == '+' || char == '-') {
                // 如果前一个节点是元素，且不是系数位置？
                // mhchem: H+ -> H^+
                // 1+ -> 1^+ ? No, 1+ usually means +1 charge?
                // 简化：总是作为上标
                // 但如果是方程式 A + B -> C，+ 是运算符
                // 判断上下文：如果在 Space 之后，或是 Start，则是运算符。
                // 如果紧跟在元素/右括号/下标之后，则是电荷。

                if (expectCoef) {
                    // 运算符模式
                    if (char == '+') {
                        nodes.add(LatexNode.Symbol("plus", "+"))
                    } else {
                        // - 可能是负号或箭头的一部分（上面已处理箭头）
                        nodes.add(LatexNode.Symbol("minus", "-"))
                    }
                } else {
                    // 电荷模式
                    // 吞掉连续的 +/-
                    val start = i
                    while (i < text.length && (text[i] == '+' || text[i] == '-')) {
                        i++
                    }
                    val chargeStr = text.substring(start, i)
                    attachSuperscript(nodes, LatexNode.Text(chargeStr))
                    continue // i 已经增加了
                }
                i++
                continue
            }

            // 4. 字母 (元素)
            if (char.isLetter()) {
                // 特殊情况：单独的 'v' 表示沉淀（向下箭头）
                if (char == 'v' &&
                    (i == text.length - 1 || text[i + 1] == ' ' || !text[i + 1].isLetter())
                ) {
                    shrinkLastNormalSpace(nodes)
                    nodes.add(LatexNode.Symbol("downarrow", "↓"))
                    i++
                    expectCoef = true
                    continue
                }

                // H2O 中: H 是元素, 2 是下标, O 是元素
                // Fe3+ 中: F 是元素, e 也是元素（Fe是一个元素），3+ 是上标
                // 但这里我们逐字符解析，所以 F 和 e 分别是独立的字母
                // 改进：判断是否是元素符号（大写字母 + 可选小写字母）
                // 化学式中的字母应该使用正体（Roman），所以包装为 TextMode
                if (char.isUpperCase() && i + 1 < text.length && text[i + 1].isLowerCase()) {
                    // 双字母元素,如 Fe, Na, Cl
                    nodes.add(LatexNode.TextMode(text.substring(i, i + 2)))
                    i += 2
                } else {
                    // 单字母元素
                    nodes.add(LatexNode.TextMode(char.toString()))
                    i++
                }
                expectCoef = false
                continue
            }

            // 5. 其他字符 (如括号)
            nodes.add(LatexNode.Text(char.toString()))
            expectCoef = false // 括号后通常不是系数
            i++
        }
    }

    private fun attachSubscript(nodes: MutableList<LatexNode>, index: LatexNode) {
        if (nodes.isNotEmpty()) {
            val last = nodes.removeAt(nodes.lastIndex)
            // 如果 last 已经是 Subscript/Superscript 怎么处理？
            // 如 SO_4^2-，先处理 _4 -> Subscript(O, 4)
            // 再处理 ^2- -> Superscript(Subscript(O, 4), 2-)
            // 这是合理的嵌套顺序。

            // 这里的 Subscript 构造函数参数顺序：base, index
            nodes.add(LatexNode.Subscript(last, index))
        } else {
            // 没有 Base，可能是前置下标（如同位素）
            nodes.add(LatexNode.Subscript(LatexNode.Text(""), index))
        }
    }

    private fun parseChemicalAnnotation(): LatexNode {
        tokenStream.advance() // [
        val content = mutableListOf<LatexNode>()
        while (!tokenStream.isEOF() && tokenStream.peek() !is LatexToken.RightBracket) {
            context.parseExpression()?.let(content::add)
        }
        if (tokenStream.peek() is LatexToken.RightBracket) tokenStream.advance()
        return if (content.size == 1) content[0] else LatexNode.Group(content)
    }

    private fun arrowDirection(symbol: LatexNode.Symbol): LatexNode.ExtensibleArrow.Direction? =
        when (symbol.symbol) {
            "rightarrow" -> LatexNode.ExtensibleArrow.Direction.RIGHT
            "leftarrow" -> LatexNode.ExtensibleArrow.Direction.LEFT
            "leftrightarrow" -> LatexNode.ExtensibleArrow.Direction.BOTH
            "Rightarrow" -> LatexNode.ExtensibleArrow.Direction.RIGHT_DOUBLE
            "Leftrightarrow" -> LatexNode.ExtensibleArrow.Direction.BOTH_DOUBLE
            else -> null
        }

    private fun attachSuperscript(nodes: MutableList<LatexNode>, exponent: LatexNode) {
        if (nodes.isNotEmpty()) {
            val last = nodes.removeAt(nodes.lastIndex)
            nodes.add(LatexNode.Superscript(last, exponent))
        } else {
            nodes.add(LatexNode.Superscript(LatexNode.Text(""), exponent))
        }
    }

    private fun shrinkLastNormalSpace(nodes: MutableList<LatexNode>) {
        if (nodes.isNotEmpty()) {
            val last = nodes.last()
            if (last is LatexNode.Space && last.type == LatexNode.Space.SpaceType.NORMAL) {
                nodes.removeAt(nodes.lastIndex)
            }
        }
    }
}
