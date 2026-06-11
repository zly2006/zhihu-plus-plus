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

import com.github.zly2006.zhihu.shared.util.Log
import io.ktor.http.Url
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface NavDestination

/**
 * 底部栏和主 pager 使用的 tab 目标。
 *
 * 为了兼容旧调用，tab 目标仍可能同时是历史遗留的 [NavDestination] 值。需要进入主壳时应使用 [MainTabs]，
 * 再由主壳选择对应 tab。
 */
interface TopLevelDestination {
    val name: String
}

/**
 * 主壳真实使用的导航目的地。
 *
 * [Home]、[Follow]、[Daily] 等旧顶层目的地仍保留为可序列化值，因为 deeplink、剪贴板解析、设置、持久化历史和旧调用点
 * 仍可能解析到它们。顶层目标应视为 tab 选择目标，而不是直接 push 到主 NavHost 的页面 route。
 */
@Serializable
data object MainTabs : NavDestination

/**
 * 主 pager 的历史顶层 tab 目标。
 *
 * 该值服务 deeplink、设置、测试和旧导航调用点。不要把它重新注册成主 NavHost 的独立页面；
 * 应导航到 [MainTabs] 后选择对应 pager 页。
 */
@Serializable
data object Home : TopLevelDestination {
    override val name: String
        get() = "Home"
}

/**
 * 主 pager 的历史顶层 tab 目标。
 *
 * [Follow] 会映射到“推荐”和“动态”两个相邻 pager 页；上次选择由 ZhihuMain 记住。
 */
@Serializable
data object Follow : TopLevelDestination {
    override val name: String
        get() = "Follow"
}

/**
 * 主 pager 的历史顶层 tab 目标。
 */
@Serializable
data object HotList : TopLevelDestination {
    override val name: String
        get() = "HotList"
}

/**
 * 历史遗留的本地浏览历史 route。
 *
 * 当前底部栏使用的是 [OnlineHistory]，所以这里仍是独立 NavHost 页面。除非本地历史重新变成可配置 tab，
 * 否则不要把它混进主 pager tab 列表。
 */
@Serializable
data object History : NavDestination, TopLevelDestination {
    override val name: String
        get() = "History"
}

/**
 * 主 pager 的历史顶层 tab 目标。
 */
@Serializable
data object OnlineHistory : TopLevelDestination {
    override val name: String
        get() = "OnlineHistory"
}

/**
 * 主 pager 的历史顶层 tab 目标。
 */
@Serializable
data object MyCollections : TopLevelDestination {
    override val name: String
        get() = "MyCollections"
}

/**
 * Legacy top-level tab target for the main pager.
 */
@Serializable
data object Account : TopLevelDestination {
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

/**
 * 主 pager 的历史顶层 tab 目标。
 */
@Serializable
data object Daily : TopLevelDestination {
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
    val restrictedMemberHashId: String = "",
    val restrictedMemberName: String = "",
) : NavDestination {
    val isRestrictedToMember: Boolean
        get() = restrictedMemberHashId.isNotBlank()
}

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
data class SegmentCommentHolder(
    val contentId: String,
    val contentType: String,
    val segmentId: String,
) : NavDestination

@Serializable
data class Question(
    val questionId: Long,
    val title: String = "loading...",
) : NavDestination {
    override fun hashCode(): Int = questionId.hashCode()

    override fun equals(other: Any?): Boolean = other is Question && other.questionId == questionId
}

/**
 * 在问题详情页发起“写回答/编辑回答”的编辑器页面。
 *
 * 说明：
 * - 目前编辑器只提供纯文本输入（可输入 Markdown），不做语法高亮等复杂编辑能力。
 * - 是否是“新回答”还是“更新已有回答”，由上传逻辑在发布前根据登录账号自动探测。
 */
@Serializable
data class WriteAnswer(
    val questionId: Long,
    val questionTitle: String = "",
    val questionDetail: String = "",
) : NavDestination {
    override fun hashCode(): Int = questionId.hashCode()

    override fun equals(other: Any?): Boolean = other is WriteAnswer && other.questionId == questionId
}

@Serializable
data class Person(
    /**
     * 32 位十六进制字符。
     */
    var id: String,
    /**
     * 用在 URL 中的可读 token。
     */
    var urlToken: String,
    val name: String = "loading...",
    val jumpTo: String = "",
) : NavDestination {
    override fun hashCode(): Int {
        if (id != EMPTY_ID) {
            // 32 位十六进制字符，通常是用户 ID。
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
                    // 32 位十六进制字符，通常是用户 ID。
                    return Person(id = urlToken, urlToken = urlToken)
                } else {
                    // 可读 token。
                    return Person(id = Person.EMPTY_ID, urlToken = urlToken)
                }
            } else if (segments.size == 2 && segments[0] == "video") {
                val videoId = segments[1].toLongOrNull() ?: return null
                return Video(id = videoId) // TODO: 视频详情页待完善。
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
            return MainTabs
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
