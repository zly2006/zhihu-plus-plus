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


package com.hrm.latex.parser.tokenizer

import com.hrm.latex.base.log.HLog
import com.hrm.latex.parser.model.SourceRange

/**
 * LaTeX 词法分析器
 *
 * @param input 完整输入文本
 * @param startOffset 起始偏移量（默认 0）。用于增量分词时从指定位置开始扫描，
 *        产出的 token 的 SourceRange 直接反映在 input 中的全局位置。
 */
class LatexTokenizer(private val input: String, startOffset: Int = 0) {
    private var position = startOffset
    private val tokens = ArrayList<LatexToken>(estimateTokenCount(input.length - startOffset))

    companion object {
        private const val TAG = "LatexTokenizer"

        /**
         * handleText() 停止字符集 — 提取为 companion object 常量，
         * 避免每个字符循环迭代都创建新的 Set 实例（P0 热路径优化）。
         * 使用 BooleanArray 查表替代 Set.contains()，O(1) 且无装箱。
         */
        private val TEXT_STOP_CHARS = BooleanArray(128).apply {
            for (ch in charArrayOf(
                '\\', '{', '}', '[', ']', '^', '\'', '_', '&',
                '\n', '\r', '(', ')', '|', '.', '~', '%', '$',
                ' ', '\t', '+', '-', '=', '<', '>', ',', ';', ':'
            )) {
                this[ch.code] = true
            }
        }

        /** 判断字符是否为 text token 的停止字符 */
        @Suppress("NOTHING_TO_INLINE")
        private inline fun isTextStopChar(ch: Char): Boolean {
            val code = ch.code
            return code < 128 && TEXT_STOP_CHARS[code]
        }

        /**
         * 根据输入长度预估 token 数量，减少 ArrayList 扩容次数。
         * 经验值：平均每 4~5 个字符产生 1 个 token。
         */
        private fun estimateTokenCount(inputLength: Int): Int {
            return (inputLength / 4).coerceAtLeast(16)
        }
    }

    /**
     * 执行词法分析
     */
    fun tokenize(): List<LatexToken> {
        HLog.d(TAG) { "开始词法分析，输入长度: ${input.length}" }

        while (position < input.length) {
            when (val char = peek()) {
                '\\' -> handleBackslash()
                '%' -> handleComment()
                '~' -> {
                    val start = position
                    advance()
                    tokens.add(LatexToken.Whitespace("\u00A0", SourceRange(start, position)))
                }
                '{' -> {
                    val start = position
                    advance()
                    tokens.add(LatexToken.LeftBrace(SourceRange(start, position)))
                }

                '}' -> {
                    val start = position
                    advance()
                    tokens.add(LatexToken.RightBrace(SourceRange(start, position)))
                }

                '[' -> {
                    val start = position
                    advance()
                    tokens.add(LatexToken.LeftBracket(SourceRange(start, position)))
                }

                ']' -> {
                    val start = position
                    advance()
                    tokens.add(LatexToken.RightBracket(SourceRange(start, position)))
                }

                '^' -> {
                    val start = position
                    advance()
                    tokens.add(LatexToken.Superscript(SourceRange(start, position)))
                }

                '\'' -> {
                    val start = position
                    advance()
                    tokens.add(LatexToken.Prime(SourceRange(start, position)))
                }

                '_' -> {
                    val start = position
                    advance()
                    tokens.add(LatexToken.Subscript(SourceRange(start, position)))
                }

                '&' -> {
                    val start = position
                    advance()
                    tokens.add(LatexToken.Ampersand(SourceRange(start, position)))
                }

                '$' -> handleMathShift()
                '+', '-', '=', '<', '>', ',', ';', ':' -> {
                    val start = position
                    tokens.add(LatexToken.Text(char.toString(), SourceRange(start, start + 1)))
                    advance()
                }
                '(', ')', '|', '.' -> {
                    val start = position
                    tokens.add(LatexToken.Text(char.toString(), SourceRange(start, start + 1)))
                    advance()
                }

                '\n', '\r' -> handleNewLine()
                ' ', '\t' -> handleWhitespace()
                else -> handleText()
            }
        }

        tokens.add(LatexToken.EOF(SourceRange(position, position)))
        HLog.d(TAG) { "词法分析完成，生成 ${tokens.size} 个 token" }
        return tokens
    }

    private fun peek(offset: Int = 0): Char? {
        val pos = position + offset
        return if (pos < input.length) input[pos] else null
    }

    private fun advance(count: Int = 1): Char? {
        val char = peek()
        position += count
        return char
    }

    private fun handleBackslash() {
        val start = position
        advance() // 跳过 \

        if (peek() == '[' || peek() == ']') {
            // \[ ... \] 是 display math 定界符，语义上等价于 $$ ... $$
            advance()
            tokens.add(LatexToken.MathShift(2, SourceRange(start, position)))
            return
        }

        if (peek() == '\\') {
            // \\ 表示换行
            advance()
            tokens.add(LatexToken.NewLine(SourceRange(start, position)))
            return
        }

        // 读取命令名 — 使用 index tracking + substring 替代 buildString
        val cmdStart = position
        if (position < input.length) {
            val firstChar = input[position]
            if (firstChar.isAsciiLetter() || firstChar == '@') {
                position++
                while (position < input.length && input[position].isAsciiLetter()) {
                    position++
                }
            }
        }
        val commandName = if (position > cmdStart) input.substring(cmdStart, position) else ""

        if (commandName.isEmpty()) {
            // 处理特殊符号，如 \{, \}, \$, \% 等
            val char = peek()
            if (char != null && !char.isWhitespace()) {
                advance()
                tokens.add(LatexToken.Command(char.toString(), SourceRange(start, position)))
            } else if (char != null) {
                // \ 代表显式空格命令。这里消费该空白字符，避免后续生成额外 Whitespace token，
                // 否则会在 AST / 渲染层留下一个未知空命令节点。
                advance()
                tokens.add(LatexToken.Command(" ", SourceRange(start, position)))
            } else {
                // \ 后跟 EOF — 产生一个空命令 token 以确保反斜杠位置被覆盖。
                // 对增量分词至关重要：如果此处不产生 token，反斜杠的位置会成为"空洞"，
                // 后续追加的字符（如 \+i → \i）不会被正确关联为 Command token。
                tokens.add(LatexToken.Command("", SourceRange(start, position)))
            }
            return
        }

        // 检查是否是环境开始或结束
        when (commandName) {
            "begin" -> {
                val envName = readEnvironmentName()
                if (envName != null) {
                    tokens.add(LatexToken.BeginEnvironment(envName, SourceRange(start, position)))
                } else {
                    tokens.add(LatexToken.Command(commandName, SourceRange(start, position)))
                }
            }

            "end" -> {
                val envName = readEnvironmentName()
                if (envName != null) {
                    tokens.add(LatexToken.EndEnvironment(envName, SourceRange(start, position)))
                } else {
                    tokens.add(LatexToken.Command(commandName, SourceRange(start, position)))
                }
            }

            else -> {
                tokens.add(LatexToken.Command(commandName, SourceRange(start, position)))
            }
        }
    }

    private fun Char.isAsciiLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'

    private fun readEnvironmentName(): String? {
        skipWhitespace()
        if (peek() != '{') return null

        advance() // 跳过 {
        val nameStart = position
        while (position < input.length && input[position] != '}') {
            position++
        }
        val name = if (position > nameStart) input.substring(nameStart, position) else ""

        if (peek() == '}') {
            advance() // 跳过 }
            return name
        }
        return null
    }

    private fun handleText() {
        val start = position
        while (position < input.length && !isTextStopChar(input[position])) {
            position++
        }

        if (position > start) {
            tokens.add(LatexToken.Text(input.substring(start, position), SourceRange(start, position)))
        }
    }

    /**
     * 处理 $ 数学模式切换符
     * $$ → count=2（display math）
     * $  → count=1（inline math）
     */
    private fun handleMathShift() {
        val start = position
        advance() // 跳过第一个 $
        if (peek() == '$') {
            advance() // 跳过第二个 $
            tokens.add(LatexToken.MathShift(2, SourceRange(start, position)))
        } else {
            tokens.add(LatexToken.MathShift(1, SourceRange(start, position)))
        }
    }

    private fun handleWhitespace() {
        val start = position
        // 跳过所有连续的空格和制表符
        while (position < input.length) {
            val char = peek() ?: break
            if (char != ' ' && char != '\t') break
            advance()
        }

        // 将所有连续空白符合并为单个空格
        tokens.add(LatexToken.Whitespace(" ", SourceRange(start, position)))
    }

    /**
     * 处理普通换行符 (\n, \r)
     *
     * 注意：区别于 LaTeX 命令 `\\`（在 handleBackslash 中处理）
     *
     * 根据 LaTeX 规范：
     * - 单个换行符 → 视为空格
     * - 连续换行符（空行）→ 段落分隔（但在数学模式下通常被忽略）
     *
     * 当前实现：**将所有连续换行符转换为单个空格**
     *
     * 示例：
     * ```latex
     * x + y
     * + z     → 解析为 "x + y + z"（换行变空格）
     *
     * a=1
     *
     * b=2     → 解析为 "a=1 b=2"（空行也变单个空格）
     * ```
     *
     * 优点：
     * - 符合 LaTeX 标准：换行视为空格
     * - 避免文本模式下空格丢失（如 "x\ny" 正确解析为 "x y"）
     * - 简化逻辑，由解析器统一处理空格
     */
    private fun handleNewLine() {
        val start = position
        // 跳过所有连续的换行符
        while (position < input.length) {
            val char = peek() ?: break
            if (char != '\n' && char != '\r') break
            advance()
        }

        // 将换行符转换为单个空格 token
        // 这样 "x\ny" 会正确解析为 "x y" 而不是 "xy"
        tokens.add(LatexToken.Whitespace(" ", SourceRange(start, position)))
    }

    /**
     * 处理 % 注释
     * % 后到行末的内容应被忽略
     */
    private fun handleComment() {
        advance() // 跳过 %
        while (position < input.length) {
            val char = peek() ?: break
            if (char == '\n' || char == '\r') {
                // 跳过换行符本身
                advance()
                if (char == '\r' && peek() == '\n') {
                    advance() // 跳过 \r\n 中的 \n
                }
                break
            }
            advance()
        }
        // 注释不生成任何 token
    }

    private fun skipWhitespace() {
        while (position < input.length) {
            val char = peek() ?: break
            if (!char.isWhitespace()) break
            advance()
        }
    }

    // ========== 流式 API（增量分词使用） ==========

    /**
     * 扫描并返回下一个 token。
     *
     * 与 [tokenize] 不同，此方法不将 token 加入内部列表，
     * 而是直接返回。到达文本末尾时返回 EOF token。
     * 用于 [IncrementalTokenizer] 的逐 token 收敛分词。
     */
    fun nextToken(): LatexToken {
        if (position >= input.length) {
            return LatexToken.EOF(SourceRange(position, position))
        }

        val savedSize = tokens.size
        when (val char = peek()) {
            '\\' -> handleBackslash()
            '%' -> handleComment()
            '~' -> {
                val start = position
                advance()
                tokens.add(LatexToken.Whitespace("\u00A0", SourceRange(start, position)))
            }
            '{' -> {
                val start = position
                advance()
                tokens.add(LatexToken.LeftBrace(SourceRange(start, position)))
            }
            '}' -> {
                val start = position
                advance()
                tokens.add(LatexToken.RightBrace(SourceRange(start, position)))
            }
            '[' -> {
                val start = position
                advance()
                tokens.add(LatexToken.LeftBracket(SourceRange(start, position)))
            }
            ']' -> {
                val start = position
                advance()
                tokens.add(LatexToken.RightBracket(SourceRange(start, position)))
            }
            '^' -> {
                val start = position
                advance()
                tokens.add(LatexToken.Superscript(SourceRange(start, position)))
            }
            '\'' -> {
                val start = position
                advance()
                tokens.add(LatexToken.Prime(SourceRange(start, position)))
            }
            '_' -> {
                val start = position
                advance()
                tokens.add(LatexToken.Subscript(SourceRange(start, position)))
            }
            '&' -> {
                val start = position
                advance()
                tokens.add(LatexToken.Ampersand(SourceRange(start, position)))
            }
            '$' -> handleMathShift()
            '+', '-', '=', '<', '>', ',', ';', ':' -> {
                val start = position
                tokens.add(LatexToken.Text(char.toString(), SourceRange(start, start + 1)))
                advance()
            }
            '(', ')', '|', '.' -> {
                val start = position
                tokens.add(LatexToken.Text(char.toString(), SourceRange(start, start + 1)))
                advance()
            }
            '\n', '\r' -> handleNewLine()
            ' ', '\t' -> handleWhitespace()
            else -> handleText()
        }

        // handleComment 不产生 token（注释被吞掉），需要递归处理
        return if (tokens.size > savedSize) {
            tokens.removeAt(tokens.lastIndex)
        } else {
            // 注释被跳过了，递归取下一个
            nextToken()
        }
    }
}
