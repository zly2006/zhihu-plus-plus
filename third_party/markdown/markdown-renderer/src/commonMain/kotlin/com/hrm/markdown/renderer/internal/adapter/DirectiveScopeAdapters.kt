package com.hrm.markdown.renderer.internal.adapter

import androidx.compose.runtime.Composable
import com.hrm.markdown.runtime.DirectiveBlockSnapshot
import com.hrm.markdown.runtime.DirectiveBlockRenderScope
import com.hrm.markdown.runtime.DirectiveInlineSnapshot
import com.hrm.markdown.runtime.DirectiveInlineRenderScope

internal fun createDirectiveBlockRenderScope(
    tagName: String,
    args: Map<String, String>,
    content: (@Composable () -> Unit)? = null,
): DirectiveBlockRenderScope {
    return DirectiveBlockRenderScope(
        directive = DirectiveBlockSnapshot(
            tagName = tagName,
            args = args,
            hasContent = content != null,
        ),
        content = content,
    )
}

internal fun createDirectiveInlineRenderScope(
    tagName: String,
    args: Map<String, String>,
    alternateText: String,
): DirectiveInlineRenderScope {
    return DirectiveInlineRenderScope(
        directive = DirectiveInlineSnapshot(
            tagName = tagName,
            args = args,
            alternateText = alternateText,
        ),
    )
}
