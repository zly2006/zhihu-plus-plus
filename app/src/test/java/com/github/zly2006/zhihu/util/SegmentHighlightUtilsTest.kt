/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.util

import com.github.zly2006.zhihu.data.SegmentInfoMark
import com.github.zly2006.zhihu.data.SegmentInfoMeta
import com.github.zly2006.zhihu.data.SegmentInfoParagraph
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SegmentHighlightUtilsTest {
    @Test
    fun build_segment_text_parts_should_keep_plain_and_highlight_ranges_in_order() {
        val parts = buildSegmentTextParts(
            text = "前半句高亮，后半句普通。",
            marks = listOf(
                SegmentInfoMark(
                    startIndex = 0,
                    endIndex = 5,
                    segInfo = SegmentInfoMeta(
                        segIds = listOf("123"),
                        likeCount = 7,
                        commentCount = 2,
                    ),
                ),
            ),
            sourceUrl = "https://www.zhihu.com/question/1/answer/2",
        )

        assertEquals(2, parts.size)
        assertEquals("前半句高亮", parts[0].text)
        assertEquals("，后半句普通。", parts[1].text)
        assertEquals(listOf("123"), parts[0].highlight?.meta?.segIds)
        assertEquals("https://www.zhihu.com/question/1/answer/2", parts[0].highlight?.sourceUrl)
    }

    @Test
    fun apply_segment_infos_to_html_should_inject_highlight_wrap_span() {
        val html = """<p data-pid="seg-1">第一句需要划线，第二句保持原样。</p>"""
        val result = applySegmentInfosToHtml(
            content = html,
            segmentInfos = listOf(
                SegmentInfoParagraph(
                    pid = "seg-1",
                    text = "第一句需要划线，第二句保持原样。",
                    marks = listOf(
                        SegmentInfoMark(
                            startIndex = 0,
                            endIndex = 7,
                            segInfo = SegmentInfoMeta(
                                segIds = listOf("abc"),
                                likeCount = 5,
                                commentCount = 1,
                            ),
                        ),
                    ),
                ),
            ),
            sourceUrl = "https://www.zhihu.com/question/1/answer/2",
        )

        assertTrue(result.contains("""class="highlight-wrap other has-comments""""))
        assertTrue(result.contains("""data-highlight-id="abc""""))
        assertTrue(result.contains("""data-highlight-source-url="https://www.zhihu.com/question/1/answer/2""""))
        assertTrue(result.contains("第二句保持原样。"))
    }

    @Test
    fun build_segment_text_parts_should_accept_master_segment_info() {
        val parts = buildSegmentTextParts(
            text = "发现全是密码的见证梗图",
            marks = listOf(
                SegmentInfoMark(
                    startIndex = 0,
                    endIndex = 11,
                    masterSegInfo = SegmentInfoMeta(
                        segIds = listOf("2040007848717967788"),
                        isLike = true,
                        likeCount = 81,
                    ),
                ),
            ),
        )

        assertEquals(1, parts.size)
        assertEquals("发现全是密码的见证梗图", parts[0].text)
        assertEquals(listOf("2040007848717967788"), parts[0].highlight?.meta?.segIds)
        assertEquals(true, parts[0].highlight?.meta?.isLike)
    }

    @Test
    fun build_segment_text_parts_should_deduplicate_same_range_seg_info_and_master_seg_info() {
        val text = "发现全是密码的见证梗图"
        val parts = buildSegmentTextParts(
            text = text,
            marks = listOf(
                SegmentInfoMark(
                    startIndex = 0,
                    endIndex = 11,
                    segInfo = SegmentInfoMeta(
                        segIds = listOf("1968601235792827980"),
                        isLike = false,
                        likeCount = 81,
                    ),
                ),
                SegmentInfoMark(
                    startIndex = 0,
                    endIndex = 11,
                    masterSegInfo = SegmentInfoMeta(
                        segIds = listOf("2040007848717967788"),
                        isLike = true,
                        likeCount = 81,
                    ),
                ),
            ),
        )

        assertEquals(text, parts.joinToString(separator = "") { it.text })
        assertEquals(1, parts.size)
        assertEquals(listOf("2040007848717967788", "1968601235792827980"), parts[0].highlight?.meta?.segIds)
        assertEquals(true, parts[0].highlight?.meta?.isLike)
    }

    @Test
    fun build_segment_text_parts_should_merge_sparse_same_range_segment_meta() {
        val parts = buildSegmentTextParts(
            text = "评论和点赞数据分散",
            marks = listOf(
                SegmentInfoMark(
                    startIndex = 0,
                    endIndex = 9,
                    segInfo = SegmentInfoMeta(
                        segIds = listOf("comment-seg"),
                        commentCount = 6,
                    ),
                ),
                SegmentInfoMark(
                    startIndex = 0,
                    endIndex = 9,
                    masterSegInfo = SegmentInfoMeta(
                        segIds = listOf("like-seg"),
                        isLike = true,
                        likeCount = 9,
                    ),
                ),
            ),
        )

        val meta = parts.single().highlight?.meta
        assertEquals(listOf("like-seg", "comment-seg"), meta?.segIds)
        assertEquals(true, meta?.isLike)
        assertEquals(9, meta?.likeCount)
        assertEquals(6, meta?.commentCount)
    }
}
