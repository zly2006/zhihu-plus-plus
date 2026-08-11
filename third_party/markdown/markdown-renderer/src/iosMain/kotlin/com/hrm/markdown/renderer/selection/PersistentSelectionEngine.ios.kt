package com.hrm.markdown.renderer.selection

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun PersistentSelectionEngine(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    SelectionContainer(modifier, content)
}
