package com.github.zly2006.zhihu.ui.components

import android.content.ClipData
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.shared.data.SegmentInfoMeta
import com.github.zly2006.zhihu.shared.util.SegmentHighlightSpan
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.util.signFetchRequest
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType

@Composable
actual fun rememberSegmentedTextRuntime(): SegmentedTextRuntime {
    val context = LocalContext.current
    return remember(context) {
        SegmentedTextRuntime(
            copyText = { label, text ->
                context.clipboardManager.setPrimaryClip(
                    ClipData.newPlainText(label, text),
                )
            },
            toggleSegmentLike = { highlight ->
                toggleSegmentLike(context, highlight)
            },
        )
    }
}

private suspend fun toggleSegmentLike(
    context: android.content.Context,
    highlight: SegmentHighlightSpan,
): SegmentInfoMeta {
    val contentId = highlight.contentId ?: return highlight.meta
    val targetType = highlight.contentType ?: return highlight.meta
    val url = zhihuSegmentReactionUrl(targetType, contentId)

    return if (highlight.meta.isLike) {
        val body = buildSegmentUnlikeBody(highlight)
        AccountData.withAuthenticatedResponse(
            context = context,
            url = url,
            block = {
                method = HttpMethod.Delete
                signFetchRequest()
                contentType(ContentType.Application.Json)
                setBody(body)
            },
        ) {
        }
        updateSegmentMetaAfterUnlike(highlight)
    } else {
        val body = buildSegmentLikeBody(highlight)
        val response = AccountData.fetchPost(context, url) {
            signFetchRequest()
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        updateSegmentMetaAfterLike(highlight, response)
    }
}
