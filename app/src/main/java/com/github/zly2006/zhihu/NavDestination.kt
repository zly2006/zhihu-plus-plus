package com.github.zly2006.zhihu

import android.net.Uri
import com.github.zly2006.zhihu.data.AccountData
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

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
    val userToken: String,
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

    override fun toString(): String = name.lowercase()
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
    override fun hashCode(): Int = id.hashCode()

    override fun equals(other: Any?): Boolean = other is Article && other.id == id
}

@Serializable
data class CommentHolder(
    val commentId: String,
    val article: NavDestination,
) : NavDestination

@Serializable
data class Question(
    val questionId: Long,
    val title: String = "loading...",
) : NavDestination {
    override fun hashCode(): Int = questionId.hashCode()

    override fun equals(other: Any?): Boolean = other is Question && other.questionId == questionId
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
    var urlToken: String,
    val name: String = "loading...",
) : NavDestination {
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

    val userTokenOrId get() = urlToken.takeIf { it.isNotEmpty() } ?: id

    companion object {
        const val EMPTY_ID = "00000000000000000000000000000000"
    }
}

@Serializable
data class Video(
    val id: Long,
) : NavDestination

fun resolveContent(uri: Uri): NavDestination? {
    if (uri.scheme == "http" || uri.scheme == "https") {
        if (uri.host == "zhihu.com" || uri.host == "www.zhihu.com") {
            if (uri.pathSegments.size == 4 &&
                uri.pathSegments[0] == "question" &&
                uri.pathSegments[2] == "answer"
            ) {
                val answerId = uri.pathSegments[3].toLong()
                return Article(type = ArticleType.Answer, id = answerId)
            } else if (uri.pathSegments.size == 2 &&
                uri.pathSegments[0] == "answer"
            ) {
                val answerId = uri.pathSegments[1].toLong()
                return Article(type = ArticleType.Answer, id = answerId)
            } else if (uri.pathSegments.size == 2 &&
                uri.pathSegments[0] == "question"
            ) {
                val questionId = uri.pathSegments[1].toLong()
                return Question(questionId)
            } else if (uri.pathSegments.size == 3 &&
                uri.pathSegments[0] == "oia" &&
                uri.pathSegments[1] == "articles"
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
            } else if (uri.pathSegments.size == 2 && uri.pathSegments[0] == "video") {
                val videoId = uri.pathSegments[1].toLongOrNull() ?: return null
                return Video(id = videoId) // todo
            }
        } else if (uri.host == "zhuanlan.zhihu.com") {
            if (uri.pathSegments.size == 2 &&
                uri.pathSegments[0] == "p"
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

suspend fun checkForAd(destination: NavDestination, context: MainActivity): Boolean {
    val appViewUrl = when (destination) {
        is Article -> when (destination.type) {
            ArticleType.Article -> "https://www.zhihu.com/api/v4/articles//${destination.id}?include=content"
            ArticleType.Answer -> "https://www.zhihu.com/api/v4/answers/${destination.id}?include=content"
        }
        else -> return false
    }
    val httpClient = AccountData.httpClient(context)
    val response = httpClient.get(appViewUrl) {
        signFetchRequest(context)
    }
    val html = response.bodyAsText()
    println(html)
    val isAd = "xg.zhihu.com" in html // 广告
    var isPayWall = "本内容版权为知乎及版权方所有，侵权必究" in html // 盐选
    runCatching {
        val jojo = Json.decodeFromString<JsonObject>(html)
        if ("paid_info" in jojo) {
            isPayWall = true
        }
    }
    return isAd || isPayWall
}
