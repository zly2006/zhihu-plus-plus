package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment

fun interface ArticleReadHistoryRecorder {
    suspend fun addReadHistory(article: Article)
}

@Composable
fun rememberArticleReadHistoryRecorder(): ArticleReadHistoryRecorder {
    val environment = rememberPaginationEnvironment(false)
    return remember(environment) {
        ArticleReadHistoryRecorder { article ->
            environment.addReadHistory(
                contentToken = article.id.toString(),
                contentTypeName = article.type.name.lowercase(),
            )
        }
    }
}
