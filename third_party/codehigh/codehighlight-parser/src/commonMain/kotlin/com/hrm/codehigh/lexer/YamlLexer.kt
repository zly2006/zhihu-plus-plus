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
 * YAML 词法分析器。
 * 标记为 internal，外部通过 LanguageRegistry 访问。
 */
internal object YamlLexer : BaseLexer() {

    private val boolValues = setOf("true", "false", "yes", "no", "on", "off", "True", "False", "Yes", "No", "On", "Off", "TRUE", "FALSE", "YES", "NO", "ON", "OFF")
    private val nullValues = setOf("null", "~", "Null", "NULL")

    override fun tokenize(code: String): List<CodeToken> {
        if (code.isEmpty()) return emptyList()
        val tokens = mutableListOf<CodeToken>()
        var pos = 0

        while (pos < code.length) {
            val c = code[pos]

            // 文档分隔符 --- 或 ...
            if ((code.startsWith("---", pos) || code.startsWith("...", pos)) &&
                (pos == 0 || code[pos - 1] == '\n')) {
                val start = pos
                pos += 3
                tokens.add(CodeToken(TokenType.KEYWORD, code.substring(start, pos), start until pos))
                continue
            }

            // 注释
            if (c == '#') {
                val start = pos
                while (pos < code.length && code[pos] != '\n') pos++
                tokens.add(CodeToken(TokenType.COMMENT, code.substring(start, pos), start until pos))
                continue
            }

            // 锚点 &anchor
            if (c == '&') {
                val start = pos
                pos++
                while (pos < code.length && !code[pos].isWhitespace() && code[pos] != '\n') pos++
                tokens.add(CodeToken(TokenType.ANNOTATION, code.substring(start, pos), start until pos))
                continue
            }

            // 引用 *alias
            if (c == '*') {
                val start = pos
                pos++
                while (pos < code.length && !code[pos].isWhitespace() && code[pos] != '\n') pos++
                tokens.add(CodeToken(TokenType.VARIABLE, code.substring(start, pos), start until pos))
                continue
            }

            // 标签 !tag
            if (c == '!') {
                val start = pos
                pos++
                while (pos < code.length && !code[pos].isWhitespace() && code[pos] != '\n') pos++
                tokens.add(CodeToken(TokenType.ANNOTATION, code.substring(start, pos), start until pos))
                continue
            }

            // 双引号字符串
            if (c == '"') {
                val start = pos
                pos++
                while (pos < code.length && code[pos] != '"') {
                    if (code[pos] == '\\') pos++
                    pos++
                }
                if (pos < code.length && code[pos] == '"') pos++
                tokens.add(CodeToken(TokenType.STRING, code.substring(start, pos), start until pos))
                continue
            }

            // 单引号字符串
            if (c == '\'') {
                val start = pos
                pos++
                while (pos < code.length && !(code[pos] == '\'' && (pos + 1 >= code.length || code[pos + 1] != '\''))) {
                    if (code[pos] == '\'' && pos + 1 < code.length && code[pos + 1] == '\'') {
                        pos += 2
                    } else {
                        pos++
                    }
                }
                if (pos < code.length && code[pos] == '\'') pos++
                tokens.add(CodeToken(TokenType.STRING, code.substring(start, pos), start until pos))
                continue
            }

            // 数字
            if (c.isDigit() || (c == '-' && pos + 1 < code.length && code[pos + 1].isDigit())) {
                val start = pos
                if (c == '-') pos++
                while (pos < code.length && code[pos].isDigit()) pos++
                if (pos < code.length && code[pos] == '.') {
                    pos++
                    while (pos < code.length && code[pos].isDigit()) pos++
                }
                if (pos < code.length && (code[pos] == 'e' || code[pos] == 'E')) {
                    pos++
                    if (pos < code.length && (code[pos] == '+' || code[pos] == '-')) pos++
                    while (pos < code.length && code[pos].isDigit()) pos++
                }
                tokens.add(CodeToken(TokenType.NUMBER, code.substring(start, pos), start until pos))
                continue
            }

            // 标识符（键名、布尔值、null）
            if (c.isLetter() || c == '_') {
                val start = pos
                while (pos < code.length && (code[pos].isLetterOrDigit() || code[pos] == '_' || code[pos] == '-')) pos++
                val word = code.substring(start, pos)
                // 检查是否是键（后面跟冒号）
                var lookAhead = pos
                while (lookAhead < code.length && code[lookAhead] == ' ') lookAhead++
                val isKey = lookAhead < code.length && code[lookAhead] == ':'
                val type = when {
                    word in boolValues -> TokenType.BUILTIN
                    word in nullValues -> TokenType.BUILTIN
                    isKey -> TokenType.KEYWORD
                    else -> TokenType.IDENTIFIER
                }
                tokens.add(CodeToken(type, word, start until pos))
                continue
            }

            // 结构符号
            if (c in "{}[]|>:,-") {
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
