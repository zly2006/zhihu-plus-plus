/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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

package com.github.zly2006.zhihu.markdown

import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.util.SegmentTextParagraph
import com.github.zly2006.zhihu.ui.components.SegmentedText
import com.github.zly2006.zhihu.ui.components.segmentedTextStyle
import com.hrm.markdown.runtime.MarkdownBlockDirectiveRenderer
import com.hrm.markdown.runtime.MarkdownDirectivePlugin
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

internal const val ZHIHU_SEGMENTED_TEXT_DIRECTIVE = "zhihu-segmented-text"
internal const val ZHIHU_VIDEO_DIRECTIVE = "zhihu-video"
internal const val DIRECTIVE_PAYLOAD_ARG = "payload"
internal const val DIRECTIVE_TEXT_ARG = "text"
internal const val DIRECTIVE_VIDEO_ID_ARG = "videoId"
internal const val DIRECTIVE_THUMBNAIL_URL_ARG = "thumbnailUrl"

internal val zhihuMarkdownDirectiveJson = Json {
    ignoreUnknownKeys = true
}

private object ZhihuMarkdownDirectivePlugin : MarkdownDirectivePlugin {
    override val id: String = "zhihu-rich-blocks"

    override val blockDirectiveRenderers: Map<String, MarkdownBlockDirectiveRenderer> = mapOf(
        ZHIHU_SEGMENTED_TEXT_DIRECTIVE to { scope ->
            val payload = scope.directive.args[DIRECTIVE_PAYLOAD_ARG]
            val paragraph = remember(payload) {
                payload?.let {
                    runCatching {
                        zhihuMarkdownDirectiveJson.decodeFromString<SegmentTextParagraph>(it)
                    }.getOrNull()
                }
            }
            if (paragraph != null) {
                SegmentedText(
                    parts = paragraph.parts,
                    style = segmentedTextStyle(),
                )
            } else {
                Text(scope.directive.args[DIRECTIVE_TEXT_ARG].orEmpty())
            }
        },
        ZHIHU_VIDEO_DIRECTIVE to { scope ->
            scope.directive.args[DIRECTIVE_VIDEO_ID_ARG]?.toLongOrNull()?.let { videoId ->
                RenderVideoBox(
                    videoId = videoId,
                    thumbnailUrl = scope.directive.args[DIRECTIVE_THUMBNAIL_URL_ARG],
                )
            }
        },
    )
}

internal val zhihuMarkdownDirectivePlugins: List<MarkdownDirectivePlugin> =
    listOf(ZhihuMarkdownDirectivePlugin)
