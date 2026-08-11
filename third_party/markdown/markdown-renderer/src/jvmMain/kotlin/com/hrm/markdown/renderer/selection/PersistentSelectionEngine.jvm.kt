package com.hrm.markdown.renderer.selection

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hrm.markdown.renderer.selection.androidx.MarkdownSelectionContainer

@Composable
internal actual fun PersistentSelectionEngine(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    MarkdownSelectionContainer(modifier, content)
}
