package com.hrm.markdown.renderer.internal.core.compile

import com.hrm.markdown.runtime.MarkdownDirectiveRegistry

data class RenderCompileEnvironment(
    val theme: RenderThemeSnapshot = RenderThemeSnapshot(),
    val config: RenderConfigSnapshot = RenderConfigSnapshot(),
    val directiveRegistry: MarkdownDirectiveRegistry = MarkdownDirectiveRegistry.Empty,
)

data class RenderThemeSnapshot(
    val darkMode: Boolean = false,
)

data class RenderConfigSnapshot(
    val enableHeadingNumbering: Boolean = false,
    val streaming: Boolean = false,
)
