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
data class Collections(
    val userToken: String
) : NavDestination

@Serializable
data class CollectionContent(
    val collectionId: String,
) : NavDestination

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
    var title: String = "loading...",
    @SerialName("article_type_1")
    val type: ArticleType,
    val id: Long,
    var authorName: String = "loading...",
    var authorBio: String = "loading...",
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
    val title: String = "loading..."
) : NavDestination {
    override fun hashCode(): Int {
        return questionId.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is Question && other.questionId == questionId
    }
}

@Serializable
data class Person(
    /**
     * 32 hex characters
     */
    var id: String,
    /**
     * human-readable token, used in URL.
     */
    val urlToken: String,
    val name: String = "loading...",
): NavDestination {
    override fun hashCode(): Int {
        if (id != EMPTY_ID) {
            // 32 hex characters, likely a user ID
            return id.hashCode()
        }
        return urlToken.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is Person) {
            if (id != EMPTY_ID && other.id != EMPTY_ID) {
                return other.id == id
            }
            return other.urlToken == urlToken
        }
        return false
    }

    companion object {
        const val EMPTY_ID = "00000000000000000000000000000000"
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
                return Article(type = ArticleType.Answer, id = answerId)
            } else if (uri.pathSegments.size == 2
                && uri.pathSegments[0] == "answer"
            ) {
                val answerId = uri.pathSegments[1].toLong()
                return Article(type = ArticleType.Answer, id = answerId)
            } else if (uri.pathSegments.size == 2
                && uri.pathSegments[0] == "question"
            ) {
                val questionId = uri.pathSegments[1].toLong()
                return Question(questionId)
            } else if (uri.pathSegments.size == 3
                && uri.pathSegments[0] == "oia"
                && uri.pathSegments[1] == "articles"
            ) {
                val articleId = uri.pathSegments[2].toLong()
                return Article(type = ArticleType.Article, id = articleId)
            } else if (uri.pathSegments.size == 2 && uri.pathSegments[0] == "people") {
                val urlToken = uri.pathSegments[1]
                if (urlToken.length == 32 && urlToken.all { it in '0'..'9' || it in 'a'..'f' }) {
                    // 32 hex characters, likely a user ID
                    return Person(id = urlToken, urlToken = urlToken)
                } else {
                    // human-readable token
                    return Person(id = Person.EMPTY_ID, urlToken = urlToken)
                }
            }
        } else if (uri.host == "zhuanlan.zhihu.com") {
            if (uri.pathSegments.size == 2
                && uri.pathSegments[0] == "p"
            ) {
                val articleId = uri.pathSegments[1].toLong()
                return Article(type = ArticleType.Article, id = articleId)
            }
        }
    }
    if (uri.scheme == "zhihu") {
        if (uri.host == "answers") {
            val answerId = uri.pathSegments[0].toLong()
            return Article(type = ArticleType.Answer, id = answerId)
        } else if (uri.host == "questions") {
            val questionId = uri.pathSegments[0].toLong()
            return Question(questionId)
        } else if (uri.host == "feed") {
            return Home
        } else if (uri.host == "articles") {
            val articleId = uri.pathSegments[0].toLong()
            return Article(type = ArticleType.Article, id = articleId)
        }
    }
    return null
}
