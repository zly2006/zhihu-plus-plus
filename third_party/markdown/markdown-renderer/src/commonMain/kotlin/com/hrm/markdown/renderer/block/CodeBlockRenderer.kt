package com.hrm.markdown.renderer.block

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.hrm.codehigh.renderer.CodeBlock
import com.hrm.codehigh.theme.LocalCodeTheme
import com.hrm.markdown.renderer.LocalCodeHighlightTheme
import com.hrm.markdown.renderer.LocalIsStreaming
import com.hrm.markdown.renderer.LocalMarkdownTheme

/**
 * 围栏代码块渲染器 (``` 或 ~~~)
 *
 * 支持通过 info-string 的 `{...}` 属性语法控制：
 * - **title**: 标题栏，如 `{title="main.kt"}`
 * - **linenos / lineNumbers**: 行号显示
 * - **highlight / hl_lines**: 高亮指定行，如 `{highlight="2,5-7"}`
 */
@Composable
internal fun FencedCodeBlockRenderer(
    text: String,
    language: String,
    title: String?,
    showLineNumbers: Boolean,
    startLine: Int,
    highlightedLines: Set<Int>,
    modifier: Modifier = Modifier,
) {
    CodeBlockText(
        text = text.ifEmpty { " " },
        language = language,
        title = title,
        showLineNumbers = showLineNumbers,
        startLine = startLine,
        highlightedLines = highlightedLines,
        modifier = modifier,
    )
}

/**
 * 缩进代码块渲染器
 */
@Composable
internal fun IndentedCodeBlockRenderer(
    text: String,
    modifier: Modifier = Modifier,
) {
    CodeBlockText(
        text = text.ifEmpty { " " },
        language = "",
        title = null,
        showLineNumbers = true,
        startLine = 1,
        highlightedLines = emptySet(),
        modifier = modifier,
    )
}

@Composable
private fun CodeBlockText(
    text: String,
    language: String,
    title: String?,
    showLineNumbers: Boolean,
    startLine: Int,
    highlightedLines: Set<Int>,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    val codeTheme = LocalCodeHighlightTheme.current ?: LocalCodeTheme.current
    val isStreaming = LocalIsStreaming.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(theme.codeBlockCornerRadius))
    ) {
        CodeBlock(
            code = text,
            language = language,
            title = title.orEmpty(),
            modifier = Modifier.fillMaxWidth(),
            isStreaming = isStreaming,
            theme = codeTheme,
            showLineNumbers = showLineNumbers,
            startLine = startLine,
            highlightedLines = highlightedLines,
        )
    }
}
