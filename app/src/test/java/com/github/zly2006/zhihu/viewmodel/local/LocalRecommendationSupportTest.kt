package com.github.zly2006.zhihu.viewmodel.local

import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.ArticleType
import com.github.zly2006.zhihu.Pin
import com.github.zly2006.zhihu.Question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun toNavDestinationRestoresAnswerArticleQuestionAndPin() {
        assertEquals(
            Article(type = ArticleType.Answer, id = 42, title = "回答标题"),
            LocalContentIdentity("answer", "42").toNavDestination("回答标题"),
        )
        assertEquals(
            Article(type = ArticleType.Article, id = 77, title = "文章标题"),
            LocalContentIdentity("article", "77").toNavDestination("文章标题"),
        )
        assertEquals(
            Question(questionId = 9, title = "问题标题"),
            LocalContentIdentity("question", "9").toNavDestination("问题标题"),
        )
        assertEquals(
            Pin(id = 88),
            LocalContentIdentity("pin", "88").toNavDestination("想法"),
        )
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
    fun applyReasonDiversityPreventsSingleReasonFromDominating() {
        val ranked =
            listOf(
                rankedResult("answer:1", CrawlingReason.Trending, 9.0),
                rankedResult("answer:2", CrawlingReason.Trending, 8.8),
                rankedResult("answer:3", CrawlingReason.Trending, 8.6),
                rankedResult("answer:4", CrawlingReason.Following, 8.5),
                rankedResult("answer:5", CrawlingReason.UpvotedQuestion, 8.4),
            )

        val diverse = applyReasonDiversity(ranked, limit = 4)

        assertEquals(4, diverse.size)
        assertTrue(diverse.count { it.result.reason == CrawlingReason.Trending } <= 2)
        assertTrue(diverse.any { it.result.reason == CrawlingReason.Following })
    }

    private fun rankedResult(contentId: String, reason: CrawlingReason, score: Double): RankedLocalResult =
        RankedLocalResult(
            result =
                CrawlingResult(
                    id = score.toLong(),
                    taskId = 1,
                    contentId = contentId,
                    title = "标题 $contentId",
                    summary = "摘要",
                    url = "https://api.zhihu.com/answers/${contentId.substringAfter(':')}",
                    reason = reason,
                    score = score,
                ),
            finalScore = score,
            reasonDisplay = "测试理由",
        )
}
