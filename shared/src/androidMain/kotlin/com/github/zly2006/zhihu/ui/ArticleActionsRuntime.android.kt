package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.ui.components.rememberShareDialogRuntime
import com.github.zly2006.zhihu.util.OpenInBrowser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun rememberArticleActionsRuntime(): ArticleActionsRuntime {
    val context = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()
    val shareRuntime = rememberShareDialogRuntime()
    val articleHost = context.articleHost()
    val ttsState = articleHost?.articleTtsState ?: TtsState.Uninitialized
    return remember(context, coroutineScope, userMessages, shareRuntime, articleHost, ttsState) {
        object : ArticleActionsRuntime {
            override val ttsState: TtsState = ttsState

            override fun toggleSpeech(
                title: String,
                content: String,
            ) {
                if (ttsState.isSpeaking) {
                    articleHost?.stopArticleSpeaking()
                } else if (ttsState !in listOf(TtsState.Error, TtsState.Uninitialized, TtsState.Initializing)) {
                    // 使用协程在后台处理文本提取，避免UI阻塞
                    coroutineScope.launch {
                        try {
                            // 在IO线程中处理文本提取
                            withContext(Dispatchers.IO) {
                                val textToRead = articleSpeechText(title, content)

                                // 回到主线程执行TTS
                                withContext(Dispatchers.Main) {
                                    if (textToRead.isNotBlank()) {
                                        articleHost?.speakArticleText(textToRead, title)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                userMessages.showMessage("朗读失败：${e.message}")
                            }
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
                coroutineScope.launch {
                    OpenInBrowser.openUrlInBrowser(context, article)
                    userMessages.showMessage("已发送到浏览器")
                }
            }
        }
    }
}
