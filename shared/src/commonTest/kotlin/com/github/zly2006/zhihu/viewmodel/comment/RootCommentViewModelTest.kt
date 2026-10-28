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

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.CommentHolder
import com.github.zly2006.zhihu.navigation.SegmentCommentHolder
import com.github.zly2006.zhihu.viewmodel.comment.RootCommentViewModel.Companion.submitCommentUrl
import kotlinx.serialization.json.jsonArray
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

        val body = target.buildSubmitCommentBody("，", replyToCommentId = null, syncToPin = true)
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

    @Test
    fun normalCommentIncludesPublishSyncPinSettingOnlyWhenEnabled() {
        val article = Article(type = ArticleType.Answer, id = 9001L)

        val defaultBody = article.buildSubmitCommentBody("普通评论", replyToCommentId = null)
        assertEquals("<p>普通评论</p>", defaultBody["content"]!!.jsonPrimitive.content)
        assertFalse("reply_comment_id" in defaultBody)
        assertFalse("selected_settings" in defaultBody)

        val syncedRootBody = article.buildSubmitCommentBody(
            commentText = "转发到想法的评论",
            replyToCommentId = null,
            syncToPin = true,
        )
        assertEquals("<p>转发到想法的评论</p>", syncedRootBody["content"]!!.jsonPrimitive.content)
        assertEquals(
            listOf("publish_sync_pin"),
            syncedRootBody["selected_settings"]!!.jsonArray.map { it.jsonPrimitive.content },
        )

        val childHolder = CommentHolder(commentId = "root-comment-1", article = article)
        val syncedChildBody = childHolder.buildSubmitCommentBody(
            commentText = "子评论同步想法",
            replyToCommentId = null,
            syncToPin = true,
        )
        assertEquals("<p>子评论同步想法</p>", syncedChildBody["content"]!!.jsonPrimitive.content)
        assertEquals("root-comment-1", syncedChildBody["reply_comment_id"]!!.jsonPrimitive.content)
        assertEquals(
            listOf("publish_sync_pin"),
            syncedChildBody["selected_settings"]!!.jsonArray.map { it.jsonPrimitive.content },
        )
    }
}
