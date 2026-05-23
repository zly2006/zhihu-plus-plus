package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.viewmodel.DesktopArticleViewModelRuntime
import kotlinx.serialization.json.JsonElement

@Composable
actual fun rememberArticleScreenRuntime(): ArticleScreenRuntime = remember {
    object : ArticleScreenRuntime {
        override val articleHost: ArticleHost? = null
        override val articleRuntime = DesktopArticleViewModelRuntime()
        override val previewPreloader = ArticlePreviewPreloader { _, _, _, _ -> }
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

@Composable
actual fun ArticleMarkdownContent(
    html: String,
    modifier: Modifier,
    header: @Composable () -> Unit,
    footer: @Composable () -> Unit,
) {
    header()
    Text(
        text = html.replace(Regex("<[^>]+>"), ""),
        modifier = modifier,
    )
    footer()
}

@Composable
actual fun ArticleVideoAttachmentContent(attachment: JsonElement?) = Unit

actual fun Modifier.articleMarkdownSelectionWorkaround(): Modifier = this
