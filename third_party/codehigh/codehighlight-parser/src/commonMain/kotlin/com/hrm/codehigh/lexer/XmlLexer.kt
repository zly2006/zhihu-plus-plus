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
 * XML 词法分析器。
 * 标记为 internal，外部通过 LanguageRegistry 访问。
 */
internal object XmlLexer : BaseLexer() {

    override fun tokenize(code: String): List<CodeToken> = tokenizeXml(code, false)
}

/**
 * HTML 词法分析器。
 * 标记为 internal，外部通过 LanguageRegistry 访问。
 */
internal object HtmlLexer : BaseLexer() {

    override fun tokenize(code: String): List<CodeToken> = tokenizeXml(code, true)
}

/**
 * XML/HTML 通用词法分析实现。
 */
private fun tokenizeXml(code: String, isHtml: Boolean): List<CodeToken> {
    if (code.isEmpty()) return emptyList()
    val tokens = mutableListOf<CodeToken>()
    var pos = 0

    while (pos < code.length) {
        val c = code[pos]

        // XML/HTML 注释 <!-- -->
        if (code.startsWith("<!--", pos)) {
            val start = pos
            pos += 4
            while (pos + 2 < code.length && !code.startsWith("-->", pos)) pos++
            if (pos + 2 < code.length) {
                pos += 3
            } else {
                pos = code.length
            }
            tokens.add(CodeToken(TokenType.COMMENT, code.substring(start, pos), start until pos))
            continue
        }

        // CDATA <![CDATA[...]]>
        if (code.startsWith("<![CDATA[", pos)) {
            val start = pos
            pos += 9
            while (pos + 2 < code.length && !code.startsWith("]]>", pos)) pos++
            if (pos + 2 < code.length) {
                pos += 3
            } else {
                pos = code.length
            }
            tokens.add(CodeToken(TokenType.STRING, code.substring(start, pos), start until pos))
            continue
        }

        // DOCTYPE <!DOCTYPE ...>
        if (code.startsWith("<!DOCTYPE", pos) || code.startsWith("<!doctype", pos)) {
            val start = pos
            while (pos < code.length && code[pos] != '>') pos++
            if (pos < code.length) pos++
            tokens.add(CodeToken(TokenType.ANNOTATION, code.substring(start, pos), start until pos))
            continue
        }

        // 处理指令 <?...?>
        if (code.startsWith("<?", pos)) {
            val start = pos
            pos += 2
            while (pos + 1 < code.length && !code.startsWith("?>", pos)) pos++
            if (pos + 1 < code.length) {
                pos += 2
            } else {
                pos = code.length
            }
            tokens.add(CodeToken(TokenType.ANNOTATION, code.substring(start, pos), start until pos))
            continue
        }

        // 标签
        if (c == '<') {
            val start = pos
            pos++
            val isClosing = pos < code.length && code[pos] == '/'
            if (isClosing) pos++

            // 标签名
            val tagNameStart = pos
            while (pos < code.length && !code[pos].isWhitespace() && code[pos] != '>' && code[pos] != '/') pos++
            val tagName = code.substring(tagNameStart, pos)

            if (tagName.isNotEmpty()) {
                tokens.add(CodeToken(TokenType.PUNCTUATION, code.substring(start, tagNameStart), start until tagNameStart))
                tokens.add(CodeToken(TokenType.FUNCTION, tagName, tagNameStart until pos))
            } else {
                tokens.add(CodeToken(TokenType.PUNCTUATION, code.substring(start, pos), start until pos))
            }

            // 属性
            while (pos < code.length && code[pos] != '>') {
                // 跳过空白
                if (code[pos].isWhitespace()) {
                    val wsStart = pos
                    while (pos < code.length && code[pos].isWhitespace()) pos++
                    tokens.add(CodeToken(TokenType.PLAIN, code.substring(wsStart, pos), wsStart until pos))
                    continue
                }

                // 自闭合 />
                if (code[pos] == '/' && pos + 1 < code.length && code[pos + 1] == '>') {
                    tokens.add(CodeToken(TokenType.PUNCTUATION, "/>", pos until pos + 2))
                    pos += 2
                    break
                }

                // 属性名
                if (code[pos].isLetter() || code[pos] == '_' || code[pos] == ':') {
                    val attrStart = pos
                    while (pos < code.length && !code[pos].isWhitespace() && code[pos] != '=' && code[pos] != '>' && code[pos] != '/') pos++
                    tokens.add(CodeToken(TokenType.IDENTIFIER, code.substring(attrStart, pos), attrStart until pos))

                    // 跳过空白
                    while (pos < code.length && code[pos] == ' ') pos++

                    // 属性值
                    if (pos < code.length && code[pos] == '=') {
                        tokens.add(CodeToken(TokenType.OPERATOR, "=", pos until pos + 1))
                        pos++
                        while (pos < code.length && code[pos] == ' ') pos++
                        if (pos < code.length && (code[pos] == '"' || code[pos] == '\'')) {
                            val quote = code[pos]
                            val valStart = pos
                            pos++
                            while (pos < code.length && code[pos] != quote) pos++
                            if (pos < code.length) pos++
                            tokens.add(CodeToken(TokenType.STRING, code.substring(valStart, pos), valStart until pos))
                        }
                    }
                    continue
                }

                // 其他字符
                tokens.add(CodeToken(TokenType.PLAIN, code[pos].toString(), pos until pos + 1))
                pos++
            }

            // 闭合 >
            if (pos < code.length && code[pos] == '>') {
                tokens.add(CodeToken(TokenType.PUNCTUATION, ">", pos until pos + 1))
                pos++
            }
            continue
        }

        // 文本内容
        val start = pos
        while (pos < code.length && code[pos] != '<') pos++
        if (start < pos) {
            tokens.add(CodeToken(TokenType.PLAIN, code.substring(start, pos), start until pos))
        }
    }

    return tokens
}
