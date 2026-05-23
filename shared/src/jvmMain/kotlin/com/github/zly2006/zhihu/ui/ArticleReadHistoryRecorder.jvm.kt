package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.data.addZhihuReadHistory
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore

@Composable
actual fun rememberArticleReadHistoryRecorder(): ArticleReadHistoryRecorder = remember {
    val store = DesktopAccountStore()
    ArticleReadHistoryRecorder { article ->
        val account = store.load()
        val dc0 = account.cookies["d_c0"] ?: return@ArticleReadHistoryRecorder
        store.createHttpClient(account.cookies).use { client ->
            addZhihuReadHistory(
                client = client,
                contentToken = article.id.toString(),
                contentType = article.type.name.lowercase(),
                dc0 = dc0,
            )
        }
    }
}
