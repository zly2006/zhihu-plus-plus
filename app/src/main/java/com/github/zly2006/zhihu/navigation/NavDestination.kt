/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
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

package com.github.zly2006.zhihu.navigation

import android.util.Log
import io.ktor.http.Url
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface NavDestination

interface TopLevelDestination {
    val name: String
}

@Serializable
data object Home : NavDestination, TopLevelDestination {
    override val name: String
        get() = "Home"
}

@Serializable
data object Follow : NavDestination, TopLevelDestination {
    override val name: String
        get() = "Follow"
}

@Serializable
data object HotList : NavDestination, TopLevelDestination {
    override val name: String
        get() = "HotList"
}

@Serializable
data object History : NavDestination, TopLevelDestination {
    override val name: String
        get() = "History"
}

@Serializable
data object OnlineHistory : NavDestination, TopLevelDestination {
    override val name: String
        get() = "OnlineHistory"
}

@Serializable
data object Account : NavDestination, TopLevelDestination {
    override val name: String
        get() = "Account"

    @Serializable
    data class AppearanceSettings(
        val setting: String = "",
    ) : NavDestination

    @Serializable
    data class RecommendSettings(
        val setting: String = "",
    ) : NavDestination {
        @Serializable
        data object Blocklist : NavDestination

        @Serializable
        data object BlockedFeedHistory : NavDestination
    }

    @Serializable
    data object SystemAndUpdateSettings : NavDestination

    @Serializable
    data object OpenSourceLicenses : NavDestination

    @Serializable
    data object DeveloperSettings : NavDestination {
        @Serializable
        data object ColorScheme : NavDestination
    }
}

@Serializable
data object Daily : NavDestination, TopLevelDestination {
    override val name: String
        get() = "Daily"
}

@Serializable
data object Notification : NavDestination {
    @Serializable
    data object NotificationSettings : NavDestination
}

@Serializable
data object SentenceSimilarityTest : NavDestination

@Serializable
data class Search(
    val query: String = "",
) : NavDestination

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

    override fun equals(other: Any?): Boolean = other is Article && other.id == id && other.type == type
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
    val jumpTo: String = "",
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

@Serializable
data class Pin(
    val id: Long,
) : NavDestination {
    override fun hashCode(): Int = id.hashCode()

    override fun equals(other: Any?): Boolean = other is Pin && other.id == id
}

fun resolveContent(url: String): NavDestination? = runCatching { resolveContent(Url(url)) }.getOrNull()

fun resolveContent(url: Url): NavDestination? {
    val segments = url.segments
    if (url.protocol.name == "http" || url.protocol.name == "https") {
        if (url.host == "zhihu.com" || url.host == "www.zhihu.com") {
            if (segments.size == 4 &&
                segments[0] == "question" &&
                segments[2] == "answer"
            ) {
                val answerId = segments[3].toLong()
                return Article(type = ArticleType.Answer, id = answerId)
            } else if (segments.size == 2 &&
                segments[0] == "answer"
            ) {
                val answerId = segments[1].toLong()
                return Article(type = ArticleType.Answer, id = answerId)
            } else if (segments.size == 2 &&
                segments[0] == "question"
            ) {
                val questionId = segments[1].toLong()
                return Question(questionId)
            } else if (segments.size == 3 &&
                segments[0] == "oia" &&
                segments[1] == "articles"
            ) {
                val articleId = segments[2].toLong()
                return Article(type = ArticleType.Article, id = articleId)
            } else if (segments.size == 2 && segments[0] == "people") {
                val urlToken = segments[1]
                if (urlToken.length == 32 && urlToken.all { it in '0'..'9' || it in 'a'..'f' }) {
                    // 32 hex characters, likely a user ID
                    return Person(id = urlToken, urlToken = urlToken)
                } else {
                    // human-readable token
                    return Person(id = Person.EMPTY_ID, urlToken = urlToken)
                }
            } else if (segments.size == 2 && segments[0] == "video") {
                val videoId = segments[1].toLongOrNull() ?: return null
                return Video(id = videoId) // todo
            } else if (segments.size == 2 && segments[0] == "pin") {
                val pinId = segments[1].toLongOrNull() ?: return null
                return Pin(id = pinId)
            } else if (segments.size == 1 &&
                segments[0] == "search"
            ) {
                val query = url.parameters["q"] ?: ""
                return Search(query)
            }
            Log.w("NavDestination", "Cannot resolve content from url: $url")
        } else if (url.host == "zhuanlan.zhihu.com") {
            if (segments.size == 2 &&
                segments[0] == "p"
            ) {
                val articleId = segments[1].toLong()
                return Article(type = ArticleType.Article, id = articleId)
            }
            Log.w("NavDestination", "Cannot resolve content from url: $url")
        } else if (url.host == "link.zhihu.com") {
            val target = url.parameters["target"] ?: return null
            return runCatching { Url(target) }.getOrNull()?.let(::resolveContent)
        }
    }
    if (url.protocol.name == "zhihu") {
        if (url.host == "answers") {
            val answerId = segments[0].toLong()
            return Article(type = ArticleType.Answer, id = answerId)
        } else if (url.host == "questions") {
            val questionId = segments[0].toLong()
            return Question(questionId)
        } else if (url.host == "feed") {
            return Home
        } else if (url.host == "articles") {
            val articleId = segments[0].toLong()
            return Article(type = ArticleType.Article, id = articleId)
        } else if (url.host == "search") {
            val query = url.parameters["q"] ?: ""
            return Search(query)
        } else if (url.host == "pin") {
            val pinId = segments[0].toLong()
            return Pin(id = pinId)
        }
        Log.w("NavDestination", "Cannot resolve content from url: $url")
    }
    return null
}
