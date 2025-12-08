package com.github.zly2006.zhihu.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationItem(
    val id: String,
    val type: String,
    @SerialName("attach_info")
    val attachInfo: String? = null,
    @SerialName("is_read")
    val isRead: Boolean = false,
    @SerialName("create_time")
    val createTime: Long = 0,
    @SerialName("merge_count")
    val mergeCount: Int = 1,
    val content: NotificationContent,
    val target: NotificationTarget? = null,
)

@Serializable
data class NotificationContent(
    val verb: String,
    val actors: List<NotificationActor> = emptyList(),
    val target: NotificationLink? = null,
    val extend: NotificationExtend? = null,
)

@Serializable
data class NotificationActor(
    val name: String,
    val type: String,
    val link: String,
    @SerialName("url_token")
    val urlToken: String? = null,
)

@Serializable
data class NotificationLink(
    val text: String,
    val link: String,
)

@Serializable
data class NotificationExtend(
    val text: String,
    val icon: String? = null,
)

@Serializable
data class NotificationTarget(
    val id: String? = null,
    val type: String? = null,
    val url: String? = null,
    val title: String? = null,
    val content: String? = null,
    @SerialName("created_time")
    val createdTime: Long? = null,
    @SerialName("updated_time")
    val updatedTime: Long? = null,
    val question: NotificationQuestion? = null,
    val author: NotificationAuthor? = null,
)

@Serializable
data class NotificationQuestion(
    val id: String,
    val type: String,
    val url: String,
    val title: String,
    @SerialName("question_type")
    val questionType: String? = null,
    val created: Long? = null,
    @SerialName("updated_time")
    val updatedTime: Long? = null,
)

@Serializable
data class NotificationAuthor(
    val name: String,
    val type: String,
    val url: String,
    val id: String,
    @SerialName("url_token")
    val urlToken: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("avatar_url_template")
    val avatarUrlTemplate: String? = null,
    val headline: String? = null,
    val gender: Int = -1,
    @SerialName("user_type")
    val userType: String? = null,
    @SerialName("is_org")
    val isOrg: Boolean = false,
    @SerialName("is_advertiser")
    val isAdvertiser: Boolean = false,
    val badge: List<String> = emptyList(),
    @SerialName("vip_info")
    val vipInfo: VipInfo? = null,
    val member: NotificationMember? = null,
    val role: String? = null,
)

@Serializable
data class NotificationMember(
    val name: String,
    val type: String,
    val url: String,
    val id: String,
    @SerialName("url_token")
    val urlToken: String = "",
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("avatar_url_template")
    val avatarUrlTemplate: String? = null,
    val headline: String? = null,
    val gender: Int = -1,
    @SerialName("user_type")
    val userType: String? = null,
    @SerialName("is_org")
    val isOrg: Boolean = false,
    @SerialName("is_advertiser")
    val isAdvertiser: Boolean = false,
    val badge: List<String> = emptyList(),
    @SerialName("vip_info")
    val vipInfo: VipInfo? = null,
)

@Serializable
data class VipInfo(
    @SerialName("is_vip")
    val isVip: Boolean = false,
)
