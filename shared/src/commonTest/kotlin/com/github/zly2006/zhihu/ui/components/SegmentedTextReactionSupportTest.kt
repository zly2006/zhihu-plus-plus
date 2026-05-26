package com.github.zly2006.zhihu.ui.components

import com.github.zly2006.zhihu.shared.data.SegmentInfoMeta
import com.github.zly2006.zhihu.shared.util.SegmentHighlightSpan
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class SegmentedTextReactionSupportTest {
    @Test
    fun buildSegmentLikeBodyKeepsMasterPayloadShape() {
        val body = buildSegmentLikeBody(
            SegmentHighlightSpan(
                text = "划线内容",
                meta = SegmentInfoMeta(segIds = listOf("old")),
                paragraphId = "p1",
                startOffset = 3,
                endOffset = 7,
            ),
        )
        val json = Json.parseToJsonElement(body).jsonObject

        val position = json["position"]!!.jsonObject
        val start = position["start"]!!.jsonObject
        val end = position["end"]!!.jsonObject

        assertEquals("old", json["seg_id"]!!.jsonPrimitive.content)
        assertEquals("划线内容", json["content"]!!.jsonPrimitive.content)
        assertEquals("p1", start["paragraph_id"]!!.jsonPrimitive.content)
        assertEquals("3", start["offset"]!!.jsonPrimitive.content)
        assertEquals("p1", end["paragraph_id"]!!.jsonPrimitive.content)
        assertEquals("7", end["offset"]!!.jsonPrimitive.content)
    }

    @Test
    fun updateSegmentMetaAfterLikeReadsPayloadSegId() {
        val highlight = SegmentHighlightSpan(
            text = "划线内容",
            meta = SegmentInfoMeta(segIds = listOf("old"), likeCount = 2),
        )
        val response = Json
            .parseToJsonElement(
                """{"payload":{"segId":"new-1,new-2"}}""",
            ).jsonObject

        val meta = updateSegmentMetaAfterLike(highlight, response)

        assertEquals(listOf("new-1", "new-2"), meta.segIds)
        assertEquals(true, meta.isLike)
        assertEquals(3, meta.likeCount)
    }

    @Test
    fun updateSegmentMetaAfterUnlikeDoesNotGoBelowZero() {
        val meta = updateSegmentMetaAfterUnlike(
            SegmentHighlightSpan(
                text = "划线内容",
                meta = SegmentInfoMeta(segIds = listOf("old"), isLike = true, likeCount = 0),
            ),
        )

        assertEquals(false, meta.isLike)
        assertEquals(0, meta.likeCount)
    }
}
