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

package com.github.zly2006.zhihu.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.github.zly2006.zhihu.data.MOBILE_NOTIFICATION_MESSAGE_URL
import com.github.zly2006.zhihu.data.MobileNotificationAuthor
import com.github.zly2006.zhihu.data.MobileNotificationColumnHead
import com.github.zly2006.zhihu.data.MobileNotificationMessageOverview
import com.github.zly2006.zhihu.data.MobileNotificationTimelineItem
import com.github.zly2006.zhihu.data.ZhihuJson
import com.github.zly2006.zhihu.data.ZhihuPaging
import com.github.zly2006.zhihu.data.ZhihuPrivateMessage
import com.github.zly2006.zhihu.data.ZhihuPrivateMessagePage
import com.github.zly2006.zhihu.notification.NotificationSettingsStore
import com.github.zly2006.zhihu.notification.matchNotificationType
import com.github.zly2006.zhihu.util.Log
import com.github.zly2006.zhihu.util.ZhihuMessageBodyEncryptor
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.formUrlEncode
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.typeOf

interface NotificationSettingsEnvironment {
    val notificationSettingsStore: NotificationSettingsStore
}

interface NotificationEnvironment :
    PaginationEnvironment,
    NotificationSettingsEnvironment

enum class MobileNotificationCategory(
    val entryName: String,
    val detailTitle: String,
) {
    Comment("comment", "评论转发@"),
    Like("like", "赞同喜欢"),
    Favorite("favlist_me", "收藏了我"),
    Follow("follow", "关注订阅"),
    ;

    val readAllUrl: String
        get() = "$MOBILE_NOTIFICATION_TIMELINE_URL/$entryName/actions/readall"
}

class NotificationViewModel :
    PaginationViewModel<MobileNotificationTimelineItem>(
        dataType = typeOf<MobileNotificationTimelineItem>(),
    ) {
    override val initialUrl = "$MOBILE_NOTIFICATION_MESSAGE_URL?limit=20"
    override val include = ""

    val categoryUnreadCounts = SnapshotStateMap<MobileNotificationCategory, Int>()

    var invitation: MobileNotificationColumnHead? by mutableStateOf(null)
        private set

    var unreadCount: Int by mutableIntStateOf(0)
        private set

    private var refreshingFirstPage = false

    init {
        MobileNotificationCategory.entries.forEach {
            categoryUnreadCounts[it] = 0
        }
    }

    override fun refresh(environment: PaginationEnvironment) {
        if (isLoading) return
        errorMessage = null
        lastPaging = null
        refreshingFirstPage = true
        loadMore(environment)
    }

    override suspend fun fetchFeeds(environment: PaginationEnvironment) {
        try {
            val url = lastPaging?.next ?: initialUrl
            val json = environment
                .mobileHomeFeedHttpClient()
                .get(url.replace("http://", "https://"))
                .body<JsonObject>()
            val page = ZhihuJson.decodeJson<MobileNotificationMessageOverview>(json)
            val rawData = json["data"]?.jsonArray ?: JsonArray(emptyList())

            if (lastPaging == null) {
                invitation = page.columnHead.firstOrNull()
                MobileNotificationCategory.entries.forEach { category ->
                    categoryUnreadCounts[category] = page.head
                        .firstOrNull { it.detailTitle == category.detailTitle }
                        ?.unreadCount ?: 0
                }
                unreadCount = categoryUnreadCounts.values.sum()
            }

            val pageData = page.data.filter { it.type != "empty" }
            if (refreshingFirstPage) {
                pageData.forEachIndexed { index, item ->
                    if (index < allData.size) {
                        allData[index] = item
                    } else {
                        allData.add(item)
                    }
                }
                while (allData.size > pageData.size) {
                    allData.removeAt(allData.lastIndex)
                }
                debugData.clear()
                debugData.addAll(rawData)
            } else {
                val existingIds = allData.mapTo(mutableSetOf()) { it.stableId }
                processResponse(
                    environment,
                    pageData.filter { existingIds.add(it.stableId) },
                    rawData,
                )
            }
            lastPaging = page.paging ?: ZhihuPaging(isEnd = true, next = "")
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            environment.handleFetchFailure(this::class.simpleName, e)
        } finally {
            refreshingFirstPage = false
            isLoading = false
        }
    }

    suspend fun markCategoryAsRead(
        category: MobileNotificationCategory,
        environment: MobileHomeFeedEnvironment,
    ): Boolean {
        if ((categoryUnreadCounts[category] ?: 0) <= 0) return true

        return try {
            val response = environment.mobileHomeFeedHttpClient().post(category.readAllUrl)
            if (!response.status.isSuccess()) {
                Log.e("NotificationViewModel", "Failed to mark ${category.entryName} notifications as read: ${response.status}")
                false
            } else {
                categoryUnreadCounts[category] = 0
                unreadCount = categoryUnreadCounts.values.sum()
                true
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("NotificationViewModel", "Failed to mark ${category.entryName} notifications as read", e)
            false
        }
    }

    suspend fun markAllAsRead(environment: MobileHomeFeedEnvironment): Boolean {
        var succeeded = true
        MobileNotificationCategory.entries.forEach { category ->
            if (!markCategoryAsRead(category, environment)) {
                succeeded = false
            }
        }
        return succeeded
    }
}

class NotificationTimelineViewModel(
    val entryName: String,
) : PaginationViewModel<MobileNotificationTimelineItem>(
        dataType = typeOf<MobileNotificationTimelineItem>(),
    ) {
    override val initialUrl = buildString {
        append("$MOBILE_NOTIFICATION_TIMELINE_URL/$entryName?")
        if (entryName == INVITATION_ENTRY_NAME) {
            append("invite_with_time_slice=1&")
        }
        append("limit=20")
    }
    override val include = ""
    private var markedAsRead = false

    val readAllUrl: String
        get() = "$MOBILE_NOTIFICATION_TIMELINE_URL/$entryName/actions/readall"

    override suspend fun fetchFeeds(environment: PaginationEnvironment) {
        try {
            val notificationEnvironment = environment as? NotificationEnvironment
                ?: error("NotificationSettingsStore is required for notification pagination")
            val url = lastPaging?.next ?: initialUrl
            val json = environment
                .mobileHomeFeedHttpClient()
                .get(url.replace("http://", "https://"))
                .body<JsonObject>()
            val rawData = json["data"]?.jsonArray ?: JsonArray(emptyList())
            val data = rawData.mapNotNull {
                try {
                    ZhihuJson.decodeJson<MobileNotificationTimelineItem>(it)
                } catch (e: Exception) {
                    if (shouldLogDecodeFailures) {
                        environment.logDecodeFailure(this::class.simpleName, it, e)
                    }
                    null
                }
            }

            val existingIds = allData.mapTo(mutableSetOf()) { it.stableId }
            processResponse(
                environment,
                data.filter { existingIds.add(it.stableId) },
                rawData,
            )
            lastPaging = json["paging"]
                ?.let { ZhihuJson.decodeJson<ZhihuPaging>(it) }
                ?: ZhihuPaging(isEnd = true, next = "")

            if (!markedAsRead && notificationEnvironment.notificationSettingsStore.getAutoMarkAsReadEnabled()) {
                markedAsRead = markAsRead(environment)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            environment.handleFetchFailure(this::class.simpleName, e)
        } finally {
            isLoading = false
        }
    }

    fun shouldShowNotification(
        settingsStore: NotificationSettingsStore,
        notification: MobileNotificationTimelineItem,
    ): Boolean {
        val content = notification.content ?: return true
        val verb = listOf(content.title, content.subTitle, content.text)
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
        val type = matchNotificationType(verb)
        return type == null || settingsStore.getDisplayInAppEnabled(type)
    }

    suspend fun markAsRead(environment: MobileHomeFeedEnvironment): Boolean = try {
        val response = environment.mobileHomeFeedHttpClient().post(readAllUrl)
        if (!response.status.isSuccess()) {
            Log.e("NotificationTimelineViewModel", "Failed to mark $entryName notifications as read: ${response.status}")
            false
        } else {
            true
        }
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.e("NotificationTimelineViewModel", "Failed to mark $entryName notifications as read", e)
        false
    }
}

class PrivateMessageViewModel(
    private val peerId: String,
) : PaginationViewModel<ZhihuPrivateMessage>(
        dataType = typeOf<ZhihuPrivateMessage>(),
    ) {
    override val initialUrl = "$MOBILE_PRIVATE_MESSAGE_URL?limit=20&sender_id=$peerId"
    override val include = ""

    var peer: MobileNotificationAuthor? by mutableStateOf(null)
        private set

    var isSending by mutableStateOf(false)
        private set

    suspend fun sendMessage(
        content: String,
        environment: MobileHomeFeedEnvironment,
    ): Boolean {
        if (content.isBlank() || isSending) return false

        isSending = true
        errorMessage = null
        return try {
            val response = environment.mobileHomeFeedHttpClient().post(MOBILE_PRIVATE_MESSAGE_URL) {
                contentType(ContentType.Application.FormUrlEncoded)
                header("X-Zse-93", "101_1_1.0")
                val form = Parameters
                    .build {
                        append("receiver_id", peerId)
                        append("content", content)
                        append("content_type", "0")
                        append("source_type", "message_list")
                    }.formUrlEncode()
                setBody(ZhihuMessageBodyEncryptor.encrypt(form))
            }
            if (!response.status.isSuccess()) {
                val responseText = response.bodyAsText()
                errorMessage = runCatching {
                    ZhihuJson.json
                        .parseToJsonElement(responseText)
                        .jsonObject["error"]
                        ?.jsonObject
                        ?.get("message")
                        ?.jsonPrimitive
                        ?.content
                }.getOrNull() ?: "发送失败（${response.status.value}）"
                Log.e("PrivateMessageViewModel", "Failed to send private message: ${response.status}, $errorMessage")
                false
            } else {
                val message = ZhihuJson.decodeJson<ZhihuPrivateMessage>(
                    ZhihuJson.json.parseToJsonElement(response.bodyAsText()),
                )
                if (allData.none { it.stableId == message.stableId }) {
                    allData.add(0, message)
                }
                true
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("PrivateMessageViewModel", "Failed to send private message", e)
            errorMessage = e.message ?: "发送失败"
            false
        } finally {
            isSending = false
        }
    }

    override suspend fun fetchFeeds(environment: PaginationEnvironment) {
        try {
            coroutineScope {
                val client = environment.mobileHomeFeedHttpClient()
                val peerRequest = if (peer == null) {
                    async {
                        client
                            .get("$MOBILE_PRIVATE_MESSAGE_USER_URL/$peerId")
                            .body<JsonObject>()
                    }
                } else {
                    null
                }
                val pageRequest = async {
                    client
                        .get((lastPaging?.next ?: initialUrl).replace("http://", "https://"))
                        .body<JsonObject>()
                }

                val json = pageRequest.await()
                val page = ZhihuJson.decodeJson<ZhihuPrivateMessagePage>(json)
                val rawData = json["data"]?.jsonArray ?: JsonArray(emptyList())
                val existingIds = allData.mapTo(mutableSetOf()) { it.stableId }
                processResponse(
                    environment,
                    page.data.filter { existingIds.add(it.stableId) },
                    rawData,
                )
                lastPaging = page.paging

                peerRequest?.let { request ->
                    runCatching {
                        peer = ZhihuJson.decodeJson<MobileNotificationAuthor>(request.await())
                    }.onFailure { error ->
                        if (error is CancellationException) throw error
                        Log.e("PrivateMessageViewModel", "Failed to load private-message peer", error)
                    }
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            environment.handleFetchFailure(this::class.simpleName, e)
        } finally {
            isLoading = false
        }
    }
}

private const val INVITATION_ENTRY_NAME = "invite"
private const val MOBILE_NOTIFICATION_TIMELINE_URL = "https://api.zhihu.com/notifications/v3/timeline/entry"
private const val MOBILE_PRIVATE_MESSAGE_URL = "https://api.zhihu.com/messages"
private const val MOBILE_PRIVATE_MESSAGE_USER_URL = "https://api.zhihu.com/messages/user"
