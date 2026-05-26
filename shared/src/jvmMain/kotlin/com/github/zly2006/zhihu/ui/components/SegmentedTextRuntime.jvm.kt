package com.github.zly2006.zhihu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.data.SegmentInfoMeta
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.util.SegmentHighlightSpan
import com.github.zly2006.zhihu.shared.util.signZhihuFetchRequest
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
actual fun rememberSegmentedTextRuntime(): SegmentedTextRuntime = remember {
    val store = DesktopAccountStore()
    SegmentedTextRuntime(
        copyText = { _, text ->
            runCatching {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
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
    val url = zhihuSegmentReactionUrl(targetType, contentId)
    val account = store.load()
    val dc0 = account.cookies["d_c0"] ?: return highlight.meta

    return if (highlight.meta.isLike) {
        val body = buildSegmentUnlikeBody(highlight)
        store.fetchAuthenticatedJson(url) {
            method = HttpMethod.Delete
            signZhihuFetchRequest(dc0 = dc0, body = body)
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        updateSegmentMetaAfterUnlike(highlight)
    } else {
        val body = buildSegmentLikeBody(highlight)
        val response = store.fetchAuthenticatedJson(url) {
            method = HttpMethod.Post
            signZhihuFetchRequest(dc0 = dc0, body = body)
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        updateSegmentMetaAfterLike(highlight, response)
    }
}
