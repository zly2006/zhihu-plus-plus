package com.github.zly2006.zhihu.v2.viewmodel.comment

import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.CommentHolder
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.Question
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.v2.viewmodel.CommentItem

class RootCommentViewModel(content: NavDestination) : BaseCommentViewModel(content) {
    override val initialUrl = when (content) {
        is Article -> {
            if (content.type == "answer") {
                "https://www.zhihu.com/api/v4/comment_v5/answers/${content.id}/root_comment"
            } else if (content.type == "article") {
                "https://www.zhihu.com/api/v4/comment_v5/articles/${content.id}/root_comment"
            } else ""
        }

        is Question -> {
            "https://www.zhihu.com/api/v4/comment_v5/questions/${content.questionId}/root_comment"
        }

        else -> ""
    }

    override fun createCommentItem(comment: DataHolder.Comment, article: NavDestination): CommentItem {
        val clickTarget = if (comment.childCommentCount > 0) {
            CommentHolder(comment.id, article)
        } else null

        val commentItem = CommentItem(comment, clickTarget)
        commentsMap[comment.id] = commentItem
        return commentItem
    }
}
