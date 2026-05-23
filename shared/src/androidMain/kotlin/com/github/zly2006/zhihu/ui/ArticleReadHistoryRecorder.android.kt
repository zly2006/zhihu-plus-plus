package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.data.AccountData

@Composable
actual fun rememberArticleReadHistoryRecorder(): ArticleReadHistoryRecorder {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        ArticleReadHistoryRecorder { article ->
            AccountData.addReadHistory(
                context,
                article.id.toString(),
                article.type.name.lowercase(),
            )
        }
    }
}
