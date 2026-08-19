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

import com.hrm.latex.base.log.HLog
import com.hrm.latex.parser.ParseDiagnostic
import com.hrm.latex.parser.SymbolMap
import com.hrm.latex.parser.component.handler.*
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.model.SourceRange
import com.hrm.latex.parser.tokenizer.LatexToken

internal class CommandParser(
    private val context: LatexParserContext,
    private val chemicalParser: ChemicalParser
) {
    private val tokenStream get() = context.tokenStream

    companion object {
        private const val TAG = "CommandParser"

        /**
         * 命令注册表：所有已知 LaTeX 命令的分发中心。
         *
         * 注册表是无状态的（只存储命令名→handler 的映射），
         * 所有 CommandParser 实例共享同一份，避免重复构建。
         */
        private val registry = CommandRegistry().apply {
            installFractionHandlers()
            installRootHandlers()
            installBigOperatorHandlers()
            installDelimiterHandlers()
            installStyleHandlers()
            installAccentHandlers()
            installArrowAndStackHandlers()
            installSpaceHandlers()
            installColorHandlers()
            installSpecialEffectHandlers()
            installHyperlinkHandlers()
            installMacroHandlers()
            installTableHandlers()
            installReferenceHandlers()
            installAdvancedHandlers()
            installOperatorHandlers()
            installSectionHandlers()
            installTextDirectionHandlers()
            installPackageCommandHandlers()
        }
    }

    /**
     * 解析命令
     */
    fun parseCommand(cmdName: String): LatexNode? {
        HLog.d(TAG) { "解析命令: \\$cmdName" }

        // 1. 优先检查自定义命令
        val customCmd = context.customCommands[cmdName]
        if (customCmd != null) {
            return expandCustomCommand(customCmd)
        }

        // 2. 委托给注册表分发
        val registryResult = registry.dispatch(cmdName, context, tokenStream)
        if (registryResult != null) {
            return registryResult
        }

        // 3. 化学公式（需要 chemicalParser 实例，不适合放入通用 handler）
        if (cmdName == "ce" || cmdName == "cf") {
            return chemicalParser.parseChemicalArgument()
        }

        // 4. 回退：符号查找或通用命令
        return parseSymbolOrGenericCommand(cmdName)
    }

    private fun parseSymbolOrGenericCommand(cmdName: String): LatexNode {
        if (cmdName.isEmpty()) {
            val range = tokenStream.peek(-1)?.range ?: SourceRange(0, 0)
            context.diagnostics.add(
                ParseDiagnostic(
                    range = range,
                    message = "Trailing backslash does not form a control sequence",
                    severity = ParseDiagnostic.Severity.ERROR,
                    category = ParseDiagnostic.Category.UNKNOWN_COMMAND
                )
            )
            return LatexNode.Text("\\")
        }

        val unicode = SymbolMap.getSymbol(cmdName)
        if (unicode != null) {
            return LatexNode.Symbol(cmdName, unicode)
        }

        val arguments = mutableListOf<LatexNode>()
        while (tokenStream.peek() is LatexToken.LeftBrace) {
            val arg = context.parseArgument()
            if (arg != null) {
                arguments.add(arg)
            } else {
                break
            }
        }

        return LatexNode.Command(cmdName, arguments)
    }

    /**
     * 展开自定义命令
     * 将 #1, #2, ... 替换为实际参数
     *
     * 支持可选参数默认值：当 customCmd.defaultArg 非空时，
     * 第一个参数为可选参数。如果调用时提供了 [value] 则使用 value，否则使用默认值。
     */
    private fun expandCustomCommand(customCmd: CustomCommand): LatexNode {
        val args = mutableListOf<LatexNode>()

        if (customCmd.acceptsDelimiterModifier) {
            while (tokenStream.peek() is LatexToken.Whitespace) tokenStream.advance()
            val star = tokenStream.peek() as? LatexToken.Text
            if (star?.content == "*") tokenStream.advance()
            if (tokenStream.peek() is LatexToken.LeftBracket) {
                tokenStream.advance()
                while (!tokenStream.isEOF() && tokenStream.peek() !is LatexToken.RightBracket) {
                    tokenStream.advance()
                }
                if (tokenStream.peek() is LatexToken.RightBracket) tokenStream.advance()
            }
        }

        if (customCmd.defaultArg != null && customCmd.numArgs > 0) {
            // 第一个参数是可选参数：检查是否提供了 [value]
            val firstArg = if (tokenStream.peek() is LatexToken.LeftBracket) {
                tokenStream.advance() // consume [
                val nodes = mutableListOf<LatexNode>()
                while (!tokenStream.isEOF() && tokenStream.peek() !is LatexToken.RightBracket) {
                    val node = context.parseExpression()
                    if (node != null) nodes.add(node)
                }
                if (!tokenStream.isEOF()) {
                    tokenStream.advance() // consume ]
                }
                if (nodes.size == 1) nodes[0] else LatexNode.Group(nodes)
            } else {
                // 使用默认值
                LatexNode.Text(customCmd.defaultArg)
            }
            args.add(firstArg)

            // 收集剩余的必选参数
            for (i in 1 until customCmd.numArgs) {
                val arg = context.parseArgument() ?: LatexNode.Text("")
                args.add(arg)
            }
        } else {
            // 所有参数都是必选参数
            for (i in 0 until customCmd.numArgs) {
                val arg = context.parseArgument() ?: LatexNode.Text("")
                args.add(arg)
            }
        }

        // 替换定义中的参数占位符
        val expanded = replaceParameters(customCmd.definition, args)

        // 返回 Group 包装展开的内容
        return LatexNode.Group(expanded)
    }

    /**
     * 递归替换参数占位符 #1, #2, ...
     * 利用 LatexNode 的自描述方法 children()/withChildren() 实现通用递归
     */
    private fun replaceParameters(nodes: List<LatexNode>, args: List<LatexNode>): List<LatexNode> {
        return nodes.flatMap { node ->
            when (node) {
                is LatexNode.Text -> {
                    // 替换 #1, #2, ... 为实际参数
                    val text = node.content
                    if (text.contains("#")) {
                        val result = mutableListOf<LatexNode>()
                        var i = 0
                        while (i < text.length) {
                            if (text[i] == '#' && i + 1 < text.length && text[i + 1].isDigit()) {
                                val paramNum = text[i + 1].toString().toInt()
                                if (paramNum > 0 && paramNum <= args.size) {
                                    result.add(args[paramNum - 1])
                                } else {
                                    result.add(LatexNode.Text("#${text[i + 1]}"))
                                }
                                i += 2
                            } else if (text[i] == '#') {
                                // lone # or # not followed by digit - treat as literal
                                result.add(LatexNode.Text("#"))
                                i++
                            } else {
                                val start = i
                                while (i < text.length && text[i] != '#') i++
                                if (i > start) {
                                    result.add(LatexNode.Text(text.substring(start, i)))
                                }
                            }
                        }
                        result
                    } else {
                        listOf(node)
                    }
                }
                is LatexNode.Command -> {
                    // 检查是否是另一个自定义命令需要展开
                    val nestedCmd = context.customCommands[node.name]
                    if (nestedCmd != null) {
                        val nestedArgs = node.arguments.map { replaceParametersInNode(it, args) }
                        replaceParameters(nestedCmd.definition, nestedArgs)
                    } else {
                        val expandedArgs = node.arguments.map { replaceParametersInNode(it, args) }
                        listOf(LatexNode.Command(node.name, expandedArgs))
                    }
                }
                else -> {
                    // 通用递归：利用节点自描述的 children()/withChildren()
                    val children = node.children()
                    if (children.isEmpty()) {
                        listOf(node)
                    } else {
                        val newChildren = children.map { replaceParametersInNode(it, args) }
                        listOf(node.withChildren(newChildren))
                    }
                }
            }
        }
    }

    /**
     * 替换单个节点中的参数
     */
    private fun replaceParametersInNode(node: LatexNode, args: List<LatexNode>): LatexNode {
        return when (node) {
            is LatexNode.Group -> LatexNode.Group(replaceParameters(node.children, args))
            else -> {
                val replaced = replaceParameters(listOf(node), args)
                if (replaced.size == 1) replaced[0] else LatexNode.Group(replaced)
            }
        }
    }
}
