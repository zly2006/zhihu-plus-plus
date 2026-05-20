package com.github.zly2006.zhihu.shared.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SegmentInfoTest {
    @Test
    fun decodesBooleanCompatibleSegmentMeta() {
        val paragraph = ZhihuJson.json.decodeFromString<SegmentInfoParagraph>(
            """
            {
              "pid": "p1",
              "text": "hello",
              "marks": [
                {
                  "startIndex": 0,
                  "endIndex": 5,
                  "segInfo": {
                    "segIds": ["s1"],
                    "isLike": 1,
                    "likeCount": 2,
                    "commentCount": 3,
                    "myCommentCount": 4,
                    "isSpan": "true"
                  },
                  "masterSegInfo": {
                    "segIds": ["master"],
                    "isLike": false
                  }
                }
              ]
            }
            """.trimIndent(),
        )

        val meta = checkNotNull(paragraph.marks.single().segInfo)

        assertEquals("p1", paragraph.pid)
        assertEquals(listOf("s1"), meta.segIds)
        assertTrue(meta.isLike)
        assertEquals(2, meta.likeCount)
        assertEquals(3, meta.commentCount)
        assertEquals(4, meta.myCommentCount)
        assertTrue(meta.isSpan)
    }

    @Test
    fun effectiveSegmentInfoPrefersSegmentInfoOverMaster() {
        val segInfo = SegmentInfoMeta(segIds = listOf("segment"))
        val masterSegInfo = SegmentInfoMeta(segIds = listOf("master"))
        val mark = SegmentInfoMark(
            startIndex = 0,
            endIndex = 1,
            segInfo = segInfo,
            masterSegInfo = masterSegInfo,
        )

        assertSame(segInfo, mark.effectiveSegInfo)
    }

    @Test
    fun effectiveSegmentInfoFallsBackToMaster() {
        val masterSegInfo = SegmentInfoMeta(
            segIds = listOf("master"),
            isLike = false,
        )
        val mark = SegmentInfoMark(
            startIndex = 0,
            endIndex = 1,
            masterSegInfo = masterSegInfo,
        )

        assertFalse(masterSegInfo.isLike)
        assertSame(masterSegInfo, mark.effectiveSegInfo)
    }
}
