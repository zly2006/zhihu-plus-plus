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

package com.github.zly2006.zhihu.viewmodel.comment

import com.github.zly2006.zhihu.navigation.SegmentCommentHolder
import com.github.zly2006.zhihu.viewmodel.comment.RootCommentViewModel.Companion.submitCommentUrl
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RootCommentViewModelTest {
    @Test
    fun segmentCommentUsesBodyTargetInsteadOfQueryParameter() {
        val target = SegmentCommentHolder(
            contentId = "123",
            contentType = "answer",
            segmentId = "segment-id",
            segmentContent = "都不能称为 Rust 的杀手级项目",
            paragraphId = "KFR9BNtv",
            startOffset = 22,
            endOffset = 39,
        )

        assertEquals(
            "https://www.zhihu.com/api/v4/comment_v5/answers/123/segment/comment",
            target.submitCommentUrl,
        )

        val body = target.buildSubmitCommentBody("，", replyToCommentId = null)
        val segment = body["segment"]!!.jsonObject
        val position = segment["position"]!!.jsonObject

        assertEquals("<p>，</p>", body["content"]!!.jsonPrimitive.content)
        assertFalse("unfriendly_check" in body)
        assertFalse("selected_settings" in body)
        assertEquals("都不能称为 Rust 的杀手级项目", segment["content"]!!.jsonPrimitive.content)
        assertEquals("22", position["start"]!!.jsonObject["offset"]!!.jsonPrimitive.content)
        assertEquals("KFR9BNtv", position["start"]!!.jsonObject["paragraph_id"]!!.jsonPrimitive.content)
        assertEquals("39", position["end"]!!.jsonObject["offset"]!!.jsonPrimitive.content)
        assertEquals("KFR9BNtv", position["end"]!!.jsonObject["paragraph_id"]!!.jsonPrimitive.content)
        assertFalse("segment_id" in body)
    }
}
