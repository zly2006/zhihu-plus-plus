package com.github.zly2006.zhihu.markdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.hrm.latex.renderer.font.MathFont

@Composable
actual fun rememberMarkdownRuntime(): MarkdownRuntime = remember {
    object : MarkdownRuntime {
        override val mathFont: MathFont? = null

        override suspend fun saveMarkdownImage(url: String) = Unit // TODO: iOS 保存Markdown图片

        override suspend fun shareMarkdownImage(url: String) = Unit // TODO: iOS 分享Markdown图片
    }
} // TODO: iOS Markdown 运行时完整实现
