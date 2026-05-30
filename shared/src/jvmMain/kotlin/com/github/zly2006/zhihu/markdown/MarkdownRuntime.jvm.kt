package com.github.zly2006.zhihu.markdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.platform.asComposeFontFamily
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.copyDesktopPlainText
import com.github.zly2006.zhihu.shared.desktop.desktopZhihuDataFile
import com.github.zly2006.zhihu.shared.desktop.saveImageToDownloads
import com.github.zly2006.zhihu.shared.platform.rememberExternalUrlOpener
import com.github.zly2006.zhihu.shared.platform.rememberImagePreviewOpener
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.hrm.latex.renderer.font.MathFont
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val FONT_VERSION = "1"
private val LM_MATH_URLS = listOf(
    "https://mirrors.ustc.edu.cn/CTAN/fonts/lm-math/opentype/latinmodern-math.otf",
    "https://mirrors.tuna.tsinghua.edu.cn/CTAN/fonts/lm-math/opentype/latinmodern-math.otf",
)

@Composable
actual fun rememberMarkdownRuntime(): MarkdownRuntime {
    val userMessages = rememberUserMessageSink()
    val store = remember { DesktopAccountStore() }
    val openExternalUrl = rememberExternalUrlOpener()
    val openImagePreview = rememberImagePreviewOpener()
    var mathFont by remember { mutableStateOf<MathFont?>(null) }

    LaunchedEffect(store) {
        mathFont = runCatching {
            loadDesktopMathFont(store)
        }.getOrNull()
    }

    return remember(store, userMessages, openExternalUrl, openImagePreview, mathFont) {
        object : MarkdownRuntime {
            override val mathFont: MathFont? = mathFont

            override fun openImage(url: String) {
                openImagePreview(url)
            }

            override fun openInBrowser(url: String) {
                openExternalUrl(url)
            }

            override suspend fun saveMarkdownImage(url: String) {
                runCatching {
                    store.saveImageToDownloads(url, "markdown_image")
                }.onSuccess { file ->
                    userMessages.showShortMessage("已保存图片: ${file.absolutePath}")
                }.onFailure { error ->
                    userMessages.showShortMessage("保存失败: ${error.message}")
                }
            }

            override suspend fun shareMarkdownImage(url: String) {
                runCatching {
                    copyDesktopPlainText(url)
                    userMessages.showShortMessage("已复制图片链接")
                }.onFailure { error ->
                    userMessages.showShortMessage("分享失败: ${error.message}")
                }
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
private suspend fun loadDesktopMathFont(store: DesktopAccountStore): MathFont = withContext(Dispatchers.IO) {
    val fontFile = desktopLatexFontDir().resolve("latinmodern-math.otf")
    if (!fontFile.exists()) {
        downloadDesktopMathFont(store, fontFile)
    }
    val fontBytes = fontFile.readBytes()
    val awtFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, fontFile)
    MathFont.OTF(fontBytes, awtFont.asComposeFontFamily())
}

private suspend fun downloadDesktopMathFont(
    store: DesktopAccountStore,
    fontFile: File,
) {
    val account = store.load()
    var lastError: Exception? = null
    store.createHttpClient(account.cookies).use { client ->
        for (url in LM_MATH_URLS) {
            try {
                val bytes = client.get(url).body<ByteArray>()
                if (isOpenTypeFont(bytes)) {
                    fontFile.parentFile.mkdirs()
                    fontFile.writeBytes(bytes)
                    return
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
    }
    throw lastError ?: IllegalStateException("Failed to download Latin Modern Math")
}

private fun desktopLatexFontDir(): File =
    desktopZhihuDataFile("latex-fonts/v$FONT_VERSION")

private fun isOpenTypeFont(bytes: ByteArray): Boolean =
    bytes.size > 4 &&
        bytes[0] == 0x4F.toByte() &&
        bytes[1] == 0x54.toByte() &&
        bytes[2] == 0x54.toByte() &&
        bytes[3] == 0x4F.toByte()
