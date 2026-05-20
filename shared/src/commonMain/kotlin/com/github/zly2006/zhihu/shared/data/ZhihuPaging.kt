package com.github.zly2006.zhihu.shared.data

import kotlinx.serialization.Serializable

@Serializable
data class ZhihuPaging(
    val page: Int = -1,
    val isEnd: Boolean = false,
    val isStart: Boolean = false,
    val previous: String? = null,
    val totals: Int = 0,
    val next: String,
    val prev: String? = null,
)
