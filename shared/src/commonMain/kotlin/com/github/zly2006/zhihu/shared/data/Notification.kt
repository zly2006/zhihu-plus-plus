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

package com.github.zly2006.zhihu.shared.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class TrySerializer<T : Any>(
    val serializer: KSerializer<T>,
) : KSerializer<T?> {
    override val descriptor: SerialDescriptor
        get() = serializer.nullable.descriptor

    override fun serialize(encoder: Encoder, value: T?) = Unit

    override fun deserialize(decoder: Decoder): T? = try {
        serializer.deserialize(decoder)
    } catch (_: Exception) {
        null
    }
}

@Serializable
data class NotificationItem(
    val id: String,
    val type: String,
    val attachInfo: String? = null,
    val isRead: Boolean = false,
    val createTime: Long = 0,
    val mergeCount: Int = 1,
    val content: NotificationContent,
    val target: NotificationTarget? = null,
)

@Serializable
data class MobileNotificationMessageOverview(
    val head: List<MobileNotificationHeadEntry> = emptyList(),
)

@Serializable
data class MobileNotificationHeadEntry(
    val detailTitle: String = "",
    val unreadCount: Int = 0,
)

@Serializable
data class MobileNotificationTimelineItem(
    val id: String = "",
    val uniqueId: String = "",
    val type: String = "",
    val cardType: String = "",
    val detailTitle: String = "",
    val isRead: Boolean = true,
    val unreadCount: Int = 0,
    val created: Long = 0,
    val createdStr: String = "",
    val head: MobileNotificationHead? = null,
    val content: MobileNotificationContent? = null,
    val targetSource: MobileNotificationTargetSource? = null,
    val target: MobileNotificationTarget? = null,
) {
    val stableId: String
        get() = uniqueId.ifBlank { id.ifBlank { "$created-$cardType-$detailTitle" } }
}

@Serializable
data class MobileNotificationHead(
    val author: MobileNotificationAuthor? = null,
    val avatarUrl: String = "",
    val targetLink: String = "",
    val avatarUrls: List<String> = emptyList(),
)

@Serializable
data class MobileNotificationAuthor(
    val id: String? = null,
    val name: String = "",
    val urlToken: String = "",
    val headline: String = "",
    val avatarUrl: String = "",
)

@Serializable
data class MobileNotificationContent(
    val title: String = "",
    val subTitle: String = "",
    val text: String = "",
    val subText: String = "",
    val abstractText: String = "",
    val targetLink: String = "",
    val subTargetLink: String = "",
    val subIcon: String = "",
    val isDeleted: Boolean = false,
    val isSubDeleted: Boolean = false,
    val hasVideo: Boolean = false,
)

@Serializable
data class MobileNotificationTargetSource(
    val topText: String = "",
    val subText: String = "",
    val text: String = "",
    val fullText: String = "",
    val targetLink: String = "",
    val isDeleted: Boolean = false,
    val hasVideo: Boolean = false,
    val image: String = "",
    val objectId: String = "",
    val subTextIsLink: Boolean = false,
)

@Serializable
data class MobileNotificationTarget(
    val id: String = "",
    val type: String = "",
    val name: String = "",
    val urlToken: String = "",
    val headline: String = "",
    val avatarUrl: String = "",
    val url: String = "",
)

object NotificationActorsSerializer : KSerializer<List<NotificationActor>> {
    override val descriptor: SerialDescriptor
        get() = ListSerializer(NotificationActor.serializer()).descriptor

    override fun serialize(
        encoder: Encoder,
        value: List<NotificationActor>,
    ) = Unit

    override fun deserialize(decoder: Decoder): List<NotificationActor> = try {
        ListSerializer(NotificationActor.serializer()).deserialize(decoder)
    } catch (_: Exception) {
        listOf(NotificationActor.serializer().deserialize(decoder))
    }
}

@Serializable
data class NotificationContent(
    val verb: String,
    @Serializable(with = NotificationActorsSerializer::class)
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
sealed interface NotificationTarget {
    val title: String?
    val content: String?

    @Serializable
    @SerialName("comment")
    class Comment(
        val url: String,
        override val content: String, // html
        val id: String,
        val createTime: Long = 0,
        val target: Feed.Target?,
    ) : NotificationTarget {
        override val title: String?
            get() = null
    }

    @Serializable
    @SerialName("question")
    class Question(
        val url: String,
        override val title: String, // html
        val id: String,
        val createTime: Long = 0,
    ) : NotificationTarget {
        override val content: String?
            get() = null
    }

    @Serializable
    @SerialName("people")
    data class People(
        val url: String,
        val id: String,
        val name: String,
        val headline: String,
        val avatarUrl: String? = null,
        val urlToken: String,
    ) : NotificationTarget {
        override val title: String
            get() = name
        override val content: String
            get() = headline
    }

    @Serializable
    @SerialName("answer")
    data class Answer(
        val url: String,
        val id: String,
        val excerpt: String,
        val question: Question,
    ) : NotificationTarget {
        override val content: String
            get() = excerpt

        override val title: String
            get() = question.title

        @Serializable
        class Question(
            val id: String,
            val title: String,
            val url: String,
        )
    }

    @Serializable
    @SerialName("article")
    data class Article(
        val url: String,
        val id: String,
        val excerpt: String,
        val question: Question? = null,
        override val title: String,
    ) : NotificationTarget {
        override val content: String
            get() = excerpt
    }

    @Serializable
    @SerialName("pin")
    data class Pin(
        val url: String,
        val id: String,
        override val title: String,
    ) : NotificationTarget {
        override val content: String
            get() = title
    }
}

@Serializable
data class ZhihuMeNotifications(
    val defaultNotificationsCount: Int = 0,
    val followNotificationsCount: Int = 0,
    val voteThankNotificationsCount: Int = 0,
) {
    val totalCount: Int get() = defaultNotificationsCount + followNotificationsCount + voteThankNotificationsCount
}
