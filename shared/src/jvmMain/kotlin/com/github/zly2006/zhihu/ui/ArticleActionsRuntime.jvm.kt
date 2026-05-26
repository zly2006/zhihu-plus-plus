package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.ui.components.rememberShareDialogRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.net.URI

@Composable
actual fun rememberArticleActionsRuntime(): ArticleActionsRuntime {
    val userMessages = rememberUserMessageSink()
    val coroutineScope = rememberCoroutineScope()
    val shareRuntime = rememberShareDialogRuntime()
    return remember(userMessages, coroutineScope, shareRuntime) {
        object : ArticleActionsRuntime {
            private var speechProcess: Process? = null
            private var currentTtsState by mutableStateOf(
                if (isDesktopSpeechCommandAvailable()) TtsState.Ready else TtsState.Error,
            )
            override val ttsState: TtsState
                get() = currentTtsState

            override fun toggleSpeech(
                title: String,
                content: String,
            ) {
                if (currentTtsState.isSpeaking) {
                    stopSpeaking()
                } else if (currentTtsState !in listOf(TtsState.Error, TtsState.Uninitialized, TtsState.Initializing)) {
                    // 使用协程在后台处理文本提取，避免UI阻塞
                    coroutineScope.launch {
                        try {
                            // 在IO线程中处理文本提取和桌面 TTS 进程，保持 UI 线程可响应
                            val textToRead = withContext(Dispatchers.IO) {
                                articleSpeechText(title, content)
                            }
                            if (textToRead.isNotBlank()) {
                                speakText(textToRead, title)
                            }
                        } catch (e: Exception) {
                            currentTtsState = TtsState.Error
                            userMessages.showMessage("朗读失败：${e.message}")
                        }
                    }
                }
            }

            override fun shareArticle(
                article: Article,
                questionId: Long,
                title: String,
                authorName: String,
            ) {
                val text = articleActionText(article, questionId, title, authorName)
                shareRuntime.share(article, text)
            }

            override fun copyArticleLink(
                article: Article,
                questionId: Long,
                title: String,
                authorName: String,
            ) {
                val text = articleActionText(article, questionId, title, authorName)
                shareRuntime.copyLink(article, text)
            }

            override fun openArticleInBrowser(article: Article) {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(URI(articleUrl(article)))
                    userMessages.showMessage("已发送到浏览器")
                }
            }

            private fun articleUrl(article: Article): String =
                when (article.type) {
                    ArticleType.Answer -> "https://www.zhihu.com/answer/${article.id}"
                    ArticleType.Article -> "https://zhuanlan.zhihu.com/p/${article.id}"
                }

            private suspend fun speakText(
                text: String,
                title: String,
            ) {
                currentTtsState = TtsState.LoadingText
                val process = withContext(Dispatchers.IO) {
                    ProcessBuilder("say")
                        .redirectErrorStream(true)
                        .start()
                }
                speechProcess = process
                currentTtsState = TtsState.Speaking
                userMessages.showMessage("开始朗读：$title")
                val exitCode = withContext(Dispatchers.IO) {
                    process.outputStream.bufferedWriter().use { writer ->
                        writer.write(text)
                    }
                    process.waitFor()
                }
                if (speechProcess == process) {
                    speechProcess = null
                    currentTtsState = if (exitCode == 0) TtsState.Ready else TtsState.Error
                }
            }

            private fun stopSpeaking() {
                speechProcess?.destroy()
                speechProcess = null
                currentTtsState = TtsState.Ready
            }
        }
    }
}

private fun isDesktopSpeechCommandAvailable(): Boolean =
    runCatching {
        ProcessBuilder("sh", "-c", "command -v say >/dev/null 2>&1")
            .start()
            .waitFor() == 0
    }.getOrDefault(false)
