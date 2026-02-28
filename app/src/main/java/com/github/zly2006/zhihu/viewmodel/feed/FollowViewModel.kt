package com.github.zly2006.zhihu.viewmodel.feed

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.util.signFetchRequest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonArray

class FollowViewModel : BaseFeedViewModel() {
    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v3/moments?limit=10&desktop=true"
}

class FollowRecommendViewModel : BaseFeedViewModel() {
    override val initialUrl: String
        get() = "https://api.zhihu.com/moments_v3?feed_type=recommend"
}

class RecentMomentsViewModel : ViewModel() {
    @Serializable
    data class Actor(
        val id: String,
        val urlToken: String,
        val name: String,
        val avatarUrl: String,
    )

    @Serializable
    data class FollowingUserItem(
        val actor: Actor,
        val unreadCount: Int,
    )

    var users = mutableStateListOf<FollowingUserItem>()
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun load(context: Context) {
        if (isLoading || users.isNotEmpty()) return
        isLoading = true
        viewModelScope.launch {
            try {
                if (!AccountData.data.login) {
                    errorMessage = "请登录后查看关注动态"
                    return@launch
                }
                val json = AccountData.fetchGet(context, "https://api.zhihu.com/moments/recent?type=raw") {
                    signFetchRequest(context)
                } ?: return@launch
                val dataArray = json["data"]?.jsonArray ?: return@launch
                users.addAll(
                    dataArray.mapNotNull { item ->
                        try {
                            AccountData.decodeJson<FollowingUserItem>(item)
                        } catch (e: Exception) {
                            Log.w("RecentMomentsVM", "Failed to parse item", e)
                            null
                        }
                    },
                )
            } catch (e: Exception) {
                Log.e("RecentMomentsVM", "Failed to load recent moments", e)
                errorMessage = "加载关注动态失败"
            } finally {
                isLoading = false
            }
        }
    }
}
