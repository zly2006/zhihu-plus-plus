package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.markdown.RenderVideoBox
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.viewmodel.DesktopArticleViewModelRuntime
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
    RenderMarkdown(
        html = html,
        modifier = modifier,
        header = header,
        footer = footer,
    )
}

@Composable
actual fun ArticleVideoAttachmentContent(attachment: JsonElement?) {
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

actual fun Modifier.articleMarkdownSelectionWorkaround(): Modifier = this
