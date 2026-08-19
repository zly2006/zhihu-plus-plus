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
 * Rust 词法分析器。
 * 标记为 internal，外部通过 LanguageRegistry 访问。
 */
internal object RustLexer : BaseLexer() {

    private val keywords = setOf(
        "fn", "let", "mut", "struct", "enum", "impl", "trait", "pub", "use", "mod",
        "crate", "super", "self", "Self", "if", "else", "match", "for", "while",
        "loop", "return", "async", "await", "unsafe", "where", "type", "const",
        "static", "ref", "move", "dyn", "box", "in", "as", "break", "continue",
        "extern", "false", "true", "abstract", "become", "do", "final", "macro",
        "override", "priv", "typeof", "unsized", "virtual", "yield"
    )

    private val builtinTypes = setOf(
        "i8", "i16", "i32", "i64", "i128", "isize",
        "u8", "u16", "u32", "u64", "u128", "usize",
        "f32", "f64", "bool", "char", "str",
        "String", "Vec", "Box", "Rc", "Arc", "Cell", "RefCell",
        "Option", "Result", "Ok", "Err", "Some", "None",
        "HashMap", "HashSet", "BTreeMap", "BTreeSet",
        "Iterator", "IntoIterator", "From", "Into", "Clone", "Copy",
        "Debug", "Display", "Default", "Drop", "Send", "Sync",
        "Fn", "FnMut", "FnOnce", "Future", "Pin", "PhantomData"
    )

    override fun tokenize(code: String): List<CodeToken> {
        if (code.isEmpty()) return emptyList()
        val tokens = mutableListOf<CodeToken>()
        var pos = 0

        while (pos < code.length) {
            val c = code[pos]

            // 文档注释 ///
            if (pos + 2 < code.length && code[pos] == '/' && code[pos + 1] == '/' && code[pos + 2] == '/') {
                val start = pos
                while (pos < code.length && code[pos] != '\n') pos++
                tokens.add(CodeToken(TokenType.COMMENT, code.substring(start, pos), start until pos))
                continue
            }

            // 模块文档注释 //!
            if (pos + 2 < code.length && code[pos] == '/' && code[pos + 1] == '/' && code[pos + 2] == '!') {
                val start = pos
                while (pos < code.length && code[pos] != '\n') pos++
                tokens.add(CodeToken(TokenType.COMMENT, code.substring(start, pos), start until pos))
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

            // 注解属性 #[...]
            if (c == '#' && pos + 1 < code.length && code[pos + 1] == '[') {
                val start = pos
                pos += 2
                var depth = 1
                while (pos < code.length && depth > 0) {
                    when (code[pos]) {
                        '[' -> depth++
                        ']' -> depth--
                    }
                    pos++
                }
                tokens.add(CodeToken(TokenType.ANNOTATION, code.substring(start, pos), start until pos))
                continue
            }

            // 原始字符串 r"..." 或 r#"..."#
            if (c == 'r' && pos + 1 < code.length && (code[pos + 1] == '"' || code[pos + 1] == '#')) {
                val start = pos
                pos++
                var hashCount = 0
                while (pos < code.length && code[pos] == '#') {
                    hashCount++
                    pos++
                }
                if (pos < code.length && code[pos] == '"') {
                    pos++
                    val endPattern = "\"" + "#".repeat(hashCount)
                    while (pos < code.length && !code.startsWith(endPattern, pos)) pos++
                    if (pos < code.length) pos += endPattern.length
                }
                tokens.add(CodeToken(TokenType.STRING, code.substring(start, pos), start until pos))
                continue
            }

            // 字节字符串 b"..."
            if (c == 'b' && pos + 1 < code.length && code[pos + 1] == '"') {
                val start = pos
                pos += 2
                while (pos < code.length && code[pos] != '"') {
                    if (code[pos] == '\\') pos++
                    pos++
                }
                if (pos < code.length) pos++
                tokens.add(CodeToken(TokenType.STRING, code.substring(start, pos), start until pos))
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
                if (pos < code.length) pos++
                tokens.add(CodeToken(TokenType.STRING, code.substring(start, pos), start until pos))
                continue
            }

            // 字符字面量
            if (c == '\'') {
                // 生命周期标注 'a, 'static, '_
                val start = pos
                pos++
                if (pos < code.length && (code[pos].isLetter() || code[pos] == '_')) {
                    while (pos < code.length && (code[pos].isLetterOrDigit() || code[pos] == '_')) pos++
                    // 如果后面没有闭合引号，则是生命周期
                    if (pos >= code.length || code[pos] != '\'') {
                        tokens.add(CodeToken(TokenType.ANNOTATION, code.substring(start, pos), start until pos))
                        continue
                    }
                }
                // 字符字面量
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
                }
                // 类型后缀 i32, u64, f32 等
                if (pos < code.length && (code[pos] == 'i' || code[pos] == 'u' || code[pos] == 'f')) {
                    val suffixStart = pos
                    pos++
                    while (pos < code.length && code[pos].isDigit()) pos++
                    // 验证是否是有效的类型后缀
                    val suffix = code.substring(suffixStart, pos)
                    if (suffix !in setOf("i8", "i16", "i32", "i64", "i128", "isize", "u8", "u16", "u32", "u64", "u128", "usize", "f32", "f64")) {
                        pos = suffixStart // 回退
                    }
                }
                tokens.add(CodeToken(TokenType.NUMBER, code.substring(start, pos), start until pos))
                continue
            }

            // 宏调用（标识符后跟 !）
            if (c.isLetter() || c == '_') {
                val start = pos
                while (pos < code.length && (code[pos].isLetterOrDigit() || code[pos] == '_')) pos++
                val word = code.substring(start, pos)
                // 检查是否是宏调用
                if (pos < code.length && code[pos] == '!') {
                    pos++
                    tokens.add(CodeToken(TokenType.BUILTIN, code.substring(start, pos), start until pos))
                    continue
                }
                val type = when {
                    word in keywords -> TokenType.KEYWORD
                    word in builtinTypes -> TokenType.TYPE
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
                    threeChar in setOf("..=", "<<=", ">>=") -> pos += 3
                    twoChar in setOf("==", "!=", "<=", ">=", "&&", "||", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<", ">>", "..", "->", "=>", "::", "?:") -> pos += 2
                    else -> pos++
                }
                tokens.add(CodeToken(TokenType.OPERATOR, code.substring(start, pos), start until pos))
                continue
            }

            // 标点符号
            if (c in "{}()[];,.$@") {
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
