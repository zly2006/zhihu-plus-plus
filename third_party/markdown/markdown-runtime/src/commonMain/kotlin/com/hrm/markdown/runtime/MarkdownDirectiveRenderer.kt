package com.hrm.markdown.runtime

import androidx.compose.runtime.Composable
import com.hrm.markdown.parser.ast.DirectiveBlock
import com.hrm.markdown.parser.ast.DirectiveInline

/**
 * 外部块级 directive 原生渲染器。
 */
typealias MarkdownBlockDirectiveRenderer = @Composable (DirectiveBlockRenderScope) -> Unit

/**
 * 向外部插件暴露的 directive 渲染上下文。
 */
data class DirectiveBlockRenderScope(
    val tagName: String,
    val args: Map<String, String>,
    val node: DirectiveBlock,
    val content: (@Composable () -> Unit)? = null,
)

/**
 * 外部行内 directive 原生渲染器。
 */
typealias MarkdownInlineDirectiveRenderer = @Composable (DirectiveInlineRenderScope) -> Unit

/**
 * 向外部插件暴露的行内 directive 渲染上下文。
 */
data class DirectiveInlineRenderScope(
    val tagName: String,
    val args: Map<String, String>,
    val node: DirectiveInline,
    val alternateText: String,
)
