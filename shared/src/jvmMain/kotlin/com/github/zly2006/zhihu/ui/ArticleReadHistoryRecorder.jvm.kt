package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberArticleReadHistoryRecorder(): ArticleReadHistoryRecorder = remember {
    ArticleReadHistoryRecorder {}
}
