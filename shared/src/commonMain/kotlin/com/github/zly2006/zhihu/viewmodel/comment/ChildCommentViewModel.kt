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

package com.github.zly2006.zhihu.viewmodel.comment

import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.navigation.CommentHolder
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.shared.comment.childCommentUrl
import com.github.zly2006.zhihu.shared.comment.submitCommentUrl
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.viewmodel.CommentItem
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 注意：此view model不按照正常VM生命期管理，不要使用viewModel()函数创建
 */
class ChildCommentViewModel(
    content: NavDestination,
) : BaseCommentViewModel(content) {
    override val initialUrl: String = when (content) {
        is CommentHolder -> childCommentUrl(content.commentId)

        else -> ""
    }

    override fun createCommentItem(comment: DataHolder.Comment, article: NavDestination): CommentItem {
        // 子评论通常不需要可点击的目标
        val commentItem = CommentItem(comment, null)
        commentsMap[comment.id] = commentItem
        return commentItem
    }

    override fun submitComment(
        content: NavDestination,
        commentText: String,
        environment: PaginationEnvironment,
        replyToCommentId: String?,
        onSuccess: () -> Unit,
    ) {
        val commentHolder = content as CommentHolder
        if (commentText.isBlank()) return

        viewModelScope.launch {
            try {
                // Escape HTML special characters to prevent HTML injection
                val escapedText = commentText.escapeCommentHtml()

                // Use buildJsonObject to properly escape JSON special characters
                val requestBody = buildJsonObject {
                    put("content", "<p>$escapedText</p>")
                    put("reply_comment_id", replyToCommentId ?: commentHolder.commentId)
                }

                val response = environment.httpClient().post(commentHolder.article.submitCommentUrl) {
                    environment.configureSignedRequest(this)
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }

                if (response.status.isSuccess()) {
                    // 评论成功后，把它添加到第一个。
                    val model = ZhihuJson.decodeJson<DataHolder.Comment>(response.body<JsonObject>())
                    allData.add(0, model)
                    onSuccess()
                } else {
                    errorMessage = "评论发送失败: ${response.status}"
                }
            } catch (e: Exception) {
                errorMessage = "评论发送异常: ${e.message}"
            }
        }
    }
}
