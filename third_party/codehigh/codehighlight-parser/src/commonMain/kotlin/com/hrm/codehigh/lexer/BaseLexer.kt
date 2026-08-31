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
 * 词法分析器基础工具类，提供通用的 Token 构建辅助方法。
 * 标记为 internal，仅供模块内部使用。
 */
internal abstract class BaseLexer : Lexer {

    /**
     * 构建 Token 列表的辅助方法。
     * 确保所有字符都被覆盖（无遗漏），未匹配字符以 PLAIN 类型填充。
     */
    protected fun buildTokens(code: String, block: TokenBuilder.() -> Unit): List<CodeToken> {
        if (code.isEmpty()) return emptyList()
        val builder = TokenBuilder(code)
        builder.block()
        return builder.build()
    }
}

/**
 * Token 构建器，用于逐步构建 Token 列表。
 * 确保所有字符都被覆盖（无遗漏字符）。
 */
internal class TokenBuilder(private val code: String) {
    private val tokens = mutableListOf<CodeToken>()
    private var pos = 0

    /** 当前解析位置 */
    val position: Int get() = pos

    /** 是否已到达字符串末尾 */
    val isEnd: Boolean get() = pos >= code.length

    /** 当前字符 */
    val current: Char get() = if (pos < code.length) code[pos] else '\u0000'

    /** 查看当前位置后 offset 个字符 */
    fun peek(offset: Int = 0): Char {
        val idx = pos + offset
        return if (idx < code.length) code[idx] else '\u0000'
    }

    /** 检查从当前位置开始是否匹配指定字符串 */
    fun startsWith(prefix: String): Boolean {
        return code.startsWith(prefix, pos)
    }

    /** 前进一个字符 */
    fun advance(): Char {
        return if (pos < code.length) code[pos++] else '\u0000'
    }

    /** 前进 n 个字符 */
    fun advance(n: Int) {
        pos = minOf(pos + n, code.length)
    }

    /** 添加一个 Token，从 start 到当前位置 */
    fun addToken(type: TokenType, start: Int) {
        if (start < pos) {
            tokens.add(CodeToken(type, code.substring(start, pos), start until pos))
        }
    }

    /** 添加一个 Token，指定范围 */
    fun addToken(type: TokenType, start: Int, end: Int) {
        if (start < end) {
            tokens.add(CodeToken(type, code.substring(start, end), start until end))
        }
    }

    /** 跳过空白字符，作为 PLAIN Token 添加 */
    fun skipWhitespace() {
        val start = pos
        while (pos < code.length && code[pos].isWhitespace()) {
            pos++
        }
        if (start < pos) {
            tokens.add(CodeToken(TokenType.PLAIN, code.substring(start, pos), start until pos))
        }
    }

    /** 构建最终 Token 列表，未覆盖的字符以 PLAIN 填充 */
    fun build(): List<CodeToken> {
        // 填充剩余未覆盖的字符
        if (pos < code.length) {
            tokens.add(CodeToken(TokenType.PLAIN, code.substring(pos), pos until code.length))
        }
        return tokens.toList()
    }
}
