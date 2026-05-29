package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import com.fleeksoft.ksoup.Ksoup
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.zhihuArticleUrl
import com.github.zly2006.zhihu.navigation.zhihuQuestionAnswerUrl

interface ArticleActionsRuntime {
    val ttsState: TtsState

    fun toggleSpeech(
        title: String,
        content: String,
    )

    fun shareArticle(
        article: Article,
        questionId: Long,
        title: String,
        authorName: String,
    )

    fun copyArticleLink(
        article: Article,
        questionId: Long,
        title: String,
        authorName: String,
    )

    fun openArticleInBrowser(article: Article)
}

@Composable
expect fun rememberArticleActionsRuntime(): ArticleActionsRuntime

fun articleActionText(
    article: Article,
    questionId: Long,
    title: String,
    authorName: String,
): String =
    when (article.type) {
        ArticleType.Answer -> {
            "${zhihuQuestionAnswerUrl(questionId, article.id)}\n【$title - $authorName 的回答】"
        }
        ArticleType.Article -> {
            "${zhihuArticleUrl(article.id)}\n【$title - $authorName 的文章】"
        }
    }

fun articleSpeechText(
    title: String,
    content: String,
    maxContentLength: Int = 50_000,
): String =
    buildString {
        append(title)
        append("。")
        if (content.isNotEmpty()) {
            val contentToProcess =
                if (content.length > maxContentLength) {
                    content.substring(0, maxContentLength) + "..."
                } else {
                    content
                }
            append(Ksoup.parse(contentToProcess).text())
        }
    }
