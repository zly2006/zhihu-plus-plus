@file:Suppress("PropertyName", "unused")

package com.github.zly2006.zhihu.data

import kotlinx.serialization.Serializable

@Serializable
data class Feed(
    val id: String,
    val type: String,
    val offset: Int = -1,
    val verb: String = "possibly ads, filter me",
    val created_time: Long = -1,
    val updated_time: Long = -1,
    val target: Target,
    val brief: String,
    val attached_info: String,
    val action_card: Boolean = false,
    /**
     * 屏蔽
     */
    val promotion_extra: String? = null,
) {
    @Serializable
    data class Target(
        val id: Long,
        val type: String,
        val url: String,
        val author: Author,
        /**
         * -1 广告
         */
        val created_time: Long = -1,
        val updated_time: Long = -1,
        val voteup_count: Int,
        val thanks_count: Int = -1,
        val comment_count: Int,
        val is_copyable: Boolean = false,
        val question: Question? = null,
        val thumbnail: String? = null,
        val excerpt: String,
        val excerpt_new: String,
        val preview_type: String,
        val preview_text: String,
        val reshipment_settings: String = "",
        val content: String,
        /**
         * null - 广告
         */
        val relationship: Relationship? = null,
        val is_labeled: Boolean,
        val visited_count: Int = 0,
        val thumbnails: List<String> = emptyList(),
        val favorite_count: Int = 0,
        val answer_type: String? = null
    )

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
        val followers_count: Int,
        val is_following: Boolean,
        val is_followed: Boolean,
        val badge: List<Badge> = emptyList(),
    )

    @Serializable
    data class Question(
        val id: Long,
        val type: String,
        val url: String,
        val author: Author,
        val title: String,
        val created: Long,
        val answer_count: Int,
        val follower_count: Int,
        val comment_count: Int,
        val bound_topic_ids: List<Long> = emptyList(),
        val is_following: Boolean,
        val excerpt: String,
        val relationship: Relationship,
        val detail: String,
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
