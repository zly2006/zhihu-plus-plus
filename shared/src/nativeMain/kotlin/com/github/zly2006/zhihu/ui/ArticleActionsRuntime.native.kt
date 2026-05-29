package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.ui.components.rememberShareDialogRuntime

@Composable
actual fun rememberArticleActionsRuntime(): ArticleActionsRuntime {
    val userMessages = rememberUserMessageSink()
    val shareRuntime = rememberShareDialogRuntime()
    return remember(userMessages, shareRuntime) {
        object : ArticleActionsRuntime {
            // TODO: iOS TTS 实现
            override var ttsState: TtsState by mutableStateOf(TtsState.Ready)
                private set

            // TODO: iOS TTS 实现
            override fun toggleSpeech(title: String, content: String) {
                userMessages.showMessage("iOS TTS 暂未实现")
            }

            override fun shareArticle(article: Article, questionId: Long, title: String, authorName: String) {
                shareRuntime.share(article, articleActionText(article, questionId, title, authorName))
            }

            override fun copyArticleLink(article: Article, questionId: Long, title: String, authorName: String) {
                shareRuntime.copyLink(article, articleActionText(article, questionId, title, authorName))
            }

            override fun openArticleInBrowser(article: Article) {
                val url = when (article.type) {
                    ArticleType.Answer -> "https://www.zhihu.com/answer/${article.id}"
                    ArticleType.Article -> "https://zhuanlan.zhihu.com/p/${article.id}"
                }
                openIosUrl(url)
            }
        }
    }
}
