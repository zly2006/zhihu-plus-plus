@file:Suppress("PropertyName", "unused")

package com.github.zly2006.zhihu.data

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
    val target: Target,
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
    sealed interface Target
    @Serializable
    @SerialName("answer")
    data class AnswerTarget(
        val id: Long,
        val url: String,
        val author: Author,
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
        val excerpt: String,
        val reshipment_settings: String = "",
        val content: String = "",
        /**
         * null - 广告
         */
        val relationship: Relationship,
        val is_labeled: Boolean = false,
        val visited_count: Int = 0,
        val thumbnails: List<String> = emptyList(),
        val favorite_count: Int = 0,
        val answer_type: String? = null
    ) : Target
    @Serializable
    @SerialName("zvideo")
    data class VideoTarget(
        val id: Long,
        val author: Author,
        val vote_count: Int = -1,
        val comment_count: Int,
        val title: String,
        val description: String,
        val excerpt: String,
    ) : Target

    @Serializable
    @SerialName("article")
    data class ArticleTarget(
        val id: Long,
        val url: String,
        val author: Author,
        val voteup_count: Int,
        val created_time: Long,
        val updated_time: Long,
        val thanks_count: Int,
        val comment_count: Int,
        val title: String,
        val excerpt: String,
        val content: String,
        val relationship: Relationship,
        val is_labeled: Boolean,
        val visited_count: Int,
        val thumbnail: String,
        val favorite_count: Int,
        val answer_type: String
    ) : Target

    @Serializable
    @SerialName("pin")
    /**
     * 知乎想法
     */
    data class PinTarget(
        val id: Long,
        val url: String,
        val author: Author,
        val comment_count: Int,
        val content: JsonArray,
        val favorite_count: Int,
    ) : Target
    @Serializable
    @SerialName("feed_advert")
    data class AdvertTarget(
        val title: String
    ) : Target

    @Serializable
    data class Badge(
        val type: String,
        val description: String,
        val topic_names: List<String>? = null,
        val topic_ids: List<Int>? = null
    )

    @Serializable
    data class Author(
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
        val is_following: Boolean,
        val is_followed: Boolean,
        val badge: List<Badge> = emptyList(),
    )

    @Serializable
    data class Question(
        val id: Long,
        val type: String,
        val url: String,
        val author: Author? = null,
        val title: String,
        val created: Long,
        val answer_count: Int = 0,
        val follower_count: Int = 0,
        val comment_count: Int = 0,
        val bound_topic_ids: List<Long> = emptyList(),
        val is_following: Boolean = false,
        val excerpt: String = "<default value, R U in question details page?>",
        val relationship: Relationship,
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
