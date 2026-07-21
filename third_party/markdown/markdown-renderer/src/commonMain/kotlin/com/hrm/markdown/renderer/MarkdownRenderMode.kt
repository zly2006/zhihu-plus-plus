package com.hrm.markdown.renderer

internal enum class MarkdownRenderMode {
    SelectableColumn,
    StaticColumn,
    LazyColumn,
}

internal fun resolveMarkdownRenderMode(
    enableSelection: Boolean,
    enableScroll: Boolean,
    isStreaming: Boolean,
): MarkdownRenderMode {
    if (isStreaming) return MarkdownRenderMode.StaticColumn
    if (enableSelection) return MarkdownRenderMode.SelectableColumn
    if (enableScroll) return MarkdownRenderMode.LazyColumn
    return MarkdownRenderMode.StaticColumn
}
