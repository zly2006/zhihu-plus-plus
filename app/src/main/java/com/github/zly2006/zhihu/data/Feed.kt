@file:Suppress("PropertyName", "unused")

package com.github.zly2006.zhihu.data

import com.github.zly2006.zhihu.data.Feed.Badge
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

@Serializable
data class Feed(
    val id: String = "",
    val type: String,
    val offset: Int = -1,
    val verb: String = "possibly ads, filter me",
    val created_time: Long = -1,
    val updated_time: Long = -1,
    /**
     * 广告没有target
     */
    val target: Target? = null,
    val brief: String = "<none>",
    val attached_info: String = "",
    val action_card: Boolean = false,
    /**
     * 屏蔽
     */
    val promotion_extra: String? = null,
    val cursor: String = ""
) {
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
                is AdvertTarget -> "广告"
            }
        }

        fun detailsText(): String
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

        override fun detailsText(): String {
            return "回答 · $voteup_count 赞同 · $comment_count 评论"
        }
    }
    @Serializable
    @SerialName("zvideo")
    data class VideoTarget(
        val id: Long,
        val author: Person,
        val vote_count: Int = -1,
        val comment_count: Int,
        val title: String,
        val description: String,
        val excerpt: String,
    ) : Target {
        override fun filterReason(): String? {
            return if (author.followers_count < 50 && vote_count < 20 && !author.is_following) {
                "规则：所有视频"
            } else null
        }

        override fun detailsText(): String {
            return "视频 · $vote_count 赞 · $comment_count 评论"
        }
    }

    @Serializable
    @SerialName("article")
    data class ArticleTarget(
        val id: Long,
        val url: String,
        val author: Person,
        val voteup_count: Int,
        val comment_count: Int,
        val title: String,
        val excerpt: String,
        val content: String,
        val created: Long,
        val updated: Long,
        val is_labeled: Boolean,
        val visited_count: Int,
        val favorite_count: Int,
    ) : Target {
        override fun filterReason(): String? {
            return if ((author.followers_count < 50 || voteup_count < 20) && !author.is_following) {
                "规则：文章；作者粉丝数 < 50 || 文章赞数 < 20，未关注作者"
            } else null
        }

        override fun detailsText(): String {
            return "文章 · $voteup_count 赞 · $comment_count 评论"
        }
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

        override fun detailsText(): String {
            return "想法 · $favorite_count 赞 · $comment_count 评论"
        }
    }
    @Serializable
    @SerialName("feed_advert")
    data class AdvertTarget(
        val title: String
    ) : Target {
        override fun filterReason(): String? {
            return "广告"
        }

        override fun detailsText(): String {
            return "广告"
        }
    }

    @Serializable
    data class Badge(
        val type: String,
        val description: String,
        val topic_names: List<String>? = null,
        val topic_ids: List<Int>? = null
    )

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

@Serializable
data class Person(
    val id: String,
    val url: String,
    val user_type: String,
    val url_token: String,
    val name: String,
    val headline: String,
    val avatar_url: String,
    val is_org: Boolean,
    val gender: Int,
    val followers_count: Int = 0,
    val is_following: Boolean = false,
    val is_followed: Boolean = false,
    val badge: List<Badge> = emptyList(),
)
