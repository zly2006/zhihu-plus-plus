package com.hrm.markdown.renderer.internal.layout.list

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hrm.markdown.renderer.MarkdownTheme

internal val UnorderedListMarkerWidth: Dp = 16.dp
internal val OrderedListMarkerWidth: Dp = 24.dp
internal val TaskListMarkerWidth: Dp = 20.dp

internal fun listMarkerWidth(
    taskListItem: Boolean,
    ordered: Boolean,
): Dp = when {
    taskListItem -> TaskListMarkerWidth
    ordered -> OrderedListMarkerWidth
    else -> UnorderedListMarkerWidth
}

internal fun Density.listItemContentIndentPx(
    theme: MarkdownTheme,
    taskListItem: Boolean,
    ordered: Boolean,
): Float = theme.listIndent.toPx() + listMarkerWidth(
    taskListItem = taskListItem,
    ordered = ordered,
).toPx()
