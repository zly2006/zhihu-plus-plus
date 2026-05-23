package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.navigation.Article

fun interface ArticleReadHistoryRecorder {
    suspend fun addReadHistory(article: Article)
}

@Composable
expect fun rememberArticleReadHistoryRecorder(): ArticleReadHistoryRecorder
