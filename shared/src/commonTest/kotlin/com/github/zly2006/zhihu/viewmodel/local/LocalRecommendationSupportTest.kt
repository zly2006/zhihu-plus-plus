package com.github.zly2006.zhihu.viewmodel.local

import kotlin.test.Test
import kotlin.test.assertEquals

class LocalRecommendationSupportTest {
    @Test
    fun getFreshnessWeightUsesSameAgeBuckets() {
        val now = 10_000L * 60L * 60L * 1000L

        assertEquals(1.08, getFreshnessWeight(now - 11L * 60L * 60L * 1000L, now))
        assertEquals(1.0, getFreshnessWeight(now - 24L * 60L * 60L * 1000L, now))
        assertEquals(0.92, getFreshnessWeight(now - 72L * 60L * 60L * 1000L, now))
        assertEquals(0.82, getFreshnessWeight(now - 200L * 60L * 60L * 1000L, now))
    }

    @Test
    fun getDefaultWeightKeepsReasonWeights() {
        assertEquals(1.2, getDefaultWeight(CrawlingReason.Following))
        assertEquals(1.0, getDefaultWeight(CrawlingReason.Trending))
        assertEquals(0.95, getDefaultWeight(CrawlingReason.UpvotedQuestion))
        assertEquals(0.88, getDefaultWeight(CrawlingReason.FollowingUpvote))
        assertEquals(0.8, getDefaultWeight(CrawlingReason.CollaborativeFiltering))
    }

    @Test
    fun createTaskForReasonKeepsUrlAndPriority() {
        val task = createTaskForReason(CrawlingReason.Following)

        assertEquals("https://api.zhihu.com/moments_v3?feed_type=recommend", task.url)
        assertEquals(CrawlingReason.Following, task.reason)
        assertEquals(8, task.priority)
    }
}
