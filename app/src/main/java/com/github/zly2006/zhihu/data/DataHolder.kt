package com.github.zly2006.zhihu.data

import android.content.Context
import android.util.Log
import com.github.zly2006.zhihu.ArticleType
import com.github.zly2006.zhihu.util.signFetchRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

object DataHolder {
    suspend fun getContentDetail(
        context: Context,
        dest: com.github.zly2006.zhihu.Article,
    ): Content? {
        val apiUrl = when (dest.type) {
            ArticleType.Article -> "https://www.zhihu.com/api/v4/articles/${dest.id}?include=content,paid_info,can_comment,excerpt,thanks_count,voteup_count,comment_count,visited_count,relationship,relationship.vote"
            ArticleType.Answer -> "https://www.zhihu.com/api/v4/answers/${dest.id}?include=content,paid_info,can_comment,excerpt,thanks_count,voteup_count,comment_count,visited_count,reaction,reaction.relation.voting"
        }

        return runCatching {
            val jo = AccountData.fetchGet(context, apiUrl) {
                signFetchRequest(context)
            }
            val jojo = buildJsonObject {
                jo.entries.forEach { (key, value) ->
                    if (key == "id") {
                        put(key, value.jsonPrimitive.long)
                    } else {
                        put(key, value)
                    }
                }
            }
            // 解析为对应的Content类型
            when (dest.type) {
                ArticleType.Answer -> AccountData.decodeJson<Answer>(jojo)
                ArticleType.Article -> AccountData.decodeJson<Article>(jojo)
            }
        }.getOrElse { e ->
            Log.e("getContentDetail", "Failed to fetch content detail for ${dest.type} id=${dest.id}", e)
            null
        }
    }

    suspend fun getContentDetail(
        context: Context,
        question: com.github.zly2006.zhihu.Question,
    ): Question? {
        val apiUrl = "https://www.zhihu.com/api/v4/questions/${question.questionId}?include=read_count,visit_count,answer_count,voteup_count,comment_count,follower_count,detail,excerpt,author,relationship.is_following,topics"

        return runCatching {
            val jo = AccountData.fetchGet(context, apiUrl) {
                signFetchRequest(context)
            }
            val jojo = buildJsonObject {
                jo.entries.forEach { (key, value) ->
                    if (key == "id") {
                        put(key, value.jsonPrimitive.long)
                    } else {
                        put(key, value)
                    }
                }
            }
            // 解析为对应的Content类型
            AccountData.decodeJson<Question>(jojo)
        }.getOrElse { e ->
            Log.e("getContentDetail", "Failed to fetch content detail for question id=${question.questionId}", e)
            null
        }
    }

    suspend fun getContentDetail(
        context: Context,
        pin: com.github.zly2006.zhihu.Pin,
    ): Pin? {
        val apiUrl = "https://www.zhihu.com/api/v4/pins/${pin.id}"

        return runCatching {
            val jo = AccountData.fetchGet(context, apiUrl) {
                signFetchRequest(context)
            }
            AccountData.decodeJson<Pin>(jo)
        }.getOrElse { e ->
            Log.e("getContentDetail", "Failed to fetch content detail for pin id=${pin.id}", e)
            null
        }
    }

    @Serializable
    sealed interface Content

    @Serializable
    object DummyContent : Content

    @Serializable
    data class Author(
        val avatarUrl: String,
        val avatarUrlTemplate: String,
        val gender: Int,
        @Serializable(HTMLDecoder::class)
        val headline: String,
        val id: String,
        val isAdvertiser: Boolean,
        val isOrg: Boolean,
        val isPrivacy: Boolean = false,
        val name: String,
        val type: String,
        val url: String,
        val urlToken: String,
        val userType: String,
        val badge: List<JsonElement> = emptyList(),
        val badgeV2: BadgeV2? = null,
        val exposedMedal: ExposedMedal? = null,
        val vipInfo: VipInfo? = null,
        val followerCount: Int = 0,
        val isFollowed: Boolean = false,
        val isBlocked: Boolean = false,
        val isBlocking: Boolean = false,
        val isCelebrity: Boolean = false,
        val isFollowing: Boolean = false,
    )

    @Serializable
    data class ExposedMedal(
        val avatarUrl: String,
        val description: String,
        val medalId: String,
        val medalName: String,
        val medalAvatarFrame: String? = null,
        val miniAvatarUrl: String? = null,
    )

    @Serializable
    data class VipInfo(
        val isVip: Boolean,
        val vipIcon: VipIcon? = null,
    )

    @Serializable
    data class VipIcon(
        val nightModeUrl: String,
        val url: String,
    )

    @Serializable
    data class BizExt(
        val shareGuide: ShareGuide? = null,
    )

    @Serializable
    data class ShareGuide(
        val hasPositiveBubble: Boolean = false,
        val hasTimeBubble: Boolean = false,
        val hitShareGuideCluster: Boolean = false,
    )

    @Serializable
    data class AnswerModelQuestion(
        val created: Long,
        val id: Long,
        val questionType: String,
        val relationship: Relationship? = null,
        val title: String,
        val type: String,
        val updatedTime: Long,
        val url: String,
    )

    @Serializable
    data class Relationship(
        // v4 API removed fields are commented out
//        val isAuthor: Boolean = false,
//        val isAuthorized: Boolean = false,
//        val isNothelp: Boolean = false,
//        val isFavorited: Boolean = false,
//        val isThanked: Boolean = false,
        val upvotedFollowees: List<String> = emptyList(),
        // 1 - 点赞， -1 - 点踩
//        val voting: Int = 0,
    )

    @Serializable
    data class RewardInfo(
        val canOpenReward: Boolean,
        val isRewardable: Boolean,
        val rewardMemberCount: Int,
        val rewardTotalMoney: Int,
        val tagline: String,
    )

    @Serializable
    data class Settings(
        val tableOfContents: TableOfContents,
    )

    @Serializable
    data class TableOfContents(
        val enabled: Boolean,
    )

    @Serializable
    data class SuggestEdit(
        val reason: String,
        val status: Boolean,
        val tip: String,
        val title: String,
        val unnormalDetails: UnnormalDetails? = null,
        val url: String,
    )

    @Serializable
    data class UnnormalDetails(
        val description: String? = null,
        val note: String? = null,
        val reason: String? = null,
        val reasonId: Int? = null,
        val status: String? = null,
    )

    @Serializable
    class Reaction(
        val relation: Relation? = null,
    )

    @Serializable
    class Relation(
        val isAuthor: Boolean = false,
        val vote: String,
        val faved: Boolean = false,
        val liked: Boolean = false,
        val following: Boolean = false,
        val subscribed: Boolean = false,
    )

    @Serializable
    data class Answer(
        val adminClosedComment: Boolean = false,
        val annotationAction: JsonElement? = null,
        val answerType: String,
        val author: Author,
        val canComment: CanComment,
        val collapseReason: String? = null,
        val collapsedBy: String? = null,
        val commentCount: Int = 0,
        val commentPermission: String? = null,
        val content: String,
        val contentNeedTruncated: Boolean = false,
        val createdTime: Long,
        val editableContent: String? = null,
        val excerpt: String,
        val extras: String? = null,
        val favlistsCount: Int = 0,
        val id: Long,
        val isCollapsed: Boolean = false,
        val isCopyable: Boolean = false,
        val isJumpNative: Boolean = false,
        val isLabeled: Boolean = false,
        val isNormal: Boolean = false,
        val isMine: Boolean = false,
        val isSticky: Boolean = false,
        val isVisible: Boolean = false,
        val question: AnswerModelQuestion,
        val reactionInstruction: ReactionInstruction? = null,
        val relationship: Relationship? = null,
        val reaction: Reaction? = null,
        val relevantInfo: RelevantInfo? = null,
        val reshipmentSettings: String? = null,
        val rewardInfo: RewardInfo? = null,
        val suggestEdit: SuggestEdit? = null,
        val thanksCount: Int,
        val type: String,
        val updatedTime: Long,
        val url: String,
        val voteupCount: Int,
        val bizExt: BizExt? = null,
        val contentMark: JsonObject? = null,
        val decorativeLabels: List<JsonElement> = emptyList(),
        val visibleOnlyToAuthor: Boolean = false,
        val zhiPlusExtraInfo: String = "",
        val thumbnailInfo: ThumbnailInfo? = null,
        val preload: Boolean = false,
        val stickyInfo: String = "",
        val ipInfo: String? = null,
        val settings: Settings? = null,
        val attachedInfo: JsonElement? = null,
        val paidInfo: JsonObject? = null,
    ) : Content

    @Serializable
    data class Article(
        val id: Long,
        val adminClosedComment: Boolean = false,
        val author: Author,
        val canComment: CanComment,
        val collapseReason: String? = null,
        val collapsedBy: String? = null,
        val commentCount: Int = 0,
        val commentPermission: String? = null,
        val title: String,
        val content: String,
        val contentNeedTruncated: Boolean = false,
        val editableContent: String? = null,
        val excerpt: String,
        val extras: String? = null,
        val favlistsCount: Int = 0,
        val isCollapsed: Boolean = false,
        val isCopyable: Boolean = false,
        val isJumpNative: Boolean = false,
        val isLabeled: Boolean = false,
        val isNormal: Boolean = false,
        val isMine: Boolean = false,
        val isSticky: Boolean = false,
        val isVisible: Boolean = false,
        val reactionInstruction: ReactionInstruction? = null,
        val reaction: Reaction? = null,
        val relevantInfo: RelevantInfo? = null,
        val reshipmentSettings: String? = null,
        val rewardInfo: RewardInfo? = null,
        val suggestEdit: SuggestEdit? = null,
        val type: String,
        val created: Long,
        val updated: Long,
        val url: String,
        val voteupCount: Int,
        val bizExt: BizExt? = null,
        val contentMark: JsonObject? = null,
        val decorativeLabels: List<JsonElement> = emptyList(),
        val visibleOnlyToAuthor: Boolean = false,
        val zhiPlusExtraInfo: String = "",
        val thumbnailInfo: ThumbnailInfo? = null,
        val preload: Boolean = false,
        val stickyInfo: String = "",
        val ipInfo: String? = null,
        val settings: Settings? = null,
        val attachedInfo: JsonElement? = null,
        val paidInfo: JsonObject? = null,
    ) : Content

    @Serializable
    data class RelevantInfo(
        val isRelevant: Boolean,
        val relevantText: String,
        val relevantType: String,
    )

    @Serializable
    data class Question(
        val type: String,
        val id: Long,
        val title: String,
        val questionType: String,
        val created: Long,
        val updatedTime: Long,
        val url: String,
        val answerCount: Int,
        val visitCount: Int,
        val commentCount: Int,
        val followerCount: Int,
//        val collapsedAnswerCount: Int = ,
        val detail: String,
//        val editableDetail: String,
//        val status: Status,
        val relationship: QuestionRelationship,
        val topics: List<Topic>,
        val author: Author,
//        val canComment: CanComment,
//        val thumbnailInfo: ThumbnailInfo,
//        val reviewInfo: ReviewInfo,
//        val relatedCards: List<RelatedCard>,
//        val muteInfo: MuteInfo,
//        val showAuthor: Boolean,
//        val isLabeled: Boolean,
//        val isBannered: Boolean,
//        val showEncourageAuthor: Boolean,
        val voteupCount: Int,
//        val canVote: Boolean,
//        val reactionInstruction: ReactionInstruction,
//        val invisibleAuthor: Boolean = false,
    ) : Content

    @Serializable
    data class Status(
        val isLocked: Boolean,
        val isClose: Boolean,
        val isEvaluate: Boolean,
        val isSuggest: Boolean,
    )

    @Serializable
    data class QuestionRelationship(
        val isAuthor: Boolean = false,
        val isFollowing: Boolean = false,
        val isAnonymous: Boolean = false,
        val voting: Int = 0,
    )

    @Serializable
    data class Topic(
        val id: String,
        val type: String,
        val url: String,
        val name: String,
        val avatarUrl: String,
        val topicType: String,
    )

    @Serializable
    data class BadgeV2(
        val title: String,
        val mergedBadges: List<JsonElement>? = null,
        val detailBadges: List<JsonElement>? = null,
        val icon: String = "",
        val nightIcon: String = "",
        val canClick: Boolean = false,
    )

    @Serializable
    data class CanComment(
        val status: Boolean,
        val reason: String,
    )

    @Serializable
    sealed interface Thumbnail

    @Suppress("unused")
    @Serializable
    @SerialName("image")
    data class ThumbnailRich(
        val url: String,
        val token: String,
        val width: Int,
        val height: Int,
    ) : Thumbnail

    @Suppress("unused")
    @Serializable
    @SerialName("video")
    data class ThumbnailVideo(
        val attachedInfo: String,
        val url: String,
        val token: String,
        val width: Int,
        val height: Int,
    ) : Thumbnail

    @Suppress("unused")
    @Serializable
    @JvmInline
    value class ThumbnailString(
        val value: String,
    ) : Thumbnail

    @Serializable
    data class ThumbnailInfo(
        val count: Int,
        val type: String,
        val thumbnails: List<Thumbnail>,
    )

    @Serializable
    data class ReviewInfo(
        val type: String,
        val tips: String,
        val editTips: String,
        val isReviewing: Boolean,
        val editIsReviewing: Boolean,
    )

    @Serializable
    data class RelatedCard(
        val type: String,
    )

    @Serializable
    data class MuteInfo(
        val type: String,
    )

    @Serializable
    @Suppress("SpellCheckingInspection")
    data class ReactionInstruction(
        val isRelevant: Boolean = false,
        val relevantText: String = "",
        val relevantType: String = "",
        /**
         * `HIDE` = 隐藏"最新回答"
         */
        val rEACTIONANSWERNEWESTLIST: String? = null,
        /**
         * `HIDE` = 隐藏"类似回答"
         */
        val rEACTIONCONTENTSEGMENTLIKE: String? = null,
    )

    @Serializable
    data class Comment(
        val id: String,
        val type: String,
        val resourceType: String,
//         val memberId: Long,
        val url: String,
//        val hot: Boolean,
        val top: Boolean = false,
        val content: String,
//        val score: Int,
        val createdTime: Long,
        val isDelete: Boolean,
        val collapsed: Boolean,
        val reviewing: Boolean,
        val replyCommentId: String? = null,
        val replyRootCommentId: String? = null,
        var liked: Boolean = false,
        var likeCount: Int = 0,
        val disliked: Boolean = false,
        val dislikeCount: Int = 0,
        val isAuthor: Boolean = false,
//         val canLike: Boolean,
//         val canDislike: Boolean,
//         val canDelete: Boolean,
//         val canReply: Boolean,
//         val canHot: Boolean,
//         val canAuthorTop: Boolean,
        val isAuthorTop: Boolean = false,
        val canCollapse: Boolean = false,
        val canShare: Boolean = false,
        val canUnfold: Boolean = false,
        val canTruncate: Boolean = false,
        val canMore: Boolean = false,
        val author: Author,
        val replyToAuthor: Author? = null,
        val authorTag: List<JsonObject> = emptyList(),
        val replyAuthorTag: List<JsonElement> = emptyList(),
        val contentTag: List<JsonElement> = emptyList(),
        val commentTag: List<CommentTag> = emptyList(),
        val childCommentCount: Int = 0,
        val childCommentNextOffset: JsonElement? = null,
        val childComments: List<Comment> = listOf(),
        val isVisibleOnlyToMyself: Boolean = false,
        @SerialName("_")
        val underscore: JsonElement? = null,
    ) {
        @Serializable
        data class Author(
            val id: String,
            val urlToken: String,
            val name: String,
            val avatarUrl: String,
            val avatarUrlTemplate: String,
            val isOrg: Boolean,
            val type: String,
            val url: String,
            val userType: String,
            val headline: String,
            val gender: Int,
            val isAdvertiser: Boolean,
            val badgeV2: JsonElement? = null,
            val exposedMedal: JsonElement? = null,
            val vipInfo: JsonElement? = null,
            val levelInfo: JsonElement? = null,
            val kvipInfo: JsonElement? = null,
        )

        @Serializable
        data class CommentTag(
            val type: String,
            val text: String,
            val color: String,
            val nightColor: String,
            val hasBorder: Boolean,
        )
    }

    data class ReferenceCount<T>(
        val value: T,
        var count: Int = 0,
    ) : AutoCloseable {
        override fun close() {
            count--
        }
    }

    @Serializable
    data class People(
        val id: String,
        val urlToken: String? = null,
        val name: String,
        val useDefaultAvatar: Boolean = false,
        val avatarUrl: String,
        val avatarUrlTemplate: String = "",
        val isOrg: Boolean = false,
        val type: String = "people",
        val url: String,
        val userType: String = "people",
        val headline: String,
        val headlineRendered: String? = null,
        val gender: Int,
        val isAdvertiser: Boolean = false,
        val ipInfo: String? = null,
        val vipInfo: VipInfo? = null,
        val kvipInfo: JsonElement? = null,
        val allowMessage: Boolean = true,
        val isFollowing: Boolean = false,
        val isFollowed: Boolean = false,
        val isBlocking: Boolean = false,
        val followerCount: Int = 0,
        val followingCount: Int = 0,
        val answerCount: Int = 0,
        val articlesCount: Int = 0,
        val availableMedalsCount: Int = 0,
        val orgVerifyStatus: JsonElement? = null,
        val isRealname: Boolean = false,
        val hasApplyingColumn: Boolean = false,
    )

    @Serializable
    data class Collection(
        val id: String,
        val isFavorited: Boolean = false,
        val type: String = "collection",
        val title: String = "",
        val isPublic: Boolean = false,
        val url: String = "",
        val description: String = "",
        val followerCount: Int = 0,
        val answerCount: Int = 0,
        val itemCount: Int = 0,
        val likeCount: Int = 0,
        val viewCount: Int = 0,
        val commentCount: Int = 0,
        val isFollowing: Boolean = false,
        val isLiking: Boolean = false,
        val createdTime: Long = 0L,
        val updatedTime: Long = 0L,
        val creator: JsonElement? = null,
        val isDefault: Boolean = false,
    )

    @Serializable
    @SerialName("pin")
    data class Pin(
        val id: String,
        val url: String = "",
        val author: Author,
        val content: List<JsonElement> = emptyList(),
        val excerptTitle: String = "",
        val contentHtml: String = "",
        val likeCount: Int = 0,
        val commentCount: Int = 0,
        val created: Long = 0L,
        val updated: Long = 0L,
        val reaction: JsonElement? = null,
        val state: String = "",
        val reactionCount: Int = 0,
        val likers: List<Author> = emptyList(),
        val topics: List<Topic> = emptyList(),
        val pinType: String = "",
        val isContainAiContent: Boolean = false,
        val commentPermission: String = "",
        val viewPermission: String = "",
        val adminClosedComment: Boolean = false,
        val comments: List<JsonElement> = emptyList(),
        val favoriteCount: Int = 0,
        val favlistsCount: Int = 0,
        val isDeleted: Boolean = false,
        val isTop: Boolean = false,
        val canTop: Boolean = false,
        val repinCount: Int = 0,
        val sourcePinId: Long = 0L,
        val selfCreate: Boolean = false,
        val tags: List<JsonElement> = emptyList(),
        val virtuals: JsonObject? = null,
        val reactionRelation: JsonObject? = null,
        val topReactions: JsonObject? = null,
    ) : Content

    @Serializable
    data class Column(
        val id: String,
        val type: String = "column",
        val url: String = "",
        val title: String = "",
        val description: String = "",
        val intro: String = "",
        val avatarUrl: String = "",
        val articlesCount: Int = 0,
        val followerCount: Int = 0,
        val isFollowing: Boolean = false,
        val created: Long = 0L,
        val updated: Long = 0L,
        val author: JsonElement? = null,
    )
}
