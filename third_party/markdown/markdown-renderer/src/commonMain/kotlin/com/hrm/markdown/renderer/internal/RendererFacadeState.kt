package com.hrm.markdown.renderer.internal

import com.hrm.codehigh.theme.CodeTheme
import com.hrm.markdown.renderer.MarkdownConfig
import com.hrm.markdown.renderer.MarkdownImageRenderer
import com.hrm.markdown.renderer.MarkdownTheme
import com.hrm.markdown.runtime.MarkdownDirectiveRegistry

data class RendererFacadeState(
    val theme: MarkdownTheme,
    val config: MarkdownConfig,
    val codeTheme: CodeTheme?,
    val imageRenderer: MarkdownImageRenderer?,
    val onLinkClick: ((String) -> Unit)?,
    val directiveRegistry: MarkdownDirectiveRegistry,
    val isStreaming: Boolean,
    val enableSelection: Boolean,
)
