package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore

@Composable
actual fun rememberArticleReadHistoryRecorder(): ArticleReadHistoryRecorder = remember {
    val store = DesktopAccountStore()
    ArticleReadHistoryRecorder { article ->
        store.addReadHistory(
            contentToken = article.id.toString(),
            contentTypeName = article.type.name.lowercase(),
        )
    }
}
