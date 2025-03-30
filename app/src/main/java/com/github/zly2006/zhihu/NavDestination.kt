package com.github.zly2006.zhihu

import android.net.Uri
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface NavDestination

@Serializable
data object Home : NavDestination

@Serializable
data object Follow : NavDestination

@Serializable
data object History : NavDestination

@Serializable
data object Account : NavDestination

@Serializable
data object Dashboard : NavDestination

@Serializable
data object Notifications : NavDestination

@Serializable
data object Settings : NavDestination

@Serializable
enum class ArticleType {
    @SerialName("article")
    Article,

    @SerialName("answer")
    Answer,
    ;

    override fun toString(): String {
        return name.lowercase()
    }
}

@Serializable
data class Article(
    var title: String,
    @SerialName("article_type_1")
    val type: String,
    val id: Long,
    var authorName: String,
    var authorBio: String,
    var avatarSrc: String? = null,
    var excerpt: String? = null,
) : NavDestination {
    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is Article && other.id == id
    }
}

@Serializable
data class CommentHolder(
    val commentId: String,
    val article: NavDestination,
) : NavDestination

@Serializable
data class Question(
    val questionId: Long,
    val title: String
) : NavDestination {
    override fun hashCode(): Int {
        return questionId.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is Question && other.questionId == questionId
    }
}

fun resolveContent(uri: Uri): NavDestination? {
    if (uri.scheme == "http" || uri.scheme == "https") {
        if (uri.host == "zhihu.com" || uri.host == "www.zhihu.com") {
            if (uri.pathSegments.size == 4
                && uri.pathSegments[0] == "question"
                && uri.pathSegments[2] == "answer"
            ) {
                val answerId = uri.pathSegments[3].toLong()
                return Article(
                    "loading...",
                    "answer",
                    answerId,
                    "loading...",
                    "loading...",
                    null,
                )
            } else if (uri.pathSegments.size == 2
                && uri.pathSegments[0] == "answer"
            ) {
                val answerId = uri.pathSegments[1].toLong()
                return Article(
                    "loading...",
                    "answer",
                    answerId,
                    "loading...",
                    "loading...",
                    null,
                    null
                )
            } else if (uri.pathSegments.size == 2
                && uri.pathSegments[0] == "question"
            ) {
                val questionId = uri.pathSegments[1].toLong()
                return Question(
                    questionId,
                    "loading...",
                )
            }
        } else if (uri.host == "zhuanlan.zhihu.com") {
            if (uri.pathSegments.size == 2
                && uri.pathSegments[0] == "p"
            ) {
                val articleId = uri.pathSegments[1].toLong()
                return Article(
                    "loading...",
                    "article",
                    articleId,
                    "loading...",
                    "loading...",
                    null,
                    null
                )
            }
        }
    }
    if (uri.scheme == "zhihu") {
        if (uri.host == "answers") {
            val answerId = uri.pathSegments[0].toLong()
            return Article(
                "loading...",
                "answer",
                answerId,
                "loading...",
                "loading...",
                null,
                null
            )
        } else if (uri.host == "questions") {
            val questionId = uri.pathSegments[0].toLong()
            return Question(
                questionId,
                "loading...",
            )
        } else if (uri.host == "feed") {
            return Home
        } else if (uri.host == "articles") {
            val articleId = uri.pathSegments[0].toLong()
            return Article(
                "loading...",
                "article",
                articleId,
                "loading...",
                "loading...",
                null,
                null
            )
        }
    }
    return null
}
