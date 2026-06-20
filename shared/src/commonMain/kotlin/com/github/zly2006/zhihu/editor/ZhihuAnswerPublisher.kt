/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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

package com.github.zly2006.zhihu.editor

import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.util.raiseForStatus
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import com.github.zly2006.zhihu.viewmodel.fetchContentDetail
import com.github.zly2006.zhihu.viewmodel.postSigned
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 将“发布/编辑知乎回答”封装为平台能力：
 * - Android：用 app 的登录态 Cookie + XSRF + zse96(v2) 签名请求知乎网页 API。
 * - Desktop：当前不实现（避免桌面端误触发导致困惑/风控），但保持编译通过。
 */
interface ZhihuAnswerPublisher {
    val isSupported: Boolean

    /**
     * 探测当前登录账号在该问题下是否已发布过回答。
     *
     * 返回值：
     * - null：未找到（视为“新回答”）
     * - answerId：找到（发布时会走“更新回答”的 payload）
     */
    suspend fun findMyAnswerId(questionId: Long): Long?

    /**
     * 获取“我已发布的回答”的可编辑内容，用于回填编辑框。
     *
     * 说明：
     * - zhihu_obsidian 的“更新回答”逻辑依赖 answerId；本 app 也用相同策略。
     * - 优先返回 editable_content（知乎用于编辑器的 HTML），否则退回 content。
     */
    suspend fun fetchAnswerForEditing(answerId: Long): ExistingAnswerForEditing?

    /**
     * 上传图片到知乎图床（对齐 zhihu_obsidian 的 image_service.ts）并返回：
     * - 编辑器所需的 `<img data-*>` 元数据
     * - 便于 Markdown 里引用的图片 URL（通常是 https://picx.zhimg.com/v2-xxx.png 这种）
     */
    suspend fun uploadImage(
        bytes: ByteArray,
        mimeType: String?,
        fileName: String?,
    ): UploadedZhihuImage

    /**
     * 写入“问题草稿”（对应 zhihu_obsidian 的 patchDraft）。
     *
     * 说明：
     * - 草稿接口和发布接口是两步（知乎网页版也是类似链路）。
     * - 对应端点：POST /api/v4/questions/{questionId}/draft
     */
    suspend fun patchDraft(
        questionId: Long,
        answerId: Long?,
        html: String,
        tocEnabled: Boolean,
    )

    /**
     * 将草稿发布为正式回答（或更新已有回答）。
     *
     * 返回值：发布成功后的 answerId
     *
     * 对应端点：POST /api/v4/content/publish
     */
    suspend fun publishAnswer(
        questionId: Long,
        answerId: Long?,
        html: String,
        tocEnabled: Boolean,
    ): Long
}

data class ExistingAnswerForEditing(
    val answerId: Long,
    val html: String,
    val tocEnabled: Boolean,
)

@Composable
expect fun rememberZhihuAnswerPublisher(): ZhihuAnswerPublisher

private const val QUESTION_RELATIONSHIP_INCLUDE = "relationship,relationship.my_answer"

internal class ZhihuApiAnswerPublisher(
    private val environment: ZhihuApiEnvironment,
) : ZhihuAnswerPublisher {
    override val isSupported: Boolean = true

    override suspend fun findMyAnswerId(questionId: Long): Long? {
        if (environment.authenticatedCookies()["d_c0"].isNullOrBlank()) return null

        val element = runCatching {
            environment.fetchJson("https://api.zhihu.com/questions/$questionId", QUESTION_RELATIONSHIP_INCLUDE)
        }.getOrNull() ?: return null

        val response = ZhihuJson.decodeJson(DataHolder.QuestionRelationshipApiResponse.serializer(), element)
        val myAnswer = response.relationship?.myAnswer ?: return null

        if (myAnswer.isDeleted == true) {
            return null
        }

        return myAnswer.answerId?.toLongOrNull()
    }

    override suspend fun fetchAnswerForEditing(answerId: Long): ExistingAnswerForEditing? {
        val destination = Article(type = ArticleType.Answer, id = answerId)
        val answer = environment.fetchContentDetail(destination) as? DataHolder.Answer ?: return null
        val html = answer.editableContent ?: answer.content
        val tocEnabled = answer.settings?.tableOfContents?.enabled ?: false

        return ExistingAnswerForEditing(
            answerId = answerId,
            html = html,
            tocEnabled = tocEnabled,
        )
    }

    override suspend fun uploadImage(
        bytes: ByteArray,
        mimeType: String?,
        fileName: String?,
    ): UploadedZhihuImage =
        uploadZhihuImage(environment, bytes, mimeType, fileName, ZhihuImageUploadSource.Article)

    override suspend fun patchDraft(
        questionId: Long,
        answerId: Long?,
        html: String,
        tocEnabled: Boolean,
    ) {
        val xsrf = environment.authenticatedCookies()["_xsrf"]
            ?: throw IllegalStateException("缺少 _xsrf Cookie，无法写入草稿；请先确保已登录。")

        val body = PatchDraftRequest(
            content = html,
            settings = PatchDraftSettings(
                tableOfContentsEnabled = tocEnabled,
            ),
        )

        environment
            .postSigned("https://www.zhihu.com/api/v4/questions/$questionId/draft") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Referrer, "https://www.zhihu.com/question/$questionId/answer/${answerId ?: ""}")
                header("x-xsrftoken", xsrf)
                setBody(body)
            }.raiseForStatus(dumpRequest = true)
    }

    override suspend fun publishAnswer(
        questionId: Long,
        answerId: Long?,
        html: String,
        tocEnabled: Boolean,
    ): Long {
        val xsrf = environment.authenticatedCookies()["_xsrf"]
            ?: throw IllegalStateException("缺少 _xsrf Cookie，无法发布回答；请先确保已登录。")

        val isPublished = answerId != null
        val requestBody = PublishAnswerRequest(
            data = PublishAnswerData(
                publish = PublishTrace(traceId = newPublishTraceId()),
                draft = PublishDraft(
                    isPublished = isPublished,
                    contentId = answerId?.toString(),
                ),
                extraInfo = PublishExtraInfo(
                    questionId = questionId.toString(),
                    pcBusinessParams = buildPcBusinessParams(tocEnabled),
                ),
                hybrid = PublishHybrid(
                    html = html,
                ),
                contentsTables = PublishContentsTables(
                    tableOfContentsEnabled = tocEnabled,
                ),
            ),
        )

        val responseElement = environment
            .postSigned("https://www.zhihu.com/api/v4/content/publish") {
                contentType(ContentType.Application.Json)
                header("x-xsrftoken", xsrf)
                setBody(requestBody)
            }.raiseForStatus(dumpRequest = true)
            .body<JsonElement>()

        val response = ZhihuJson.decodeJson(DataHolder.ContentPublishResponse.serializer(), responseElement)
        if (response.message == "success") {
            val resultText = response.data?.result
                ?: throw IllegalStateException("发布成功但返回缺少 data.result: $responseElement")

            return parsePublishContentId(resultText)
                ?: throw IllegalStateException("发布成功但无法解析 publish.id")
        }

        val code = response.code
        if (code == 103003) {
            throw IllegalStateException(response.message ?: "已回答过该问题，创建回答失败")
        }

        throw IllegalStateException(
            "发布失败: ${response.message ?: "unknown"}\n$responseElement",
        )
    }
}

internal object UnsupportedZhihuAnswerPublisher : ZhihuAnswerPublisher {
    override val isSupported: Boolean = false

    override suspend fun findMyAnswerId(questionId: Long): Long? = null

    override suspend fun fetchAnswerForEditing(answerId: Long): ExistingAnswerForEditing? = null

    override suspend fun uploadImage(
        bytes: ByteArray,
        mimeType: String?,
        fileName: String?,
    ): UploadedZhihuImage = throw UnsupportedOperationException("当前平台暂不支持上传图片")

    override suspend fun patchDraft(
        questionId: Long,
        answerId: Long?,
        html: String,
        tocEnabled: Boolean,
    ): Unit = throw UnsupportedOperationException("当前平台暂不支持发布/编辑知乎回答")

    override suspend fun publishAnswer(
        questionId: Long,
        answerId: Long?,
        html: String,
        tocEnabled: Boolean,
    ): Long = throw UnsupportedOperationException("当前平台暂不支持发布/编辑知乎回答")
}

@Serializable
data class PatchDraftRequest(
    val content: String,
    @SerialName("draft_type")
    val draftType: String = "normal",
    @SerialName("delta_time")
    val deltaTime: Int = 30,
    val settings: PatchDraftSettings,
)

@Serializable
data class PatchDraftSettings(
    @SerialName("reshipment_settings")
    val reshipmentSettings: String = "allowed",
    @SerialName("comment_permission")
    val commentPermission: String = "all",
    @SerialName("can_reward")
    val canReward: Boolean = false,
    val tagline: String = "",
    @SerialName("disclaimer_status")
    val disclaimerStatus: String = "close",
    @SerialName("disclaimer_type")
    val disclaimerType: String = "none",
    @SerialName("commercial_report_info")
    val commercialReportInfo: CommercialReportInfo = CommercialReportInfo(isReport = true),
    @SerialName("push_activity")
    val pushActivity: Boolean = false,
    @SerialName("table_of_contents_enabled")
    val tableOfContentsEnabled: Boolean,
    @SerialName("thank_inviter_status")
    val thankInviterStatus: String = "close",
    @SerialName("thank_inviter")
    val thankInviter: String = "",
)

@Serializable
data class CommercialReportInfo(
    @SerialName("is_report")
    val isReport: Boolean = true,
)

@Serializable
data class PublishAnswerRequest(
    val action: String = "answer",
    val data: PublishAnswerData,
)

@Serializable
data class PublishAnswerData(
    val publish: PublishTrace,
    val hybridInfo: JsonObject = buildJsonObject { },
    val draft: PublishDraft,
    @SerialName("extra_info")
    val extraInfo: PublishExtraInfo,
    val hybrid: PublishHybrid,
    val reprint: PublishReprint = PublishReprint(),
    val commentsPermission: PublishCommentsPermission = PublishCommentsPermission(),
    val appreciate: PublishAppreciate = PublishAppreciate(),
    val publishSwitch: PublishSwitch = PublishSwitch(),
    val creationStatement: PublishCreationStatement = PublishCreationStatement(),
    val commercialReportInfo: PublishCommercialReportInfo = PublishCommercialReportInfo(),
    val toFollower: JsonObject = buildJsonObject { },
    val contentsTables: PublishContentsTables,
    val thanksInvitation: PublishThanksInvitation = PublishThanksInvitation(),
)

@Serializable
data class PublishDraft(
    val disabled: Int = 1,
    val isPublished: Boolean,
    val contentId: String? = null,
)

@Serializable
data class PublishExtraInfo(
    @SerialName("question_id")
    val questionId: String,
    val publisher: String = "pc",
    val include: String = DEFAULT_PUBLISH_INCLUDE,
    @SerialName("pc_business_params")
    val pcBusinessParams: String,
)

@Serializable
data class PublishHybrid(
    val html: String,
)

@Serializable
data class PublishReprint(
    @SerialName("reshipment_settings")
    val reshipmentSettings: String = "allowed",
)

@Serializable
data class PublishAppreciate(
    @SerialName("can_reward")
    val canReward: Boolean = false,
)

@Serializable
data class PublishSwitch(
    @SerialName("draft_type")
    val draftType: String = "normal",
)

@Serializable
data class PublishCreationStatement(
    @SerialName("disclaimer_status")
    val disclaimerStatus: String = "close",
    @SerialName("disclaimer_type")
    val disclaimerType: String = "none",
)

@Serializable
data class PublishCommercialReportInfo(
    val isReport: Int = 0,
)

@Serializable
data class PublishContentsTables(
    @SerialName("table_of_contents_enabled")
    val tableOfContentsEnabled: Boolean,
)

@Serializable
data class PublishThanksInvitation(
    @SerialName("thank_inviter_status")
    val thankInviterStatus: String = "close",
    @SerialName("thank_inviter")
    val thankInviter: String = "",
)

/**
 * zhihu_obsidian 里 include 是一段很长的 fields 列表；复制过来尽量保持一致，
 * 以减少服务端字段缺失导致的返回差异。
 */
private const val DEFAULT_PUBLISH_INCLUDE: String =
    "is_visible,paid_info,paid_info_content,has_column,admin_closed_comment,reward_info,annotation_action,annotation_detail,collapse_reason,is_normal,is_sticky,collapsed_by,suggest_edit,comment_count,thanks_count,favlists_count,can_comment,content,editable_content,voteup_count,reshipment_settings,comment_permission,created_time,updated_time,review_info,relevant_info,question,excerpt,attachment,content_source,is_labeled,endorsements,reaction_instruction,ip_info,relationship.is_authorized,voting,is_thanked,is_author,is_nothelp,is_favorited;author.vip_info,kvip_info,badge[*].topics;settings.table_of_contents.enabled"

/**
 * pc_business_params 在 publish 接口里是字符串化 JSON（zhihu_obsidian 也是这样做的）。
 */
fun buildPcBusinessParams(tocEnabled: Boolean): String = buildJsonObject {
    put("reshipment_settings", "allowed")
    put("comment_permission", "all")
    put("reward_setting", buildJsonObject { put("can_reward", false) })
    put("disclaimer_status", "close")
    put("disclaimer_type", "none")
    put("commercial_report_info", buildJsonObject { put("is_report", false) })
    put("commercial_zhitask_bind_info", null)
    put("is_report", false)
    put("table_of_contents_enabled", tocEnabled)
    put("thank_inviter_status", "close")
    put("thank_inviter", "")
}.toString()
