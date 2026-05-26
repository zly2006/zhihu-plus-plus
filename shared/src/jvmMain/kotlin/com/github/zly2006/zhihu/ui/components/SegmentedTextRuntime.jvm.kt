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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
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
    val url = "https://www.zhihu.com/api/v4/reaction/${targetType}s/$contentId/segment_reaction"
    val account = store.load()
    val dc0 = account.cookies["d_c0"] ?: return highlight.meta

    return if (highlight.meta.isLike) {
        val body = buildJsonObject {
            put("seg_ids", highlight.meta.segIds.joinToString(","))
        }.toString()
        store.fetchAuthenticatedJson(url) {
            method = HttpMethod.Delete
            signZhihuFetchRequest(dc0 = dc0, body = body)
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        highlight.meta.copy(
            isLike = false,
            likeCount = (highlight.meta.likeCount - 1).coerceAtLeast(0),
        )
    } else {
        val body = buildJsonObject {
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
        }.toString()
        val response = store.fetchAuthenticatedJson(url) {
            method = HttpMethod.Post
            signZhihuFetchRequest(dc0 = dc0, body = body)
            contentType(ContentType.Application.Json)
            setBody(body)
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
