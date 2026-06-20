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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.viewmodel.CommentItem
import com.github.zly2006.zhihu.viewmodel.ContentBlocklistEnvironment
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.PaginationViewModel
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import com.github.zly2006.zhihu.viewmodel.deleteSigned
import com.github.zly2006.zhihu.viewmodel.postSigned
import io.ktor.http.isSuccess
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlin.reflect.typeOf

enum class CommentSortOrder {
    SCORE, // 按热度
    TIME, // 按时间
}

abstract class BaseCommentViewModel(
    val article: NavDestination,
) : PaginationViewModel<DataHolder.Comment>(typeOf<DataHolder.Comment>()) {
    protected val commentsMap = mutableMapOf<String, CommentItem>()
    var sortOrder by mutableStateOf(CommentSortOrder.SCORE)

    override fun processResponse(environment: PaginationEnvironment, data: List<DataHolder.Comment>, rawData: JsonArray) {
        debugData.addAll(rawData) // 保存原始JSON
        filterBlockedComments(environment, data).forEach { comment ->
            if (allData.none { it.id == comment.id }) {
                // 避免服务器返回重复评论时重复添加，造成LazyColumn key冲突
                allData.add(comment)
            }
            val commentItem = createCommentItem(comment, article)
            commentsMap[comment.id] = commentItem
            // 载入可见的子评论
            comment.childComments.forEach {
                val childCommentItem = createCommentItem(it, article)
                commentsMap[it.id] = childCommentItem
            }
        }
    }

    private fun filterBlockedComments(
        environment: ContentBlocklistEnvironment,
        comments: List<DataHolder.Comment>,
    ): List<DataHolder.Comment> {
        val blockedUserIds = environment.blockedUserIds()
        if (blockedUserIds.isEmpty()) return comments
        return comments.mapNotNull { comment ->
            if (comment.author.id in blockedUserIds) {
                null
            } else {
                comment.copy(
                    childComments = comment.childComments.filterNot { it.author.id in blockedUserIds },
                )
            }
        }
    }

    abstract fun createCommentItem(comment: DataHolder.Comment, article: NavDestination): CommentItem

    fun changeSortOrder(newSortOrder: CommentSortOrder, environment: PaginationEnvironment) {
        if (sortOrder != newSortOrder) {
            sortOrder = newSortOrder
            refresh(environment)
        }
    }

    abstract fun submitComment(
        content: NavDestination,
        commentText: String,
        environment: ZhihuApiEnvironment,
        replyToCommentId: String? = null,
        onSuccess: () -> Unit,
    )

    var isLikeLoading by mutableStateOf(false)

    fun toggleLikeComment(
        commentData: DataHolder.Comment,
        environment: ZhihuApiEnvironment,
        onSuccess: () -> Unit,
    ) {
        if (isLikeLoading) return
        isLikeLoading = true

        val commentId = commentData.id
        val newLikeState = !commentData.liked
        viewModelScope.launch {
            try {
                val likeUrl = "https://www.zhihu.com/api/v4/comments/$commentId/like"
                val response = if (newLikeState) {
                    environment.postSigned(likeUrl)
                } else {
                    environment.deleteSigned(likeUrl)
                }

                if (response.status.isSuccess()) {
                    onSuccess()
                } else {
                    errorMessage = "操作失败：${response.status}"
                }
            } catch (e: Exception) {
                errorMessage = "操作失败：${e.message}"
            } finally {
                isLikeLoading = false
            }
        }
    }
}
