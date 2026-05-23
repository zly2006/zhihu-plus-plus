package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI

@Composable
actual fun rememberArticleActionsRuntime(): ArticleActionsRuntime {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        object : ArticleActionsRuntime {
            override val ttsState: TtsState = TtsState.Uninitialized

            override fun toggleSpeech(
                title: String,
                content: String,
            ) = Unit

            override fun shareArticle(
                article: Article,
                questionId: Long,
                title: String,
                authorName: String,
            ) {
                copyText(articleActionText(article, questionId, title, authorName))
                userMessages.showMessage("已复制分享文本")
            }

            override fun copyArticleLink(
                article: Article,
                questionId: Long,
                title: String,
                authorName: String,
            ) {
                copyText(articleActionText(article, questionId, title, authorName))
                userMessages.showMessage("已复制链接")
            }

            override fun openArticleInBrowser(article: Article) {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(URI(articleUrl(article)))
                    userMessages.showMessage("已发送到浏览器")
                }
            }

            private fun copyText(text: String) {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
            }

            private fun articleUrl(article: Article): String =
                when (article.type) {
                    ArticleType.Answer -> "https://www.zhihu.com/answer/${article.id}"
                    ArticleType.Article -> "https://zhuanlan.zhihu.com/p/${article.id}"
                }
        }
    }
}
