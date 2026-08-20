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

package com.hrm.codehigh.stream

import com.hrm.codehigh.ast.CodeAst
import com.hrm.codehigh.ast.CodeToken
import com.hrm.codehigh.lexer.LanguageRegistry

/**
 * 增量高亮引擎，用于流式场景下的高效代码高亮更新。
 * 作为 parser 层的流式解析接口公开，render 层基于该接口消费解析结果。
 *
 * 核心策略：
 * 1. 稳定前缀 Token 直接复用，不重新解析
 * 2. 仅对尾部脏区域（从最后一个受影响 Token 到文本末尾）重新解析
 * 3. 相同代码字符串和语言命中 AST 缓存，直接返回缓存结果
 */
class IncrementalHighlighter {
    data class UpdateResult(
        val ast: CodeAst,
        val firstChangedLine: Int,
        val reparseStart: Int,
    )

    private companion object {
        private const val CONTEXT_LOOKBACK_CHARS = 64
    }

    /** AST 缓存：key = language + "|" + code */
    private var cachedAst: CodeAst? = null

    /** 上一次解析的语言 */
    private var lastLanguage: String = ""

    /**
     * 增量更新高亮结果。
     *
     * @param code 新的代码字符串
     * @param language 语言标识符
     * @return 更新后的 CodeAst
     */
    fun update(code: String, language: String): CodeAst {
        return updateDetailed(code, language).ast
    }

    fun updateDetailed(code: String, language: String): UpdateResult {
        // 语言变化时触发全量重新解析
        if (language != lastLanguage) {
            lastLanguage = language
            cachedAst = null
        }

        val cached = cachedAst
        // 命中缓存
        if (cached != null && cached.source == code && cached.language == language) {
            return UpdateResult(
                ast = cached,
                firstChangedLine = 0,
                reparseStart = 0,
            )
        }

        // 尝试增量更新
        val update = if (cached != null && code.startsWith(cached.source)) {
            // 代码是旧代码的扩展（追加场景）
            incrementalParse(code, language, cached)
        } else {
            // 全量解析
            UpdateResult(
                ast = fullParse(code, language),
                firstChangedLine = 0,
                reparseStart = 0,
            )
        }

        cachedAst = update.ast
        return update
    }

    /**
     * 全量解析。
     */
    private fun fullParse(code: String, language: String): CodeAst {
        val lexer = LanguageRegistry.getOrPlain(language)
        val tokens = lexer.tokenize(code)
        return CodeAst(tokens, code, language)
    }

    /**
     * 增量解析：复用稳定前缀，仅重新解析尾部脏区域。
     */
    private fun incrementalParse(newCode: String, language: String, oldAst: CodeAst): UpdateResult {
        val oldCode = oldAst.source
        val appendedStart = oldCode.length
        val tokenStart = determineReparseStart(oldAst, appendedStart)
        // IMPORTANT:
        // Do NOT force reparseStart to a line boundary. If we move backward to the line start,
        // we might end up *inside* a previously tokenized multi-line token (e.g. block comment),
        // then the "stable prefix" will drop that token and the dirty lexer will see "*/" etc.
        // Re-lexing from a token boundary keeps tokenization consistent while still being cheap.
        val reparseStart = tokenStart
        val stableTokens = oldAst.tokens.takeWhile { it.range.last < reparseStart }

        val dirtyCode = newCode.substring(reparseStart)
        val lexer = LanguageRegistry.getOrPlain(language)
        val dirtyTokens = lexer.tokenize(dirtyCode).map { token ->
            CodeToken(
                type = token.type,
                text = token.text,
                range = (token.range.first + reparseStart)..(token.range.last + reparseStart)
            )
        }

        val allTokens = if (reparseStart > 0) {
            stableTokens + dirtyTokens
        } else {
            dirtyTokens
        }

        return UpdateResult(
            ast = CodeAst(allTokens, newCode, language),
            firstChangedLine = newCode.countLinesBefore(reparseStart),
            reparseStart = reparseStart,
        )
    }

    private fun determineReparseStart(oldAst: CodeAst, appendedStart: Int): Int {
        if (appendedStart == 0) return 0

        val oldCode = oldAst.source
        val nearbyTokenStart = oldAst.tokens
            .lastOrNull { it.range.last < appendedStart && appendedStart - it.range.first <= CONTEXT_LOOKBACK_CHARS }
            ?.range
            ?.first
        val unfinishedTokenStart = oldAst.tokens
            .asReversed()
            .firstOrNull {
                it.range.last < appendedStart &&
                    appendedStart - it.range.first <= CONTEXT_LOOKBACK_CHARS &&
                    shouldReparseFromTokenStart(it)
            }
            ?.range
            ?.first

        return unfinishedTokenStart ?: nearbyTokenStart ?: 0
    }

    private fun shouldReparseFromTokenStart(token: CodeToken): Boolean {
        return when (token.type) {
            com.hrm.codehigh.ast.TokenType.COMMENT -> token.text.startsWith("/*") && !token.text.endsWith("*/")
            com.hrm.codehigh.ast.TokenType.STRING -> {
                (token.text.startsWith("\"\"\"") && !token.text.endsWith("\"\"\"")) ||
                    (token.text.startsWith("\"") && !token.text.endsWith("\"")) ||
                    (token.text.startsWith("'") && !token.text.endsWith("'"))
            }
            else -> false
        }
    }

    /**
     * 清除缓存，强制下次全量解析。
     */
    fun invalidate() {
        cachedAst = null
        lastLanguage = ""
    }
}

private fun String.countLinesBefore(charIndex: Int): Int {
    if (charIndex <= 0) return 0
    val end = charIndex.coerceAtMost(length)
    var count = 0
    for (i in 0 until end) {
        if (this[i] == '\n') count++
    }
    return count
}
