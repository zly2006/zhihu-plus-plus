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
 * C 语言词法分析器。
 * 标记为 internal，外部通过 LanguageRegistry 访问。
 */
internal object CLexer : BaseLexer() {

    private val keywords = setOf(
        "int", "char", "float", "double", "void", "struct", "union", "enum",
        "typedef", "if", "else", "for", "while", "do", "switch", "case",
        "return", "break", "continue", "goto", "static", "extern", "const",
        "volatile", "sizeof", "register", "auto", "signed", "unsigned",
        "short", "long", "inline", "restrict", "_Bool", "_Complex", "_Imaginary",
        "default", "NULL", "true", "false"
    )

    override fun tokenize(code: String): List<CodeToken> = tokenizeC(code, false)
}

/**
 * C++ 语言词法分析器。
 * 标记为 internal，外部通过 LanguageRegistry 访问。
 */
internal object CppLexer : BaseLexer() {

    private val keywords = setOf(
        // C 关键字
        "int", "char", "float", "double", "void", "struct", "union", "enum",
        "typedef", "if", "else", "for", "while", "do", "switch", "case",
        "return", "break", "continue", "goto", "static", "extern", "const",
        "volatile", "sizeof", "register", "auto", "signed", "unsigned",
        "short", "long", "inline", "default",
        // C++ 扩展关键字
        "class", "namespace", "template", "typename", "virtual", "override",
        "final", "new", "delete", "this", "nullptr", "decltype", "constexpr",
        "explicit", "friend", "operator", "using", "public", "private",
        "protected", "mutable", "noexcept", "throw", "try", "catch",
        "dynamic_cast", "static_cast", "reinterpret_cast", "const_cast",
        "typeid", "export", "import", "module", "co_await", "co_return",
        "co_yield", "concept", "requires", "consteval", "constinit",
        "true", "false", "NULL", "nullptr"
    )

    override fun tokenize(code: String): List<CodeToken> = tokenizeC(code, true)
}

/**
 * C/C++ 通用词法分析实现。
 */
private fun tokenizeC(code: String, isCpp: Boolean): List<CodeToken> {
    if (code.isEmpty()) return emptyList()
    val tokens = mutableListOf<CodeToken>()
    var pos = 0

    val keywords = if (isCpp) {
        setOf(
            "int", "char", "float", "double", "void", "struct", "union", "enum",
            "typedef", "if", "else", "for", "while", "do", "switch", "case",
            "return", "break", "continue", "goto", "static", "extern", "const",
            "volatile", "sizeof", "register", "auto", "signed", "unsigned",
            "short", "long", "inline", "default",
            "class", "namespace", "template", "typename", "virtual", "override",
            "final", "new", "delete", "this", "nullptr", "decltype", "constexpr",
            "explicit", "friend", "operator", "using", "public", "private",
            "protected", "mutable", "noexcept", "throw", "try", "catch",
            "dynamic_cast", "static_cast", "reinterpret_cast", "const_cast",
            "typeid", "export", "import", "module", "co_await", "co_return",
            "co_yield", "concept", "requires", "consteval", "constinit",
            "true", "false", "NULL", "nullptr"
        )
    } else {
        setOf(
            "int", "char", "float", "double", "void", "struct", "union", "enum",
            "typedef", "if", "else", "for", "while", "do", "switch", "case",
            "return", "break", "continue", "goto", "static", "extern", "const",
            "volatile", "sizeof", "register", "auto", "signed", "unsigned",
            "short", "long", "inline", "restrict", "_Bool", "_Complex", "_Imaginary",
            "default", "NULL", "true", "false"
        )
    }

    while (pos < code.length) {
        val c = code[pos]

        // 预处理指令
        if (c == '#') {
            val start = pos
            while (pos < code.length && code[pos] != '\n') {
                // 处理行续接
                if (code[pos] == '\\' && pos + 1 < code.length && code[pos + 1] == '\n') {
                    pos += 2
                } else {
                    pos++
                }
            }
            tokens.add(CodeToken(TokenType.ANNOTATION, code.substring(start, pos), start until pos))
            continue
        }

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

        // C++ 原始字符串 R"(...)"
        if (isCpp && c == 'R' && pos + 1 < code.length && code[pos + 1] == '"') {
            val start = pos
            pos += 2
            val delimStart = pos
            while (pos < code.length && code[pos] != '(') pos++
            val delim = code.substring(delimStart, pos)
            if (pos < code.length) pos++ // 跳过 (
            val endPattern = ")$delim\""
            while (pos < code.length && !code.startsWith(endPattern, pos)) pos++
            if (pos < code.length) pos += endPattern.length
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
            } else {
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
            }
            // 类型后缀
            while (pos < code.length && code[pos] in "uUlLfF") pos++
            tokens.add(CodeToken(TokenType.NUMBER, code.substring(start, pos), start until pos))
            continue
        }

        // 标识符、关键字
        if (c.isLetter() || c == '_') {
            val start = pos
            while (pos < code.length && (code[pos].isLetterOrDigit() || code[pos] == '_')) pos++
            val word = code.substring(start, pos)
            val type = when {
                word in keywords -> TokenType.KEYWORD
                word[0].isUpperCase() -> TokenType.TYPE
                pos < code.length && code[pos] == '(' -> TokenType.FUNCTION
                else -> TokenType.IDENTIFIER
            }
            tokens.add(CodeToken(type, word, start until pos))
            continue
        }

        // 运算符
        if (c in "+-*/%=!<>&|^~?:") {
            val start = pos
            val twoChar = if (pos + 1 < code.length) code.substring(pos, pos + 2) else ""
            val threeChar = if (pos + 2 < code.length) code.substring(pos, pos + 3) else ""
            when {
                threeChar in setOf("<<=", ">>=") -> pos += 3
                twoChar in setOf("==", "!=", "<=", ">=", "&&", "||", "++", "--", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<", ">>", "->", "::") -> pos += 2
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
