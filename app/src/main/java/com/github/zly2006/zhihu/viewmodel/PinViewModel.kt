package com.github.zly2006.zhihu.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.Pin
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.util.signFetchRequest
import io.ktor.client.HttpClient
import io.ktor.http.HttpMethod
import kotlinx.coroutines.launch
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

class PinViewModel(
    private val pin: Pin,
    val httpClient: HttpClient?,
) : ViewModel() {
    var pinContent by mutableStateOf<DataHolder.Pin?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var isLiked by mutableStateOf(false)
        private set
    var likeCount by mutableIntStateOf(0)
        private set

    fun loadPinDetail(context: Context) {
        if (isLoading) return
        isLoading = true
        errorMessage = null

        viewModelScope.launch {
            try {
                val content = DataHolder.getContentDetail(context, pin)
                if (content != null) {
                    pinContent = content
                    likeCount = content.likeCount
                    isLiked = content.virtuals
                        ?.get("isLiked")
                        ?.jsonPrimitive
                        ?.boolean ?: false
                } else {
                    errorMessage = "无法加载想法详情"
                }
            } catch (e: Exception) {
                Log.e("PinViewModel", "Failed to load pin detail", e)
                errorMessage = e.message ?: "未知错误"
            } finally {
                isLoading = false
            }
        }
    }

    fun toggleLike(context: Context) {
        if (httpClient == null) return
        viewModelScope.launch {
            try {
                val method = if (isLiked) HttpMethod.Delete else HttpMethod.Post
                val endpoint = "https://www.zhihu.com/api/v4/pins/${pin.id}/voters/up"

                val jojo = AccountData.fetch(context, endpoint) {
                    this.method = method
                    signFetchRequest(context)
                }!!

                isLiked = !isLiked
                likeCount = jojo["liked_count"]?.jsonPrimitive?.intOrNull ?: -1
            } catch (e: Exception) {
                Log.e("PinViewModel", "Toggle like failed", e)
                Toast.makeText(context, "操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun refresh(context: Context) {
        pinContent = null
        loadPinDetail(context)
    }
}
