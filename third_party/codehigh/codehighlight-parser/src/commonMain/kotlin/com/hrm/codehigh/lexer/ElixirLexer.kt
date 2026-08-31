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

internal object ElixirLexer : BaseLexer() {
    private val spec = ConfigurableLexerSpec(
        keywords = setOf(
            "after", "alias", "case", "catch", "cond", "def", "defmodule", "defp", "defstruct",
            "do", "else", "end", "false", "fn", "for", "if", "import", "in", "nil", "quote",
            "raise", "receive", "require", "rescue", "super", "throw", "true", "try", "unless",
            "unquote", "use", "when", "with"
        ),
        builtins = setOf("IO.puts", "IO.inspect", "Enum.map", "Enum.each", "String.trim"),
        lineComments = listOf("#"),
        tripleStrings = setOf("\"\"\"", "'''"),
        stringQuotes = setOf('"', '\''),
        uppercaseIdentifiersAreTypes = true,
        extraWordChars = setOf('.', '?', '!'),
        operators = setOf("|>", "<-", "=>", "::", "==", "!=", "<=", ">=", "&&", "||", "++", "--", "\\\\", "=", "+", "-", "*", "/", "%", "<", ">", "&", "|", "!"),
        punctuation = setOf('{', '}', '(', ')', '[', ']', ';', ',', '.', ':')
    )

    override fun tokenize(code: String): List<CodeToken> = tokenizeWithSpec(code, spec)
}
