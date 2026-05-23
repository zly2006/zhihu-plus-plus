package com.github.zly2006.zhihu.markdown

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.latex.rememberLatexFonts
import com.github.zly2006.zhihu.ui.components.OpenImageDialog
import com.github.zly2006.zhihu.util.luoTianYiUrlLauncher
import com.github.zly2006.zhihu.util.saveImageToGallery
import com.github.zly2006.zhihu.util.shareImage
import com.hrm.latex.renderer.font.MathFont

@Composable
actual fun rememberMarkdownRuntime(): MarkdownRuntime {
    val context = LocalContext.current
    val httpClient = AccountData.httpClient(context)
    val fontResult = rememberLatexFonts(context, httpClient)

    return object : MarkdownRuntime {
        override val mathFont: MathFont? = fontResult.downloaded?.mathFont

        override fun openImage(url: String) {
            OpenImageDialog(context, httpClient, url).show()
        }

        override fun openInBrowser(url: String) {
            luoTianYiUrlLauncher(context, url.toUri())
        }

        override suspend fun saveMarkdownImage(url: String) {
            saveImageToGallery(context, httpClient, url)
        }

        override suspend fun shareMarkdownImage(url: String) {
            shareImage(context, httpClient, url)
        }
    }
}
