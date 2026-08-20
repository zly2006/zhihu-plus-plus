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

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrm.latex.renderer.EditorRenderInfo
import com.hrm.latex.renderer.LatexEditorCanvas
import com.hrm.latex.renderer.model.LatexConfig
import kotlinx.coroutines.delay

/**
 * LaTeX 编辑器组件
 *
 * 三层架构：
 * 1. **隐藏的 BasicTextField** — 接收键盘输入和 IME 事件
 * 2. **LatexEditorCanvas** — 渲染 LaTeX 公式
 * 3. **光标/选区叠加层** — 绘制闪烁光标和选区高亮
 *
 * 注意：编辑器功能目前处于实验阶段（Experimental），API 可能在后续版本中变更。
 * 使用 @ExperimentalComposeUiApi 标注，表明此 API 尚未稳定。
 *
 * @param editorState 编辑器状态（通过 [rememberEditorState] 创建）
 * @param modifier 修饰符
 * @param config 渲染配置
 * @param isDarkTheme 当前环境是否为深色模式。
 * 仅在 `config.theme = LatexTheme.auto(...)` 时用于选择 light/dark 色板。
 * @param cursorColor 光标颜色
 * @param selectionColor 选区背景色
 * @param showSourceText 是否显示源码文本框（调试用）
 */
@ExperimentalComposeUiApi
@Composable
fun LatexEditor(
    editorState: EditorState,
    modifier: Modifier = Modifier,
    config: LatexConfig = LatexConfig(),
    isDarkTheme: Boolean = false,
    cursorColor: Color = MaterialTheme.colorScheme.primary,
    selectionColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
    showSourceText: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }
    val density = LocalDensity.current

    // 渲染信息（由 LatexEditorCanvas 回调更新）
    var renderInfo by remember { mutableStateOf<EditorRenderInfo?>(null) }

    // 焦点状态：只有获得焦点的编辑器才显示光标
    var isFocused by remember { mutableStateOf(false) }

    // 光标闪烁状态：仅在有焦点时闪烁
    var cursorVisible by remember { mutableStateOf(false) }
    LaunchedEffect(isFocused, editorState.cursorOffset, editorState.text) {
        if (!isFocused) {
            cursorVisible = false
            return@LaunchedEffect
        }
        // 获得焦点或光标移动/文本变化时重置闪烁
        cursorVisible = true
        while (true) {
            delay(530)
            cursorVisible = !cursorVisible
        }
    }

    // 同步 BasicTextField 的值与 EditorState
    var textFieldValue by remember(editorState.text, editorState.cursorOffset) {
        mutableStateOf(
            TextFieldValue(
                text = editorState.text,
                selection = TextRange(editorState.cursorOffset)
            )
        )
    }

    Column(modifier = modifier) {
        // 层1：渲染层 + 光标叠加层
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(4.dp)
                .pointerInput(renderInfo) {
                    detectTapGestures { offset ->
                        // 点击定位光标
                        val info = renderInfo ?: return@detectTapGestures
                        val newOffset = CursorCalculator.hitTestToOffset(
                            px = offset.x,
                            py = offset.y,
                            layoutMap = editorState.layoutMap,
                            horizontalPadding = info.horizontalPadding,
                            verticalPadding = info.verticalPadding,
                            textLength = editorState.text.length
                        )
                        editorState.moveCursorTo(newOffset)
                        focusRequester.requestFocus()
                    }
                }
        ) {
            LatexEditorCanvas(
                children = editorState.document.children,
                config = config,
                isDarkTheme = isDarkTheme,
                layoutMap = editorState.layoutMap,
                onRenderInfoChanged = { renderInfo = it },
                overlay = {
                    drawEditorOverlay(
                        editorState = editorState,
                        renderInfo = renderInfo,
                        cursorVisible = cursorVisible,
                        cursorColor = cursorColor,
                        selectionColor = selectionColor
                    )
                }
            )
        }

        // 层2：隐藏的 BasicTextField（接收键盘输入）
        // 使用极小的高度，视觉上不可见但保持焦点和 IME 连接
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                val oldText = textFieldValue.text
                val newText = newValue.text

                if (newText != oldText) {
                    // 检测 Enter 键：新文本中出现了 '\n'，尝试 Slot 导航
                    val isEnterInsert = newText.length == oldText.length + 1
                            && newText.contains('\n')
                            && !oldText.contains('\n')
                    if (isEnterInsert && editorState.navigateToNextSlot()) {
                        // Slot 导航成功，丢弃 '\n' 输入，同步 textFieldValue
                        textFieldValue = TextFieldValue(
                            text = editorState.text,
                            selection = TextRange(editorState.cursorOffset)
                        )
                        return@BasicTextField
                    }

                    // 非 Slot 导航场景：正常同步（过滤掉 '\n'）
                    val filteredText = newText.replace("\n", "")
                    if (filteredText != oldText) {
                        editorState.updateText(filteredText)
                    }
                }

                // 同步光标位置
                val newCursor = newValue.selection.start
                if (newCursor != editorState.cursorOffset) {
                    editorState.moveCursorTo(newCursor)
                }

                textFieldValue = TextFieldValue(
                    text = editorState.text,
                    selection = TextRange(editorState.cursorOffset)
                )
            },
            modifier = Modifier
                .focusRequester(focusRequester)
                .onFocusChanged { focusState -> isFocused = focusState.isFocused }
                .fillMaxWidth()
                .heightIn(min = if (showSourceText) 40.dp else 1.dp)
                .then(
                    if (!showSourceText) {
                        Modifier.padding(0.dp)
                    } else {
                        Modifier
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(8.dp)
                    }
                ),
            textStyle = if (showSourceText) {
                TextStyle(
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                TextStyle(fontSize = 1.sp, color = Color.Transparent)
            },
            cursorBrush = if (showSourceText) {
                SolidColor(MaterialTheme.colorScheme.primary)
            } else {
                SolidColor(Color.Transparent)
            },
            singleLine = false
        )
    }
}

/**
 * 绘制编辑器叠加层（光标 + 选区）
 */
private fun DrawScope.drawEditorOverlay(
    editorState: EditorState,
    renderInfo: EditorRenderInfo?,
    cursorVisible: Boolean,
    cursorColor: Color,
    selectionColor: Color
) {
    val info = renderInfo ?: return

    // 绘制选区高亮
    val selection = editorState.selection
    if (selection != null) {
        drawSelectionHighlight(
            selection = selection,
            editorState = editorState,
            renderInfo = info,
            color = selectionColor
        )
    }

    // 绘制光标
    if (cursorVisible && editorState.parseSuccess) {
        val cursorPos = CursorCalculator.calculate(
            cursorOffset = editorState.cursorOffset,
            layoutMap = editorState.layoutMap,
            horizontalPadding = info.horizontalPadding,
            verticalPadding = info.verticalPadding
        )
        if (cursorPos != null) {
            drawLine(
                color = cursorColor,
                start = Offset(cursorPos.x, cursorPos.y),
                end = Offset(cursorPos.x, cursorPos.y + cursorPos.height),
                strokeWidth = 2f
            )
        }
    }
}

/**
 * 绘制选区高亮
 */
private fun DrawScope.drawSelectionHighlight(
    selection: IntRange,
    editorState: EditorState,
    renderInfo: EditorRenderInfo,
    color: Color
) {
    val entries = editorState.layoutMap.entriesInRange(
        com.hrm.latex.parser.model.SourceRange(selection.first, selection.last + 1)
    )
    for (entry in entries) {
        val range = entry.node.sourceRange ?: continue

        // 计算选区在节点内的覆盖比例
        val overlapStart = maxOf(selection.first, range.start)
        val overlapEnd = minOf(selection.last + 1, range.end)
        if (overlapStart >= overlapEnd) continue

        val startRatio = if (range.length > 0) {
            (overlapStart - range.start).toFloat() / range.length
        } else 0f
        val endRatio = if (range.length > 0) {
            (overlapEnd - range.start).toFloat() / range.length
        } else 1f

        val x1 = entry.relX + entry.width * startRatio + renderInfo.horizontalPadding
        val x2 = entry.relX + entry.width * endRatio + renderInfo.horizontalPadding
        val y = entry.relY + renderInfo.verticalPadding

        drawRect(
            color = color,
            topLeft = Offset(x1, y),
            size = androidx.compose.ui.geometry.Size(x2 - x1, entry.height)
        )
    }
}

/**
 * 创建并记住一个 [EditorState] 实例
 *
 * 注意：编辑器功能目前处于实验阶段（Experimental），API 可能在后续版本中变更。
 *
 * @param initialText 初始 LaTeX 文本
 */
@ExperimentalComposeUiApi
@Composable
fun rememberEditorState(initialText: String = ""): EditorState {
    return remember { EditorState(initialText) }
}
