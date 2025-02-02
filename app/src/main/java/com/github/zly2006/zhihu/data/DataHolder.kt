package com.github.zly2006.zhihu.data

import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.jsoup.Jsoup
import org.jsoup.nodes.DataNode

object DataHolder {
    val definitelyAd = listOf(
        "href=\"https://xg.zhihu.com/", // 知乎营销效果统计平台
    )

    @Serializable
    data class Author(
        val avatarUrl: String,
        val avatarUrlTemplate: String,
        val gender: Int,
        val headline: String,
        val id: String,
        val isAdvertiser: Boolean,
        val isOrg: Boolean,
        val isPrivacy: Boolean,
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
        val medalAvatarFrame: String = "",
        val miniAvatarUrl: String? = null,
    )

    @Serializable
    data class VipInfo(
        val isVip: Boolean,
        val vipIcon: VipIcon? = null
    )

    @Serializable
    data class VipIcon(
        val nightModeUrl: String,
        val url: String
    )

    @Serializable
    data class BizExt(
        val shareGuide: ShareGuide
    )

    @Serializable
    data class ShareGuide(
        val hasPositiveBubble: Boolean,
        val hasTimeBubble: Boolean,
        val hitShareGuideCluster: Boolean
    )

    @Serializable
    data class AnswerModelQuestion(
        val created: Long,
        val id: Long,
        val questionType: String,
        val relationship: Relationship,
        val title: String,
        val type: String,
        val updatedTime: Long,
        val url: String
    )

    @Serializable
    data class Relationship(
        val isAuthor: Boolean = false,
        val isAuthorized: Boolean = false,
        val isNothelp: Boolean = false,
        val isFavorited: Boolean = false,
        val isThanked: Boolean = false,
        val upvotedFollowees: List<String> = emptyList(),
        val voting: Int = 0
    )

    @Serializable
    data class RewardInfo(
        val canOpenReward: Boolean,
        val isRewardable: Boolean,
        val rewardMemberCount: Int,
        val rewardTotalMoney: Int,
        val tagline: String
    )

    @Serializable
    data class Settings(
        val tableOfContents: TableOfContents
    )

    @Serializable
    data class TableOfContents(
        val enabled: Boolean
    )

    @Serializable
    data class SuggestEdit(
        val reason: String,
        val status: Boolean,
        val tip: String,
        val title: String,
        val unnormalDetails: UnnormalDetails,
        val url: String
    )

    @Serializable
    data class UnnormalDetails(
        val description: String,
        val note: String,
        val reason: String,
        val reasonId: Int,
        val status: String
    )

    @Serializable
    data class Answer(
        val adminClosedComment: Boolean = false,
        val annotationAction: String? = null,
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
        val attachedInfo: JsonElement? = null
    )

    @Serializable
    data class RelevantInfo(
        val isRelevant: Boolean,
        val relevantText: String,
        val relevantType: String
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
        val isMuted: Boolean,
        val isVisible: Boolean,
        val isNormal: Boolean,
        val isEditable: Boolean,
        val adminClosedComment: Boolean,
        val hasPublishingDraft: Boolean,
        val answerCount: Int,
        val visitCount: Int,
        val commentCount: Int,
        val followerCount: Int,
        val collapsedAnswerCount: Int,
        val excerpt: String,
        val commentPermission: String,
        val detail: String,
        val editableDetail: String,
        val status: Status,
        val relationship: QuestionRelationship,
        val topics: List<Topic>,
        val author: Author,
        val canComment: CanComment,
        val thumbnailInfo: ThumbnailInfo,
        val reviewInfo: ReviewInfo,
        val relatedCards: List<RelatedCard>,
        val muteInfo: MuteInfo,
        val showAuthor: Boolean,
        val isLabeled: Boolean,
        val isBannered: Boolean,
        val showEncourageAuthor: Boolean,
        val voteupCount: Int,
        val canVote: Boolean,
        val reactionInstruction: ReactionInstruction,
        val invisibleAuthor: Boolean = false,
    )

    @Serializable
    data class Status(
        val isLocked: Boolean,
        val isClose: Boolean,
        val isEvaluate: Boolean,
        val isSuggest: Boolean
    )

    @Serializable
    data class QuestionRelationship(
        val isAuthor: Boolean,
        val isFollowing: Boolean,
        val isAnonymous: Boolean,
        val canLock: Boolean,
        val canStickAnswers: Boolean,
        val canCollapseAnswers: Boolean,
        val voting: Int
    )

    @Serializable
    data class Topic(
        val id: String,
        val type: String,
        val url: String,
        val name: String,
        val avatarUrl: String,
        val topicType: String
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
        val reason: String
    )

    @Serializable
    sealed interface Thumbnail

    @Serializable
    @SerialName("image")
    data class ThumbnailRich(
        val url: String,
        val token: String,
        val width: Int,
        val height: Int
    ) : Thumbnail

    @Serializable
    @JvmInline
    value class ThumbnailString(val value: String) : Thumbnail

    @Serializable
    data class ThumbnailInfo(
        val count: Int,
        val type: String,
        val thumbnails: List<Thumbnail>
    )

    @Serializable
    data class ReviewInfo(
        val type: String,
        val tips: String,
        val editTips: String,
        val isReviewing: Boolean,
        val editIsReviewing: Boolean
    )

    @Serializable
    data class RelatedCard(
        val type: String
    )

    @Serializable
    data class MuteInfo(
        val type: String
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
        @SerialName("resource_type") val resourceType: String,
//        @SerialName("member_id") val memberId: Long,
        val url: String,
//        val hot: Boolean,
        val top: Boolean,
        val content: String,
//        val score: Int,
        @SerialName("created_time") val createdTime: Long,
        @SerialName("is_delete") val isDelete: Boolean,
        val collapsed: Boolean,
        val reviewing: Boolean,
        @SerialName("reply_comment_id") val replyCommentId: String? = null,
        @SerialName("reply_root_comment_id") val replyRootCommentId: String? = null,
        val liked: Boolean = false,
        @SerialName("like_count") val likeCount: Int = 0,
        val disliked: Boolean = false,
        @SerialName("dislike_count") val dislikeCount: Int = 0,
        @SerialName("is_author") val isAuthor: Boolean,
//        @SerialName("can_like") val canLike: Boolean,
//        @SerialName("can_dislike") val canDislike: Boolean,
//        @SerialName("can_delete") val canDelete: Boolean,
//        @SerialName("can_reply") val canReply: Boolean,
//        @SerialName("can_hot") val canHot: Boolean,
//        @SerialName("can_author_top") val canAuthorTop: Boolean,
        @SerialName("is_author_top") val isAuthorTop: Boolean = false,
        @SerialName("can_collapse") val canCollapse: Boolean,
//        @SerialName("can_share") val canShare: Boolean,
//        @SerialName("can_unfold") val canUnfold: Boolean,
//        @SerialName("can_truncate") val canTruncate: Boolean,
//        @SerialName("can_more") val canMore: Boolean,
        val author: AuthorData,
        @SerialName("author_tag") val authorTag: List<JsonElement> = emptyList(),
        @SerialName("reply_author_tag") val replyAuthorTag: List<JsonElement> = emptyList(),
        @SerialName("content_tag") val contentTag: List<JsonElement> = emptyList(),
        @SerialName("comment_tag") val commentTag: List<CommentTag> = emptyList(),
        @SerialName("child_comment_count") val childCommentCount: Int,
        @SerialName("child_comment_next_offset") val childCommentNextOffset: JsonElement? = null,
        @SerialName("child_comments") val childComments: List<ChildComment>,
        @SerialName("is_visible_only_to_myself") val isVisibleOnlyToMyself: Boolean = false,
        @SerialName("_") val underscore: JsonElement? = null
    ) {

        @Serializable
        data class Author(
            val id: String,
            @SerialName("url_token") val urlToken: String,
            val name: String,
            @SerialName("avatar_url") val avatarUrl: String,
            @SerialName("avatar_url_template") val avatarUrlTemplate: String,
            @SerialName("is_org") val isOrg: Boolean,
            val type: String,
            val url: String,
            @SerialName("user_type") val userType: String,
            val headline: String,
            val gender: Int,
            @SerialName("is_advertiser") val isAdvertiser: Boolean,
            @SerialName("badge_v2") val badgeV2: JsonElement? = null,
            @SerialName("exposed_medal") val exposedMedal: JsonElement? = null,
            @SerialName("vip_info") val vipInfo: JsonElement? = null,
            @SerialName("level_info") val levelInfo: JsonElement? = null,
            @SerialName("kvip_info") val kvipInfo: JsonElement? = null,
        )

        @Serializable
        data class AuthorData(
            val role: String,
            val member: Author
        )

        @Serializable
        data class CommentTag(
            val type: String,
            val text: String,
            val color: String,
            @SerialName("night_color") val nightColor: String,
            @SerialName("has_border") val hasBorder: Boolean
        )
    }

    @Serializable
    data class CommentTag(
        val type: String,
        val text: String,
        val color: String,
        @SerialName("night_color") val nightColor: String,
        @SerialName("has_border") val hasBorder: Boolean
    )

    @Serializable
    data class ChildComment(
        val id: String,
        val type: String,
        @SerialName("resource_type") val resourceType: String,
//        @SerialName("member_id") val memberId: Long,
        val url: String,
        val content: String,
        @SerialName("created_time") val createdTime: Long,
        @SerialName("is_delete") val isDelete: Boolean,
        val collapsed: Boolean,
        val reviewing: Boolean,
        @SerialName("reply_comment_id") val replyCommentId: String? = null,
        @SerialName("reply_root_comment_id") val replyRootCommentId: String? = null,
        val liked: Boolean = false,
        @SerialName("like_count") val likeCount: Int = 0,
        val disliked: Boolean = false,
        @SerialName("dislike_count") val dislikeCount: Int = 0,
        val author: Comment.AuthorData,
        @SerialName("author_tag") val authorTag: List<JsonElement> = emptyList(),
        @SerialName("reply_author_tag") val replyAuthorTag: List<JsonElement> = emptyList(),
        @SerialName("content_tag") val contentTag: List<JsonElement> = emptyList(),
        @SerialName("comment_tag") val commentTag: List<CommentTag> = emptyList()
    )

    data class ReferenceCount<T>(
        val value: T,
        var count: Int = 0
    ) : AutoCloseable {
        override fun close() {
            count--
        }
    }

    private val questions = mutableMapOf<Long, ReferenceCount<Question>>()
    private val answers = mutableMapOf<Long, ReferenceCount<Answer>>()
    private val feeds = mutableMapOf<String, Feed>()

    private suspend fun get(httpClient: HttpClient, url: String) {
        val html = httpClient.get(url).bodyAsText()
        val document = Jsoup.parse(html)
        val jojo = Json.decodeFromString<JsonObject>(
            (document.getElementById("js-initialData")?.childNode(0) as? DataNode)?.wholeData ?: "{}"
        )
        val entities = jojo["initialState"]!!.jsonObject["entities"]!!.jsonObject
        if ("questions" in entities) {
            val questions = entities["questions"]!!.jsonObject
            for ((_, question) in questions) {
                try {
                    val questionModel = Json.decodeFromJsonElement<Question>(question)
                    this.questions[questionModel.id] = ReferenceCount(questionModel)
                } catch (e: Exception) {
                    println(jojo.toString())
                    e.printStackTrace()

                    val questionModel = AccountData.decodeJson<Question>(question)
                    this.questions[questionModel.id] = ReferenceCount(questionModel)
                }
            }
        }
        if ("answers" in entities) {
            val answers = entities["answers"]!!.jsonObject
            for ((_, answer) in answers) {
                try {
                    val answerModel = Json.decodeFromJsonElement<Answer>(answer)
                    this.answers[answerModel.id] = ReferenceCount(answerModel)
                    this.questions[answerModel.question.id]!!.count++
                } catch (e: Exception) {
                    println(jojo.toString())
                    e.printStackTrace()

                    val answerModel = AccountData.decodeJson<Answer>(answer)
                    this.answers[answerModel.id] = ReferenceCount(answerModel)
                    this.questions[answerModel.question.id]!!.count++
                }
            }
        }
    }

    private fun removeAnswer(answerId: Long) {
        val answer = answers[answerId]!!
        val question = questions[answer.value.question.id]!!
        if (--question.count == 0) {
            questions.remove(question.value.id)
        }
        answers.remove(answerId)
    }

    suspend fun getAnswer(activity: FragmentActivity, httpClient: HttpClient, id: Long): ReferenceCount<Answer>? {
        try {
            if (id !in answers) {
                get(httpClient, "https://www.zhihu.com/answer/$id")
            }
            return answers[id]?.also { it.count++ }
        } catch (e: Exception) {
            Log.e("DataHolder", "Failed to get answer $id", e)
            Toast.makeText(activity, "Failed to get answer $id", Toast.LENGTH_LONG).show()
            return null
        }
    }

    fun getAnswerCallback(activity: FragmentActivity, httpClient: HttpClient, id: Long, callback: (Answer?) -> Unit) {
        GlobalScope.launch {
            try {
                if ("answer/$id" !in feeds) {
                    val feed = feeds["answer/$id"]!!.target
                    callback(
                        Answer(
                            adminClosedComment = false,
                            annotationAction = null,
                            answerType = feed.answer_type ?: "",
                            author = Author(
                                avatarUrl = feed.author.avatar_url,
                                avatarUrlTemplate = "",
                                gender = feed.author.gender,
                                headline = feed.author.headline,
                                id = feed.author.id,
                                isAdvertiser = false,
                                isOrg = false,
                                isPrivacy = false,
                                name = feed.author.name,
                                type = "mock",
                                url = feed.author.url,
                                urlToken = feed.author.url_token,
                                userType = feed.author.user_type
                            ),
                            canComment = CanComment(
                                status = false,
                                reason = ""
                            ),
                            commentCount = feed.comment_count,
                            content = feed.content,
                            createdTime = feed.created_time,
                            excerpt = feed.excerpt,
                            favlistsCount = feed.favorite_count,
                            id = feed.id,
                            question = AnswerModelQuestion(
                                created = 0,
                                id = feed.question?.id ?: 0,
                                questionType = "",
                                relationship = Relationship(),
                                title = feed.question?.title ?: "",
                                type = "",
                                updatedTime = 0,
                                url = feed.question?.url ?: ""
                            ),
                            reshipmentSettings = "",
                            thanksCount = feed.thanks_count,
                            type = feed.type,
                            updatedTime = feed.updated_time,
                            url = feed.url,
                            voteupCount = feed.voteup_count,
                        )
                    )
                }
                if (id !in answers) {
                    get(httpClient, "https://www.zhihu.com/answer/$id")
                }
                callback(answers[id]?.also { it.count++ }?.value)
            } catch (e: Exception) {
                Log.e("DataHolder", "Failed to get answer $id", e)
                Toast.makeText(activity, "Failed to get answer $id", Toast.LENGTH_LONG).show()
                callback(null)
            }
        }
    }

    suspend fun getQuestion(activity: FragmentActivity, httpClient: HttpClient, id: Long): ReferenceCount<Question>? {
        try {
            if (id !in questions) {
                get(httpClient, "https://www.zhihu.com/question/$id")
            }
            return questions[id]?.also { it.count++ }
        } catch (e: Exception) {
            Log.e("DataHolder", "Failed to get question $id", e)
            Toast.makeText(activity, "Failed to get question $id", Toast.LENGTH_LONG).show()
            return null
        }
    }

    fun getAnswersFor(questionId: Long): List<ReferenceCount<Answer>> {
        return answers.filter { it.value.value.question.id == questionId }.values.toList()
    }

    fun putFeed(feed: Feed) {
        if (feed.target.type == "answer") {
            feeds["answer/${feed.target.id}"] = feed
        }
    }
}
