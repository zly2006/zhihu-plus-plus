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
import com.github.zly2006.zhihu.shared.data.MobileNotificationMessageOverview
import com.github.zly2006.zhihu.shared.data.MobileNotificationTimelineItem
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.ZhihuPaging
import com.github.zly2006.zhihu.shared.notification.NotificationSettingsStore
import com.github.zly2006.zhihu.shared.notification.matchNotificationType
import com.github.zly2006.zhihu.shared.util.Log
import io.ktor.client.call.body
import io.ktor.client.request.get
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

    val initialUrl: String
        get() = "$MOBILE_NOTIFICATION_ENTRY_BASE_URL/$entryName?limit=20"
}

class NotificationViewModel :
    PaginationViewModel<MobileNotificationTimelineItem>(
        dataType = typeOf<MobileNotificationTimelineItem>(),
    ) {
    override val initialUrl = MobileNotificationCategory.Comment.initialUrl
    override val include = ""
    private val categoryPaging = mutableMapOf<MobileNotificationCategory, ZhihuPaging?>()
    private val categoryData = MobileNotificationCategory.entries.associateWith { mutableListOf<MobileNotificationTimelineItem>() }.toMutableMap()
    private val endedCategories = mutableSetOf<MobileNotificationCategory>()
    private var loadingCategory = MobileNotificationCategory.Comment
    override val isEnd: Boolean
        get() = selectedCategory in endedCategories

    var selectedCategory: MobileNotificationCategory by mutableStateOf(MobileNotificationCategory.Comment)
        private set

    var categoryUnreadCounts: Map<MobileNotificationCategory, Int> by mutableStateOf(
        MobileNotificationCategory.entries.associateWith { 0 },
    )
        private set

    // 未读消息数量
    var unreadCount: Int by mutableIntStateOf(0)
        private set

    fun selectCategory(
        category: MobileNotificationCategory,
        environment: PaginationEnvironment,
    ) {
        if (selectedCategory == category) return
        selectedCategory = category
        allData.clear()
        allData.addAll(categoryData.getValue(category))
        if (allData.isEmpty() && category !in endedCategories) {
            loadMore(environment)
        }
    }

    @Suppress("HttpUrlsUsage")
    override suspend fun fetchFeeds(environment: PaginationEnvironment) {
        val category = selectedCategory
        try {
            val notificationSettingsEnvironment = environment as? NotificationSettingsEnvironment
                ?: error("NotificationSettingsStore is required for notification pagination")
            updateUnreadCounts(environment)

            loadingCategory = category
            val url = categoryPaging[category]?.next ?: category.initialUrl
            val json = environment
                .mobileHomeFeedHttpClient()
                .get(url.replace("http://", "https://"))
                .body<JsonObject>()
            val jsonArray = json["data"]?.jsonArray ?: JsonArray(emptyList())
            val decodedData = jsonArray.mapNotNull {
                if (it.jsonObject["type"]?.jsonPrimitive?.content == "empty") {
                    return@mapNotNull null
                }
                try {
                    ZhihuJson.decodeJson<MobileNotificationTimelineItem>(it)
                } catch (e: Exception) {
                    if (shouldLogDecodeFailures) {
                        environment.logDecodeFailure(this::class.simpleName, it, e)
                    }
                    null
                }
            }

            processResponse(
                environment,
                decodedData,
                jsonArray,
            )

            val paging = json["paging"]?.let { ZhihuJson.decodeJson<ZhihuPaging>(it) }
            categoryPaging[category] = paging
            if (paging == null || paging.isEnd) {
                endedCategories += category
            }
            if (notificationSettingsEnvironment.notificationSettingsStore.getAutoMarkAsReadEnabled()) {
                markAllAsRead(environment)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            environment.handleFetchFailure(this::class.simpleName, e)
        } finally {
            isLoading = false
            if (
                selectedCategory != category &&
                categoryData.getValue(selectedCategory).isEmpty() &&
                selectedCategory !in endedCategories
            ) {
                loadMore(environment)
            }
        }
    }

    override fun refresh(environment: PaginationEnvironment) {
        categoryPaging.remove(selectedCategory)
        categoryData.getValue(selectedCategory).clear()
        endedCategories.remove(selectedCategory)
        super.refresh(environment)
    }

    override fun processResponse(environment: PaginationEnvironment, data: List<MobileNotificationTimelineItem>, rawData: JsonArray) {
        debugData.addAll(rawData)
        val cachedData = categoryData.getValue(loadingCategory)
        val existingIds = cachedData.mapTo(mutableSetOf()) { it.stableId }
        val merged = (cachedData + data.filter { existingIds.add(it.stableId) })
            .sortedByDescending { it.created }
        cachedData.clear()
        cachedData.addAll(merged)
        if (loadingCategory == selectedCategory) {
            allData.clear()
            allData.addAll(merged)
        }
    }

    /**
     * 检查是否需要显示通知
     */
    fun shouldShowNotification(settingsStore: NotificationSettingsStore, notification: MobileNotificationTimelineItem): Boolean {
        val content = notification.content ?: return true
        val verb = listOf(content.title, content.subTitle, content.text)
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
        val type = matchNotificationType(verb)
        return if (type != null) {
            settingsStore.getDisplayInAppEnabled(type)
        } else {
            true
        }
    }

    /**
     * 标记所有消息为已读
     */
    suspend fun markAllAsRead(environment: ZhihuApiEnvironment) {
        listOf(
            "https://www.zhihu.com/api/v4/notifications/v2/default/actions/readall",
            "https://www.zhihu.com/api/v4/notifications/v2/follow/actions/readall",
            "https://www.zhihu.com/api/v4/notifications/v2/vote_thank/actions/readall",
        ).forEach { url ->
            environment.postSigned(url)
        }
        unreadCount = 0
    }

    private suspend fun updateUnreadCounts(environment: PaginationEnvironment) {
        try {
            val overview = environment
                .mobileHomeFeedHttpClient()
                .get(MOBILE_NOTIFICATION_MESSAGE_URL)
                .body<JsonObject>()
                .let { ZhihuJson.decodeJson<MobileNotificationMessageOverview>(it) }
            val nextCounts = MobileNotificationCategory.entries.associateWith { category ->
                overview
                    .head
                    .firstOrNull { it.detailTitle == category.detailTitle }
                    ?.unreadCount ?: 0
            }
            categoryUnreadCounts = nextCounts
            unreadCount = nextCounts.values.sum()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("NotificationViewModel", "Failed to fetch mobile notification unread counts", e)
        }
    }
}

private const val MOBILE_NOTIFICATION_ENTRY_BASE_URL = "https://api.zhihu.com/notifications/v3/timeline/entry"
private const val MOBILE_NOTIFICATION_MESSAGE_URL = "https://api.zhihu.com/notifications/v3/message/v3?limit=20"
