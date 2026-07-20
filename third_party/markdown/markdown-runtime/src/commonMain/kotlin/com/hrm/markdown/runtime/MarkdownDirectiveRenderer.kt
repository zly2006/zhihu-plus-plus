package com.hrm.markdown.runtime

import androidx.compose.runtime.Composable

/**
 * 外部块级 directive 原生渲染器。
 */
typealias MarkdownBlockDirectiveRenderer = @Composable (DirectiveBlockRenderScope) -> Unit

data class DirectiveBlockSnapshot(
    val tagName: String,
    val args: Map<String, String>,
    val hasContent: Boolean,
)

/**
 * 向外部插件暴露的 directive 渲染上下文。
 */
data class DirectiveBlockRenderScope(
    val directive: DirectiveBlockSnapshot,
    val content: (@Composable () -> Unit)? = null,
)

/**
 * 外部行内 directive 原生渲染器。
 */
typealias MarkdownInlineDirectiveRenderer = @Composable (DirectiveInlineRenderScope) -> Unit

data class DirectiveInlineSnapshot(
    val tagName: String,
    val args: Map<String, String>,
    val alternateText: String,
)

/**
 * 向外部插件暴露的行内 directive 渲染上下文。
 */
data class DirectiveInlineRenderScope(
    val directive: DirectiveInlineSnapshot,
)
