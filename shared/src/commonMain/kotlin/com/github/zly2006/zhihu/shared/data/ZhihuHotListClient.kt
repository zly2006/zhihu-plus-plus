package com.github.zly2006.zhihu.shared.data

import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments

const val ZHIHU_HOT_LIST_TOTAL_URL = "https://www.zhihu.com/api/v3/feed/topstory/hot-lists/total"

fun zhihuHotListUrl(
    limit: Int = 50,
    mobile: Boolean = true,
): String = URLBuilder("https://www.zhihu.com")
    .apply {
        appendPathSegments("api", "v3", "feed", "topstory", "hot-lists", "total")
        parameters.append("limit", limit.toString())
        parameters.append("mobile", mobile.toString())
    }.buildString()
