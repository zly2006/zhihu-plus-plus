package com.github.zly2006.zhihu.shared.recommendation

import com.github.zly2006.zhihu.shared.data.Feed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LocalRecommendationSupportTest {
    @Test
    fun parseLocalContentIdentitySupportsTypedIdsAndUrlFallback() {
        assertEquals(
            LocalContentIdentity("answer", "42"),
            parseLocalContentIdentity(
                contentId = "answer:42",
                url = "",
            ),
        )

        assertEquals(
            LocalContentIdentity("question", "29993655"),
            parseLocalContentIdentity(
                contentId = "29993655",
                url = "https://www.zhihu.com/question/29993655",
            ),
        )
    }

    @Test
    fun parseLocalContentIdentityTrimsTypedIds() {
        assertEquals(
            LocalContentIdentity("article", "77"),
            parseLocalContentIdentity(
                contentId = " article : 77 ",
                url = "",
            ),
        )
    }

    @Test
    fun normalizeLocalContentIdUsesTypedValue() {
        assertEquals("answer:42", normalizeLocalContentId("answer", "42"))
    }

    @Test
    fun buildReasonPreferenceRewardsPositiveSignals() {
        val preference = buildReasonPreference(
            LocalReasonStats(
                clicks = 2,
                likes = 1,
                dislikes = 0,
            ),
        )

        assertTrue(preference.multiplier > 1.0)
        assertNotNull(preference.explanation)
    }

    @Test
    fun buildContentAffinityPenalizesDislikesAndRewardsLikes() {
        val liked = buildContentAffinity(LocalContentStats(clicks = 1, likes = 1, dislikes = 0))
        val disliked = buildContentAffinity(LocalContentStats(clicks = 0, likes = 0, dislikes = 2))

        assertTrue(liked.multiplier > 1.0)
        assertTrue(disliked.multiplier < 1.0)
    }

    @Test
    fun scoreFeedTargetUsesEngagementAndContentLength() {
        val target = Feed.AnswerTarget(
            id = 42,
            url = "https://www.zhihu.com/question/1/answer/42",
            author = null,
            question = Feed.QuestionTarget(
                id = 1,
                _title = "问题标题",
                url = "https://www.zhihu.com/question/1",
                type = "question",
            ),
            voteupCount = 250,
            commentCount = 40,
            excerpt = "a".repeat(120),
        )

        assertEquals(5.3, scoreFeedTarget(target))
    }

    @Test
    fun applyReasonDiversityPreventsSingleReasonFromDominating() {
        val ranked = listOf(
            TestRankedResult("answer:1", reason = "trending", score = 9.0),
            TestRankedResult("answer:2", reason = "trending", score = 8.8),
            TestRankedResult("answer:3", reason = "trending", score = 8.6),
            TestRankedResult("answer:4", reason = "following", score = 8.5),
            TestRankedResult("answer:5", reason = "upvoted_question", score = 8.4),
        )

        val diverse = applyReasonDiversity(rankedResults = ranked, limit = 4) { it.reason }

        assertEquals(4, diverse.size)
        assertTrue(diverse.count { it.reason == "trending" } <= 2)
        assertTrue(diverse.any { it.reason == "following" })
    }

    @Test
    fun buildLocalRecommendationReasonCombinesNonBlankReasons() {
        assertEquals(
            "基础理由 · 你明确喜欢过类似内容 · 你经常点开这类来源",
            buildLocalRecommendationReason(
                baseReason = "基础理由",
                reasonPreference = LocalReasonPreference(
                    multiplier = 1.2,
                    explanation = "你经常点开这类来源",
                ),
                contentAffinity = LocalContentAffinity(
                    multiplier = 1.3,
                    explanation = "你明确喜欢过类似内容",
                ),
            ),
        )
    }

    @Test
    fun stableLocalFeedIdReplacesTypedSeparator() {
        assertEquals("local_feed_answer_42", stableLocalFeedId("answer:42"))
    }

    private data class TestRankedResult(
        val contentId: String,
        val reason: String,
        val score: Double,
    )
}
