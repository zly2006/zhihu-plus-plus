package com.github.zly2006.zhihu.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.NotificationItem
import com.github.zly2006.zhihu.util.signFetchRequest
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.reflect.typeOf

class NotificationViewModel :
    PaginationViewModel<NotificationItem>(
        dataType = typeOf<NotificationItem>(),
    ) {
    override val initialUrl = "https://www.zhihu.com/api/v4/notifications/v2/recent?limit=20"

    // 未读消息数量
    var unreadCount: Int by mutableIntStateOf(0)
        private set

    @Suppress("HttpUrlsUsage")
    override suspend fun fetchFeeds(context: Context) {
        super.fetchFeeds(context)
        if (lastPaging?.next?.startsWith("http://") == true) {
            lastPaging = lastPaging!!.copy(next = lastPaging!!.next.replace("http://", "https://"))
        }
    }

    /**
     * 标记消息为已读
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun markAsRead(context: Context, notificationId: String) {
        GlobalScope.launch {
            try {
                // TODO: 实现标记已读的API调用
            } catch (e: Exception) {
                errorHandle(e)
            }
        }
    }

    /**
     * 标记所有消息为已读
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun markAllAsRead(context: Context) {
        AccountData.fetchPost(context, "https://www.zhihu.com/api/v4/notifications/v2/default/actions/readall") {
            signFetchRequest(context)
        }
    }
}
