package com.github.zly2006.zhihu.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DailyStoriesResponse(
    val date: String,
    val stories: List<DailyStory>,
)

@Serializable
data class DailyStory(
    val id: Long,
    val title: String,
    val url: String,
    val hint: String,
    val images: List<String>,
    val type: Int,
)
