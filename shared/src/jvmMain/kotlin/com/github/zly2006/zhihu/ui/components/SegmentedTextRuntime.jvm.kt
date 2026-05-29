package com.github.zly2006.zhihu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.data.SegmentInfoMeta
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.copyDesktopPlainText
import com.github.zly2006.zhihu.shared.desktop.signedFetchJson
import com.github.zly2006.zhihu.shared.util.SegmentHighlightSpan
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType

@Composable
actual fun rememberSegmentedTextRuntime(): SegmentedTextRuntime = remember {
    val store = DesktopAccountStore()
    SegmentedTextRuntime(
        copyText = { _, text ->
            runCatching {
                copyDesktopPlainText(text)
            }
        },
        toggleSegmentLike = { highlight ->
            toggleSegmentLike(store, highlight)
        },
    )
}

private suspend fun toggleSegmentLike(
    store: DesktopAccountStore,
    highlight: SegmentHighlightSpan,
): SegmentInfoMeta {
    val contentId = highlight.contentId ?: return highlight.meta
    val targetType = highlight.contentType ?: return highlight.meta
    val url = "https://www.zhihu.com/api/v4/reaction/${targetType}s/$contentId/segment_reaction"
    if (store.load().cookies["d_c0"] == null) return highlight.meta

    return if (highlight.meta.isLike) {
        val body = buildSegmentUnlikeBody(highlight)
        store.signedFetchJson(url) {
            method = HttpMethod.Delete
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        updateSegmentMetaAfterUnlike(highlight)
    } else {
        val body = buildSegmentLikeBody(highlight)
        val response = store.signedFetchJson(url) {
            method = HttpMethod.Post
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        updateSegmentMetaAfterLike(highlight, response)
    }
}
