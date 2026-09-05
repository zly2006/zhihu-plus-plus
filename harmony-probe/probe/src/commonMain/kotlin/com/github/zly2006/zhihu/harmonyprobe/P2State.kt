package com.github.zly2006.zhihu.harmonyprobe

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import com.github.zly2006.zhihu.shared.data.DailyStoriesResponse
import com.github.zly2006.zhihu.shared.data.DailyStoryDetail
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object P2State {
    private val json = Json { ignoreUnknownKeys = true }

    var home by mutableStateOf<DailyStoriesResponse?>(null)
    var detail by mutableStateOf<DailyStoryDetail?>(null)
    val images = mutableStateMapOf<String, String>()
    var showArticle by mutableStateOf(false)
    var showStress by mutableStateOf(false)
    var sessionStatus by mutableStateOf("正在初始化访客会话…")
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun applyHomeJson(value: String) {
        runCatching { json.decodeFromString<DailyStoriesResponse>(value) }
            .onSuccess { home = it; error = null }
            .onFailure { error = "首页数据解析失败" }
    }

    fun applyDetailJson(value: String) {
        runCatching { json.decodeFromString<DailyStoryDetail>(value) }
            .onSuccess { detail = it; error = null }
            .onFailure { error = "文章数据解析失败" }
    }

    fun applyImageBase64(value: String) {
        runCatching {
            val payload = json.parseToJsonElement(value).jsonObject
            val url = payload.getValue("url").jsonPrimitive.content
            val data = payload.getValue("data").jsonPrimitive.content
            if (images.size < 24 || images.containsKey(url)) images[url] = data
        }.onFailure { error = "图片桥接数据解析失败" }
    }

    fun applySessionStatus(value: String) {
        sessionStatus = value
    }

    fun applyError(value: String) {
        error = value
    }
}
