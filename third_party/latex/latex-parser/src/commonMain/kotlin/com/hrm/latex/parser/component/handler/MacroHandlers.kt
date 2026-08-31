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

package com.hrm.latex.parser.component.handler

import com.hrm.latex.base.log.HLog
import com.hrm.latex.parser.component.CustomCommand
import com.hrm.latex.parser.component.CustomEnvironment
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.tokenizer.LatexToken

private const val TAG = "MacroHandlers"

/**
 * 宏定义命令：\newcommand, \renewcommand, \def, \DeclareMathOperator
 */
internal fun CommandRegistry.installMacroHandlers() {
    // \newcommand, \renewcommand
    val newCommandHandler = CommandHandler { _, ctx, stream ->
        val commandName = parseCommandName(stream) ?: return@CommandHandler LatexNode.Text("")

        // 可选参数 [numArgs]
        var numArgs = 0
        var defaultArg: String? = null
        if (stream.peek() is LatexToken.LeftBracket) {
            stream.advance() // [
            val numNodes = ParseUtils.parseUntil(ctx, stream) { it is LatexToken.RightBracket }
            if (!stream.isEOF()) {
                stream.expect("]")
            }
            numArgs = ParseUtils.extractText(numNodes).toIntOrNull() ?: 0

            // 可选默认值 [defaultArg]（紧跟在 [numArgs] 后面的第二个方括号参数）
            if (stream.peek() is LatexToken.LeftBracket) {
                stream.advance() // [
                val defaultNodes = ParseUtils.parseUntil(ctx, stream) { it is LatexToken.RightBracket }
                if (!stream.isEOF()) {
                    stream.expect("]")
                }
                defaultArg = ParseUtils.extractText(defaultNodes)
            }
        }

        // 定义 {definition}
        val defArg = ctx.parseArgument() ?: return@CommandHandler LatexNode.Text("")
        val definition = when (defArg) {
            is LatexNode.Group -> defArg.children
            else -> listOf(defArg)
        }

        ctx.customCommands[commandName] = CustomCommand(commandName, numArgs, definition, defaultArg)
        HLog.d(TAG) { "注册自定义命令: \\$commandName[$numArgs]${if (defaultArg != null) "[default=$defaultArg]" else ""}" }
        LatexNode.NewCommand(commandName, numArgs, definition, defaultArg)
    }

    register("newcommand", "renewcommand", handler = newCommandHandler)

    // \def
    register("def") { _, ctx, stream ->
        val nameToken = if (!stream.isEOF()) stream.advance() else null
        val commandName = when (nameToken) {
            is LatexToken.Command -> nameToken.name
            is LatexToken.Text -> nameToken.content.removePrefix("\\")
            else -> return@register LatexNode.Text("\\def")
        }

        // 计算参数个数
        var numArgs = 0
        while (!stream.isEOF()) {
            val token = stream.peek()
            if (token is LatexToken.Text && token.content.startsWith("#")) {
                stream.advance()
                val argNum = token.content.removePrefix("#").toIntOrNull()
                if (argNum != null && argNum > numArgs) numArgs = argNum
            } else {
                break
            }
        }

        // 解析定义 {body}
        val defArg = ctx.parseArgument() ?: return@register LatexNode.Text("\\def")
        val definition = when (defArg) {
            is LatexNode.Group -> defArg.children
            else -> listOf(defArg)
        }

        ctx.customCommands[commandName] = CustomCommand(commandName, numArgs, definition)
        HLog.d(TAG, "注册自定义命令 (def): \\$commandName[$numArgs]")
        LatexNode.NewCommand(commandName, numArgs, definition)
    }

    // \DeclareMathOperator
    register("DeclareMathOperator") { _, ctx, _ ->
        val commandName = parseCommandName(ctx.tokenStream) ?: return@register LatexNode.Text("")

        val opArg = ctx.parseArgument() ?: return@register LatexNode.Text("")
        val operatorName = when (opArg) {
            is LatexNode.Text -> opArg.content
            is LatexNode.Group -> ParseUtils.extractText(opArg.children)
            else -> ""
        }

        val definition = listOf(LatexNode.OperatorName(operatorName))
        ctx.customCommands[commandName] = CustomCommand(commandName, 0, definition)
        HLog.d(TAG) { "注册运算符: \\$commandName → operatorname{$operatorName}" }
        LatexNode.NewCommand(commandName, 0, definition)
    }

    // mathtools: \DeclarePairedDelimiter{\name}{left}{right}
    // The declared command expands through the existing custom-command machinery.
    register("DeclarePairedDelimiter") { _, ctx, stream ->
        val commandName = parseCommandName(stream) ?: return@register LatexNode.Text("")
        val left = ParseUtils.extractDelimiter(ctx.parseArgument() ?: LatexNode.Text(""))
        val right = ParseUtils.extractDelimiter(ctx.parseArgument() ?: LatexNode.Text(""))
        val definition = listOf(
            LatexNode.Delimited(left, right, listOf(LatexNode.Text("#1")))
        )
        ctx.customCommands[commandName] = CustomCommand(
            commandName,
            1,
            definition,
            acceptsDelimiterModifier = true
        )
        HLog.d(TAG) { "注册配对定界符: \\$commandName → $left…$right" }
        LatexNode.NewCommand(commandName, 1, definition)
    }

    // \newenvironment, \renewenvironment
    val newEnvHandler = CommandHandler { _, ctx, stream ->
        val nameArg = ctx.parseArgument() ?: return@CommandHandler LatexNode.Text("")
        val envName = ParseUtils.extractText(
            when (nameArg) {
                is LatexNode.Group -> nameArg.children
                else -> listOf(nameArg)
            }
        ).trim()

        // 可选参数 [numArgs]
        var numArgs = 0
        var defaultArg: String? = null
        if (stream.peek() is LatexToken.LeftBracket) {
            stream.advance() // [
            val numNodes = ParseUtils.parseUntil(ctx, stream) { it is LatexToken.RightBracket }
            if (!stream.isEOF()) {
                stream.expect("]")
            }
            numArgs = ParseUtils.extractText(numNodes).toIntOrNull() ?: 0

            // 可选默认值 [defaultArg]
            if (stream.peek() is LatexToken.LeftBracket) {
                stream.advance() // [
                val defaultNodes = ParseUtils.parseUntil(ctx, stream) { it is LatexToken.RightBracket }
                if (!stream.isEOF()) {
                    stream.expect("]")
                }
                defaultArg = ParseUtils.extractText(defaultNodes)
            }
        }

        // {begin-def}
        val beginArg = ctx.parseArgument() ?: return@CommandHandler LatexNode.Text("")
        val beginDef = when (beginArg) {
            is LatexNode.Group -> beginArg.children
            else -> listOf(beginArg)
        }

        // {end-def}
        val endArg = ctx.parseArgument() ?: return@CommandHandler LatexNode.Text("")
        val endDef = when (endArg) {
            is LatexNode.Group -> endArg.children
            else -> listOf(endArg)
        }

        ctx.customEnvironments[envName] = CustomEnvironment(envName, numArgs, beginDef, endDef, defaultArg)
        HLog.d(TAG) { "注册自定义环境: $envName[$numArgs]" }
        LatexNode.NewEnvironment(envName, numArgs, beginDef, endDef, defaultArg)
    }

    register("newenvironment", "renewenvironment", handler = newEnvHandler)
}

/**
 * Reads the control sequence being defined without dispatching it as a command.
 * This is important when a user intentionally overrides a built-in command,
 * for example `\newcommand{\abs}[1]{...}`.
 */
private fun parseCommandName(stream: com.hrm.latex.parser.component.LatexTokenStream): String? {
    while (stream.peek() is LatexToken.Whitespace) stream.advance()
    val grouped = stream.peek() is LatexToken.LeftBrace
    if (grouped) {
        stream.advance()
        while (stream.peek() is LatexToken.Whitespace) stream.advance()
    }

    val name = when (val token = stream.advance()) {
        is LatexToken.Command -> token.name
        is LatexToken.Text -> token.content.removePrefix("\\").trim()
        else -> null
    }

    if (grouped) {
        while (stream.peek() is LatexToken.Whitespace) stream.advance()
        if (stream.peek() is LatexToken.RightBrace) stream.advance()
    }
    return name?.takeIf { it.isNotEmpty() }
}
