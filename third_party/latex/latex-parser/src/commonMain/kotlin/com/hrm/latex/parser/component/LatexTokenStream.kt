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

import com.hrm.latex.parser.model.SourceRange
import com.hrm.latex.parser.tokenizer.LatexToken

/**
 * 封装 Token 流的操作，如 peek, advance, expect
 */
class LatexTokenStream(private val initialTokens: List<LatexToken>) {
    private var mutableTokens: MutableList<LatexToken>? = null
    private val tokens: List<LatexToken>
        get() = mutableTokens ?: initialTokens
    private var position = 0

    fun peek(offset: Int = 0): LatexToken? {
        val pos = position + offset
        return if (pos in tokens.indices) tokens[pos] else null
    }

    /**
     * 向前查看跳过满足条件的 token，返回第一个不匹配的 token
     * 不改变当前位置
     */
    fun peekSkipping(skip: (LatexToken) -> Boolean): LatexToken? {
        var offset = 0
        while (true) {
            val token = peek(offset) ?: return null
            if (token is LatexToken.EOF) return null
            if (!skip(token)) return token
            offset++
        }
    }

    fun advance(): LatexToken? {
        val token = peek()
        position++
        return token
    }

    /**
     * 消费当前文本 token 的一个 Unicode 字符。
     *
     * TeX 的无花括号参数只消费后面的一个 token；普通文本在分词阶段会合并，
     * 因此解析命令参数或上下标时需要从合并后的文本 token 中仅取出第一个字符。
     */
    internal fun consumeTextAtom(): LatexToken.Text? {
        val token = peek() as? LatexToken.Text ?: return null
        val atomLength = if (
            token.content.length > 1 &&
            token.content[0].isHighSurrogate() &&
            token.content[1].isLowSurrogate()
        ) {
            2
        } else {
            1
        }
        val atomEnd = token.range.start + atomLength
        val atom = LatexToken.Text(
            token.content.substring(0, atomLength),
            SourceRange(token.range.start, atomEnd)
        )

        if (atomLength < token.content.length) {
            val editableTokens = mutableTokens ?: initialTokens.toMutableList().also { mutableTokens = it }
            editableTokens[position] = atom
            editableTokens.add(
                position + 1,
                LatexToken.Text(
                    token.content.substring(atomLength),
                    SourceRange(atomEnd, token.range.end)
                )
            )
        }
        advance()
        return atom
    }

    fun isEOF(): Boolean {
        val token = peek()
        return token == null || token is LatexToken.EOF
    }

    fun expect(type: String, message: String? = null): LatexToken {
        val token = peek()
        if (token == null) {
            throw Exception(message ?: "期望 $type，但到达文件末尾")
        }
        advance()
        return token
    }

    /**
     * 获取当前 token 的源码位置起始偏移
     * 用于 Parser 记录节点的 sourceRange.start
     */
    fun currentSourceOffset(): Int {
        return peek()?.range?.start ?: (peek(-1)?.range?.end ?: 0)
    }

    /**
     * 获取上一个已消费 token 的结束偏移
     * 用于 Parser 记录节点的 sourceRange.end
     */
    fun previousEndOffset(): Int {
        if (position <= 0) return 0
        val prevPos = position - 1
        return if (prevPos < tokens.size) tokens[prevPos].range.end else 0
    }

    /**
     * 构建从 start 到当前已消费位置的 SourceRange
     */
    fun rangeFrom(startOffset: Int): SourceRange {
        return SourceRange(startOffset, previousEndOffset())
    }

    fun reset() {
        position = 0
    }
}
