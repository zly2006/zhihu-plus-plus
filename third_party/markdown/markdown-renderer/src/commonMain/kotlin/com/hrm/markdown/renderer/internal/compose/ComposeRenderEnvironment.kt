package com.hrm.markdown.renderer.internal.compose

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hrm.markdown.renderer.MarkdownRenderMode

internal class ComposeRenderEnvironment(
    val modifier: Modifier = Modifier,
    val renderMode: MarkdownRenderMode = MarkdownRenderMode.StaticColumn,
    val enableScroll: Boolean = true,
    val scrollState: ScrollState? = null,
    val lazyListState: LazyListState? = null,
    val header: (@Composable () -> Unit)? = null,
    val footer: (@Composable () -> Unit)? = null,
)
