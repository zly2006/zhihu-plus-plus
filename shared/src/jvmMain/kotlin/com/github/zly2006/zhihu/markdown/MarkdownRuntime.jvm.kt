package com.github.zly2006.zhihu.markdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.hrm.latex.renderer.font.MathFont
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI

@Composable
actual fun rememberMarkdownRuntime(): MarkdownRuntime {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        val store = DesktopAccountStore()
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
                runCatching {
                    saveDesktopMarkdownImage(store, url)
                }.onSuccess { file ->
                    userMessages.showShortMessage("已保存图片: ${file.absolutePath}")
                }.onFailure { error ->
                    userMessages.showShortMessage("保存失败: ${error.message}")
                }
            }

            override suspend fun shareMarkdownImage(url: String) {
                runCatching {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(url), null)
                    userMessages.showShortMessage("已复制图片链接")
                }.onFailure { error ->
                    userMessages.showShortMessage("分享失败: ${error.message}")
                }
            }
        }
    }
}

private suspend fun saveDesktopMarkdownImage(
    store: DesktopAccountStore,
    url: String,
): File = withContext(Dispatchers.IO) {
    val account = store.load()
    val imageBytes = store.createHttpClient(account.cookies).use { client ->
        client.get(url).body<ByteArray>()
    }
    val downloadsDir = File(System.getProperty("user.home"), "Downloads/Zhihu++")
    if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
        throw IllegalStateException("无法创建下载目录")
    }
    val file = File(downloadsDir, desktopMarkdownImageFileName(url))
    file.writeBytes(imageBytes)
    file
}

private fun desktopMarkdownImageFileName(url: String): String {
    val pathName = runCatching {
        URI(url).path.substringAfterLast('/').substringBefore('?')
    }.getOrNull().orEmpty()
    val extension = pathName.substringAfterLast('.', "").takeIf { it.length in 2..5 } ?: "jpg"
    return "markdown_image_${System.currentTimeMillis()}.$extension"
}
