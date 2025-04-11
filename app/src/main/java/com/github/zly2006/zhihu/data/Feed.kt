@file:Suppress("PropertyName", "unused")

package com.github.zly2006.zhihu.data

import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.data.Feed.Badge
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

@Serializable
sealed interface Feed {
    @Serializable
    sealed interface Target {
        fun filterReason(): String? {
            return null
        }

        fun description(): String {
            return when (this) {
                is AnswerTarget -> "回答"
                is VideoTarget -> "视频"
                is ArticleTarget -> "文章"
                is PinTarget -> "想法"
                is QuestionTarget -> "问题"
            }
        }

        val detailsText: String
        val title: String
        val navDestination: NavDestination?
    }

    @Serializable
    @SerialName("answer")
    data class AnswerTarget(
        val id: Long,
        val url: String,
        val author: Person,
        /**
         * -1 广告
         */
        val created_time: Long = -1,
        val updated_time: Long = -1,
        val voteup_count: Int = -1,
        val thanks_count: Int = -1,
        val comment_count: Int = -1,
        val is_copyable: Boolean = false,
        val question: Question,
        val thumbnail: String? = null,
        val excerpt: String? = null,
        val reshipment_settings: String = "",
        val content: String = "",
        val relationship: Relationship,
        val is_labeled: Boolean = false,
        val visited_count: Int = 0,
        val thumbnails: List<String> = emptyList(),
        val favorite_count: Int = 0,
        val answer_type: String? = null
    ) : Target {
        override fun filterReason(): String? {
            return if (voteup_count < 10 && !author.is_following) {
                "规则：回答；赞数 < 10，未关注作者"
            } else null
        }

        override val detailsText = "回答 · $voteup_count 赞同 · $comment_count 评论"
        override val title: String
            get() = question.title
        override val navDestination = Article(
            title = question.title,
            type = "answer",
            id = id,
            authorName = author.name,
            authorBio = author.headline,
            avatarSrc = author.avatar_url,
            excerpt = excerpt
        )
    }

    @Serializable
    @SerialName("zvideo")
    data class VideoTarget(
        val id: Long,
        val author: Person,
        val vote_count: Int = -1,
        val comment_count: Int,
        override val title: String,
        val description: String,
        val excerpt: String,
    ) : Target {
        override fun filterReason(): String? {
            return if (author.followers_count < 50 && vote_count < 20 && !author.is_following) {
                "规则：所有视频"
            } else null
        }

        override val detailsText = "视频 · $vote_count 赞 · $comment_count 评论"

        override val navDestination = null
    }

    @Serializable
    @SerialName("article")
    data class ArticleTarget(
        val id: Long,
        val url: String,
        val author: Person,
        val voteup_count: Int,
        val comment_count: Int,
        override val title: String,
        val excerpt: String,
        val content: String,
        val created: Long,
        val updated: Long,
        val is_labeled: Boolean = false,
        val visited_count: Int = 0,
        val favorite_count: Int = 0,
    ) : Target {
        override fun filterReason(): String? {
            return if ((author.followers_count < 50 || voteup_count < 20) && !author.is_following) {
                "规则：文章；作者粉丝数 < 50 或 文章赞数 < 20，未关注作者"
            } else null
        }

        override val detailsText = "文章 · $voteup_count 赞 · $comment_count 评论"

        override val navDestination = Article(
            title = title,
            type = "article",
            id = id,
            authorName = author.name,
            authorBio = author.headline,
            avatarSrc = author.avatar_url,
            excerpt = excerpt
        )
    }

    @Serializable
    @SerialName("pin")
    /**
     * 知乎想法
     */
    data class PinTarget(
        val id: Long,
        val url: String,
        val author: Person,
        val comment_count: Int,
        val content: JsonArray,
        val favorite_count: Int,
    ) : Target {
        override fun filterReason(): String? {
            return null
        }

        override val detailsText = "想法 · $favorite_count 赞 · $comment_count 评论"
        override val title: String
            get() = "想法"
        override val navDestination = null
    }

    @Serializable
    @SerialName("question")
    data class QuestionTarget(
        val id: String,
        override val title: String,
        val url: String,
        val type: String,
        val question_type: String,
        val created: Long,
        val answer_count: Int = 0,
        val comment_count: Int = 0,
        val follower_count: Int = 0,
        val detail: String,
        val excerpt: String = "",
        val bound_topic_ids: List<Long> = emptyList(),
        val relationship: Relationship? = null,
        val is_following: Boolean = false,
        val author: Person? = null
    ) : Target {
        override fun filterReason(): String? {
            return if (answer_count < 5 && follower_count < 50) {
                "规则：问题；回答数 < 5，关注数 < 50"
            } else null
        }

        override val detailsText = "问题 · $follower_count 关注 · $answer_count 回答"

        override val navDestination = com.github.zly2006.zhihu.Question(
            questionId = id.toLong(),
            title = title
        )
    }

    @Serializable
    data class Badge(
        val type: String,
        val description: String,
        val topic_names: List<String>? = null,
        val topic_ids: List<Int>? = null
    )

    // todo
    @Deprecated("TODO: QuestionTarget instead")
    @Serializable
    data class Question(
        val id: Long,
        val type: String,
        val url: String,
        val author: Person? = null,
        val title: String,
        val created: Long,
        val answer_count: Int = 0,
        val follower_count: Int = 0,
        val comment_count: Int = 0,
        val bound_topic_ids: List<Long> = emptyList(),
        val is_following: Boolean = false,
        val excerpt: String = "<default value, R U in question details page?>",
        val relationship: Relationship? = null,
        val detail: String = "<default value, R U in question details page?>",
        val question_type: String,
    )

    @Serializable
    data class Relationship(
        val is_author: Boolean = false,
        val is_thanked: Boolean = false,
        val is_nothelp: Boolean = false,
        val voting: Int = 0
    )
}

val Feed.target: Feed.Target?
    get() = when (this) {
        is CommonFeed -> target
        is QuestionFeedCard -> target
        else -> null
    }

@Serializable
@SerialName("feed_advert")
class AdvertisementFeed(
    val action_text: String = "",
) : Feed

@Serializable
@SerialName("feed_group")
class GroupFeed(
    val id: String = "",
    val attached_info: String = "",
    val brief: String,
    val group_text: String,
    val list: List<CommonFeed>,
    val style_type: Int = 0,
) : Feed

@Serializable
@SerialName("question_feed_card")
class QuestionFeedCard(
    val position: Int = 0,
    val target: Feed.Target,
    val cursor: String,
    val target_type: String,
    val is_jump_native: Boolean = false,
    val skip_count: Boolean = false,
) : Feed

@Serializable
@SerialName("feed")
data class CommonFeed(
    val id: String = "",
    val type: String,
    val verb: String = "possibly ads, filter me",
    val created_time: Long = -1,
    val updated_time: Long = -1,
    /**
     * 广告没有target
     */
    val target: Feed.Target? = null,
    val brief: String = "<none>",
    val attached_info: String = "",
    val action_card: Boolean = false,
    /**
     * 屏蔽
     */
    val promotion_extra: String? = null,
    val cursor: String = "",
    val action_text: String = "",
) : Feed {
}

@Serializable
data class Person(
    val id: String,
    val url: String,
    val user_type: String,
    val url_token: String? = null,
    val name: String,
    @Serializable(HTMLDecoder::class)
    val headline: String,
    val avatar_url: String,
    val is_org: Boolean = false,
    val gender: Int,
    val followers_count: Int = 0,
    val is_following: Boolean = false,
    val is_followed: Boolean = false,
    val badge: List<Badge>? = null,
)
