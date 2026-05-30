package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.markdown.RenderVideoBox
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel.CachedAnswerContent
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

interface ArticleScreenRuntime {
    val articleHost: ArticleHost?
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
fun ArticleMarkdownContent(
    html: String,
    modifier: Modifier,
    header: @Composable () -> Unit,
    footer: @Composable () -> Unit,
) {
    RenderMarkdown(
        html = html,
        modifier = modifier,
        selectable = true,
        enableScroll = false,
        header = header,
        footer = footer,
    )
}

@Composable
fun ArticleVideoAttachmentContent(attachment: JsonElement?) {
    if (attachment
            ?.jsonObject
            ?.get("type")
            ?.jsonPrimitive
            ?.content == "video"
    ) {
        val videoId = attachment
            .jsonObject["attachmentId"]
            ?.jsonPrimitive
            ?.content
            ?.toLongOrNull()
        if (videoId != null) {
            val thumbnail = attachment
                .jsonObject["video"]!!
                .jsonObject["videoInfo"]!!
                .jsonObject["thumbnail"]!!
                .jsonPrimitive.content
            RenderVideoBox(
                videoId = videoId,
                thumbnailUrl = thumbnail,
            )
        }
    }
}

expect fun Modifier.articleMarkdownSelectionWorkaround(): Modifier
