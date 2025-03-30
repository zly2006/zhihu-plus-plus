package com.github.zly2006.zhihu.v2.viewmodel.comment

import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.CommentHolder
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.v2.viewmodel.CommentItem

class RootCommentViewModel : BaseCommentViewModel() {
    
    override fun getCommentUrl(content: NavDestination?, isRefresh: Boolean): String? {
        return when (content) {
            is Article -> {
                if (content.type == "answer") {
                    "https://www.zhihu.com/api/v4/comment_v5/answers/${content.id}/root_comment"
                } else if (content.type == "article") {
                    "https://www.zhihu.com/api/v4/comment_v5/articles/${content.id}/root_comment"
                } else null
            }
            else -> null
        }
    }
    
    override fun createCommentItem(comment: DataHolder.Comment, content: NavDestination?): CommentItem {
        val clickTarget = if (comment.childCommentCount > 0) {
            CommentHolder(comment.id, content!!)
        } else null
        
        val commentItem = CommentItem(comment, clickTarget)
        commentsMap[comment.id] = commentItem
        return commentItem
    }
}
