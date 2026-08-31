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
 * Go 词法分析器。
 * 标记为 internal，外部通过 LanguageRegistry 访问。
 */
internal object GoLexer : BaseLexer() {

    private val keywords = setOf(
        "func", "package", "import", "var", "const", "type", "struct", "interface",
        "map", "chan", "go", "defer", "select", "case", "default", "if", "else",
        "for", "range", "return", "break", "continue", "fallthrough", "goto",
        "switch", "nil", "true", "false", "iota"
    )

    private val builtins = setOf(
        "make", "new", "len", "cap", "append", "copy", "delete", "close",
        "panic", "recover", "print", "println", "error", "string", "int",
        "int8", "int16", "int32", "int64", "uint", "uint8", "uint16", "uint32",
        "uint64", "uintptr", "float32", "float64", "complex64", "complex128",
        "bool", "byte", "rune", "any", "comparable"
    )

    override fun tokenize(code: String): List<CodeToken> {
        if (code.isEmpty()) return emptyList()
        val tokens = mutableListOf<CodeToken>()
        var pos = 0

        while (pos < code.length) {
            val c = code[pos]

            // 单行注释
            if (pos + 1 < code.length && code[pos] == '/' && code[pos + 1] == '/') {
                val start = pos
                while (pos < code.length && code[pos] != '\n') pos++
                tokens.add(CodeToken(TokenType.COMMENT, code.substring(start, pos), start until pos))
                continue
            }

            // 多行注释
            if (pos + 1 < code.length && code[pos] == '/' && code[pos + 1] == '*') {
                val start = pos
                pos += 2
                while (pos + 1 < code.length && !(code[pos] == '*' && code[pos + 1] == '/')) pos++
                if (pos + 1 < code.length) {
                    pos += 2
                } else {
                    pos = code.length
                }
                tokens.add(CodeToken(TokenType.COMMENT, code.substring(start, pos), start until pos))
                continue
            }

            // 反引号原始字符串
            if (c == '`') {
                val start = pos
                pos++
                while (pos < code.length && code[pos] != '`') pos++
                if (pos < code.length) pos++
                tokens.add(CodeToken(TokenType.STRING, code.substring(start, pos), start until pos))
                continue
            }

            // 双引号字符串
            if (c == '"') {
                val start = pos
                pos++
                while (pos < code.length && code[pos] != '"' && code[pos] != '\n') {
                    if (code[pos] == '\\') pos++
                    pos++
                }
                if (pos < code.length && code[pos] == '"') pos++
                tokens.add(CodeToken(TokenType.STRING, code.substring(start, pos), start until pos))
                continue
            }

            // 字符字面量
            if (c == '\'') {
                val start = pos
                pos++
                while (pos < code.length && code[pos] != '\'' && code[pos] != '\n') {
                    if (code[pos] == '\\') pos++
                    pos++
                }
                if (pos < code.length && code[pos] == '\'') pos++
                tokens.add(CodeToken(TokenType.STRING, code.substring(start, pos), start until pos))
                continue
            }

            // 数字字面量
            if (c.isDigit()) {
                val start = pos
                if (c == '0' && pos + 1 < code.length && (code[pos + 1] == 'x' || code[pos + 1] == 'X')) {
                    pos += 2
                    while (pos < code.length && (code[pos].isDigit() || code[pos] in 'a'..'f' || code[pos] in 'A'..'F' || code[pos] == '_')) pos++
                } else if (c == '0' && pos + 1 < code.length && (code[pos + 1] == 'b' || code[pos + 1] == 'B')) {
                    pos += 2
                    while (pos < code.length && (code[pos] == '0' || code[pos] == '1' || code[pos] == '_')) pos++
                } else if (c == '0' && pos + 1 < code.length && (code[pos + 1] == 'o' || code[pos + 1] == 'O')) {
                    pos += 2
                    while (pos < code.length && (code[pos] in '0'..'7' || code[pos] == '_')) pos++
                } else {
                    while (pos < code.length && (code[pos].isDigit() || code[pos] == '_')) pos++
                    if (pos < code.length && code[pos] == '.') {
                        pos++
                        while (pos < code.length && code[pos].isDigit()) pos++
                    }
                    if (pos < code.length && (code[pos] == 'e' || code[pos] == 'E')) {
                        pos++
                        if (pos < code.length && (code[pos] == '+' || code[pos] == '-')) pos++
                        while (pos < code.length && code[pos].isDigit()) pos++
                    }
                    if (pos < code.length && (code[pos] == 'i')) pos++ // 虚数
                }
                tokens.add(CodeToken(TokenType.NUMBER, code.substring(start, pos), start until pos))
                continue
            }

            // 标识符、关键字、内置
            if (c.isLetter() || c == '_') {
                val start = pos
                while (pos < code.length && (code[pos].isLetterOrDigit() || code[pos] == '_')) pos++
                val word = code.substring(start, pos)
                val type = when {
                    word in keywords -> TokenType.KEYWORD
                    word in builtins -> TokenType.BUILTIN
                    word[0].isUpperCase() -> TokenType.TYPE
                    pos < code.length && code[pos] == '(' -> TokenType.FUNCTION
                    else -> TokenType.IDENTIFIER
                }
                tokens.add(CodeToken(type, word, start until pos))
                continue
            }

            // 运算符
            if (c in "+-*/%=!<>&|^~:") {
                val start = pos
                val twoChar = if (pos + 1 < code.length) code.substring(pos, pos + 2) else ""
                val threeChar = if (pos + 2 < code.length) code.substring(pos, pos + 3) else ""
                when {
                    threeChar == "<<=" || threeChar == ">>=" -> pos += 3
                    twoChar in setOf("==", "!=", "<=", ">=", "&&", "||", "++", "--", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<", ">>", ":=", "<-") -> pos += 2
                    else -> pos++
                }
                tokens.add(CodeToken(TokenType.OPERATOR, code.substring(start, pos), start until pos))
                continue
            }

            // 标点符号
            if (c in "{}()[];,.$") {
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
