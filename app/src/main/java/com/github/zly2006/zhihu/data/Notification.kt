package com.github.zly2006.zhihu.data

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

    override fun serialize(encoder: Encoder, value: T?) {
        // do nothing
    }

    override fun deserialize(decoder: Decoder): T? = try {
        serializer.deserialize(decoder)
    } catch (_: Exception) {
        null
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

object NotificationActorsSerializer : KSerializer<List<NotificationActor>> {
    override val descriptor: SerialDescriptor
        get() = ListSerializer(NotificationActor.serializer()).descriptor

    override fun serialize(
        encoder: Encoder,
        value: List<NotificationActor>,
    ) {
    }

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
        val question: Question,
        override val title: String,
    ) : NotificationTarget {
        override val content: String
            get() = excerpt
    }
}
