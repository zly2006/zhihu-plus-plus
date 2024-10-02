package com.github.zly2006.zhihu.data

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
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
        val badge: List<JsonElement>,
        val badgeV2: BadgeV2,
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
        val medalName: String
    )

    @Serializable
    data class VipInfo(
        val isVip: Boolean,
        val vipIcon: VipIcon
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
        val adminClosedComment: Boolean,
        val annotationAction: String?,
        val answerType: String,
        val author: Author,
        val bizExt: BizExt,
        val canComment: CanComment,
        val collapseReason: String,
        val collapsedBy: String,
        val commentCount: Int,
        val commentPermission: String,
        val content: String,
        val contentNeedTruncated: Boolean,
        val createdTime: Long,
        val editableContent: String,
        val excerpt: String,
        val extras: String,
        val favlistsCount: Int,
        val id: Long,
        val ipInfo: String,
        val isCollapsed: Boolean,
        val isCopyable: Boolean,
        val isJumpNative: Boolean,
        val isLabeled: Boolean,
        val isNormal: Boolean,
        val isSticky: Boolean,
        val isVisible: Boolean,
        val question: AnswerModelQuestion,
        val reactionInstruction: ReactionInstruction,
        val relationship: Relationship,
        val relevantInfo: RelevantInfo,
        val reshipmentSettings: String,
        val rewardInfo: RewardInfo,
        val settings: Settings? = null,
        val suggestEdit: SuggestEdit,
        val thanksCount: Int,
        val type: String,
        val updatedTime: Long,
        val url: String,
        val voteupCount: Int
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
        val reactionInstruction: ReactionInstruction
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
        val mergedBadges: List<JsonElement>,
        val detailBadges: List<JsonElement>,
        val icon: String,
        val nightIcon: String
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
    data class ReactionInstruction(
        val isRelevant: Boolean = false,
        val relevantText: String = "",
        val relevantType: String = "",
        @Suppress("SpellCheckingInspection")
        /**
         * `HIDE` = 隐藏"最新回答"
         */
        val rEACTIONANSWERNEWESTLIST: String? = null
    )

    class ReferenceCount<T>(
        val value: T,
        var count: Int = 0
    ): AutoCloseable {
        override fun close() {
            count--
        }
    }

    private val questions = mutableMapOf<Long, ReferenceCount<Question>>()
    private val answers = mutableMapOf<Long, ReferenceCount<Answer>>()

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
                    e.printStackTrace()
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
                    e.printStackTrace()
                }
            }
        }
        println(jojo)
    }

    private fun removeAnswer(answerId: Long) {
        val answer = answers[answerId]!!
        val question = questions[answer.value.question.id]!!
        if (--question.count == 0) {
            questions.remove(question.value.id)
        }
        answers.remove(answerId)
    }

    suspend fun getAnswer(httpClient: HttpClient, id: Long): ReferenceCount<Answer>? {
        if (id !in answers) {
            get(httpClient, "https://www.zhihu.com/answer/$id")
        }
        return answers[id]?.also { it.count++ }
    }

    suspend fun getQuestion(httpClient: HttpClient, id: Long): ReferenceCount<Question>? {
        if (id !in questions) {
            get(httpClient, "https://www.zhihu.com/question/$id")
        }
        return questions[id]?.also { it.count++ }
    }
}

