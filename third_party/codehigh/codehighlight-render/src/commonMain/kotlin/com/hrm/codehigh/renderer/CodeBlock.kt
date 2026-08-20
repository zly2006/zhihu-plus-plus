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

package com.hrm.codehigh.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrm.codehigh.ast.CodeToken
import com.hrm.codehigh.ast.TokenType
import com.hrm.codehigh.i18n.Strings
import com.hrm.codehigh.stream.IncrementalHighlighter
import com.hrm.codehigh.theme.CodeTheme
import com.hrm.codehigh.theme.LocalCodeTheme
import kotlinx.coroutines.delay

/**
 * 代码块渲染组件，唯一对外渲染入口。
 * 对外公开，支持完整的代码高亮功能。
 *
 * @param code 代码字符串
 * @param language 语言标识符（如 "kotlin"、"python"）
 * @param modifier Compose Modifier
 * @param isStreaming 是否处于流式输出状态，为 true 时在末尾显示光标动画
 * @param theme 代码主题，默认使用 LocalCodeTheme
 * @param showLineNumbers 是否显示行号
 * @param showCopyButton 是否显示复制按钮
 * @param maxVisibleLines 最大可见行数，超出时显示折叠按钮（null 表示不限制）
 * @param onTokenClick Token 点击回调
 */
@Composable
fun CodeBlock(
    code: String,
    language: String = "",
    title: String = "",
    modifier: Modifier = Modifier,
    isStreaming: Boolean = false,
    theme: CodeTheme = LocalCodeTheme.current,
    showLineNumbers: Boolean = false,
    startLine: Int = 1,
    highlightedLines: Set<Int> = emptySet(),
    showCopyButton: Boolean = true,
    maxVisibleLines: Int? = null,
    onTokenClick: ((CodeToken) -> Unit)? = null
) {
    val highlighter = remember(language) { IncrementalHighlighter() }

    var isExpanded by remember { mutableStateOf(false) }
    val lines = code.split("\n")
    val totalLines = lines.size
    val isCollapsible = maxVisibleLines != null && totalLines > maxVisibleLines
    val visibleLineCount = when {
        !isCollapsible || isExpanded -> totalLines
        else -> maxVisibleLines // isCollapsible 为 true 时 maxVisibleLines 一定非空
    }

    val visibleLines = lines.take(visibleLineCount)
    val visibleCode = visibleLines.joinToString("\n")
    val visibleAst = remember(visibleCode, language) { highlighter.update(visibleCode, language) }
    val lineHighlights = remember(visibleAst, theme, language, highlightedLines, visibleLineCount) {
        buildLineRenders(
            sourceLines = visibleLines,
            tokens = visibleAst.tokens,
            theme = theme,
            language = language,
            highlightedLines = highlightedLines,
        )
    }
    val fallbackToPlainLines = remember(lineHighlights, visibleLines) {
        visibleLines.isNotEmpty() && (
            lineHighlights.isEmpty() ||
                lineHighlights.size < visibleLines.size ||
                lineHighlights.all { it.text.text.isBlank() }
            )
    }
    val showToolbar = title.isNotBlank() || language.isNotBlank() || showCopyButton
    val density = LocalDensity.current
    val codeLineHeight = 20.sp
    val codeLineHeightDp = with(density) { codeLineHeight.toDp() }
    val lineNumberStyle = remember(theme) {
        TextStyle(
            color = theme.colorFor(TokenType.COMMENT).copy(alpha = 0.5f),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = codeLineHeight,
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
    val diffMarkerStyle = remember(theme) {
        TextStyle(
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            lineHeight = codeLineHeight
        )
    }

    Column(
        modifier = modifier
            .background(theme.background)
            .fillMaxWidth()
    ) {
        if (showToolbar) {
            DisableSelection {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CodeBlockHeaderLabels(
                        title = title,
                        language = language,
                        theme = theme,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    if (showCopyButton) {
                        CopyButton(code = code, theme = theme)
                    }
                }
            }
        }

        // 代码内容区域（逐行渲染，行号与代码行严格对齐）
        val horizontalScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                val renderedLines = if (fallbackToPlainLines) {
                    visibleLines.map { line ->
                        CodeLineRender(
                            kind = CodeLineKind.NORMAL,
                            text = AnnotatedString(line),
                        )
                    }
                } else {
                    lineHighlights
                }
                renderedLines.forEachIndexed { index, lineRender ->
                    key(index) {
                        val isLastLine = index == renderedLines.lastIndex
                        val diffMarker = diffMarkerForLine(lineRender.kind)
                        val rawLine = visibleLines.getOrElse(index) { "" }
                        val displayText = if (lineRender.text.text.isEmpty() && rawLine.isNotEmpty()) {
                            AnnotatedString(rawLine)
                        } else {
                            lineRender.text
                        }
                        Row(
                            modifier = Modifier.height(codeLineHeightDp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (showLineNumbers) {
                                DisableSelection {
                                    BasicText(
                                        text = (startLine + index).toString(),
                                        style = lineNumberStyle,
                                        modifier = Modifier
                                            .width(40.dp)
                                            .padding(end = 8.dp),
                                        maxLines = 1
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(codeLineHeightDp)
                                            .background(
                                                theme.colorFor(TokenType.COMMENT)
                                                    .copy(alpha = 0.3f)
                                            )
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .background(theme.backgroundForLine(lineRender.kind))
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                if (diffMarker != null) {
                                    BasicText(
                                        text = diffMarker,
                                        style = diffMarkerStyle.copy(
                                            color = theme.diffMarkerColorForLine(lineRender.kind)
                                        ),
                                        modifier = Modifier
                                            .background(theme.diffMarkerBackgroundForLine(lineRender.kind))
                                            .padding(horizontal = 6.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                BasicText(
                                    text = displayText,
                                    style = theme.textStyleForLine(lineRender.kind)
                                )
                                if (isLastLine) {
                                    StreamingCursor(
                                        isStreaming = isStreaming,
                                        color = theme.colorFor(TokenType.PLAIN)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 折叠/展开按钮
        if (isCollapsible) {
            DisableSelection {
                BasicText(
                    text = if (isExpanded) Strings.collapse() else Strings.expand(totalLines - visibleLineCount),
                    style = TextStyle(
                        color = theme.colorFor(TokenType.FUNCTION),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clickable { isExpanded = !isExpanded }
                )
            }
        }
    }
}

private fun diffMarkerForLine(kind: CodeLineKind): String? = when (kind) {
    CodeLineKind.DIFF_ADDED -> "+"
    CodeLineKind.DIFF_REMOVED -> "-"
    CodeLineKind.DIFF_META_HEADER -> "⋯"
    CodeLineKind.DIFF_META_HUNK -> "@"
    else -> null
}

private fun CodeTheme.diffMarkerBackgroundForLine(kind: CodeLineKind) = when (kind) {
    CodeLineKind.DIFF_ADDED -> if (isDark) androidx.compose.ui.graphics.Color(0xFF224D35) else androidx.compose.ui.graphics.Color(0xFFD9F5E0)
    CodeLineKind.DIFF_REMOVED -> if (isDark) androidx.compose.ui.graphics.Color(0xFF5A2730) else androidx.compose.ui.graphics.Color(0xFFFADADD)
    CodeLineKind.DIFF_META_HEADER -> if (isDark) androidx.compose.ui.graphics.Color(0xFF374151) else androidx.compose.ui.graphics.Color(0xFFE5E7EB)
    CodeLineKind.DIFF_META_HUNK -> if (isDark) androidx.compose.ui.graphics.Color(0xFF1F4B70) else androidx.compose.ui.graphics.Color(0xFFDCEEFF)
    else -> androidx.compose.ui.graphics.Color.Transparent
}

private fun CodeTheme.diffMarkerColorForLine(kind: CodeLineKind) = when (kind) {
    CodeLineKind.DIFF_ADDED -> if (isDark) androidx.compose.ui.graphics.Color(0xFF9BE9A8) else androidx.compose.ui.graphics.Color(0xFF1F7A38)
    CodeLineKind.DIFF_REMOVED -> if (isDark) androidx.compose.ui.graphics.Color(0xFFFFA8B5) else androidx.compose.ui.graphics.Color(0xFFB42318)
    CodeLineKind.DIFF_META_HEADER -> if (isDark) androidx.compose.ui.graphics.Color(0xFFD1D5DB) else androidx.compose.ui.graphics.Color(0xFF4B5563)
    CodeLineKind.DIFF_META_HUNK -> if (isDark) androidx.compose.ui.graphics.Color(0xFF9CDCFE) else androidx.compose.ui.graphics.Color(0xFF0958D9)
    else -> colorFor(TokenType.PLAIN)
}

private fun CodeTheme.textColorForLine(kind: CodeLineKind) = when (kind) {
    CodeLineKind.DIFF_META_HEADER -> if (isDark) androidx.compose.ui.graphics.Color(0xFFE5E7EB) else androidx.compose.ui.graphics.Color(0xFF374151)
    CodeLineKind.DIFF_META_HUNK -> if (isDark) androidx.compose.ui.graphics.Color(0xFFBFE3FF) else androidx.compose.ui.graphics.Color(0xFF0B4F8A)
    else -> colorFor(TokenType.PLAIN)
}

private fun CodeTheme.fontWeightForLine(kind: CodeLineKind) = when (kind) {
    CodeLineKind.DIFF_META_HEADER, CodeLineKind.DIFF_META_HUNK -> FontWeight.SemiBold
    else -> FontWeight.Normal
}

private fun CodeTheme.textStyleForLine(kind: CodeLineKind) = when (kind) {
    CodeLineKind.DIFF_META_HEADER, CodeLineKind.DIFF_META_HUNK -> TextStyle(
        color = textColorForLine(kind),
        fontSize = 13.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = fontWeightForLine(kind),
        lineHeight = 20.sp
    )
    else -> TextStyle(
        color = textColorForLine(kind),
        fontSize = 13.sp,
        fontFamily = FontFamily.Monospace,
        lineHeight = 20.sp
    )
}

/**
 * 复制按钮组件。
 * 标记为 internal，仅供 CodeBlock 内部使用。
 */
@Composable
internal fun CopyButton(
    code: String,
    theme: CodeTheme
) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }

    DisableSelection {
        BasicText(
            text = if (copied) "✓ ${Strings.copied()}" else Strings.copy(),
            style = TextStyle(
                color = theme.colorFor(TokenType.FUNCTION).copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            ),
            modifier = Modifier
                .padding(4.dp)
                .clickable {
                    @Suppress("DEPRECATION")
                    clipboardManager.setText(AnnotatedString(code))
                    copied = true
                }
        )
    }
}
