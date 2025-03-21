package com.github.zly2006.zhihu.legacy.placeholder

import com.github.zly2006.zhihu.data.Feed

data class PlaceholderItem(
    val title: String,
    val summary: String,
    val details: String,
    val dto: Feed? = null,
    var touched: Boolean = false
) {
    override fun toString(): String = summary
}
