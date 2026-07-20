package com.hrm.markdown.renderer

internal enum class MarkdownRenderMode {
    StaticColumn,
    LazyColumn,
}

internal fun resolveMarkdownRenderMode(enableScroll: Boolean): MarkdownRenderMode {
    if (enableScroll) {
        return MarkdownRenderMode.LazyColumn
    }
    return MarkdownRenderMode.StaticColumn
}
