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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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
