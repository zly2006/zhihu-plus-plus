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
import com.github.zly2006.zhihu.data.SegmentInfoParagraph
import org.jsoup.nodes.Element

typealias SegmentHighlightSpan = com.github.zly2006.zhihu.shared.util.SegmentHighlightSpan
typealias SegmentTextPart = com.github.zly2006.zhihu.shared.util.SegmentTextPart
typealias SegmentTextParagraph = com.github.zly2006.zhihu.shared.util.SegmentTextParagraph

fun buildSegmentTextParts(
    text: String,
    marks: List<SegmentInfoMark>,
    sourceUrl: String? = null,
    contentId: String? = null,
    contentType: String? = null,
    paragraphId: String? = null,
): List<SegmentTextPart> = com.github.zly2006.zhihu.shared.util.buildSegmentTextParts(
    text = text,
    marks = marks,
    sourceUrl = sourceUrl,
    contentId = contentId,
    contentType = contentType,
    paragraphId = paragraphId,
)

fun applySegmentInfosToHtml(
    content: String,
    segmentInfos: List<SegmentInfoParagraph>,
    sourceUrl: String? = null,
    contentId: String? = null,
    contentType: String? = null,
): String = com.github.zly2006.zhihu.shared.util.applySegmentInfosToHtml(
    content = content,
    segmentInfos = segmentInfos,
    sourceUrl = sourceUrl,
    contentId = contentId,
    contentType = contentType,
)

fun parseSegmentTextParagraph(element: Element): SegmentTextParagraph? =
    com.github.zly2006.zhihu.shared.util
        .parseSegmentTextParagraphHtml(element.outerHtml())
