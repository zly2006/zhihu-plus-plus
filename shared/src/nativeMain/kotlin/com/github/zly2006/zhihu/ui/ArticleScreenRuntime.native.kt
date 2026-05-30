package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.github.zly2006.zhihu.navigation.Article

@Composable
actual fun rememberArticleScreenRuntime(): ArticleScreenRuntime = remember {
    object : ArticleScreenRuntime {
        override val articleHost: ArticleHost? = null

        // TODO: iOS 预加载实现
        override val previewPreloader = ArticlePreviewPreloader { _, _, _, _ -> }
    }
}

// TODO: iOS WebView 实现
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
) = Unit

actual fun Modifier.articleMarkdownSelectionWorkaround(): Modifier = this
