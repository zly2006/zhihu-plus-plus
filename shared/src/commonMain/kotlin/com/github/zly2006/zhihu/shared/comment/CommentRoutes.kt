package com.github.zly2006.zhihu.shared.comment

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.SegmentCommentHolder

enum class CommentSortOrder {
    SCORE, // 按热度
    TIME, // 按时间
}

fun commentLikeUrl(commentId: String): String =
    "https://www.zhihu.com/api/v4/comments/$commentId/like"

fun childCommentUrl(commentId: String): String =
    "https://www.zhihu.com/api/v4/comment_v5/comment/$commentId/child_comment"

val NavDestination.submitCommentUrl: String
    get() = when (this) {
        is Article -> {
            when (type) {
                ArticleType.Answer -> "https://www.zhihu.com/api/v4/comment_v5/answers/$id/comment"
                ArticleType.Article -> "https://www.zhihu.com/api/v4/comment_v5/articles/$id/comment"
            }
        }

        is Pin -> {
            "https://www.zhihu.com/api/v4/comment_v5/pins/$id/comment"
        }

        is Question -> {
            "https://www.zhihu.com/api/v4/comment_v5/questions/$questionId/comment"
        }

        is SegmentCommentHolder -> {
            "https://www.zhihu.com/api/v4/comment_v5/${normalizedContentType}s/$contentId/segment/comment?segment_id=$segmentId"
        }

        else -> ""
    }

val NavDestination.rootCommentUrl: String
    get() = when (this) {
        is Article -> {
            when (type) {
                ArticleType.Answer -> "https://www.zhihu.com/api/v4/comment_v5/answers/$id/root_comment"
                ArticleType.Article -> "https://www.zhihu.com/api/v4/comment_v5/articles/$id/root_comment"
            }
        }

        is Pin -> {
            "https://www.zhihu.com/api/v4/comment_v5/pins/$id/root_comment"
        }

        is Question -> {
            "https://www.zhihu.com/api/v4/comment_v5/questions/$questionId/root_comment"
        }

        is SegmentCommentHolder -> {
            "https://www.zhihu.com/api/v4/comment_v5/${normalizedContentType}s/$contentId/segment/root_comment?segment_id=$segmentId&limit=20&offset="
        }

        else -> ""
    }

private val SegmentCommentHolder.normalizedContentType: String
    get() = contentType.removeSuffix("s")
