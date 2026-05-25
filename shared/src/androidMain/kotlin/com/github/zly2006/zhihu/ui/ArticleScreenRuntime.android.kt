package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.shared.util.Log
import com.github.zly2006.zhihu.ui.components.setupUpWebviewClient
import com.github.zly2006.zhihu.util.fuckHonorService
import com.github.zly2006.zhihu.viewmodel.AndroidArticleViewModelRuntime
import com.github.zly2006.zhihu.viewmodel.ArticleViewModelRuntime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
actual fun rememberArticleScreenRuntime(): ArticleScreenRuntime {
    val context = LocalContext.current
    return remember(context) {
        val articleHost = context.articleHost()
        object : ArticleScreenRuntime {
            override val articleHost: ArticleHost? = articleHost
            override val articleRuntime: ArticleViewModelRuntime = AndroidArticleViewModelRuntime(context)
            override val previewPreloader: ArticlePreviewPreloader = ArticlePreviewPreloader { cached, isNext, title, onImageLoadFailed ->
                val previewWebViewStore = articleHost?.articleAnswerSwitchState as? ArticlePreviewWebViewStore
                    ?: return@ArticlePreviewPreloader
                val wv = previewWebViewStore.getOrCreatePreviewWebView(context, isNext, cached.article.id)
                val articleId = cached.article.id.toString()
                if (wv.contentId != articleId) {
                    wv.contentId = articleId
                    wv.loadZhihu(
                        "https://www.zhihu.com/answer/${cached.article.id}",
                        prepareContentDocument(cached.content, onImageLoadFailed),
                        title,
                    )
                }
            }
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
    val coroutineScope = rememberCoroutineScope()
    com.github.zly2006.zhihu.ui.components.WebviewComp(
        onDoubleTap = onDoubleTap,
        scrollState = scrollState,
    ) {
        it.isVerticalScrollBarEnabled = false
        it.setupUpWebviewClient {
            if (!rememberedScrollYSync) {
                coroutineScope.launch {
                    while (scrollState.maxValue < rememberedScrollY) {
                        delay(100)
                    }
                    Log.i("zhihu-scroll", "scroll to $rememberedScrollY, max= ${scrollState.maxValue}, sync on")
                    scrollState.animateScrollTo(rememberedScrollY)
                    onRememberedScrollYSyncChange(true)
                }
            }
        }
        it.contentId = article.id.toString()
        it.loadZhihu(
            "https://www.zhihu.com/${article.type}/${article.id}",
            prepareContentDocument(html, onImageLoadFailed),
            title,
        )
    }
}

actual fun Modifier.articleMarkdownSelectionWorkaround(): Modifier = fuckHonorService()
