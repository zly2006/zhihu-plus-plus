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

internal data class ConfigurableLexerSpec(
    val keywords: Set<String>,
    val builtins: Set<String> = emptySet(),
    val types: Set<String> = emptySet(),
    val fixedTokens: Map<String, TokenType> = emptyMap(),
    val lineComments: List<String> = emptyList(),
    val blockComments: List<Pair<String, String>> = emptyList(),
    val blockStrings: List<Pair<String, String>> = emptyList(),
    val stringQuotes: Set<Char> = setOf('"', '\''),
    val tripleStrings: Set<String> = emptySet(),
    val annotationPrefix: Char? = null,
    val variablePrefixes: List<String> = emptyList(),
    val caseInsensitiveWords: Boolean = false,
    val uppercaseIdentifiersAreTypes: Boolean = false,
    val extraWordStartChars: Set<Char> = emptySet(),
    val extraWordChars: Set<Char> = emptySet(),
    val operators: Set<String> = emptySet(),
    val punctuation: Set<Char> = setOf('{', '}', '(', ')', '[', ']', ';', ',', '.', ':')
)

internal fun tokenizeWithSpec(code: String, spec: ConfigurableLexerSpec): List<CodeToken> {
    if (code.isEmpty()) return emptyList()

    val tokens = mutableListOf<CodeToken>()
    var pos = 0

    val fixedTokens = spec.fixedTokens.keys.sortedByDescending { it.length }
    val variablePrefixes = spec.variablePrefixes.sortedByDescending { it.length }
    val lineComments = spec.lineComments.sortedByDescending { it.length }
    val blockComments = spec.blockComments.sortedByDescending { it.first.length }
    val blockStrings = spec.blockStrings.sortedByDescending { it.first.length }
    val tripleStrings = spec.tripleStrings.sortedByDescending { it.length }
    val operators = spec.operators.sortedByDescending { it.length }

    val keywords = normalizeWords(spec.keywords, spec.caseInsensitiveWords)
    val builtins = normalizeWords(spec.builtins, spec.caseInsensitiveWords)
    val types = normalizeWords(spec.types, spec.caseInsensitiveWords)

    while (pos < code.length) {
        val fixedToken = fixedTokens.firstOrNull { code.startsWith(it, pos) }
        if (fixedToken != null) {
            val end = pos + fixedToken.length
            tokens.add(CodeToken(spec.fixedTokens.getValue(fixedToken), code.substring(pos, end), pos until end))
            pos = end
            continue
        }

        val blockComment = blockComments.firstOrNull { code.startsWith(it.first, pos) }
        if (blockComment != null) {
            val start = pos
            val end = findDelimitedEnd(code, pos + blockComment.first.length, blockComment.second)
            pos = end
            tokens.add(CodeToken(TokenType.COMMENT, code.substring(start, pos), start until pos))
            continue
        }

        val lineComment = lineComments.firstOrNull { code.startsWith(it, pos) }
        if (lineComment != null) {
            val start = pos
            while (pos < code.length && code[pos] != '\n') pos++
            tokens.add(CodeToken(TokenType.COMMENT, code.substring(start, pos), start until pos))
            continue
        }

        val blockString = blockStrings.firstOrNull { code.startsWith(it.first, pos) }
        if (blockString != null) {
            val start = pos
            val end = findDelimitedEnd(code, pos + blockString.first.length, blockString.second)
            pos = end
            tokens.add(CodeToken(TokenType.STRING, code.substring(start, pos), start until pos))
            continue
        }

        val tripleString = tripleStrings.firstOrNull { code.startsWith(it, pos) }
        if (tripleString != null) {
            val start = pos
            val end = findDelimitedEnd(code, pos + tripleString.length, tripleString)
            pos = end
            tokens.add(CodeToken(TokenType.STRING, code.substring(start, pos), start until pos))
            continue
        }

        val annotationPrefix = spec.annotationPrefix
        if (annotationPrefix != null && code[pos] == annotationPrefix && pos + 1 < code.length && isWordStart(code[pos + 1], spec)) {
            val start = pos
            pos++
            while (pos < code.length && isWordPart(code[pos], spec)) pos++
            tokens.add(CodeToken(TokenType.ANNOTATION, code.substring(start, pos), start until pos))
            continue
        }

        val variablePrefix = variablePrefixes.firstOrNull { code.startsWith(it, pos) }
        if (variablePrefix != null) {
            val start = pos
            pos += variablePrefix.length
            when {
                pos < code.length && code[pos] == '{' -> {
                    pos++
                    var depth = 1
                    while (pos < code.length && depth > 0) {
                        when (code[pos]) {
                            '{' -> depth++
                            '}' -> depth--
                        }
                        pos++
                    }
                }
                pos < code.length && isWordStart(code[pos], spec) -> {
                    while (pos < code.length && isWordPart(code[pos], spec)) pos++
                }
                pos < code.length && code[pos].isDigit() -> {
                    while (pos < code.length && code[pos].isDigit()) pos++
                }
            }
            tokens.add(CodeToken(TokenType.VARIABLE, code.substring(start, pos), start until pos))
            continue
        }

        val current = code[pos]

        if (current in spec.stringQuotes) {
            val start = pos
            val quote = current
            pos++
            while (pos < code.length && code[pos] != quote && code[pos] != '\n') {
                if (code[pos] == '\\' && pos + 1 < code.length) pos++
                pos++
            }
            if (pos < code.length && code[pos] == quote) pos++
            tokens.add(CodeToken(TokenType.STRING, code.substring(start, pos), start until pos))
            continue
        }

        if (current.isDigit()) {
            val start = pos
            if (current == '0' && pos + 1 < code.length && (code[pos + 1] == 'x' || code[pos + 1] == 'X')) {
                pos += 2
                while (pos < code.length && (code[pos].isDigit() || code[pos] in 'a'..'f' || code[pos] in 'A'..'F' || code[pos] == '_')) pos++
            } else if (current == '0' && pos + 1 < code.length && (code[pos + 1] == 'b' || code[pos + 1] == 'B')) {
                pos += 2
                while (pos < code.length && (code[pos] == '0' || code[pos] == '1' || code[pos] == '_')) pos++
            } else if (current == '0' && pos + 1 < code.length && (code[pos + 1] == 'o' || code[pos + 1] == 'O')) {
                pos += 2
                while (pos < code.length && (code[pos] in '0'..'7' || code[pos] == '_')) pos++
            } else {
                while (pos < code.length && (code[pos].isDigit() || code[pos] == '_')) pos++
                if (pos < code.length && code[pos] == '.') {
                    pos++
                    while (pos < code.length && (code[pos].isDigit() || code[pos] == '_')) pos++
                }
                if (pos < code.length && (code[pos] == 'e' || code[pos] == 'E')) {
                    pos++
                    if (pos < code.length && (code[pos] == '+' || code[pos] == '-')) pos++
                    while (pos < code.length && code[pos].isDigit()) pos++
                }
            }
            tokens.add(CodeToken(TokenType.NUMBER, code.substring(start, pos), start until pos))
            continue
        }

        if (isWordStart(current, spec)) {
            val start = pos
            pos++
            while (pos < code.length && isWordPart(code[pos], spec)) pos++
            val word = code.substring(start, pos)
            val normalized = if (spec.caseInsensitiveWords) word.lowercase() else word
            val type = when {
                normalized in keywords -> TokenType.KEYWORD
                normalized in builtins -> TokenType.BUILTIN
                normalized in types -> TokenType.TYPE
                spec.uppercaseIdentifiersAreTypes && word.firstOrNull()?.isUpperCase() == true -> TokenType.TYPE
                pos < code.length && code[pos] == '(' -> TokenType.FUNCTION
                else -> TokenType.IDENTIFIER
            }
            tokens.add(CodeToken(type, word, start until pos))
            continue
        }

        val operator = operators.firstOrNull { code.startsWith(it, pos) }
        if (operator != null) {
            val end = pos + operator.length
            tokens.add(CodeToken(TokenType.OPERATOR, code.substring(pos, end), pos until end))
            pos = end
            continue
        }

        if (current in spec.punctuation) {
            tokens.add(CodeToken(TokenType.PUNCTUATION, current.toString(), pos until pos + 1))
            pos++
            continue
        }

        tokens.add(CodeToken(TokenType.PLAIN, current.toString(), pos until pos + 1))
        pos++
    }

    return tokens
}

private fun normalizeWords(words: Set<String>, caseInsensitive: Boolean): Set<String> {
    return if (caseInsensitive) words.map { it.lowercase() }.toSet() else words
}

private fun findDelimitedEnd(code: String, from: Int, endDelimiter: String): Int {
    val endIndex = code.indexOf(endDelimiter, from)
    return if (endIndex >= 0) endIndex + endDelimiter.length else code.length
}

private fun isWordStart(char: Char, spec: ConfigurableLexerSpec): Boolean {
    return char.isLetter() || char == '_' || char in spec.extraWordStartChars
}

private fun isWordPart(char: Char, spec: ConfigurableLexerSpec): Boolean {
    return char.isLetterOrDigit() || char == '_' || char in spec.extraWordChars || char in spec.extraWordStartChars
}
