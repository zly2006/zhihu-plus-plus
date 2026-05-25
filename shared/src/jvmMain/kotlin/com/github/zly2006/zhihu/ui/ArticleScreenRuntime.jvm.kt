package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.viewmodel.DesktopArticleViewModelRuntime

@Composable
actual fun rememberArticleScreenRuntime(): ArticleScreenRuntime {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        object : ArticleScreenRuntime {
            override val articleHost: ArticleHost? = null
            override val articleRuntime = DesktopArticleViewModelRuntime(userMessages = userMessages)
            override val previewPreloader = ArticlePreviewPreloader { _, _, _, _ -> }
        }
    }
}

@Composable
actual fun ArticleWebViewContent(
    article: Article,
    html: String,
    title: String,
    scrollState: ScrollState,
    rememberedScrollY: Int,
    rememberedScrollYSync: Boolean,
    onRememberedScrollYSyncChange: (Boolean) -> Unit,
    onImageLoadFailed: () -> Unit,
    onDoubleTap: () -> Unit,
) {
    ArticleMarkdownContent(
        html = html,
        modifier = Modifier,
        header = {},
        footer = {},
    )
}

actual fun Modifier.articleMarkdownSelectionWorkaround(): Modifier = this
