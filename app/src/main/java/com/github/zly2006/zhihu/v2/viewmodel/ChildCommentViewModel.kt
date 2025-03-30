package com.github.zly2006.zhihu.v2.viewmodel

import com.github.zly2006.zhihu.CommentHolder
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.data.DataHolder

class ChildCommentViewModel : BaseCommentViewModel() {
    
    override fun getCommentUrl(content: NavDestination?, isRefresh: Boolean): String? {
        return when (content) {
            is CommentHolder -> {
                "https://www.zhihu.com/api/v4/comment_v5/comment/${content.commentId}/child_comment"
            }
            else -> null
        }
    }
    
    override fun createCommentItem(comment: DataHolder.Comment, content: NavDestination?): CommentItem {
        // 子评论通常不需要可点击的目标
        val commentItem = CommentItem(comment, null)
        commentsMap[comment.id] = commentItem
        return commentItem
    }
}
