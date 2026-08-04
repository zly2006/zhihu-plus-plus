package com.hrm.markdown.parser.streaming

/**
 * 行内未关闭结构自动修复器。
 *
 * 用于 LLM 流式输出场景：大模型输出到一半时，
 * 强调、行内代码、链接等行内结构可能缺少关闭符号。
 * 本类通过轻量级单遍扫描分析未关闭的结构，
 * 并生成修复后缀追加到内容末尾，使行内解析器能产生正确的 AST。
 *
 * 性能：O(n) 单遍扫描，n 为输入内容长度。
 */
object InlineAutoCloser {

    /**
     * 分析内容中未关闭的行内结构，返回需要追加的修复后缀。
     * 如果内容完整（无未关闭结构），返回空字符串。
     */
    fun buildRepairSuffix(content: String): String {
        if (content.isEmpty()) return ""

        val state = ScanState()
        var i = 0
        while (i < content.length) {
            val c = content[i]
            when {
                // 转义：跳过下一个字符
                c == '\\' && i + 1 < content.length -> {
                    i += 2
                    continue
                }
                // 反引号：行内代码
                c == '`' -> {
                    val run = countRun(content, i, '`')
                    if (!state.tryCloseBackticks(run)) {
                        state.pushBackticks(run)
                    }
                    i += run
                    continue
                }
                // 如果在行内代码中，跳过所有内容直到扫描器自己处理
                state.inBackticks() -> {
                    i++
                    continue
                }
                // $ 符号：行内数学
                c == '$' -> {
                    if (i + 1 < content.length && content[i + 1] == '$') {
                        // $$ 行内数学
                        if (!state.tryCloseDoubleDollar()) {
                            state.pushDoubleDollar()
                        }
                        i += 2
                        continue
                    } else {
                        // 单 $ 行内数学
                        // 检查前面是否是数字（不作为数学公式）
                        val prevIsDigit = i > 0 && content[i - 1].isDigit()
                        if (!prevIsDigit) {
                            if (!state.tryCloseSingleDollar()) {
                                state.pushSingleDollar()
                            }
                        }
                        i++
                        continue
                    }
                }
                // 如果在数学公式中，跳过所有内容
                state.inMath() -> {
                    i++
                    continue
                }
                // * 和 _ 强调分隔符
                c == '*' || c == '_' -> {
                    val run = countRun(content, i, c)
                    state.handleEmphasisRun(c, run)
                    i += run
                    continue
                }
                // ~~ 删除线 / ~ 下标
                c == '~' -> {
                    val run = countRun(content, i, '~')
                    if (run == 2) {
                        state.handlePairedDelim('~', 2)
                    } else if (run == 1) {
                        state.handlePairedDelim('~', 1)
                    }
                    i += run
                    continue
                }
                // == 高亮
                c == '=' && i + 1 < content.length && content[i + 1] == '=' -> {
                    state.handlePairedDelim('=', 2)
                    i += 2
                    continue
                }
                // ++ 插入文本
                c == '+' && i + 1 < content.length && content[i + 1] == '+' -> {
                    state.handlePairedDelim('+', 2)
                    i += 2
                    continue
                }
                // ^ 上标
                c == '^' -> {
                    state.handlePairedDelim('^', 1)
                    i++
                    continue
                }
                // ![ 图片开始
                c == '!' && i + 1 < content.length && content[i + 1] == '[' -> {
                    state.pushBracket(isImage = true)
                    i += 2
                    continue
                }
                // [ 链接开始
                c == '[' -> {
                    state.pushBracket(isImage = false)
                    i++
                    continue
                }
                // ] 方括号关闭
                c == ']' -> {
                    if (state.hasBracket()) {
                        // 检查后面是否有 (
                        if (i + 1 < content.length && content[i + 1] == '(') {
                            // 进入链接 URL 模式
                            state.enterLinkUrl()
                            i += 2
                            // 扫描到 ) 或末尾
                            var depth = 1
                            while (i < content.length) {
                                when (content[i]) {
                                    ')' -> {
                                        depth--
                                        if (depth == 0) {
                                            state.closeLinkUrl()
                                            i++
                                            break
                                        }
                                    }
                                    '(' -> depth++
                                    '\\' -> if (i + 1 < content.length) i++ // 跳过转义
                                }
                                i++
                            }
                            if (depth > 0) {
                                // 未关闭的链接 URL
                                state.markLinkUrlUnclosed()
                            }
                            continue
                        } else {
                            // ] 后面没有 ( → 可能是引用链接或纯文本，关闭方括号
                            state.closeBracket()
                        }
                    }
                    i++
                    continue
                }
            }
            i++
        }

        return state.buildSuffix()
    }

    private fun countRun(content: String, start: Int, char: Char): Int {
        var count = 0
        var i = start
        while (i < content.length && content[i] == char) {
            count++
            i++
        }
        return count
    }

    // ────── 扫描状态 ──────

    private sealed class OpenStructure {
        /** 未关闭的反引号（行内代码） */
        data class Backticks(val count: Int) : OpenStructure()
        /** 未关闭的单 $ 行内数学 */
        data object SingleDollar : OpenStructure()
        /** 未关闭的 $$ 行内数学 */
        data object DoubleDollar : OpenStructure()
        /** 强调分隔符 */
        data class EmphasisDelim(val char: Char, val count: Int) : OpenStructure()
        /** 配对分隔符（~~, ==, ++, ^, ~） */
        data class PairedDelim(val char: Char, val count: Int) : OpenStructure()
        /** 未关闭的方括号 */
        data class Bracket(val isImage: Boolean) : OpenStructure()
        /** 未关闭的链接 URL：[text]( 后面没有 ) */
        data object UnclosedLinkUrl : OpenStructure()
    }

    private class ScanState {
        private val stack = mutableListOf<OpenStructure>()

        fun inBackticks(): Boolean = stack.lastOrNull() is OpenStructure.Backticks
        fun inMath(): Boolean {
            val last = stack.lastOrNull()
            return last is OpenStructure.SingleDollar || last is OpenStructure.DoubleDollar
        }

        fun pushBackticks(count: Int) {
            stack.add(OpenStructure.Backticks(count))
        }

        fun tryCloseBackticks(count: Int): Boolean {
            val last = stack.lastOrNull()
            if (last is OpenStructure.Backticks && last.count == count) {
                stack.removeAt(stack.size - 1)
                return true
            }
            return false
        }

        fun pushSingleDollar() {
            stack.add(OpenStructure.SingleDollar)
        }

        fun tryCloseSingleDollar(): Boolean {
            val last = stack.lastOrNull()
            if (last is OpenStructure.SingleDollar) {
                stack.removeAt(stack.size - 1)
                return true
            }
            return false
        }

        fun pushDoubleDollar() {
            stack.add(OpenStructure.DoubleDollar)
        }

        fun tryCloseDoubleDollar(): Boolean {
            val last = stack.lastOrNull()
            if (last is OpenStructure.DoubleDollar) {
                stack.removeAt(stack.size - 1)
                return true
            }
            return false
        }

        fun handleEmphasisRun(char: Char, count: Int) {
            // 简化模型：尝试从栈顶向下匹配关闭
            // 先尝试关闭已有的开启分隔符
            var remaining = count
            val toRemove = mutableListOf<Int>()
            for (idx in stack.indices.reversed()) {
                if (remaining <= 0) break
                val item = stack[idx]
                if (item is OpenStructure.EmphasisDelim && item.char == char) {
                    val consumed = minOf(remaining, item.count)
                    remaining -= consumed
                    val newCount = item.count - consumed
                    if (newCount <= 0) {
                        toRemove.add(idx)
                    } else {
                        stack[idx] = OpenStructure.EmphasisDelim(char, newCount)
                    }
                }
            }
            toRemove.sortedDescending().forEach { stack.removeAt(it) }
            // 如果还有剩余，作为新的开启分隔符
            if (remaining > 0) {
                stack.add(OpenStructure.EmphasisDelim(char, remaining))
            }
        }

        fun handlePairedDelim(char: Char, count: Int) {
            // 尝试关闭栈中同类型的分隔符
            for (idx in stack.indices.reversed()) {
                val item = stack[idx]
                if (item is OpenStructure.PairedDelim && item.char == char && item.count == count) {
                    stack.removeAt(idx)
                    return
                }
            }
            // 未找到匹配，作为新的开启分隔符
            stack.add(OpenStructure.PairedDelim(char, count))
        }

        fun pushBracket(isImage: Boolean) {
            stack.add(OpenStructure.Bracket(isImage))
        }

        fun hasBracket(): Boolean = stack.any { it is OpenStructure.Bracket }

        fun enterLinkUrl() {
            // 移除最近的 Bracket，替换为 UnclosedLinkUrl
            for (idx in stack.indices.reversed()) {
                if (stack[idx] is OpenStructure.Bracket) {
                    stack[idx] = OpenStructure.UnclosedLinkUrl
                    return
                }
            }
        }

        fun closeLinkUrl() {
            // 成功关闭了链接 URL，移除标记
            for (idx in stack.indices.reversed()) {
                if (stack[idx] is OpenStructure.UnclosedLinkUrl) {
                    stack.removeAt(idx)
                    return
                }
            }
        }

        fun markLinkUrlUnclosed() {
            // UnclosedLinkUrl 已在栈中，无需额外操作
        }

        fun closeBracket() {
            // 关闭最近的方括号
            for (idx in stack.indices.reversed()) {
                if (stack[idx] is OpenStructure.Bracket) {
                    stack.removeAt(idx)
                    return
                }
            }
        }

        /**
         * 构建修复后缀：从最内层（栈顶）向外逐层关闭。
         */
        fun buildSuffix(): String {
            if (stack.isEmpty()) return ""
            val sb = StringBuilder()
            for (item in stack.reversed()) {
                when (item) {
                    is OpenStructure.Backticks -> sb.append("`".repeat(item.count))
                    is OpenStructure.SingleDollar -> sb.append("$")
                    is OpenStructure.DoubleDollar -> sb.append("$$")
                    is OpenStructure.EmphasisDelim -> sb.append(item.char.toString().repeat(item.count))
                    is OpenStructure.PairedDelim -> sb.append(item.char.toString().repeat(item.count))
                    is OpenStructure.Bracket -> sb.append("]")
                    is OpenStructure.UnclosedLinkUrl -> sb.append(")")
                }
            }
            return sb.toString()
        }
    }
}
