package com.github.zly2006.zhihu.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TopicContractTest {
    @Test
    fun buildsHistoricalTopicFeedUrls() {
        assertEquals(
            "https://www.zhihu.com/api/v4/topics/19550517/feeds/essence?limit=20&offset=0",
            topicFeedUrl("19550517", TopicFeedTab.Essence),
        )
    }

    @Test
    fun topicSearchRequestsAllTopicsAndEncodesQuery() {
        val url = topicSearchUrl("机器 学习")
        assertTrue("show_all_topics=1" in url)
        assertTrue("q=%E6%9C%BA%E5%99%A8+%E5%AD%A6%E4%B9%A0" in url)
    }
}
