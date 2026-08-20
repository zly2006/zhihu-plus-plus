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

package com.hrm.latex.parser.incremental

import com.hrm.latex.parser.model.SourceRange
import com.hrm.latex.parser.tokenizer.LatexToken
import com.hrm.latex.parser.tokenizer.LatexTokenizer

/**
 * 增量词法分析器
 *
 * 参考 tree-sitter 的增量 tokenization 策略：
 * 当文本发生编辑时，只重新分词编辑影响的区域，
 * 前后未变更的 token 直接复用（偏移量平移）。
 *
 * 算法：
 * 1. 根据 [TextEdit] 确定编辑影响的 token 范围（脏区域）
 * 2. 脏区域前的 token 直接保留（偏移不变）
 * 3. 脏区域内重新分词
 * 4. 脏区域后的 token 平移偏移量后复用
 * 5. 拼接三段得到新 token 列表
 *
 * 收敛条件（关键优化）：
 * 重新分词时，一旦新产生的 token 与旧 token（平移后）完全匹配，即停止。
 * 这使得绝大多数编辑只需重新分词少量 token。
 */
class IncrementalTokenizer {

    /** 缓存的完整 token 列表（不含 EOF） */
    private var cachedTokens: MutableList<LatexToken> = mutableListOf()

    /** 当前文本内容（使用 StringBuilder 避免 append 场景 O(n²) 拷贝） */
    private val textBuffer: StringBuilder = StringBuilder()

    /** 缓存的不可变 token 列表（含 EOF），在 token 变更时失效 */
    private var cachedResult: List<LatexToken>? = null

    /**
     * 首次全量分词
     */
    fun tokenize(text: String): List<LatexToken> {
        textBuffer.clear()
        textBuffer.append(text)
        val tokenizer = LatexTokenizer(text)
        val allTokens = tokenizer.tokenize()
        // 移除末尾的 EOF token，我们自行管理
        // 使用 in-place 过滤替代 filterNot + toMutableList 双拷贝
        cachedTokens = ArrayList<LatexToken>(allTokens.size).also { list ->
            for (token in allTokens) {
                if (token !is LatexToken.EOF) list.add(token)
            }
        }
        invalidateResultCache()
        return allTokens
    }

    /**
     * 增量更新：根据编辑操作更新 token 列表
     *
     * @param newText 编辑后的完整文本
     * @param edit 编辑描述
     * @return 更新后的完整 token 列表（含 EOF）
     */
    fun update(newText: String, edit: TextEdit): List<LatexToken> {
        val oldTokens = cachedTokens

        // 空编辑（无变化）
        if (edit.delta == 0 && edit.oldLength == 0) {
            textBuffer.clear()
            textBuffer.append(newText)
            return buildResult(newText.length)
        }

        // 旧 token 为空或完全重写 → 全量分词
        if (oldTokens.isEmpty() || edit.startOffset == 0 && edit.oldEndOffset >= textBuffer.length) {
            return tokenize(newText)
        }

        val delta = edit.delta

        // === 第 1 步：确定脏区域在旧 token 列表中的范围 ===
        // 找到第一个被编辑影响的 token 索引
        val dirtyStart = findFirstAffectedToken(edit.startOffset)
        // 找到最后一个被编辑影响的 token 索引（在旧 token 列表中）
        val dirtyEnd = findLastAffectedToken(edit.oldEndOffset)

        // === 第 2 步：保留脏区域前的 token（偏移不变） ===
        // 不再拷贝 prefix，直接在 oldTokens 上按索引操作

        // === 第 3 步：确定重新分词的文本范围 ===
        // 重新分词的起点 = 第一个脏 token 的起始位置（在新文本中）
        val retokenizeStart = if (dirtyStart < oldTokens.size) {
            oldTokens[dirtyStart].range.start
        } else {
            edit.startOffset
        }

        // 后缀 token（编辑区域之后的旧 token，待偏移平移后复用）
        val suffixStartIdx = dirtyEnd + 1
        // 使用 subList view 而非 toList() 拷贝（只读用途）
        val suffixTokens: List<LatexToken> = if (suffixStartIdx < oldTokens.size) {
            oldTokens.subList(suffixStartIdx, oldTokens.size)
        } else {
            emptyList()
        }

        // 后缀 token 在新文本中的预期起始位置（用于收敛检测）
        val suffixExpectedStart = if (suffixTokens.isNotEmpty()) {
            suffixTokens.first().range.start + delta
        } else {
            newText.length
        }

        // === 第 4 步：从 retokenizeStart 开始对新文本进行分词，直到收敛 ===
        val newMiddleTokens = retokenizeRegion(
            newText,
            retokenizeStart,
            suffixExpectedStart,
            suffixTokens,
            delta
        )

        // === 第 5 步：拼接结果 ===
        // 如果重新分词超过了预期的后缀起点，需要裁剪后缀
        val adjustedSuffixTokens = adjustSuffix(
            suffixTokens, newMiddleTokens, delta, suffixExpectedStart
        )

        // 就地构建：prefix(0..dirtyStart) + middle + adjustedSuffix
        cachedTokens = ArrayList<LatexToken>(dirtyStart + newMiddleTokens.size + adjustedSuffixTokens.size).apply {
            for (i in 0 until dirtyStart) {
                add(oldTokens[i])
            }
            addAll(newMiddleTokens)
            addAll(adjustedSuffixTokens)
        }
        textBuffer.clear()
        textBuffer.append(newText)
        invalidateResultCache()

        return buildResult(newText.length)
    }

    /**
     * 获取当前文本
     */
    fun getCurrentText(): String = textBuffer.toString()

    /**
     * 直接在 StringBuilder 上追加文本，避免 toString() + 拼接的 O(n) 拷贝。
     * 返回追加后的完整文本长度。
     */
    fun appendText(text: String): Int {
        textBuffer.append(text)
        return textBuffer.length
    }

    /**
     * 获取当前文本长度（避免 toString() 分配）
     */
    fun getTextLength(): Int = textBuffer.length

    /**
     * 获取当前 token 列表（含 EOF），使用缓存避免重复创建
     */
    fun getCurrentTokens(): List<LatexToken> {
        cachedResult?.let { return it }
        val result = buildResult(textBuffer.length)
        cachedResult = result
        return result
    }

    /**
     * 查找第一个被编辑影响的 token 索引
     *
     * 使用 range.end >= editStart（而非严格 >），确保紧邻 token 末尾的
     * 插入操作也会将该 token 纳入脏区域。这是因为新字符可能与前一个 token
     * 的末尾字符合并（例如在 Text("ab") 后追加 "c" 应产生 Text("abc")）。
     */
    private fun findFirstAffectedToken(editStart: Int): Int {
        var lo = 0
        var hi = cachedTokens.size
        while (lo < hi) {
            val mid = (lo + hi) / 2
            if (cachedTokens[mid].range.end < editStart) {
                lo = mid + 1
            } else {
                hi = mid
            }
        }
        return lo
    }

    /**
     * 查找最后一个被编辑影响的 token 索引
     *
     * 使用 range.start <= editEnd（而非严格 <），确保编辑终点恰好在某个
     * token 起始位置时也会将该 token 纳入脏区域。
     */
    private fun findLastAffectedToken(editEnd: Int): Int {
        var lo = 0
        var hi = cachedTokens.size - 1
        var result = -1
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            if (cachedTokens[mid].range.start <= editEnd) {
                result = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return maxOf(result, 0)
    }

    /**
     * 对新文本的指定区域进行逐 token 重新分词（收敛即停）
     *
     * 从 [startOffset] 开始在 [newText] 上逐 token 扫描，每产出一个 token 就
     * 检查是否与后缀 token（偏移平移后）收敛。一旦收敛立即停止，避免对整个
     * 尾部进行全量分词。
     */
    private fun retokenizeRegion(
        newText: String,
        startOffset: Int,
        suffixExpectedStart: Int,
        suffixTokens: List<LatexToken>,
        delta: Int
    ): List<LatexToken> {
        if (startOffset >= newText.length) return emptyList()

        // 直接在完整 newText 上从 startOffset 开始逐 token 扫描
        val scanner = LatexTokenizer(newText, startOffset)
        val result = ArrayList<LatexToken>()

        // 预计算收敛目标（后缀首 token 偏移平移后的形态）
        val convergenceTarget = if (suffixTokens.isNotEmpty()) {
            shiftToken(suffixTokens.first(), delta)
        } else {
            null
        }

        while (true) {
            val token = scanner.nextToken()
            if (token is LatexToken.EOF) break

            // 收敛检测：新 token 已达到或超过后缀预期起始位置，且与后缀首 token 匹配
            if (convergenceTarget != null
                && token.range.start >= suffixExpectedStart
                && tokensMatch(token, convergenceTarget)
            ) {
                // 收敛！后续 token 必然与旧后缀完全相同，停止分词
                break
            }

            result.add(token)
        }

        return result
    }

    /**
     * 根据重新分词结果调整后缀 token
     */
    private fun adjustSuffix(
        suffixTokens: List<LatexToken>,
        newMiddleTokens: List<LatexToken>,
        delta: Int,
        suffixExpectedStart: Int
    ): List<LatexToken> {
        if (suffixTokens.isEmpty()) return emptyList()

        // 确定新中间 token 的结束位置
        val middleEnd = if (newMiddleTokens.isNotEmpty()) {
            newMiddleTokens.last().range.end
        } else {
            suffixExpectedStart
        }

        // 跳过被新中间 token 覆盖的后缀 token
        var skipCount = 0
        for (token in suffixTokens) {
            val shiftedStart = token.range.start + delta
            if (shiftedStart < middleEnd) {
                skipCount++
            } else {
                break
            }
        }

        // 对剩余后缀 token 进行偏移平移
        return suffixTokens.drop(skipCount).map { token ->
            shiftToken(token, delta)
        }
    }

    /**
     * 检查两个 token 是否"匹配"（类型相同、内容相同且位置相同）
     * 用于收敛检测。
     * 因为 LatexToken 子类全部是 data class，== 已正确实现语义相等。
     */
    private fun tokensMatch(a: LatexToken, b: LatexToken): Boolean = a == b

    /**
     * 将 token 的 SourceRange 平移指定偏移量。
     * 使用 LatexToken.withRange() 多态方法，消除 when 分派。
     */
    private fun shiftToken(token: LatexToken, offset: Int): LatexToken {
        if (offset == 0) return token
        val newRange = SourceRange(token.range.start + offset, token.range.end + offset)
        return token.withRange(newRange)
    }

    /**
     * 构建最终结果（含 EOF）。
     * 使用 ArrayList + add 替代 cachedTokens + EOF 的 list 拼接拷贝。
     */
    private fun buildResult(textLength: Int): List<LatexToken> {
        val result = ArrayList<LatexToken>(cachedTokens.size + 1)
        result.addAll(cachedTokens)
        result.add(LatexToken.EOF(SourceRange(textLength, textLength)))
        return result
    }

    /**
     * 失效结果缓存（在 token 列表或文本变更时调用）
     */
    private fun invalidateResultCache() {
        cachedResult = null
    }
}
