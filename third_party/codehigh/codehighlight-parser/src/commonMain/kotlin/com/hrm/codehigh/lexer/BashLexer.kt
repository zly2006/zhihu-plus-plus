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

package com.hrm.codehigh.lexer

import com.hrm.codehigh.ast.CodeToken
import com.hrm.codehigh.ast.TokenType

/**
 * Bash/Shell 词法分析器。
 * 标记为 internal，外部通过 LanguageRegistry 访问。
 */
internal object BashLexer : BaseLexer() {

    private val keywords = setOf(
        "if", "then", "else", "elif", "fi", "for", "do", "done", "while", "until",
        "case", "esac", "function", "return", "exit", "break", "continue",
        "local", "export", "readonly", "declare", "typeset", "source", ".",
        "in", "select", "time", "coproc"
    )

    private val builtins = setOf(
        "echo", "cd", "ls", "mkdir", "rm", "cp", "mv", "grep", "sed", "awk",
        "cat", "chmod", "chown", "pwd", "touch", "find", "sort", "uniq",
        "head", "tail", "wc", "cut", "tr", "xargs", "tee", "read", "printf",
        "test", "true", "false", "set", "unset", "shift", "eval", "exec",
        "trap", "wait", "jobs", "kill", "bg", "fg", "type", "which", "alias",
        "unalias", "history", "help", "let", "expr", "getopts", "basename",
        "dirname", "realpath", "readlink", "stat", "file", "env", "printenv"
    )

    override fun tokenize(code: String): List<CodeToken> {
        if (code.isEmpty()) return emptyList()
        val tokens = mutableListOf<CodeToken>()
        var pos = 0

        while (pos < code.length) {
            val c = code[pos]

            // Shebang 和注释
            if (c == '#') {
                val start = pos
                while (pos < code.length && code[pos] != '\n') pos++
                tokens.add(CodeToken(TokenType.COMMENT, code.substring(start, pos), start until pos))
                continue
            }

            // 单引号字符串（字面量，不展开变量）
            if (c == '\'') {
                val start = pos
                pos++
                while (pos < code.length && code[pos] != '\'') pos++
                if (pos < code.length) pos++
                tokens.add(CodeToken(TokenType.STRING, code.substring(start, pos), start until pos))
                continue
            }

            // 双引号字符串（支持变量展开）
            if (c == '"') {
                val start = pos
                pos++
                while (pos < code.length && code[pos] != '"') {
                    if (code[pos] == '\\') pos++
                    pos++
                }
                if (pos < code.length) pos++
                tokens.add(CodeToken(TokenType.STRING, code.substring(start, pos), start until pos))
                continue
            }

            // 反引号命令替换
            if (c == '`') {
                val start = pos
                pos++
                while (pos < code.length && code[pos] != '`') {
                    if (code[pos] == '\\') pos++
                    pos++
                }
                if (pos < code.length) pos++
                tokens.add(CodeToken(TokenType.STRING, code.substring(start, pos), start until pos))
                continue
            }

            // 变量 $VAR, ${VAR}, $1-$9, $@, $#, $?, $$
            if (c == '$') {
                val start = pos
                pos++
                when {
                    pos < code.length && code[pos] == '{' -> {
                        pos++
                        while (pos < code.length && code[pos] != '}') pos++
                        if (pos < code.length) pos++
                    }
                    pos < code.length && code[pos] == '(' -> {
                        pos++
                        var depth = 1
                        while (pos < code.length && depth > 0) {
                            when (code[pos]) {
                                '(' -> depth++
                                ')' -> depth--
                            }
                            pos++
                        }
                    }
                    pos < code.length && (code[pos].isLetterOrDigit() || code[pos] == '_') -> {
                        while (pos < code.length && (code[pos].isLetterOrDigit() || code[pos] == '_')) pos++
                    }
                    pos < code.length && code[pos] in "@#?$!-*" -> pos++
                }
                tokens.add(CodeToken(TokenType.VARIABLE, code.substring(start, pos), start until pos))
                continue
            }

            // 数字
            if (c.isDigit()) {
                val start = pos
                while (pos < code.length && code[pos].isDigit()) pos++
                if (pos < code.length && code[pos] == '.') {
                    pos++
                    while (pos < code.length && code[pos].isDigit()) pos++
                }
                tokens.add(CodeToken(TokenType.NUMBER, code.substring(start, pos), start until pos))
                continue
            }

            // 标识符、关键字、内置命令
            if (c.isLetter() || c == '_') {
                val start = pos
                while (pos < code.length && (code[pos].isLetterOrDigit() || code[pos] == '_' || code[pos] == '-')) pos++
                val word = code.substring(start, pos)
                val type = when {
                    word in keywords -> TokenType.KEYWORD
                    word in builtins -> TokenType.BUILTIN
                    else -> TokenType.IDENTIFIER
                }
                tokens.add(CodeToken(type, word, start until pos))
                continue
            }

            // 运算符
            if (c in "+-*/%=!<>&|^~") {
                val start = pos
                val twoChar = if (pos + 1 < code.length) code.substring(pos, pos + 2) else ""
                when {
                    twoChar in setOf("==", "!=", "<=", ">=", "&&", "||", ">>", "<<", "+=", "-=", "*=", "/=") -> pos += 2
                    else -> pos++
                }
                tokens.add(CodeToken(TokenType.OPERATOR, code.substring(start, pos), start until pos))
                continue
            }

            // 标点符号
            if (c in "{}()[];,.:") {
                tokens.add(CodeToken(TokenType.PUNCTUATION, c.toString(), pos until pos + 1))
                pos++
                continue
            }

            // 其他字符
            tokens.add(CodeToken(TokenType.PLAIN, c.toString(), pos until pos + 1))
            pos++
        }

        return tokens
    }
}
