package com.github.zly2006.zhihu.data

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.github.zly2006.zhihu.LoginActivity
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import org.jsoup.Jsoup
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Document
import java.net.UnknownHostException

object DataHolder {
    val definitelyAd = listOf(
        "href=\"https://xg.zhihu.com/", // 知乎营销效果统计平台
    )

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
        val isAuthor: Boolean = false,
        val isAuthorized: Boolean = false,
        val isNothelp: Boolean = false,
        val isFavorited: Boolean = false,
        val isThanked: Boolean = false,
        val upvotedFollowees: List<String> = emptyList(),
        // 1 - 点赞， -1 - 点踩
        val voting: Int = 0,
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
        val id: Long,
        val isCollapsed: Boolean = false,
        val isCopyable: Boolean = false,
        val isJumpNative: Boolean = false,
        val isLabeled: Boolean = false,
        val isNormal: Boolean = false,
        val isMine: Boolean = false,
        val isSticky: Boolean = false,
        val isVisible: Boolean = false,
        val reactionInstruction: ReactionInstruction? = null,
        val relationship: Relationship? = null,
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
        val isAuthor: Boolean,
        val isFollowing: Boolean,
        val isAnonymous: Boolean,
        val canLock: Boolean,
        val canStickAnswers: Boolean,
        val canCollapseAnswers: Boolean,
        val voting: Int,
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

    private val questions = mutableMapOf<Long, ReferenceCount<Question>>()
    private val answers = mutableMapOf<Long, ReferenceCount<Answer>>()
    private val articles = mutableMapOf<Long, ReferenceCount<Article>>()
    private val feeds = mutableMapOf<String, CommonFeed>()

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun get(httpClient: HttpClient, url: String, activity: Context, retry: Int = 1) {
        val response = httpClient.get(url)
        if ("https://www.zhihu.com/404" == response.call.request.url
                .toString() &&
            response.status.value == 200
        ) {
            activity.mainExecutor.execute {
                Toast.makeText(activity, "疑似触发风控", Toast.LENGTH_SHORT).show()
            }
        }
        val html = response.bodyAsText()
        val document = Jsoup.parse(html)
        if (document.getElementById("js-initialData") == null || extractData(document) == 0) { // 触发风控
            // 知乎安全cookie v4检验
            val job = Job()
            activity.mainExecutor.execute {
                val webView = WebView(activity)
                val cm = CookieManager.getInstance()
                cm.removeAllCookies { }
                webView.settings.javaScriptEnabled = true
                val cookies = AccountData.data.cookies
                cookies.forEach { (key, value) ->
                    cm.setCookie("https://www.zhihu.com", "$key=$value")
                }
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url1: String) {
                        if (url == url1) {
                            val cookies =
                                CookieManager.getInstance().getCookie("https://www.zhihu.com/").split(";").associate {
                                    it.substringBefore("=").trim() to it.substringAfter("=")
                                }
                            AccountData.data.cookies.putAll(cookies)
                            AccountData.saveData(activity, AccountData.data)
                            view.evaluateJavascript("document.getElementsByTagName('html')[0].outerHTML") {
                                val document = Jsoup.parse(it)
                                Log.i("DataHolder", "Fetched data from $url:\n$document")
                                job.complete()
                            }
                        }
                    }
                }
                webView.loadUrl(url)
            }
            try {
                job.join()
                if (retry > 0) {
                    get(httpClient, url, activity, retry - 1)
                } else {
                    error("")
                }
                return
            } catch (_: Exception) {
                activity.mainExecutor.execute {
                    AlertDialog
                        .Builder(activity)
                        .setTitle("触发风控")
                        .setMessage("请稍后尝试重新进入此页，或重新登录")
                        .setPositiveButton("重新登录") { _, _ ->
                            AccountData.delete(activity)
                            val myIntent = Intent(activity, LoginActivity::class.java)
                            activity.startActivity(myIntent)
                        }.show()
                }
                return
            }
        }
        extractData(document)
    }

    /**
     * 从网页中提取数据
     * @return 提取的数据数量，如果为0则表示 404，或者触发了风控
     */
    private fun extractData(document: Document): Int {
        var extractedCount = 0
        val jojo = Json.decodeFromString<JsonObject>(
            (document.getElementById("js-initialData")?.childNode(0) as? DataNode)?.wholeData ?: "{}",
        )
        val entities = jojo["initialState"]!!.jsonObject["entities"]!!.jsonObject
        if ("questions" in entities) {
            val questions = entities["questions"]!!.jsonObject
            for ((_, question) in questions) {
                try {
                    val questionModel = Json.decodeFromJsonElement<Question>(question)
                    this.questions[questionModel.id] = ReferenceCount(questionModel)
                    extractedCount++
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
                    val answerModel = AccountData.decodeJson<Answer>(answer)
                    this.answers[answerModel.id] = ReferenceCount(answerModel)
                    this.questions[answerModel.question.id]!!.count++
                    extractedCount++
                } catch (e: Exception) {
                    println(jojo.toString())
                    e.printStackTrace()
                }
            }
        }
        if ("articles" in entities) {
            val articles = entities["articles"]!!.jsonObject
            for ((_, article) in articles) {
                try {
                    val articleModel = AccountData.decodeJson<Article>(article)
                    this.articles[articleModel.id] = ReferenceCount(articleModel)
                    extractedCount++
                } catch (e: Exception) {
                    println(jojo.toString())
                    e.printStackTrace()
                }
            }
        }
        return extractedCount
    }

    private fun Person?.mock() = Author(
        avatarUrl = this?.avatarUrl ?: "",
        avatarUrlTemplate = "",
        gender = this?.gender ?: 0,
        headline = this?.headline ?: "",
        id = this?.id ?: "",
        isAdvertiser = false,
        isOrg = false,
        name = this?.name ?: "",
        type = "mock",
        url = this?.url ?: "",
        urlToken = this?.urlToken ?: "",
        userType = this?.userType ?: "",
    )

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
    data class Pin(
        val id: String,
        val type: String = "pin",
        val url: String = "",
        val author: Author,
        val content: List<JsonElement> = emptyList(),
        val excerpt: String = "",
        val excerptTitle: String = "",
        val contentHtml: String = "",
        val likeCount: Int = 0,
        val commentCount: Int = 0,
        val created: Long = 0L,
        val updated: Long = 0L,
        val isLiking: Boolean = false,
        val reaction: JsonElement? = null,
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

    @OptIn(DelicateCoroutinesApi::class)
    fun getAnswerCallback(activity: Context, httpClient: HttpClient, id: Long, callback: (Answer?) -> Unit) {
        GlobalScope.launch {
            try {
                if ("answer/$id" in feeds && id !in answers) {
                    val feed = feeds["answer/$id"]!!.target as Feed.AnswerTarget
                    callback(
                        Answer(
                            adminClosedComment = false,
                            answerType = "feed mock data",
                            author = feed.author.mock(),
                            canComment = CanComment(
                                status = false,
                                reason = "",
                            ),
                            commentCount = feed.commentCount,
                            content = feed.content,
                            createdTime = feed.createdTime,
                            excerpt = feed.excerpt ?: "",
                            favlistsCount = feed.favoriteCount,
                            id = feed.id,
                            question = AnswerModelQuestion(
                                created = 0,
                                id = feed.question.id,
                                questionType = "",
                                relationship = Relationship(),
                                title = feed.question.title,
                                type = "",
                                updatedTime = 0,
                                url = feed.question.url,
                            ),
                            reshipmentSettings = "",
                            thanksCount = feed.thanksCount,
                            type = "answer",
                            updatedTime = feed.updatedTime,
                            url = feed.url,
                            voteupCount = feed.voteupCount,
                        ),
                    )
                } else {
                    answers[id]?.also { it.count++ }?.value?.let(callback)
                }
                get(httpClient, "https://www.zhihu.com/answer/$id", activity)
                callback(answers[id]?.also { it.count++ }?.value)
            } catch (e: UnknownHostException) {
                Log.e("DataHolder", "Failed to get answer $id", e)
                activity.mainExecutor.execute {
                    Toast.makeText(activity, "请检查网络连接", Toast.LENGTH_LONG).show()
                }
                callback(null)
            } catch (e: Exception) {
                Log.e("DataHolder", "Failed to get answer $id", e)
                activity.mainExecutor.execute {
                    Toast.makeText(activity, "Failed to get answer $id", Toast.LENGTH_LONG).show()
                }
                callback(null)
            }
        }
    }

    suspend fun getQuestion(activity: Context, httpClient: HttpClient, id: Long): ReferenceCount<Question>? {
        try {
            get(httpClient, "https://www.zhihu.com/question/$id", activity)
            return questions[id]?.also { it.count++ }
        } catch (e: Exception) {
            Log.e("DataHolder", "Failed to get question $id", e)
            activity.mainExecutor.execute {
                Toast.makeText(activity, "Failed to get question $id", Toast.LENGTH_LONG).show()
            }
            return null
        }
    }

    fun putFeed(feed: Feed) {
        if (feed is CommonFeed) {
            if (feed.target is Feed.AnswerTarget) {
                feeds["answer/${feed.target.id}"] = feed
            } else if (feed.target is Feed.ArticleTarget) {
                feeds["article/${feed.target.id}"] = feed
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun getArticleCallback(activity: Context, httpClient: HttpClient, id: Long, callback: (Article?) -> Unit) {
        GlobalScope.launch {
            try {
                if ("article/$id" in feeds && id !in articles) {
                    val feed = feeds["article/$id"]!!.target as Feed.ArticleTarget
                    callback(
                        Article(
                            adminClosedComment = false,
                            author = feed.author.mock(),
                            content = feed.content,
                            contentNeedTruncated = false,
                            excerpt = feed.excerpt,
                            title = feed.title,
                            commentCount = feed.commentCount,
                            id = feed.id,
                            canComment = CanComment(
                                status = false,
                                reason = "",
                            ),
                            type = "article",
                            created = feed.created,
                            updated = feed.updated,
                            voteupCount = feed.voteupCount,
                            url = feed.url,
                        ),
                    )
                } else {
                    articles[id]?.also { it.count++ }?.value?.let(callback)
                }
                get(httpClient, "https://zhuanlan.zhihu.com/p/$id", activity)
                callback(articles[id]?.also { it.count++ }?.value)
            } catch (e: Exception) {
                Log.e("DataHolder", "Failed to get article $id", e)
                activity.mainExecutor.execute {
                    Toast.makeText(activity, "Failed to get article $id", Toast.LENGTH_LONG).show()
                }
                callback(null)
            }
        }
    }
}
