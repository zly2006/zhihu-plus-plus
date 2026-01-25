package com.github.zly2006.zhihu.data

import kotlinx.serialization.Serializable

@Serializable
data class OnlineHistoryResponse(
    val paging: Paging,
    val data: List<OnlineHistoryItem>,
)

@Serializable
data class Paging(
    val isEnd: Boolean,
    val isStart: Boolean,
    val next: String?,
    val previous: String?,
    val totals: Int,
)

@Serializable
data class OnlineHistoryItem(
    val cardType: String,
    val data: OnlineHistoryData,
)

@Serializable
data class OnlineHistoryData(
    val header: OnlineHistoryHeader,
    val content: OnlineHistoryContent? = null,
    val action: OnlineHistoryAction,
    val extra: OnlineHistoryExtra,
    val matrix: List<OnlineHistoryMatrixItem>? = null,
)

@Serializable
data class OnlineHistoryMatrixItem(
    val type: String,
    val data: OnlineHistoryMatrixData,
)

@Serializable
data class OnlineHistoryMatrixData(
    val text: String,
)

@Serializable
data class OnlineHistoryHeader(
    val icon: String,
    val title: String,
    val action: OnlineHistoryAction,
)

@Serializable
data class OnlineHistoryContent(
    val authorName: String? = null,
    val summary: String? = null,
    val coverImage: String? = null,
)

@Serializable
data class OnlineHistoryAction(
    val type: String,
    val url: String,
)

@Serializable
data class OnlineHistoryExtra(
    val contentToken: String,
    val contentType: String,
    val readTime: Long,
    val questionToken: String,
)
