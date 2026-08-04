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

package com.github.zly2006.zhihu.ui.article

import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.markdown.RenderVideoBox
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** 渲染知乎接口中 `attachment.type=video` 的文章附件。 */
@Composable
internal fun ArticleVideoAttachmentContent(attachment: JsonElement?) {
    if (attachment
            ?.jsonObject
            ?.get("type")
            ?.jsonPrimitive
            ?.content == "video"
    ) {
        val videoId = attachment
            .jsonObject["attachmentId"]
            ?.jsonPrimitive
            ?.content
            ?.toLongOrNull()
        if (videoId != null) {
            val thumbnail = attachment
                .jsonObject["video"]!!
                .jsonObject["videoInfo"]!!
                .jsonObject["thumbnail"]!!
                .jsonPrimitive.content
            RenderVideoBox(
                videoId = videoId,
                thumbnailUrl = thumbnail,
            )
        }
    }
}
