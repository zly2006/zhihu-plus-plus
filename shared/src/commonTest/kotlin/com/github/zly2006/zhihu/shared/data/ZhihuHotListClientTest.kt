package com.github.zly2006.zhihu.shared.data

import kotlin.test.Test
import kotlin.test.assertEquals

class ZhihuHotListClientTest {
    @Test
    fun buildsDefaultHotListUrl() {
        assertEquals(
            "https://www.zhihu.com/api/v3/feed/topstory/hot-lists/total?limit=50&mobile=true",
            zhihuHotListUrl(),
        )
    }

    @Test
    fun buildsCustomHotListUrl() {
        assertEquals(
            "https://www.zhihu.com/api/v3/feed/topstory/hot-lists/total?limit=20&mobile=false",
            zhihuHotListUrl(limit = 20, mobile = false),
        )
    }
}
