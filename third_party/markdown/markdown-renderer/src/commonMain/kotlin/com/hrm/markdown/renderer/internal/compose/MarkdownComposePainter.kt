package com.hrm.markdown.renderer.internal.compose

import androidx.compose.runtime.Composable
import com.hrm.markdown.renderer.internal.layout.engine.LazyLayoutBlockResult
import com.hrm.markdown.renderer.internal.layout.model.InternalLayoutDocumentModel

internal interface MarkdownComposePainter {
    @Composable
    fun Paint(
        document: InternalLayoutDocumentModel,
        environment: ComposeRenderEnvironment,
    )

    @Composable
    fun PaintLazy(
        documentRevision: Long,
        blockCount: Int,
        blockKeyAt: (Int) -> Long,
        blockAt: (Int) -> LazyLayoutBlockResult?,
        environment: ComposeRenderEnvironment,
    )
}
