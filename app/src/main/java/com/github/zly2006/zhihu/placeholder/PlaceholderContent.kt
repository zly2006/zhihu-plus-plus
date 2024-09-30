package com.github.zly2006.zhihu.placeholder

import com.github.zly2006.zhihu.data.Feed
import java.util.ArrayList
import java.util.HashMap

data class PlaceholderItem(val title: String, val summary: String, val details: String, val dto: Feed? = null) {
    override fun toString(): String = summary
}
