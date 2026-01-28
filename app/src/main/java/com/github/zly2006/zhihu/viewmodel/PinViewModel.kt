package com.github.zly2006.zhihu.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.Pin
import com.github.zly2006.zhihu.data.DataHolder
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch

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

    fun loadPinDetail(context: Context) {
        if (isLoading) return
        isLoading = true
        errorMessage = null

        viewModelScope.launch {
            try {
                val content = DataHolder.getContentDetail(context, pin)
                if (content != null) {
                    pinContent = content
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

    fun refresh(context: Context) {
        pinContent = null
        loadPinDetail(context)
    }
}
