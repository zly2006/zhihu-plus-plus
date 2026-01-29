package com.github.zly2006.zhihu.viewmodel.comment

import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.ArticleType
import com.github.zly2006.zhihu.CommentHolder
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.Pin
import com.github.zly2006.zhihu.Question
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.viewmodel.CommentItem

class RootCommentViewModel(
    content: NavDestination,
) : BaseCommentViewModel(content) {
    override val submitCommentUrl = when (content) {
        is Article -> {
            when (content.type) {
                ArticleType.Answer -> "https://www.zhihu.com/api/v4/comment_v5/answers/${content.id}/comment"
                ArticleType.Article -> "https://www.zhihu.com/api/v4/comment_v5/articles/${content.id}/comment"
            }
        }

        is Pin -> {
            "https://www.zhihu.com/api/v4/comment_v5/pins/${content.id}/comment"
        }

        is Question -> {
            "https://www.zhihu.com/api/v4/comment_v5/questions/${content.questionId}/comment"
        }

        else -> ""
    }

    override val initialUrl = when (content) {
        is Article -> {
            when (content.type) {
                ArticleType.Answer -> "https://www.zhihu.com/api/v4/comment_v5/answers/${content.id}/root_comment"
                ArticleType.Article -> "https://www.zhihu.com/api/v4/comment_v5/articles/${content.id}/root_comment"
            }
        }

        is Pin -> {
            "https://www.zhihu.com/api/v4/comment_v5/pins/${content.id}/root_comment"
        }

        is Question -> {
            "https://www.zhihu.com/api/v4/comment_v5/questions/${content.questionId}/root_comment"
        }

        else -> ""
    }

    override fun createCommentItem(comment: DataHolder.Comment, article: NavDestination): CommentItem {
        val clickTarget = CommentHolder(comment.id, article)

        val commentItem = CommentItem(comment, clickTarget)
        commentsMap[comment.id] = commentItem
        return commentItem
    }
}
