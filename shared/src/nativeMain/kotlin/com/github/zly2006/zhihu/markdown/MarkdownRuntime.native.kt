package com.github.zly2006.zhihu.markdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.hrm.latex.renderer.font.MathFont

// TODO: iOS Markdown 运行时完整实现
@Composable
actual fun rememberMarkdownRuntime(): MarkdownRuntime = remember {
    object : MarkdownRuntime {
        override val mathFont: MathFont? = null

        override fun openImage(url: String) = Unit

        override fun openInBrowser(url: String) = Unit

        override suspend fun saveMarkdownImage(url: String) = Unit

        override suspend fun shareMarkdownImage(url: String) = Unit
    }
}
