@file:Suppress("unused")

package com.github.zly2006.zhihu.data

import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.ArticleType
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.data.Feed.Badge
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray

@Serializable
sealed interface Feed {
    @Serializable
    sealed interface Target {
        fun filterReason(): String? = null

        fun description(): String = when (this) {
            is AnswerTarget -> "回答"
            is VideoTarget -> "视频"
            is ArticleTarget -> "文章"
            is PinTarget -> "想法"
            is QuestionTarget -> "问题"
        }

        val detailsText: String
        val title: String
        val url: String
        val excerpt: String?
        val author: Person?
        val createdTime: Long
        val updatedTime: Long
        val navDestination: NavDestination?
    }

    private object LegacyAuthorSerCompat : KSerializer<Person?> {
        override val descriptor: SerialDescriptor = Person.serializer().descriptor.nullable

        override fun serialize(
            encoder: Encoder,
            value: Person?,
        ) {
        }

        override fun deserialize(decoder: Decoder) = try {
            Person.serializer().deserialize(decoder)
        } catch (_: Exception) {
            // consume a string -- legacy api compatibility
            decoder.decodeString()
            null
        }
    }

    @Serializable
    @SerialName("answer")
    data class AnswerTarget(
        val id: Long,
        override val url: String,
        @Serializable(with = LegacyAuthorSerCompat::class)
        override val author: Person? = null,
        /**
         * -1 广告
         */
        override val createdTime: Long = -1,
        override val updatedTime: Long = -1,
        val voteupCount: Int = -1,
        val thanksCount: Int = -1,
        val commentCount: Int = -1,
        val isCopyable: Boolean = false,
        val question: QuestionTarget,
        val thumbnail: String? = null,
        override val excerpt: String? = null,
        val reshipmentSettings: String = "",
        val content: String = "",
        val relationship: Relationship? = null,
        val isLabeled: Boolean = false,
        val visitedCount: Int = 0,
        val thumbnails: List<String> = emptyList(),
        val favoriteCount: Int = 0,
        val answerType: String? = null,
    ) : Target {
        override fun filterReason(): String? = if (voteupCount < 10 && author?.isFollowing == false) {
            "规则：回答；赞数 < 10，未关注作者"
        } else {
            null
        }

        override val detailsText = "回答 · $voteupCount 赞同 · $commentCount 评论"
        override val title: String
            get() = question.title
        override val navDestination = Article(
            title = question.title,
            type = ArticleType.Answer,
            id = id,
            authorName = author?.name ?: "loading...",
            authorBio = author?.headline ?: "",
            avatarSrc = author?.avatarUrl,
            excerpt = excerpt,
        )
    }

    @Serializable
    @SerialName("zvideo")
    data class VideoTarget(
        val id: Long,
        override val url: String = "",
        override val author: Person,
        val voteCount: Int = -1,
        val commentCount: Int,
        override val title: String,
        val description: String,
        override val excerpt: String,
    ) : Target {
        override fun filterReason(): String? = if (author.followersCount < 50 && voteCount < 20 && !author.isFollowing) {
            "规则：所有视频"
        } else {
            null
        }

        override val detailsText = "视频 · $voteCount 赞 · $commentCount 评论"

        override val navDestination = null

        override val createdTime: Long
            get() = -1

        override val updatedTime: Long
            get() = -1
    }

    @Serializable
    @SerialName("article")
    data class ArticleTarget(
        val id: Long,
        override val url: String,
        override val author: Person,
        val voteupCount: Int = 0,
        val commentCount: Int = 0,
        override val title: String,
        override val excerpt: String = "",
        /**
         * 老API不支持
         */
        val content: String = "",
        /**
         * 老API不支持
         */
        val created: Long = 0,
        /**
         * 老API不支持
         */
        val updated: Long = 0,
        val isLabeled: Boolean = false,
        val visitedCount: Int = 0,
        val favoriteCount: Int = 0,
    ) : Target {
        override fun filterReason(): String? = if ((author.followersCount < 50 || voteupCount < 20) && !author.isFollowing) {
            "规则：文章；作者粉丝数 < 50 或 文章赞数 < 20，未关注作者"
        } else {
            null
        }

        override val detailsText = "文章 · $voteupCount 赞 · $commentCount 评论"

        override val navDestination = Article(
            title = title,
            type = ArticleType.Article,
            id = id,
            authorName = author.name,
            authorBio = author.headline,
            avatarSrc = author.avatarUrl,
            excerpt = excerpt,
        )

        override val updatedTime: Long
            get() = updated

        override val createdTime: Long
            get() = created
    }

    @Serializable
    @SerialName("pin")
    /**
     * 知乎想法
     */
    data class PinTarget(
        val id: Long,
        override val url: String,
        override val author: Person,
        val commentCount: Int,
        val content: JsonArray,
        val favoriteCount: Int = 0,
    ) : Target {
        override fun filterReason(): String? = null

        override val detailsText = "想法 · $favoriteCount 赞 · $commentCount 评论"
        override val title: String
            get() = "想法"
        override val excerpt = null
        override val navDestination = null

        override val updatedTime: Long
            get() = -1

        override val createdTime: Long
            get() = -1
    }

    @Suppress("PropertyName")
    @Serializable
    @SerialName("question")
    data class QuestionTarget(
        val id: Long,
        @SerialName("name")
        val _name: String? = null,
        @SerialName("title")
        val _title: String? = null,
        override val url: String,
        val type: String,
        /**
         * 不存在于老API
         */
        val questionType: String = "",
        /**
         * 不存在于老API
         */
        val created: Long = 0,
        val answerCount: Int = 0,
        val commentCount: Int = 0,
        val followerCount: Int = 0,
        /**
         * 不存在于老API
         */
        val detail: String = "",
        override val excerpt: String = "",
        val boundTopicIds: List<Long> = emptyList(),
        val relationship: Relationship? = null,
        val isFollowing: Boolean = false,
    ) : Target {
        override val title: String
            get() = _title ?: _name.orEmpty()

        override val author: Person? = null

        override fun filterReason(): String? = if (answerCount < 5 && followerCount < 50) {
            "规则：问题；回答数 < 5，关注数 < 50"
        } else {
            null
        }

        override val detailsText = "问题 · $followerCount 关注 · $answerCount 回答"

        override val navDestination = com.github.zly2006.zhihu.Question(
            questionId = id,
            title = title,
        )

        override val updatedTime: Long
            get() = -1
        override val createdTime: Long
            get() = created
    }

    @Serializable
    data class Badge(
        val type: String,
        val description: String,
    )

    @Serializable
    data class Card(
        val id: String,
        val type: String,
        val target: Target,
        val attachedInfo: String = "",
        val brief: String = "",
        /**
         * 广告相关
         */
        val feedSpecific: JsonArray? = null,
        /**
         * 广告相关
         */
        val adInfo: JsonArray? = null,
        val debug: JsonArray? = null,
        val uninterest: JsonArray? = null,
        val extra: JsonArray? = null,
        /**
         * 广告相关
         */
        val monitorUrls: JsonArray? = null,
        /**
         * 相关的badge
         */
        val badge: List<Badge> = emptyList(),
        /**
         * 是否已读
         */
        val hasRead: Boolean = false,
    ) {
        /**
         * 过滤原因
         */
        val filterReason: String?
            get() = target.filterReason()
    }
}

val Feed.target: Feed.Target?
    get() = when (this) {
        is CommonFeed -> target
        is QuestionFeedCard -> target
        is MomentsFeed -> target
        is FeedItemIndexGroup -> target
        is HotListFeed -> target
        else -> null
    }

val Feed.actionText: String?
    get() = when (this) {
        is CommonFeed -> actionText
        is GroupFeed -> groupText
        is AdvertisementFeed -> actionText
        is QuestionFeedCard -> null
        is MomentsFeed -> null
        is FeedItemIndexGroup -> actionText
        is HotListFeed -> detailText
    }

@Serializable
@SerialName("feed_advert")
class AdvertisementFeed(
    val actionText: String = "",
) : Feed

@Serializable
@SerialName("feed_group")
class GroupFeed(
    val id: String = "",
    val attachedInfo: String = "",
    val brief: String,
    val groupText: String,
    val list: List<CommonFeed>,
    val styleType: Int = 0,
) : Feed

@Serializable
@SerialName("question_feed_card")
class QuestionFeedCard(
    val position: Int = 0,
    val target: Feed.Target,
    val cursor: String,
    val targetType: String,
    val isJumpNative: Boolean = false,
    val skipCount: Boolean = false,
) : Feed

@Serializable
@SerialName("feed")
data class CommonFeed(
    val id: String = "",
    val type: String,
    val verb: String = "possibly ads, filter me",
    val createdTime: Long = -1,
    val updatedTime: Long = -1,
    /**
     * 广告没有target
     */
    val target: Feed.Target? = null,
    val brief: String = "<none>",
    val attachedInfo: String = "",
    val actionCard: Boolean = false,
    /**
     * 屏蔽
     */
    val promotionExtra: String? = null,
    val cursor: String = "",
    val actionText: String? = null,
) : Feed

@Serializable
@SerialName("feed_item_index_group")
data class FeedItemIndexGroup(
    val id: String = "",
    val target: Feed.Target,
    val actors: List<Person>,
    val actionText: String,
    val actionTime: Long,
) : Feed

@Serializable
@SerialName("moments_feed")
data class MomentsFeed(
    val id: String,
//    val interaction: Interaction? = null,
    val momentDesc: String = "",
    val target: Feed.Target,
    val targetType: String,
) : Feed {
    @Serializable
    class FeedSource(
        val actionText: String,
        val actor: FeedSourceActor,
    )

    @Serializable
    class FeedSourceActor(
        val name: String,
        val id: String,
        val urlToken: String,
    )
}

@Serializable
data class Person(
    val id: String,
    val url: String,
    val userType: String,
    val urlToken: String? = null,
    val name: String,
    @Serializable(HTMLDecoder::class)
    val headline: String,
    val avatarUrl: String,
    val isOrg: Boolean = false,
    val gender: Int = 0, // todo: 0做默认合适吗？
    val followersCount: Int = 0,
    val isFollowing: Boolean = false,
    val isFollowed: Boolean = false,
    val badge: List<Badge>? = null,
)

@Serializable
data class Relationship(
    val isFollowing: Boolean = false,
    val isFollowed: Boolean = false,
)

@Serializable
@SerialName("hot_list_feed")
class HotListFeed(
    val id: String,
    val detailText: String,
    val target: Feed.Target,
) : Feed
