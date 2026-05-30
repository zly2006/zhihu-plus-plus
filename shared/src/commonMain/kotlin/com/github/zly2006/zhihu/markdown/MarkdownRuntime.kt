package com.github.zly2006.zhihu.markdown

import androidx.compose.runtime.Composable
import com.hrm.latex.renderer.font.MathFont

interface MarkdownRuntime {
    val mathFont: MathFont?

    suspend fun saveMarkdownImage(url: String)

    suspend fun shareMarkdownImage(url: String)
}

@Composable
expect fun rememberMarkdownRuntime(): MarkdownRuntime
