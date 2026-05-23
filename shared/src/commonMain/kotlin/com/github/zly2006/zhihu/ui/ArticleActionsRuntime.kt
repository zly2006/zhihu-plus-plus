package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.navigation.Article

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
