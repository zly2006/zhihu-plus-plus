package com.hrm.markdown.renderer.selection

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal expect fun PersistentSelectionEngine(
    modifier: Modifier,
    content: @Composable () -> Unit,
)
