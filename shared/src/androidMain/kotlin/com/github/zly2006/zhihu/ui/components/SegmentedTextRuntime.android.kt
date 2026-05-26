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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

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
        AccountData.withAuthenticatedResponse(
            context = context,
            url = url,
            block = {
                method = HttpMethod.Delete
                signFetchRequest()
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("seg_ids", highlight.meta.segIds.joinToString(","))
                    }.toString(),
                )
            },
        ) {
        }
        highlight.meta.copy(
            isLike = false,
            likeCount = (highlight.meta.likeCount - 1).coerceAtLeast(0),
        )
    } else {
        val response = AccountData.fetchPost(context, url) {
            signFetchRequest()
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    if (highlight.meta.segIds.isNotEmpty()) {
                        put("seg_id", highlight.meta.segIds.joinToString(","))
                    }
                    put("content", highlight.text)
                    put(
                        "position",
                        buildJsonObject {
                            put(
                                "start",
                                buildJsonObject {
                                    put("paragraph_id", highlight.paragraphId.orEmpty())
                                    put("offset", highlight.startOffset ?: 0)
                                },
                            )
                            put(
                                "end",
                                buildJsonObject {
                                    put("paragraph_id", highlight.paragraphId.orEmpty())
                                    put("offset", highlight.endOffset ?: 0)
                                },
                            )
                        },
                    )
                }.toString(),
            )
        }
        val segId = response
            ?.get("payload")
            ?.jsonObject
            ?.get("segId")
            ?.jsonPrimitive
            ?.content
            ?.split(',')
            ?.filter(String::isNotEmpty)
            ?: highlight.meta.segIds
        highlight.meta.copy(
            segIds = segId,
            isLike = true,
            likeCount = highlight.meta.likeCount + 1,
        )
    }
}
