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

internal object DiffLexer : BaseLexer() {
    override fun tokenize(code: String): List<CodeToken> {
        if (code.isEmpty()) return emptyList()

        val tokens = mutableListOf<CodeToken>()
        var offset = 0
        val lines = code.split("\n")

        lines.forEachIndexed { index, line ->
            val lineLength = line.length
            val rangeEnd = offset + lineLength

            if (line.isNotEmpty()) {
                when {
                    line.startsWith("diff ") || line.startsWith("index ") -> {
                        tokens.add(CodeToken(TokenType.ANNOTATION, line, offset until rangeEnd))
                    }
                    line.startsWith("@@") -> {
                        tokens.add(CodeToken(TokenType.FUNCTION, line, offset until rangeEnd))
                    }
                    line.startsWith("+++") || line.startsWith("---") -> {
                        tokens.add(CodeToken(TokenType.TYPE, line, offset until rangeEnd))
                    }
                    line.startsWith("+") || line.startsWith("-") -> {
                        tokens.add(CodeToken(TokenType.OPERATOR, line.substring(0, 1), offset until offset + 1))
                        if (lineLength > 1) {
                            tokens.add(CodeToken(TokenType.PLAIN, line.substring(1), offset + 1 until rangeEnd))
                        }
                    }
                    else -> {
                        tokens.add(CodeToken(TokenType.PLAIN, line, offset until rangeEnd))
                    }
                }
            }

            if (index < lines.lastIndex) {
                tokens.add(CodeToken(TokenType.PLAIN, "\n", rangeEnd until rangeEnd + 1))
                offset = rangeEnd + 1
            } else {
                offset = rangeEnd
            }
        }

        return tokens
    }
}
