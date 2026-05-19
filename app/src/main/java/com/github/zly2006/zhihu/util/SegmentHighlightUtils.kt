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
import com.github.zly2006.zhihu.data.effectiveSegInfo
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

data class SegmentHighlightSpan(
    val text: String,
    val meta: SegmentInfoMeta,
    val sourceUrl: String? = null,
    val contentId: String? = null,
    val contentType: String? = null,
    val paragraphId: String? = null,
    val startOffset: Int? = null,
    val endOffset: Int? = null,
)

data class SegmentTextPart(
    val text: String,
    val highlight: SegmentHighlightSpan? = null,
)

data class SegmentTextParagraph(
    val pid: String?,
    val text: String,
    val parts: List<SegmentTextPart>,
)

private data class NormalizedSegmentMark(
    val startIndex: Int,
    val endIndex: Int,
    val meta: SegmentInfoMeta,
    val isMaster: Boolean,
)

fun buildSegmentTextParts(
    text: String,
    marks: List<SegmentInfoMark>,
    sourceUrl: String? = null,
    contentId: String? = null,
    contentType: String? = null,
    paragraphId: String? = null,
): List<SegmentTextPart> {
    if (text.isEmpty()) return emptyList()
    if (marks.isEmpty()) return listOf(SegmentTextPart(text))

    val normalized = marks
        .mapNotNull { mark ->
            val start = mark.startIndex.coerceIn(0, text.length)
            val end = mark.endIndex.coerceIn(start, text.length)
            val meta = mark.effectiveSegInfo
            if (start >= end || meta == null) {
                null
            } else {
                NormalizedSegmentMark(
                    startIndex = start,
                    endIndex = end,
                    meta = meta,
                    isMaster = mark.masterSegInfo != null,
                )
            }
        }.groupBy { it.startIndex to it.endIndex }
        .map { (_, sameRangeMarks) ->
            sameRangeMarks.mergeSameRangeMarks()
        }.sortedWith(compareBy<NormalizedSegmentMark> { it.startIndex }.thenBy { it.endIndex })
    if (normalized.isEmpty()) return listOf(SegmentTextPart(text))

    val parts = mutableListOf<SegmentTextPart>()
    var cursor = 0
    normalized.forEach { mark ->
        if (mark.startIndex < cursor) return@forEach
        if (mark.startIndex > cursor) {
            parts += SegmentTextPart(text.substring(cursor, mark.startIndex))
        }
        parts += SegmentTextPart(
            text = text.substring(mark.startIndex, mark.endIndex),
            highlight = SegmentHighlightSpan(
                text = text.substring(mark.startIndex, mark.endIndex),
                meta = mark.meta,
                sourceUrl = sourceUrl,
                contentId = contentId,
                contentType = contentType,
                paragraphId = paragraphId,
                startOffset = mark.startIndex,
                endOffset = mark.endIndex,
            ),
        )
        cursor = mark.endIndex
    }
    if (cursor < text.length) {
        parts += SegmentTextPart(text.substring(cursor))
    }
    return parts.filter { it.text.isNotEmpty() }
}

private fun List<NormalizedSegmentMark>.mergeSameRangeMarks(): NormalizedSegmentMark {
    val masterMarks = filter { it.isMaster }
    val ordered = masterMarks + filterNot { it.isMaster }
    val mergedMeta = SegmentInfoMeta(
        segIds = ordered
            .flatMap { it.meta.segIds }
            .distinct(),
        isLike = any { it.meta.isLike },
        likeCount = maxOf { it.meta.likeCount },
        commentCount = maxOf { it.meta.commentCount },
        myCommentCount = maxOf { it.meta.myCommentCount },
        isSpan = any { it.meta.isSpan },
    )
    val first = first()
    return first.copy(
        meta = mergedMeta,
        isMaster = masterMarks.isNotEmpty(),
    )
}

fun applySegmentInfosToHtml(
    content: String,
    segmentInfos: List<SegmentInfoParagraph>,
    sourceUrl: String? = null,
    contentId: String? = null,
    contentType: String? = null,
): String {
    if (content.isBlank() || segmentInfos.isEmpty()) return content

    val document = Jsoup.parseBodyFragment(content)
    segmentInfos.forEach { paragraph ->
        val target = document.selectFirst("""p[data-pid="${paragraph.pid}"]""") ?: return@forEach
        if (target.text() != paragraph.text) return@forEach

        target.empty()
        buildSegmentTextParts(
            text = paragraph.text,
            marks = paragraph.marks,
            sourceUrl = sourceUrl,
            contentId = contentId,
            contentType = contentType,
            paragraphId = paragraph.pid,
        ).forEach { part ->
            val highlight = part.highlight
            if (highlight == null) {
                target.appendChild(TextNode(part.text))
            } else {
                target.appendChild(
                    Element("span").apply {
                        addClass("highlight-wrap")
                        addClass("other")
                        if (highlight.meta.commentCount > 0) {
                            addClass("has-comments")
                        }
                        attr("data-highlight-id", highlight.meta.segIds.joinToString(","))
                        attr("data-highlight-like-count", highlight.meta.likeCount.toString())
                        attr("data-highlight-comment-count", highlight.meta.commentCount.toString())
                        attr("data-highlight-my-comment-count", highlight.meta.myCommentCount.toString())
                        attr("data-highlight-is-like", highlight.meta.isLike.toString())
                        attr("data-highlight-is-span", highlight.meta.isSpan.toString())
                        attr(
                            "data-highlight-split-type",
                            when {
                                part.text == paragraph.text -> "both"
                                paragraph.text.startsWith(part.text) -> "head"
                                paragraph.text.endsWith(part.text) -> "tail"
                                else -> "middle"
                            },
                        )
                        attr("data-highlight-id-extra", "")
                        highlight.sourceUrl?.let { attr("data-highlight-source-url", it) }
                        contentId?.let { attr("data-highlight-content-id", it) }
                        contentType?.let { attr("data-highlight-content-type", it) }
                        highlight.paragraphId?.let { attr("data-highlight-pid", it) }
                        highlight.startOffset?.let { attr("data-highlight-start-offset", it.toString()) }
                        highlight.endOffset?.let { attr("data-highlight-end-offset", it.toString()) }
                        text(part.text)
                    },
                )
            }
        }
    }
    return document.body().html()
}

fun parseSegmentTextParagraph(element: Element): SegmentTextParagraph? {
    if (element.tagName() != "p") return null
    val parts = element.childNodes().mapNotNull(::parseSegmentNode)
    if (parts.isEmpty() || parts.none { it.highlight != null }) return null
    return SegmentTextParagraph(
        pid = element.attr("data-pid").ifBlank { null },
        text = parts.joinToString(separator = "") { it.text },
        parts = parts,
    )
}

private fun parseSegmentNode(node: Node): SegmentTextPart? = when (node) {
    is TextNode -> node.text().takeIf { it.isNotEmpty() }?.let(::SegmentTextPart)
    is Element -> {
        if (!node.hasClass("highlight-wrap")) {
            return null
        }
        SegmentTextPart(
            text = node.text(),
            highlight = SegmentHighlightSpan(
                text = node.text(),
                meta = SegmentInfoMeta(
                    segIds = node
                        .attr("data-highlight-id")
                        .split(',')
                        .map(String::trim)
                        .filter(String::isNotEmpty),
                    isLike = node.attr("data-highlight-is-like").toBoolean(),
                    likeCount = node.attr("data-highlight-like-count").toIntOrNull() ?: 0,
                    commentCount = node.attr("data-highlight-comment-count").toIntOrNull() ?: 0,
                    myCommentCount = node.attr("data-highlight-my-comment-count").toIntOrNull() ?: 0,
                    isSpan = node.attr("data-highlight-is-span").toBoolean(),
                ),
                sourceUrl = node.attr("data-highlight-source-url").ifBlank { null },
                contentId = node.attr("data-highlight-content-id").ifBlank { null },
                contentType = node.attr("data-highlight-content-type").ifBlank { null },
                paragraphId = node.attr("data-highlight-pid").ifBlank { null },
                startOffset = node.attr("data-highlight-start-offset").toIntOrNull(),
                endOffset = node.attr("data-highlight-end-offset").toIntOrNull(),
            ),
        )
    }
    else -> null
}
