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
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

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
    val url = "https://www.zhihu.com/api/v4/reaction/${targetType}s/$contentId/segment_reaction"

    return if (highlight.meta.isLike) {
        val body = buildSegmentUnlikeBody(highlight)
        AccountData.httpClient(context).delete(url) {
            signFetchRequest()
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        updateSegmentMetaAfterUnlike(highlight)
    } else {
        val body = buildSegmentLikeBody(highlight)
        val response = AccountData
            .httpClient(context)
            .post(url) {
                signFetchRequest()
                contentType(ContentType.Application.Json)
                setBody(body)
            }.let { response ->
                if (response.status == HttpStatusCode.NoContent) {
                    null
                } else {
                    response.body<JsonElement>() as? JsonObject
                }
            }
        updateSegmentMetaAfterLike(highlight, response)
    }
}
