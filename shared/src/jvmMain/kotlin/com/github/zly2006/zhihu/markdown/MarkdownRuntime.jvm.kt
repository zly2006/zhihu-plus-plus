package com.github.zly2006.zhihu.markdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.hrm.latex.renderer.font.MathFont
import java.awt.Desktop
import java.net.URI

@Composable
actual fun rememberMarkdownRuntime(): MarkdownRuntime = remember {
    object : MarkdownRuntime {
        override val mathFont: MathFont? = null

        override fun openImage(url: String) {
            openInBrowser(url)
        }

        override fun openInBrowser(url: String) {
            runCatching {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(URI(url))
                }
            }
        }

        override suspend fun saveMarkdownImage(url: String) {
        }

        override suspend fun shareMarkdownImage(url: String) {
        }
    }
}
