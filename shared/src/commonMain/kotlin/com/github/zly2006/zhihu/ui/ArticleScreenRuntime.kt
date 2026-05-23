package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.shared.article.CachedAnswerContent
import com.github.zly2006.zhihu.viewmodel.ArticleViewModelRuntime
import kotlinx.serialization.json.JsonElement

interface ArticleScreenRuntime {
    val articleHost: ArticleHost?
    val articleRuntime: ArticleViewModelRuntime
    val previewPreloader: ArticlePreviewPreloader
}

fun interface ArticlePreviewPreloader {
    fun preloadPreview(
        cached: CachedAnswerContent,
        isNext: Boolean,
        title: String,
        onImageLoadFailed: () -> Unit,
    )
}

@Composable
expect fun rememberArticleScreenRuntime(): ArticleScreenRuntime

@Composable
expect fun ArticleWebViewContent(
    article: Article,
    html: String,
    title: String,
    scrollState: ScrollState,
    rememberedScrollY: Int,
    rememberedScrollYSync: Boolean,
    onRememberedScrollYSyncChange: (Boolean) -> Unit,
    onImageLoadFailed: () -> Unit,
    onDoubleTap: () -> Unit,
)

@Composable
expect fun ArticleMarkdownContent(
    html: String,
    modifier: Modifier,
    header: @Composable () -> Unit,
    footer: @Composable () -> Unit,
)

@Composable
expect fun ArticleVideoAttachmentContent(attachment: JsonElement?)

expect fun Modifier.articleMarkdownSelectionWorkaround(): Modifier
