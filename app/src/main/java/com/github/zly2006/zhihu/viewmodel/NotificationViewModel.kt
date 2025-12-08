package com.github.zly2006.zhihu.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.github.zly2006.zhihu.data.NotificationItem
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlin.reflect.typeOf

class NotificationViewModel :
    PaginationViewModel<NotificationItem>(
        dataType = typeOf<NotificationItem>(),
    ) {
    override val initialUrl = "https://www.zhihu.com/api/v4/notifications/v2/recent?limit=20"

    // 未读消息数量
    var unreadCount: Int by mutableIntStateOf(0)
        private set

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
    fun markAllAsRead(context: Context) {
        GlobalScope.launch {
            try {
                // TODO: 实现标记所有已读的API调用
            } catch (e: Exception) {
                errorHandle(e)
            }
        }
    }
}
