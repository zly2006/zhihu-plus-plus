package com.github.zly2006.zhihu.viewmodel.local

import com.github.zly2006.zhihu.shared.recommendation.LocalContentAffinity
import com.github.zly2006.zhihu.shared.recommendation.LocalReasonPreference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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

    @Test
    fun rankCandidateNormalizesContentIdAndAppliesWeights() {
        val ranked = rankCandidate(
            candidate = CrawlingResult(
                id = 1L,
                taskId = 1L,
                contentId = "42",
                title = "标题",
                summary = "摘要",
                url = "https://www.zhihu.com/answer/42",
                reason = CrawlingReason.Trending,
                score = 10.0,
                createdAt = 0L,
            ),
            behaviorProfile = UserBehaviorAnalyzer.RecommendationBehaviorProfile(
                reasonPreferences = mapOf(
                    CrawlingReason.Trending to LocalReasonPreference(
                        multiplier = 1.2,
                        explanation = "你最近更偏好这类来源",
                    ),
                ),
                contentAffinities = mapOf(
                    "answer:42" to LocalContentAffinity(
                        multiplier = 1.5,
                        explanation = "你明确喜欢过类似内容",
                    ),
                ),
            ),
        )

        assertNotNull(ranked)
        assertEquals("answer:42", ranked.result.contentId)
        assertTrue(ranked.finalScore > 0.0)
        assertEquals("热门推荐 · 你明确喜欢过类似内容 · 你最近更偏好这类来源", ranked.reasonDisplay)
    }
}
