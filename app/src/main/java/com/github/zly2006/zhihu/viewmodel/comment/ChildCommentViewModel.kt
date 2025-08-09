package com.github.zly2006.zhihu.viewmodel.comment

import com.github.zly2006.zhihu.CommentHolder
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.viewmodel.CommentItem

/**
 * 注意：此view model不按照正常VM生命期管理，不要使用viewModel()函数创建
 */
class ChildCommentViewModel(
    content: NavDestination,
) : BaseCommentViewModel(content) {
    override val initialUrl: String = when (content) {
        is CommentHolder -> {
            "https://www.zhihu.com/api/v4/comment_v5/comment/${content.commentId}/child_comment"
        }

        else -> ""
    }

    override fun createCommentItem(comment: DataHolder.Comment, article: NavDestination): CommentItem {
        // 子评论通常不需要可点击的目标
        val commentItem = CommentItem(comment, null)
        commentsMap[comment.id] = commentItem
        return commentItem
    }
}
