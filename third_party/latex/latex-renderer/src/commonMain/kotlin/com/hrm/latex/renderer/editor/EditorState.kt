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

package com.hrm.latex.renderer.editor

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hrm.latex.parser.LatexParser
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.LayoutMap

/**
 * LaTeX 编辑器状态机
 *
 * 管理文本内容、光标位置、选区、解析结果和布局映射。
 * 所有状态变更通过该类的方法进行，确保一致性。
 *
 * 注意：编辑器功能目前处于实验阶段（Experimental），API 可能在后续版本中变更。
 *
 * @param initialText 初始 LaTeX 文本
 */
@Stable
class EditorState(initialText: String = "") {

    /** 当前 LaTeX 文本内容 */
    var text by mutableStateOf(initialText)
        private set

    /** 光标在源码中的字符偏移量 */
    var cursorOffset by mutableIntStateOf(initialText.length)
        private set

    /** 选区范围（null 表示无选区） */
    var selection by mutableStateOf<IntRange?>(null)
        private set

    /** 解析后的 AST 文档 */
    var document by mutableStateOf(LatexNode.Document(emptyList()))
        private set

    /** 布局映射表（由 LatexEditor 测量阶段填充） */
    val layoutMap = LayoutMap()

    /** 解析是否成功 */
    var parseSuccess by mutableStateOf(true)
        private set

    private val parser = LatexParser()

    init {
        if (initialText.isNotEmpty()) {
            reparse()
        }
    }

    /**
     * 替换全部文本
     */
    fun updateText(newText: String) {
        text = newText
        cursorOffset = newText.length.coerceAtMost(newText.length)
        selection = null
        reparse()
    }

    /**
     * 在光标位置插入文本
     */
    fun insert(insertion: String) {
        val before = text.substring(0, cursorOffset)
        val after = text.substring(cursorOffset)
        text = before + insertion + after
        cursorOffset += insertion.length
        selection = null
        reparse()
    }

    /**
     * 插入模板并将光标定位到模板内的占位位置
     *
     * @param template 要插入的 [LatexTemplate]
     */
    fun insertTemplate(template: LatexTemplate) {
        val expanded = template.expand()
        val before = text.substring(0, cursorOffset)
        val after = text.substring(cursorOffset)
        text = before + expanded.text + after
        cursorOffset += expanded.cursorDelta
        selection = null
        reparse()
    }

    /**
     * 删除光标前的一个字符（Backspace 行为）
     */
    fun backspace() {
        if (selection != null) {
            deleteSelection()
            return
        }
        if (cursorOffset > 0) {
            val before = text.substring(0, cursorOffset - 1)
            val after = text.substring(cursorOffset)
            text = before + after
            cursorOffset--
            reparse()
        }
    }

    /**
     * 删除光标后的一个字符（Delete 行为）
     */
    fun delete() {
        if (selection != null) {
            deleteSelection()
            return
        }
        if (cursorOffset < text.length) {
            val before = text.substring(0, cursorOffset)
            val after = text.substring(cursorOffset + 1)
            text = before + after
            reparse()
        }
    }

    /**
     * 删除选区内容
     */
    private fun deleteSelection() {
        val sel = selection ?: return
        val before = text.substring(0, sel.first)
        val after = text.substring(sel.last + 1)
        text = before + after
        cursorOffset = sel.first
        selection = null
        reparse()
    }

    /**
     * 移动光标到指定偏移量
     */
    fun moveCursorTo(offset: Int) {
        cursorOffset = offset.coerceIn(0, text.length)
        selection = null
    }

    /**
     * 设置选区
     */
    fun updateSelection(range: IntRange?) {
        selection = range?.let {
            val start = it.first.coerceIn(0, text.length)
            val end = it.last.coerceIn(0, text.length)
            if (start <= end) start..end else null
        }
    }

    /**
     * 光标左移
     */
    fun moveCursorLeft() {
        if (cursorOffset > 0) {
            cursorOffset--
            selection = null
        }
    }

    /**
     * 光标右移
     */
    fun moveCursorRight() {
        if (cursorOffset < text.length) {
            cursorOffset++
            selection = null
        }
    }

    /**
     * 导航到下一个 Slot
     *
     * 在复合结构（分数、上下标、根号等）内使用 Enter 键跳转到下一个输入槽位。
     * 例如在 `\frac{|}{}` 中按 Enter → 光标跳到分母 `\frac{}{|}`，
     * 再按 Enter → 光标跳到分数之后 `\frac{}{}|`。
     *
     * @return true 如果成功导航到下一个 slot，false 如果当前不在复合结构内
     */
    fun navigateToNextSlot(): Boolean {
        val nextOffset = SlotNavigator.nextSlotOffset(cursorOffset, document) ?: return false
        moveCursorTo(nextOffset)
        return true
    }

    private fun reparse() {
        try {
            document = parser.parse(text)
            parseSuccess = true
        } catch (_: Exception) {
            parseSuccess = false
        }
    }
}
